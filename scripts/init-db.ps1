param(
    [string]$MySql = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe",
    [string]$DbUser = "root",
    [string]$Password = "orange",
    [string]$DbHost = "127.0.0.1",
    [int]$Port = 3306
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$schema = Join-Path $root "database\schema.sql"
$seed = Join-Path $root "database\seed.sql"

if (-not (Test-Path $MySql)) {
    $candidates = @(
        "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe",
        "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe",
        "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
    ) | Where-Object { Test-Path $_ }

    if ($candidates.Count -eq 0) {
        $cmd = Get-Command mysql -ErrorAction SilentlyContinue
        if ($cmd) { $MySql = $cmd.Source }
        else { throw "未找到 mysql.exe，请通过 -MySql 指定路径" }
    } else {
        $MySql = $candidates[0]
    }
}

if (-not (Test-Path $schema) -or -not (Test-Path $seed)) {
    throw "未找到 database/schema.sql 或 database/seed.sql"
}

Write-Host "正在初始化数据库 campus_secondhand ..." -ForegroundColor Cyan
$env:MYSQL_PWD = $Password

$schemaExit = cmd /c "`"$MySql`" -h $DbHost -P $Port -u $DbUser --default-character-set=utf8mb4 < `"$schema`"" 2>&1
if ($LASTEXITCODE -ne 0) {
    Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
    Write-Host $schemaExit
    throw "schema.sql 执行失败（请检查用户名、密码与 MySQL 服务是否启动）"
}

$seedExit = cmd /c "`"$MySql`" -h $DbHost -P $Port -u $DbUser --default-character-set=utf8mb4 < `"$seed`"" 2>&1
if ($LASTEXITCODE -ne 0) {
    Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
    Write-Host $seedExit
    throw "seed.sql 执行失败"
}

$summary = & $MySql -h $DbHost -P $Port -u $DbUser -e "USE campus_secondhand; SELECT COUNT(*) AS users FROM sys_user; SELECT COUNT(*) AS items FROM item; SELECT COUNT(*) AS orders FROM trade_order;" 2>&1
Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue

Write-Host $summary
Write-Host "数据库初始化完成 ✅" -ForegroundColor Green
Write-Host "演示账号：admin/admin123、seller01/123456、buyer01/123456、buyer02/123456、newbie01/123456、banned01/123456"
