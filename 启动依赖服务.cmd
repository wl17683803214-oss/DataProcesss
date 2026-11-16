@echo off
setlocal
chcp 65001 >nul
rem 使用系统自带的脚本运行环境，路径始终以本入口所在目录为基准。
rem 避免继承其他版本终端的模块路径，子进程使用系统默认模块目录。
set "PSModulePath="
"%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe" -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\start-local-services.ps1" %*
set "DEPENDENCY_START_RESULT=%ERRORLEVEL%"
echo.
rem 保留执行结果，便于双击启动时查看失败原因和日志位置。
echo 按任意键关闭此窗口，后台依赖会继续运行。
pause >nul
exit /b %DEPENDENCY_START_RESULT%
