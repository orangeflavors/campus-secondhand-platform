$ErrorActionPreference = "Stop"
$pidFile = Join-Path $PSScriptRoot ".run\backend.pid"
if (-not (Test-Path $pidFile)) {
    Write-Host "未找到后端 PID 文件，无需停止。"
    exit 0
}
$backendPid = [int](Get-Content $pidFile -Raw)
$process = Get-Process -Id $backendPid -ErrorAction SilentlyContinue
if ($process) {
    Stop-Process -Id $backendPid -Force
    Write-Host "已停止后端 PID $backendPid"
} else {
    Write-Host "后端 PID $backendPid 已不存在"
}
Remove-Item $pidFile -Force
