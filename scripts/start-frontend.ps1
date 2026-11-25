param(
    [switch]$Install,
    [int]$Port = 5173
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$frontend = Join-Path $root "frontend"

function Find-Node {
    $cmd = Get-Command node -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    foreach ($candidate in @(
            "$env:ProgramFiles\nodejs\node.exe",
            "$env:LOCALAPPDATA\Programs\nodejs\node.exe",
            "C:\Users\$env:USERNAME\.dsh\dsh-runtimes\dsh-primary-runtime\dependencies\node\bin\node.exe",
            "E:\nodejs\node.exe"
        )) {
        if (Test-Path $candidate) { return $candidate }
    }
    throw "未找到 Node.js，请安装 Node.js 20+ 后重试"
}

function Find-Pnpm {
    $cmd = Get-Command pnpm -ErrorAction SilentlyContinue
    if ($cmd) {
        return @{ Mode = "bin"; Path = $cmd.Source }
    }
    foreach ($candidate in @(
            "C:\Users\$env:USERNAME\.dsh\dsh-runtimes\dsh-primary-runtime\dependencies\pnpm\bin\pnpm.mjs",
            "$env:APPDATA\npm\pnpm.cmd"
        )) {
        if (Test-Path $candidate) {
            if ($candidate.EndsWith(".mjs")) {
                return @{ Mode = "node"; Path = $candidate }
            }
            return @{ Mode = "bin"; Path = $candidate }
        }
    }
    throw "未找到 pnpm，请先执行 npm install -g pnpm"
}

$node = Find-Node
$pnpm = Find-Pnpm

# 关键：vite / pnpm 在 Windows 上通过 .cmd 启动脚本调用 PATH 中的 node。
# 如果 Node 是绿色版或由其他工具托管（不在 PATH 中），需要显式把其目录加入 PATH，
# 否则会出现 "'node' is not recognized as an internal or external command"。
$nodeDir = Split-Path -Parent $node
$pathParts = $env:Path -split ';'
$alreadyInPath = $pathParts | Where-Object { $_ -and ($_.TrimEnd('\') -ieq $nodeDir.TrimEnd('\')) }
if (-not $alreadyInPath) {
    $env:Path = "$nodeDir;$env:Path"
    Write-Host "已将 Node 目录加入 PATH：$nodeDir" -ForegroundColor DarkGray
}

function Invoke-Pnpm {
    param([string[]]$Arguments)
    Push-Location $frontend
    try {
        if ($pnpm.Mode -eq "node") {
            & $node $pnpm.Path @Arguments
        } else {
            & $pnpm.Path @Arguments
        }
        if ($LASTEXITCODE -ne 0) { throw "pnpm 执行失败" }
    } finally {
        Pop-Location
    }
}

$nodeModules = Join-Path $frontend "node_modules"
if ($Install -or -not (Test-Path $nodeModules)) {
    Write-Host "正在安装前端依赖..." -ForegroundColor Cyan
    Invoke-Pnpm @("install")
}

Write-Host "前端启动中：http://localhost:$Port" -ForegroundColor Green
Write-Host "请确保后端已在 http://localhost:8080 运行" -ForegroundColor DarkGray

Push-Location $frontend
try {
    if ($pnpm.Mode -eq "node") {
        & $node $pnpm.Path "dev" "--port" "$Port"
    } else {
        & $pnpm.Path "dev" "--port" "$Port"
    }
} finally {
    Pop-Location
}
