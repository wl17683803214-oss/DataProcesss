param(
    # 只检查依赖状态，不启动进程、不创建日志目录、不修改系统服务。
    [switch]$CheckOnly,
    # 安装路径及本机参数集中放在相邻配置文件中。
    [string]$ConfigPath = (Join-Path $PSScriptRoot 'local-services.psd1')
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

function Write-ServiceStatus {
    param([string]$Name, [string]$Status, [string]$Detail)
    # 只打印中文状态及定位信息，不输出启动环境中的账号和密码。
    Write-Host ('[{0}] {1}：{2}' -f $Status, $Name, $Detail)
}

function Test-LocalPort {
    param([int]$Port)
    # 使用短超时建立本机连接，不让单次检查阻塞整个启动过程。
    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $pending = $client.ConnectAsync('127.0.0.1', $Port)
        return ($pending.Wait(400) -and $client.Connected)
    } catch {
        return $false
    } finally {
        $client.Dispose()
    }
}

function Test-ServiceReady {
    param([hashtable]$Specification)
    # 第一步：所有必要端口都可连接才继续检查应用健康状态。
    foreach ($port in $Specification.Ports) {
        if (-not (Test-LocalPort $port)) { return $false }
    }
    # 第二步：支持健康检查的服务必须返回成功，不能只看端口已经打开。
    if ($Specification.HealthUrl) {
        try {
            $response = Invoke-WebRequest -Uri $Specification.HealthUrl -UseBasicParsing -TimeoutSec 2
            return $response.StatusCode -eq 200
        } catch {
            return $false
        }
    }
    return $true
}

function Assert-InstalledPath {
    param([string]$Path, [string]$Description, [string]$PathType = 'Leaf')
    # 不自动安装软件或新建数据目录，避免误用空目录启动出另一套环境。
    if (-not (Test-Path -LiteralPath $Path -PathType $PathType)) {
        throw ('找不到{0}：{1}，请检查脚本配置文件。' -f $Description, $Path)
    }
}

