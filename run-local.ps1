param(
    [switch]$BackendOnly,
    [switch]$FrontendOnly
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSCommandPath
$backendDir = Join-Path $repoRoot "backend"
$frontendDir = Join-Path $repoRoot "frontend"

function Require-Path {
    param(
        [string]$Path,
        [string]$FriendlyName
    )

    if (-not (Test-Path $Path)) {
        throw "$FriendlyName nao foi encontrado em '$Path'."
    }
}

function Test-PortInUse {
    param([int]$Port)

    return [bool](Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
}

function Get-ListeningProcessIds {
    param([int]$Port)

    return @(Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty OwningProcess -Unique)
}

function Wait-PortReleased {
    param(
        [int]$Port,
        [int]$TimeoutSeconds = 10
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)

    while ((Get-Date) -lt $deadline) {
        if (-not (Test-PortInUse -Port $Port)) {
            return
        }

        Start-Sleep -Milliseconds 250
    }

    throw "A porta $Port nao foi liberada a tempo."
}

function Stop-ProcessesOnPort {
    param([int]$Port)

    $processIds = Get-ListeningProcessIds -Port $Port
    if ($processIds.Count -eq 0) {
        return
    }

    foreach ($processId in $processIds) {
        $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
        $processName = if ($process) { $process.ProcessName } else { "PID $processId" }

        Write-Host "Encerrando $processName na porta $Port..." -ForegroundColor Yellow
        Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
    }

    Wait-PortReleased -Port $Port
}

function Require-Command {
    param(
        [string]$CommandName,
        [string]$FriendlyName
    )

    if (-not (Get-Command $CommandName -ErrorAction SilentlyContinue)) {
        throw "$FriendlyName nao foi encontrado no PATH."
    }
}

function Resolve-MavenCommand {
    $fromPath = Get-Command "mvn" -ErrorAction SilentlyContinue
    if ($fromPath) {
        return $fromPath.Source
    }

    $candidates = @()

    if ($env:MAVEN_HOME) {
        $candidates += (Join-Path $env:MAVEN_HOME "bin\mvn.cmd")
    }

    $candidates += (Join-Path $backendDir ".tools\apache-maven-3.9.9\bin\mvn.cmd")
    $candidates += "C:\Tools\apache-maven-3.9.14\bin\mvn.cmd"
    $candidates += "C:\Tools\apache-maven-3.9.9\bin\mvn.cmd"

    foreach ($candidate in ($candidates | Select-Object -Unique)) {
        if ($candidate -and (Test-Path $candidate)) {
            return $candidate
        }
    }

    throw "Maven nao foi encontrado no PATH, no MAVEN_HOME ou na pasta .tools do backend."
}

function Start-InNewWindow {
    param(
        [string]$Title,
        [string]$Command
    )

    $fullCommand = "`$Host.UI.RawUI.WindowTitle = '$Title'; $Command"
    Start-Process powershell.exe -ArgumentList "-NoExit", "-Command", $fullCommand | Out-Null
}

if ($BackendOnly -and $FrontendOnly) {
    throw "Use apenas um dos parametros: -BackendOnly ou -FrontendOnly."
}

Require-Path -Path $backendDir -FriendlyName "Diretorio do backend"
Require-Path -Path $frontendDir -FriendlyName "Diretorio do frontend"

$mavenExe = $null

if (-not $FrontendOnly) {
    $mavenExe = Resolve-MavenCommand
}

if (-not $BackendOnly) {
    Require-Command -CommandName "npm" -FriendlyName "npm"
}

if (-not (Test-PortInUse -Port 3306)) {
    throw "A porta 3306 nao esta em uso. Inicie o MySQL do XAMPP e, se for a primeira vez, rode .\scripts\setup-xampp-db.ps1."
}

if (-not $FrontendOnly) {
    Stop-ProcessesOnPort -Port 8080

    $backendCommand = "Set-Location '$backendDir'; `$env:SPRING_PROFILES_ACTIVE='local'; & '$mavenExe' spring-boot:run"
    Start-InNewWindow -Title "Reserva+ Backend" -Command $backendCommand
    Write-Host "Abrindo backend em nova janela..." -ForegroundColor Green
}

if (-not $BackendOnly) {
    Stop-ProcessesOnPort -Port 4200

    $frontendCommand = "Set-Location '$frontendDir'; npm start"
    Start-InNewWindow -Title "Reserva+ Frontend" -Command $frontendCommand
    Write-Host "Abrindo frontend em nova janela..." -ForegroundColor Green
}

Write-Host ""
Write-Host "URLs do projeto:" -ForegroundColor Cyan
Write-Host "Frontend: http://localhost:4200"
Write-Host "Backend:  http://localhost:8080"
Write-Host "API:      http://localhost:8080/api"
Write-Host "Health:   http://localhost:8080/actuator/health"
Write-Host ""
Write-Host "Banco local esperado:" -ForegroundColor Cyan
Write-Host "Host:     127.0.0.1"
Write-Host "Porta:    3306"
Write-Host "Banco:    reserva_plus"
Write-Host "Usuario:  reserva_app"
Write-Host ""
Write-Host "Parar tudo: .\stop-local.ps1" -ForegroundColor DarkGray
