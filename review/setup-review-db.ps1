param(
    [string]$MySql = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe",
    [string]$DbUser = "root",
    [string]$Password = "orange",
    [string]$DbHost = "127.0.0.1",
    [int]$Port = 3306
)

# 在隔离的 campus_secondhand_review 库中重建 schema + demo seed。
# 原始 database/schema.sql 与 seed.sql 只做字符串替换，不修改被检查代码。
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$reviewDb = "campus_secondhand_review"
$schemaPath = Join-Path $root "database\schema.sql"
$seedPath = Join-Path $root "database\seed.sql"
$tmpDir = Join-Path $env:TEMP "campus-review-db"
New-Item -ItemType Directory -Force -Path $tmpDir | Out-Null

if (-not (Test-Path $MySql)) {
    throw "未找到 mysql.exe：$MySql"
}

$utf8 = New-Object System.Text.UTF8Encoding($false)
foreach ($pair in @(
        @{ Source = $schemaPath; Name = "schema-review.sql" },
        @{ Source = $seedPath; Name = "seed-review.sql" })) {
    $text = [System.IO.File]::ReadAllText($pair.Source, [System.Text.Encoding]::UTF8)
    $text = $text.Replace("campus_secondhand", $reviewDb)
    $target = Join-Path $tmpDir $pair.Name
    [System.IO.File]::WriteAllText($target, $text, $utf8)
}

$env:MYSQL_PWD = $Password
try {
    foreach ($name in @("schema-review.sql", "seed-review.sql")) {
        $file = Join-Path $tmpDir $name
        $output = cmd /c "`"$MySql`" -h $DbHost -P $Port -u $DbUser --default-character-set=utf8mb4 < `"$file`"" 2>&1
        if ($LASTEXITCODE -ne 0) {
            throw "$name 执行失败：$output"
        }
    }
    $summary = & $MySql -h $DbHost -P $Port -u $DbUser -N -B -e "SELECT CONCAT('users=', (SELECT COUNT(*) FROM $reviewDb.sys_user), ', items=', (SELECT COUNT(*) FROM $reviewDb.item), ', orders=', (SELECT COUNT(*) FROM $reviewDb.trade_order));" 2>&1
    Write-Host "隔离数据库 $reviewDb 已重建：$summary"
} finally {
    Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
}