function ConvertTo-CommandToken {
    param([string]$Value)
    # 批处理参数使用双引号保护，拒绝能够破坏当前引用或再次展开的内容。
    if ($Value.Contains('"') -or $Value.Contains('%') -or $Value.Contains('!') `
            -or $Value.Contains("`r") -or $Value.Contains("`n")) {
        throw '启动参数包含不支持的引号、展开符或换行，请检查安装路径。'
    }
    return '"' + $Value + '"'
}

function Assert-ListenerOwner {
    param([hashtable]$Specification)
    # 已有监听只能复用当前依赖对应的进程，不能把端口被其他软件占用当作成功。
    $listeners = @(Get-NetTCPConnection -State Listen -LocalPort $Specification.Ports[0] -ErrorAction SilentlyContinue)
    foreach ($listener in $listeners) {
        $owner = Get-CimInstance Win32_Process -Filter ('ProcessId={0}' -f $listener.OwningProcess)
        if ($null -eq $owner -or -not $owner.CommandLine `
                -or $owner.CommandLine -notmatch $Specification.ProcessPattern) {
            throw ('{0}的端口{1}被其他或无法确认身份的进程占用，进程编号：{2}。' `
                    -f $Specification.Name, $Specification.Ports[0], $listener.OwningProcess)
        }
    }
}

function Start-HiddenProcess {
    param([string]$Name, [string]$Executable, [string]$Arguments, [string]$WorkingDirectory)
    # 每个新进程使用独立的输出文件，服务在后台运行，关闭启动入口不会主动停止服务。
    $prefix = Join-Path $script:LogDirectory ($Name + '-' + [DateTime]::Now.ToString('yyyyMMdd-HHmmss-fff'))
    $process = Start-Process -FilePath $Executable -ArgumentList $Arguments `
            -WorkingDirectory $WorkingDirectory -WindowStyle Hidden -PassThru `
            -RedirectStandardOutput ($prefix + '.out.log') -RedirectStandardError ($prefix + '.err.log')
    # 系统自带脚本环境需要提前保留进程句柄，才能在进程结束后稳定读取退出码。
    $null = $process.Handle
    return @{ Process = $process; LogPrefix = $prefix }
}

function Ensure-WindowsService {
    param([string]$Name, [string]$ServiceName, [int]$Port)
    # 第一步：读取已有系统服务；缺失时明确报错，不创建另一份数据库或缓存实例。
    $service = Get-Service -Name $ServiceName -ErrorAction SilentlyContinue
    if ($null -eq $service) { throw ('未安装{0}系统服务：{1}。' -f $Name, $ServiceName) }
    if ($service.Status -ne 'Running') {
        Write-ServiceStatus $Name '启动中' ('正在启动系统服务：' + $ServiceName)
        try { Start-Service -Name $ServiceName } catch {
            throw ('无法启动{0}系统服务，请以管理员身份运行入口；原因：{1}' -f $Name, $_.Exception.Message)
        }
    }
    # 第二步：确认系统服务状态及应用端口，已运行的服务保持原样。
    $deadline = [DateTime]::UtcNow.AddSeconds($script:Settings.StartupTimeoutSeconds)
    do {
        $service.Refresh()
        if ($service.Status -eq 'Running' -and (Test-LocalPort $Port)) {
            Write-ServiceStatus $Name '就绪' ('本机端口：' + $Port)
            return
        }
        Start-Sleep -Milliseconds 500
    } while ([DateTime]::UtcNow -lt $deadline)
    throw ('等待{0}系统服务就绪超时，端口：{1}。' -f $Name, $Port)
}

function Ensure-BackgroundService {
    param([hashtable]$Specification)
    # 第一步：端口已打开时核对进程身份；启动中的同类进程继续等待，不重复拉起。
    $existing = $null
    if (Test-LocalPort $Specification.Ports[0]) {
        Assert-ListenerOwner $Specification
    } else {
        $existing = Get-CimInstance Win32_Process | Where-Object {
            $_.Name -eq $Specification.ProcessName -and $_.CommandLine `
                    -and $_.CommandLine -match $Specification.ProcessPattern
        } | Select-Object -First 1
    }
    $started = $null
    if ((Test-LocalPort $Specification.Ports[0]) -or $null -ne $existing) {
        Write-ServiceStatus $Specification.Name '检查中' '复用已运行或正在启动的进程。'
    } else {
        Write-ServiceStatus $Specification.Name '启动中' '正在后台启动，等待必要端口与健康检查。'
        $started = Start-HiddenProcess $Specification.Key $Specification.Executable `
                $Specification.Arguments $Specification.WorkingDirectory
        Write-ServiceStatus $Specification.Name '日志' ($started.LogPrefix + '.out.log / .err.log')
    }
    # 第二步：等待真实就绪；启动进程提前退出时立即提示对应日志。
    $deadline = [DateTime]::UtcNow.AddSeconds($script:Settings.StartupTimeoutSeconds)
    do {
        if (Test-ServiceReady $Specification) {
            Assert-ListenerOwner $Specification
            Write-ServiceStatus $Specification.Name '就绪' ('本机端口：' + ($Specification.Ports -join '、'))
            return
        }
        if ($null -ne $started) {
            $started.Process.Refresh()
            if ($started.Process.HasExited) {
                throw ('{0}启动进程已退出，退出码：{1}。请查看：{2}.out.log 和 .err.log' `
                        -f $Specification.Name, $started.Process.ExitCode, $started.LogPrefix)
            }
        }
        Start-Sleep -Milliseconds 500
    } while ([DateTime]::UtcNow -lt $deadline)
    throw ('等待{0}就绪超时，请查看启动日志及软件自身日志：{1}' -f $Specification.Name, $script:LogDirectory)
}

function Test-BrokerRegistration {
    # 第一步：通过官方管理工具查询名称服务，端口就绪不能代替代理注册成功。
    $classpath = (Join-Path $script:Settings.RocketmqHome 'conf') + ';' `
            + (Join-Path $script:Settings.RocketmqHome 'lib\*')
    $arguments = '-Xms64m -Xmx128m -cp ' + (ConvertTo-CommandToken $classpath) `
            + ' org.apache.rocketmq.tools.command.MQAdminStartup clusterList -n ' `
            + ('127.0.0.1:' + $script:Settings.NameServerPort)
    $probe = Start-HiddenProcess 'broker-route' $script:JavaExecutable $arguments $script:Settings.RocketmqHome
    # 第二步：探测命令有独立超时，只终止本次创建的单个探测进程，不操作业务进程。
    if (-not $probe.Process.WaitForExit(10000)) {
        $probe.Process.Kill()
        $probe.Process.WaitForExit()
        return $false
    }
    $probe.Process.WaitForExit()
    if ($null -ne $probe.Process.ExitCode -and $probe.Process.ExitCode -ne 0) { return $false }
    $output = Get-Content -LiteralPath ($probe.LogPrefix + '.out.log') -Raw
    # 只接受管理工具表格中的代理行，错误信息提及同一个地址不能冒充有效路由。
    $routePattern = '(?m)^\S+\s+\S+\s+\d+\s+' `
            + [regex]::Escape('127.0.0.1:' + $script:Settings.BrokerPort) + '\s+'
    return $output -match $routePattern
}

