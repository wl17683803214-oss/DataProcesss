# 第一步：定义输出文件、帧结构和固定遥测帧头。
$outputDirectory = $PSScriptRoot
$dataFilePath = Join-Path $outputDirectory '遥测10000帧-含野值.dat'
$manifestFilePath = Join-Path $outputDirectory '遥测10000帧-野值对照.txt'
$frameCount = 10000
$frameLength = 256
$telemetryHeader = [byte[]](0xAA, 0xBB, 0xCC, 0xDD)

# 第二步：定义三种检测方法对应的野值帧，帧间距超过实时缓存上限。
$wrightFrames = [System.Collections.Generic.HashSet[int]]::new()
$thresholdFrames = [System.Collections.Generic.HashSet[int]]::new()
$chauvenetFrames = [System.Collections.Generic.HashSet[int]]::new()
foreach ($frameNumber in @(1500, 3300, 5100, 6900, 8700)) {
    [void]$wrightFrames.Add($frameNumber)
}
foreach ($frameNumber in @(1520, 3320, 5120, 6920, 8720)) {
    [void]$thresholdFrames.Add($frameNumber)
}
foreach ($frameNumber in @(1540, 3340, 5140, 6940, 8740)) {
    [void]$chauvenetFrames.Add($frameNumber)
}

# 第三步：准备野值清单，说明每类数据需要关联的检测方法。
$manifestLines = [System.Collections.Generic.List[string]]::new()
$manifestLines.Add('文件格式：10000帧，每帧256字节，共2560000字节。')
$manifestLines.Add('帧结构：4字节遥测帧头AA BB CC DD + 5个32位小端无符号整数 + 232字节补零。')
$manifestLines.Add('字段顺序：FKT001电压、FKT002电流、FKT003温度、FKT004电压1、FKT005电压2。')
$manifestLines.Add('检测配置：FKT001关联莱特准则，FKT002关联阈值法，FKT003关联肖维涅法。')
$manifestLines.Add('野值说明：每种方法前面均有不少于1499帧正常波动样本，野值间隔超过1000帧。')
$manifestLines.Add('')
$manifestLines.Add("帧号`t遥测代号`t遥测名称`t检测方式`t野值")

# 第四步：逐帧写入二进制数据，正常值保持轻微波动以形成非零标准差。
$fileStream = [System.IO.File]::Open(
    $dataFilePath,
    [System.IO.FileMode]::Create,
    [System.IO.FileAccess]::Write,
    [System.IO.FileShare]::None)
try {
    for ($frameNumber = 1; $frameNumber -le $frameCount; $frameNumber++) {
        $frameBytes = [byte[]]::new($frameLength)
        [Array]::Copy($telemetryHeader, 0, $frameBytes, 0, $telemetryHeader.Length)

        # 第五步：五个遥测量使用三点循环形成稳定、可统计的正常基线。
        $cycleIndex = ($frameNumber - 1) % 3
        $values = [uint32[]]@(
            @(41, 42, 43)[$cycleIndex],
            @(27, 28, 29)[$cycleIndex],
            @(10, 15, 20)[$cycleIndex],
            @(27, 28, 29)[$cycleIndex],
            @(27, 28, 29)[2 - $cycleIndex]
        )

        # 第六步：在三个不同参数中插入足够明显且互不干扰的野值。
        if ($wrightFrames.Contains($frameNumber)) {
            $values[0] = 1000000
            $manifestLines.Add("$frameNumber`tFKT001`t电压`t莱特准则`t1000000")
        }
        if ($thresholdFrames.Contains($frameNumber)) {
            $values[1] = 1000000
            $manifestLines.Add("$frameNumber`tFKT002`t电流`t阈值法`t1000000")
        }
        if ($chauvenetFrames.Contains($frameNumber)) {
            $values[2] = 1000000
            $manifestLines.Add("$frameNumber`tFKT003`t温度`t肖维涅法`t1000000")
        }

        # 第七步：按项目解析顺序把五个无符号整数写成小端字节。
        for ($fieldIndex = 0; $fieldIndex -lt $values.Length; $fieldIndex++) {
            $valueBytes = [BitConverter]::GetBytes($values[$fieldIndex])
            [Array]::Copy($valueBytes, 0, $frameBytes, 4 + $fieldIndex * 4, 4)
        }
        $fileStream.Write($frameBytes, 0, $frameBytes.Length)
    }
}
finally {
    $fileStream.Dispose()
}

