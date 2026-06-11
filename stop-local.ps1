param(
    [switch]$BackendOnly,
    [switch]$FrontendOnly,
    [switch]$DatabaseOnly,
    [switch]$IncludeDatabase,
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
        throw "$FriendlyName não foi encontrado em '$Path'."
    }
}

function Require-Command {
    param(
        [string]$CommandName,
        [string]$FriendlyName
    )

    if (-not (Get-Command $CommandName -ErrorAction SilentlyContinue)) {
        throw "$FriendlyName não foi encontrado no PATH."
    }
}

function Resolve-ServiceSelection {
    if (($BackendOnly -or $FrontendOnly -or $DatabaseOnly) -and $IncludeDatabase) {
        throw "Use -IncludeDatabase sem combinar com -BackendOnly, -FrontendOnly ou -DatabaseOnly."
    }

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
        throw "Use apenas um dos parâmetros: -BackendOnly, -FrontendOnly ou -DatabaseOnly."
    }

    if ($IncludeDatabase) {
        return @("mysql", "backend", "frontend")
    }

    if ($selectedServices.Count -eq 0) {
        return @("backend", "frontend")
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
$stoppingWholeStack = $IncludeDatabase

if ($RemoveVolumes -and -not $stoppingWholeStack) {
    throw "Use -RemoveVolumes apenas com -IncludeDatabase."
}

if ($stoppingWholeStack) {
    $composeArgs = @("down")
    if ($RemoveVolumes) {
        $composeArgs += "-v"
    }

    Write-Host "Encerrando stack Docker completa..." -ForegroundColor Yellow
    Invoke-DockerCompose -ComposeArgs $composeArgs
    Write-Host "Stack encerrada." -ForegroundColor Green
    Write-Host "Para subir novamente: $repoRoot\\run-local.ps1" -ForegroundColor DarkGray
    return
}

if ($DatabaseOnly) {
    Write-Host "Parando o MySQL Docker selecionado..." -ForegroundColor Yellow
}
elseif ($BackendOnly -or $FrontendOnly) {
    Write-Host "Parando serviços Docker selecionados..." -ForegroundColor Yellow
}
else {
    Write-Host "Parando frontend/backend e preservando o MySQL compartilhado..." -ForegroundColor Yellow
}

Invoke-DockerCompose -ComposeArgs (@("stop") + $services)

Write-Host ""
Write-Host "Status da stack:" -ForegroundColor Cyan
Invoke-DockerCompose -ComposeArgs @("ps")
Write-Host ""
if (-not ($BackendOnly -or $FrontendOnly -or $DatabaseOnly)) {
    Write-Host "Para derrubar também o MySQL: $repoRoot\\stop-local.ps1 -IncludeDatabase" -ForegroundColor DarkGray
}
Write-Host "Para subir novamente: $repoRoot\\run-local.ps1" -ForegroundColor DarkGray
