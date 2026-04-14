param(
    [string]$MysqlExe = "C:\xampp\mysql\bin\mysql.exe",
    [string]$RootUser = "root",
    [string]$RootPassword = ""
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $MysqlExe)) {
    throw "Nao encontrei o cliente MySQL do XAMPP em '$MysqlExe'."
}

$sql = @"
CREATE DATABASE IF NOT EXISTS reserva_plus;
CREATE USER IF NOT EXISTS 'reserva_app'@'127.0.0.1' IDENTIFIED BY 'reserva123';
GRANT ALL PRIVILEGES ON *.* TO 'reserva_app'@'127.0.0.1';
"@

$args = @(
    "-u", $RootUser,
    "--protocol=TCP",
    "-h", "127.0.0.1",
    "--password=$RootPassword",
    "-e", $sql
)

& $MysqlExe @args

Write-Host "Banco local configurado com sucesso." -ForegroundColor Green
Write-Host "Usuario: reserva_app"
Write-Host "Senha:   reserva123"