function Wait-BrokerRegistration {
    # 等待名称服务返回本机代理地址，失败时保留探测日志便于排查旧配置。
    $deadline = [DateTime]::UtcNow.AddSeconds($script:Settings.StartupTimeoutSeconds)
    do {
        if (Test-BrokerRegistration) {
            Write-ServiceStatus 'RocketMQ代理' '路由就绪' '名称服务已返回本机代理地址。'
            return
        }
        Start-Sleep -Seconds 2
    } while ([DateTime]::UtcNow -lt $deadline)
    throw ('代理端口已打开，但名称服务尚未返回正确路由，请检查broker-local.conf；探测日志：{0}' -f $script:LogDirectory)
}

$launcherMutex = $null
$mutexOwned = $false
$savedEnvironment = @{}
try {
    # 第一步：读取本机配置并校验文件，不发布配置、不修改现有软件目录。
    Assert-InstalledPath $ConfigPath '本机依赖配置文件'
    $script:Settings = Import-PowerShellDataFile -LiteralPath $ConfigPath
    $projectDirectory = Split-Path -Parent $PSScriptRoot
    $script:LogDirectory = [IO.Path]::GetFullPath((Join-Path $projectDirectory $Settings.LogDirectory))
    $script:JavaExecutable = Join-Path $Settings.JavaHome 'bin\java.exe'
    $nacosScript = Join-Path $Settings.NacosHome 'bin\startup.cmd'
    $greptimeExecutable = Join-Path $Settings.GreptimeHome 'greptime.exe'
    $nameServerScript = Join-Path $Settings.RocketmqHome 'bin\mqnamesrv.cmd'
    $brokerScript = Join-Path $Settings.RocketmqHome 'bin\mqbroker.cmd'
    foreach ($path in @($JavaExecutable, $nacosScript, $greptimeExecutable, $nameServerScript, `
            $brokerScript, $Settings.BrokerConfig, $Settings.MinioExecutable)) {
        Assert-InstalledPath $path '依赖程序或配置文件'
    }
    Assert-InstalledPath (Join-Path $Settings.NacosHome 'target\nacos-server.jar') '配置中心程序'
    Assert-InstalledPath (Join-Path $Settings.GreptimeHome 'greptimedb_data') '原有时序数据库数据目录' 'Container'
    Assert-InstalledPath $Settings.MinioDataDirectory '原有对象存储数据目录' 'Container'
    if ($Settings.StartupTimeoutSeconds -lt 1) { throw '依赖启动等待时间必须大于零。' }

    # 第二步：把依赖启动及健康检查参数集中定义，后续增加服务可沿用相同执行逻辑。
    $services = @(
        @{ Key = 'nacos'; Name = '配置中心'; Ports = @($Settings.NacosPort, $Settings.NacosGrpcPort)
            HealthUrl = ('http://127.0.0.1:{0}/nacos/v1/console/health/readiness' -f $Settings.NacosPort)
            ProcessName = 'java.exe'; ProcessPattern = ([regex]::Escape($Settings.NacosHome) + '.*nacos-server\.jar')
            Executable = $env:ComSpec; Arguments = ('/d /s /c "' + (ConvertTo-CommandToken $nacosScript) + ' -m standalone"')
            WorkingDirectory = $Settings.NacosHome },
        @{ Key = 'greptime'; Name = '时序数据库'; Ports = @($Settings.GreptimeHttpPort, $Settings.GreptimeGrpcPort, $Settings.GreptimeMysqlPort)
            HealthUrl = ('http://127.0.0.1:{0}/health' -f $Settings.GreptimeHttpPort)
            ProcessName = 'greptime.exe'; ProcessPattern = [regex]::Escape($greptimeExecutable)
            Executable = $greptimeExecutable; Arguments = 'standalone start'; WorkingDirectory = $Settings.GreptimeHome },
        @{ Key = 'nameserver'; Name = 'RocketMQ名称服务'; Ports = @($Settings.NameServerPort); HealthUrl = ''
            ProcessName = 'java.exe'; ProcessPattern = ([regex]::Escape($Settings.RocketmqHome) + '.*org\.apache\.rocketmq\.namesrv\.NamesrvStartup')
            Executable = $env:ComSpec; Arguments = ('/d /s /c "' + (ConvertTo-CommandToken $nameServerScript) + '"')
            WorkingDirectory = $Settings.RocketmqHome },
        @{ Key = 'broker'; Name = 'RocketMQ代理'; Ports = @($Settings.BrokerPort); HealthUrl = ''
            ProcessName = 'java.exe'; ProcessPattern = ([regex]::Escape($Settings.RocketmqHome) + '.*org\.apache\.rocketmq\.broker\.BrokerStartup')
            Executable = $env:ComSpec; Arguments = ('/d /s /c "' + (ConvertTo-CommandToken $brokerScript) `
                    + ' -n ' + (ConvertTo-CommandToken ('127.0.0.1:' + $Settings.NameServerPort)) `
                    + ' -c ' + (ConvertTo-CommandToken $Settings.BrokerConfig) + '"')
            WorkingDirectory = $Settings.RocketmqHome },
        @{ Key = 'minio'; Name = '对象存储'; Ports = @($Settings.MinioApiPort, $Settings.MinioConsolePort)
            HealthUrl = ('http://127.0.0.1:{0}/minio/health/ready' -f $Settings.MinioApiPort)
            ProcessName = [IO.Path]::GetFileName($Settings.MinioExecutable); ProcessPattern = [regex]::Escape($Settings.MinioExecutable)
            Executable = $Settings.MinioExecutable; Arguments = ('server ' + (ConvertTo-CommandToken $Settings.MinioDataDirectory) `
                    + ' --address ' + (ConvertTo-CommandToken (':' + $Settings.MinioApiPort)) `
                    + ' --console-address ' + (ConvertTo-CommandToken (':' + $Settings.MinioConsolePort)))
            WorkingDirectory = (Split-Path -Parent $Settings.MinioExecutable) }
    )

    # 第三步：检查模式只读取服务状态，不创建目录或后台进程。
    if ($CheckOnly) {
        $allReady = $true
        foreach ($item in @(@{Name = '金仓数据库'; Service = $Settings.KingbaseServiceName; Port = $Settings.KingbasePort}, `
                @{Name = '缓存服务'; Service = $Settings.RedisServiceName; Port = $Settings.RedisPort})) {
            $windowsService = Get-Service -Name $item.Service -ErrorAction SilentlyContinue
            if ($null -ne $windowsService -and $windowsService.Status -eq 'Running' -and (Test-LocalPort $item.Port)) {
                Write-ServiceStatus $item.Name '就绪' ('本机端口：' + $item.Port)
            } else {
                Write-ServiceStatus $item.Name '未就绪' ('系统服务或端口不可用：' + $item.Port)
                $allReady = $false
            }
        }
        foreach ($item in $services) {
            if (Test-ServiceReady $item) {
                Assert-ListenerOwner $item
                Write-ServiceStatus $item.Name '就绪' ('本机端口：' + ($item.Ports -join '、'))
            } else {
                Write-ServiceStatus $item.Name '未就绪' ('必要端口或健康检查不可用：' + ($item.Ports -join '、'))
                $allReady = $false
            }
        }
        if ($allReady) { exit 0 } else { exit 1 }
    }

    # 第四步：同一登录会话只允许一个启动器操作，避免连续双击产生重复进程。
    $launcherMutex = New-Object System.Threading.Mutex($false, 'Local\DataProcessingDependencyLauncher')
    try { $mutexOwned = $launcherMutex.WaitOne(0) } catch [System.Threading.AbandonedMutexException] { $mutexOwned = $true }
    if (-not $mutexOwned) { throw '另一个依赖启动脚本正在运行，请等待其完成。' }
    New-Item -ItemType Directory -Path $LogDirectory -Force | Out-Null
    # 环境变量只影响当前启动器及其子进程，结束时恢复，不修改系统级环境配置。
    foreach ($name in @('JAVA_HOME', 'ROCKETMQ_HOME', 'PATH', 'MINIO_ROOT_USER', 'MINIO_ROOT_PASSWORD')) {
        $savedEnvironment[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
    }
    $env:JAVA_HOME = $Settings.JavaHome
    $env:ROCKETMQ_HOME = $Settings.RocketmqHome
    $env:PATH = (Join-Path $Settings.JavaHome 'bin') + ';' + $env:PATH
    $env:MINIO_ROOT_USER = $Settings.MinioRootUser
    $env:MINIO_ROOT_PASSWORD = $Settings.MinioRootPassword

    # 第五步：按依赖顺序启动，任一组件失败就停止后续启动并保留已运行服务。
    Ensure-WindowsService '金仓数据库' $Settings.KingbaseServiceName $Settings.KingbasePort
    Ensure-WindowsService '缓存服务' $Settings.RedisServiceName $Settings.RedisPort
    foreach ($item in $services) {
        Ensure-BackgroundService $item
        if ($item.Key -eq 'broker') { Wait-BrokerRegistration }
    }
    Write-Host ''
    Write-Host '所有本机依赖已就绪，可以在开发工具中启动管理、处理和模拟源服务。' -ForegroundColor Green
    Write-Host ('本次后台进程日志目录：' + $LogDirectory)
    exit 0
} catch {
    Write-Host ('依赖启动或检查失败：' + $_.Exception.Message) -ForegroundColor Red
    exit 1
} finally {
    # 无论启动成功或失败都恢复临时环境，并释放启动互斥锁。
    foreach ($name in $savedEnvironment.Keys) {
        [Environment]::SetEnvironmentVariable($name, $savedEnvironment[$name], 'Process')
    }
    if ($mutexOwned) { $launcherMutex.ReleaseMutex() }
    if ($null -ne $launcherMutex) { $launcherMutex.Dispose() }
}
