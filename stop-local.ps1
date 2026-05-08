param(
    [switch]$BackendOnly,
    [switch]$FrontendOnly,
    [switch]$DatabaseOnly,
    [switch]$RemoveVolumes
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSCommandPath
$composeFile = Join-Path $repoRoot "compose.yaml"

function Require-Path {
    param(
        [string]$Path,
        [string]$FriendlyName
    )

    if (-not (Test-Path $Path)) {
        throw "$FriendlyName nao foi encontrado em '$Path'."
    }
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

function Resolve-ServiceSelection {
    $selectedServices = @()

    if ($BackendOnly) {
        $selectedServices += "backend"
    }

    if ($FrontendOnly) {
        $selectedServices += "frontend"
    }

    if ($DatabaseOnly) {
        $selectedServices += "mysql"
    }

    if ($selectedServices.Count -gt 1) {
        throw "Use apenas um dos parametros: -BackendOnly, -FrontendOnly ou -DatabaseOnly."
    }

    return @($selectedServices)
}

function Invoke-DockerCompose {
    param([string[]]$ComposeArgs)

    $dockerArgs = @("compose") + $ComposeArgs
    & docker @dockerArgs

    if ($LASTEXITCODE -ne 0) {
        throw "Falha ao executar 'docker $($dockerArgs -join ' ')'."
    }
}

Require-Path -Path $composeFile -FriendlyName "Arquivo compose"
Require-Command -CommandName "docker" -FriendlyName "Docker"

$services = Resolve-ServiceSelection

if ($RemoveVolumes -and $services.Count -gt 0) {
    throw "Use -RemoveVolumes apenas ao derrubar a stack completa."
}

if ($services.Count -eq 0) {
    $composeArgs = @("down")
    if ($RemoveVolumes) {
        $composeArgs += "-v"
    }

    Write-Host "Encerrando stack Docker..." -ForegroundColor Yellow
    Invoke-DockerCompose -ComposeArgs $composeArgs
    Write-Host "Stack encerrada." -ForegroundColor Green
    Write-Host "Para subir novamente: $repoRoot\\run-local.ps1" -ForegroundColor DarkGray
    return
}

Write-Host "Parando servicos Docker selecionados..." -ForegroundColor Yellow
Invoke-DockerCompose -ComposeArgs (@("stop") + $services)

Write-Host ""
Write-Host "Status da stack:" -ForegroundColor Cyan
Invoke-DockerCompose -ComposeArgs @("ps")
Write-Host ""
Write-Host "Para subir novamente: $repoRoot\\run-local.ps1" -ForegroundColor DarkGray