# 第八步：保存野值帧号、参数和检测方式对照表。
$utf8WithBom = [System.Text.UTF8Encoding]::new($true)
[System.IO.File]::WriteAllLines($manifestFilePath, $manifestLines, $utf8WithBom)

# 第九步：回读整个文件，逐帧校验长度、帧头、字段值和补零区域。
$verifiedBytes = [System.IO.File]::ReadAllBytes($dataFilePath)
$expectedLength = $frameCount * $frameLength
if ($verifiedBytes.Length -ne $expectedLength) {
    throw "文件长度错误，期望$expectedLength，实际$($verifiedBytes.Length)"
}
$detectedOutlierCount = 0
for ($frameNumber = 1; $frameNumber -le $frameCount; $frameNumber++) {
    $frameOffset = ($frameNumber - 1) * $frameLength
    for ($headerIndex = 0; $headerIndex -lt $telemetryHeader.Length; $headerIndex++) {
        if ($verifiedBytes[$frameOffset + $headerIndex] -ne $telemetryHeader[$headerIndex]) {
            throw "第${frameNumber}帧的遥测帧头不正确"
        }
    }

    # 第十步：检查三种方法的目标字段只在预定帧出现野值。
    $voltage = [BitConverter]::ToUInt32($verifiedBytes, $frameOffset + 4)
    $current = [BitConverter]::ToUInt32($verifiedBytes, $frameOffset + 8)
    $temperature = [BitConverter]::ToUInt32($verifiedBytes, $frameOffset + 12)
    $voltageOne = [BitConverter]::ToUInt32($verifiedBytes, $frameOffset + 16)
    $voltageTwo = [BitConverter]::ToUInt32($verifiedBytes, $frameOffset + 20)
    if (($voltage -eq 1000000) -ne $wrightFrames.Contains($frameNumber)) {
        throw "第${frameNumber}帧的莱特准则野值位置不正确"
    }
    if (($current -eq 1000000) -ne $thresholdFrames.Contains($frameNumber)) {
        throw "第${frameNumber}帧的阈值法野值位置不正确"
    }
    if (($temperature -eq 1000000) -ne $chauvenetFrames.Contains($frameNumber)) {
        throw "第${frameNumber}帧的肖维涅法野值位置不正确"
    }
    if ($voltage -eq 1000000) { $detectedOutlierCount++ }
    if ($current -eq 1000000) { $detectedOutlierCount++ }
    if ($temperature -eq 1000000) { $detectedOutlierCount++ }
    $auxiliaryVoltageInvalid = ($voltageOne -lt 27) -or
            ($voltageOne -gt 29) -or
            ($voltageTwo -lt 27) -or
            ($voltageTwo -gt 29)
    if ($auxiliaryVoltageInvalid) {
        throw "第${frameNumber}帧的辅助电压值超出正常范围"
    }
    for ($paddingIndex = 24; $paddingIndex -lt $frameLength; $paddingIndex++) {
        if ($verifiedBytes[$frameOffset + $paddingIndex] -ne 0) {
            throw "第${frameNumber}帧的补零区域不正确"
        }
    }
}

# 第十一步：核对野值总数并输出最终校验结果。
if ($detectedOutlierCount -ne 15) {
    throw "野值总数错误，期望15，实际$detectedOutlierCount"
}
Write-Output "校验通过：$frameCount 帧，每帧 $frameLength 字节，共 $expectedLength 字节，包含 15 个野值。"
Write-Output $dataFilePath
Write-Output $manifestFilePath
