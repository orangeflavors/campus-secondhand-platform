@echo off
REM 校园集市 · 前端启动入口（双击或在 cmd 中运行）
REM 自动调用 PowerShell 脚本，由脚本负责定位 Node/pnpm 并设置 PATH
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-frontend.ps1" %*
pause
