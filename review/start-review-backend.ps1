param(
    [switch]$SkipBuild,
    [int]$Port = 18080,
    [string]$MavenRepo = ""
)

# 启动指向隔离库 campus_secondhand_review 的后端，默认监听 18080，避免干扰演示环境。
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$backend = Join-Path $root "backend"
$jar = Join-Path $backend "target\campus-trade.jar"
$logDir = Join-Path $PSScriptRoot "logs"
$runDir = Join-Path $PSScriptRoot ".run"
New-Item -ItemType Directory -Force -Path $logDir, $runDir | Out-Null

$jdk = "E:\Java\jdk-21"
if (-not (Test-Path (Join-Path $jdk "bin\java.exe"))) {
    throw "未找到 JDK 21：$jdk"
}
$env:JAVA_HOME = $jdk

if (-not $SkipBuild) {
    $mvn = "E:\apache-maven\apache-maven-3.9.14\bin\mvn.cmd"
    if (-not (Test-Path $mvn)) { throw "未找到 Maven：$mvn" }
    if (-not $MavenRepo) { $MavenRepo = Join-Path $root ".m2repo" }
    Push-Location $backend
    try {
        & $mvn -B -o -DskipTests "-Dmaven.repo.local=$MavenRepo" package | Out-Host
        if ($LASTEXITCODE -ne 0) { throw "后端构建失败" }
    } finally {
        Pop-Location
    }
}

if (-not (Test-Path $jar)) { throw "未找到 $jar，请先构建" }

$oldPidFile = Join-Path $runDir "backend.pid"
if (Test-Path $oldPidFile) {
    $oldPid = [int](Get-Content $oldPidFile -Raw)
    $old = Get-Process -Id $oldPid -ErrorAction SilentlyContinue
    if ($old) { throw "已有后端进程在运行（PID $oldPid），请先执行 review/stop-review-backend.ps1" }
}

$env:DB_HOST = "127.0.0.1"
$env:DB_PORT = "3306"
$env:DB_NAME = "campus_secondhand_review"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "orange"

$outLog = Join-Path $logDir "backend.out.log"
$errLog = Join-Path $logDir "backend.err.log"
$process = Start-Process -FilePath (Join-Path $jdk "bin\java.exe") `
    -ArgumentList @("-jar", $jar, "--server.port=$Port") `
    -WorkingDirectory $backend `
    -RedirectStandardOutput $outLog `
    -RedirectStandardError $errLog `
    -WindowStyle Hidden `
    -PassThru

$process.Id | Set-Content -Path $oldPidFile -Encoding ascii
Write-Host "后端启动中（PID $($process.Id)，端口 $Port）..."
$base = "http://127.0.0.1:$Port/api/meta"
$ready = $false
for ($i = 0; $i -lt 60; $i++) {
    Start-Sleep -Milliseconds 500
    try {
        $response = Invoke-WebRequest -Uri $base -UseBasicParsing -TimeoutSec 2
        if ($response.StatusCode -eq 200) { $ready = $true; break }
    } catch {
        if ($process.HasExited) { break }
    }
}
if (-not $ready) {
    Write-Host "---- backend.out.log (tail) ----"
    Get-Content $outLog -Tail 30 -ErrorAction SilentlyContinue
    Write-Host "---- backend.err.log (tail) ----"
    Get-Content $errLog -Tail 30 -ErrorAction SilentlyContinue
    throw "后端未在 30 秒内就绪"
}
Write-Host "后端就绪：http://127.0.0.1:$Port/api（隔离库 campus_secondhand_review）"
