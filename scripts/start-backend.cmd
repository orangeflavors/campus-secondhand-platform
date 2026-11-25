@echo off
REM 校园集市 · 后端启动入口（双击或在 cmd 中运行）
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-backend.ps1" %*
pause
