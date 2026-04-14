param(
    [switch]$BackendOnly,
    [switch]$FrontendOnly
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSCommandPath
$backendDir = Join-Path $repoRoot "backend"
$frontendDir = Join-Path $repoRoot "frontend"

function Test-PortInUse {
    param([int]$Port)

    return [bool](Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
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

    $candidates += "C:\Tools\apache-maven-3.9.14\bin\mvn.cmd"

    foreach ($candidate in ($candidates | Select-Object -Unique)) {
        if ($candidate -and (Test-Path $candidate)) {
            return $candidate
        }
    }

    throw "Maven nao foi encontrado no PATH nem no MAVEN_HOME."
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

$mavenExe = $null

if (-not $FrontendOnly) {
    $mavenExe = Resolve-MavenCommand
}

if (-not $BackendOnly) {
    Require-Command -CommandName "npm" -FriendlyName "npm"
}

if (-not (Test-PortInUse -Port 3306)) {
    throw "A porta 3306 nao esta em uso. Inicie o MySQL do XAMPP antes de rodar este script."
}

if (-not $FrontendOnly) {
    if (Test-PortInUse -Port 8080) {
        Write-Host "Backend ja esta rodando na porta 8080." -ForegroundColor Yellow
    } else {
        $backendCommand = "Set-Location '$backendDir'; `$env:SPRING_PROFILES_ACTIVE='local'; & '$mavenExe' spring-boot:run"
        Start-InNewWindow -Title "Reserva+ Backend" -Command $backendCommand
        Write-Host "Abrindo backend em nova janela..." -ForegroundColor Green
    }
}

if (-not $BackendOnly) {
    if (Test-PortInUse -Port 4200) {
        Write-Host "Frontend ja esta rodando na porta 4200." -ForegroundColor Yellow
    } else {
        $frontendCommand = "Set-Location '$frontendDir'; npm start"
        Start-InNewWindow -Title "Reserva+ Frontend" -Command $frontendCommand
        Write-Host "Abrindo frontend em nova janela..." -ForegroundColor Green
    }
}

Write-Host ""
Write-Host "URLs do projeto:" -ForegroundColor Cyan
Write-Host "Frontend: http://localhost:4200"
Write-Host "Backend:  http://localhost:8080"
Write-Host "API:      http://localhost:8080/api"
