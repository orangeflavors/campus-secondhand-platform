param(
    [switch]$SkipBuild,
    [string]$MavenRepo = "",
    [switch]$Foreground
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$backend = Join-Path $root "backend"
$jar = Join-Path $backend "target\campus-trade.jar"

function Find-Jdk21 {
    $candidates = @(
        "E:\Java\jdk-21",
        "C:\Program Files\Java\jdk-21",
        "C:\Program Files\Eclipse Adoptium\jdk-21"
    )
    if ($env:JAVA_HOME) { $candidates = @($env:JAVA_HOME) + $candidates }
    foreach ($homeDir in $candidates) {
        if ($homeDir -and (Test-Path (Join-Path $homeDir "bin\java.exe"))) {
            $release = Join-Path $homeDir "release"
            if (Test-Path $release) {
                $content = Get-Content $release -Raw
                if ($content -match 'JAVA_VERSION="21') { return $homeDir }
            } else {
                return $homeDir
            }
        }
    }
    return $null
}

function Find-Java {
    $jdk21 = Find-Jdk21
    if ($jdk21) { return (Join-Path $jdk21 "bin\java.exe") }
    if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME "bin\java.exe"))) {
        return (Join-Path $env:JAVA_HOME "bin\java.exe")
    }
    $cmd = Get-Command java -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    foreach ($candidate in @("E:\Java\jdk-21\bin\java.exe", "C:\Program Files\Java\jdk-21\bin\java.exe")) {
        if (Test-Path $candidate) { return $candidate }
    }
    throw "未找到 Java 21+，请安装 JDK 并配置 JAVA_HOME"
}

$java = Find-Java
$jdk21Home = Find-Jdk21
if ($jdk21Home) {
    $env:JAVA_HOME = $jdk21Home
}

if (-not $SkipBuild) {
    $mvn = Get-Command mvn -ErrorAction SilentlyContinue
    if ($mvn) {
        $mvnCmd = $mvn.Source
    } elseif (Test-Path "E:\apache-maven\apache-maven-3.9.14\bin\mvn.cmd") {
        $mvnCmd = "E:\apache-maven\apache-maven-3.9.14\bin\mvn.cmd"
    } else {
        throw "未找到 Maven，请安装 Maven 后重试，或先手动打包并使用 -SkipBuild"
    }

    if (-not $MavenRepo) {
        $MavenRepo = Join-Path $root ".m2repo"
    }

    Write-Host "正在构建后端（依赖仓库：$MavenRepo）..." -ForegroundColor Cyan
    Push-Location $backend
    try {
        & $mvnCmd -B -DskipTests "-Dmaven.repo.local=$MavenRepo" package
        if ($LASTEXITCODE -ne 0) { throw "后端构建失败" }
    } finally {
        Pop-Location
    }
}

if (-not (Test-Path $jar)) {
    throw "未找到 $jar，请先构建后端"
}

$uploadDir = Join-Path $backend "uploads"
New-Item -ItemType Directory -Force -Path $uploadDir | Out-Null

Write-Host "后端启动中：http://localhost:8080" -ForegroundColor Green
Write-Host "演示账号：admin/admin123、seller01/123456、buyer01/123456" -ForegroundColor DarkGray

Push-Location $backend
try {
    if ($Foreground) {
        & $java -jar $jar
    } else {
        Start-Process -FilePath $java -ArgumentList "-jar", "`"$jar`"" -WorkingDirectory $backend -WindowStyle Normal
        Write-Host "已在新窗口启动，关闭该窗口即可停止后端。"
    }
} finally {
    Pop-Location
}
