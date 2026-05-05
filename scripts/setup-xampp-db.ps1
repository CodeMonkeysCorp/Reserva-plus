param(
    [string]$MysqlExe,
    [string]$RootUser = "root",
    [string]$RootPassword = ""
)

$ErrorActionPreference = "Stop"

function Test-PortInUse {
    param([int]$Port)

    return [bool](Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
}

function Resolve-MysqlCommand {
    param([string]$PreferredPath)

    $candidates = @()

    if ($PreferredPath) {
        $candidates += $PreferredPath
    }

    $fromPath = Get-Command "mysql" -ErrorAction SilentlyContinue
    if ($fromPath) {
        $candidates += $fromPath.Source
    }

    $candidates += "C:\xampp\mysql\bin\mysql.exe"
    $candidates += "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"

    foreach ($candidate in ($candidates | Select-Object -Unique)) {
        if ($candidate -and (Test-Path $candidate)) {
            return $candidate
        }
    }

    throw "Nao encontrei o cliente MariaDB/MySQL. Informe -MysqlExe ou instale o cliente no PATH."
}

function Invoke-MysqlScript {
    param(
        [string]$Executable,
        [string]$User,
        [string]$Password,
        [string]$Sql
    )

    $args = @(
        "-u", $User,
        "--protocol=TCP",
        "-h", "127.0.0.1"
    )

    if ($Password) {
        $args += "--password=$Password"
    }

    $args += "-e", $Sql

    & $Executable @args

    if ($LASTEXITCODE -ne 0) {
        throw "Falha ao executar o script SQL de configuracao local."
    }
}

$repoRoot = Split-Path -Parent (Split-Path -Parent $PSCommandPath)
$resolvedMysqlExe = Resolve-MysqlCommand -PreferredPath $MysqlExe

if (-not (Test-PortInUse -Port 3306)) {
    throw "A porta 3306 nao esta em uso. Inicie o MySQL do XAMPP antes de rodar este script."
}

$sql = @"
CREATE DATABASE IF NOT EXISTS reserva_plus;
CREATE USER IF NOT EXISTS 'reserva_app'@'127.0.0.1' IDENTIFIED BY 'reserva123';
CREATE USER IF NOT EXISTS 'reserva_app'@'localhost' IDENTIFIED BY 'reserva123';
GRANT ALL PRIVILEGES ON reserva_plus.* TO 'reserva_app'@'127.0.0.1';
GRANT ALL PRIVILEGES ON reserva_plus.* TO 'reserva_app'@'localhost';
FLUSH PRIVILEGES;
"@

Invoke-MysqlScript -Executable $resolvedMysqlExe -User $RootUser -Password $RootPassword -Sql $sql

Write-Host "Banco local configurado com sucesso." -ForegroundColor Green
Write-Host ""
Write-Host "Resumo da configuracao:" -ForegroundColor Cyan
Write-Host "Host:    127.0.0.1"
Write-Host "Porta:   3306"
Write-Host "Banco:   reserva_plus"
Write-Host "Usuario: reserva_app"
Write-Host "Senha:   reserva123"
Write-Host ""
Write-Host "Proximo passo: $repoRoot\\run-local.ps1" -ForegroundColor DarkGray
