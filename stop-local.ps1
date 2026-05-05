param(
    [switch]$BackendOnly,
    [switch]$FrontendOnly
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSCommandPath

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

function Get-ChildProcessIds {
    param([int]$ParentProcessId)

    $children = @(Get-CimInstance Win32_Process -Filter "ParentProcessId = $ParentProcessId" -ErrorAction SilentlyContinue)
    $descendants = @()

    foreach ($child in $children) {
        $descendants += $child.ProcessId
        $descendants += Get-ChildProcessIds -ParentProcessId $child.ProcessId
    }

    return @($descendants | Select-Object -Unique)
}

function Stop-ProcessTree {
    param([int]$ProcessId)

    $processIds = @((Get-ChildProcessIds -ParentProcessId $ProcessId) + $ProcessId |
        Select-Object -Unique |
        Sort-Object -Descending)

    foreach ($id in $processIds) {
        $process = Get-Process -Id $id -ErrorAction SilentlyContinue
        if (-not $process) {
            continue
        }

        Write-Host "Encerrando $($process.ProcessName) (PID $id)..." -ForegroundColor Yellow
        Stop-Process -Id $id -Force -ErrorAction SilentlyContinue
    }
}

function Get-LauncherProcessIds {
    param([string]$Marker)

    return @(Get-CimInstance Win32_Process -ErrorAction SilentlyContinue |
        Where-Object {
            ($_.Name -ieq "powershell.exe" -or $_.Name -ieq "pwsh.exe") -and
            $_.CommandLine -like "*$Marker*"
        } |
        Select-Object -ExpandProperty ProcessId -Unique)
}

function Stop-Target {
    param(
        [string]$Name,
        [int]$Port,
        [string]$LauncherMarker
    )

    $stoppedSomething = $false

    foreach ($launcherPid in (Get-LauncherProcessIds -Marker $LauncherMarker)) {
        if (Get-Process -Id $launcherPid -ErrorAction SilentlyContinue) {
            Write-Host "Fechando janela do $Name..." -ForegroundColor Yellow
            Stop-ProcessTree -ProcessId $launcherPid
            $stoppedSomething = $true
        }
    }

    foreach ($processId in (Get-ListeningProcessIds -Port $Port)) {
        $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
        if (-not $process) {
            continue
        }

        Write-Host "Encerrando $($process.ProcessName) na porta $Port..." -ForegroundColor Yellow
        Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
        $stoppedSomething = $true
    }

    if (Test-PortInUse -Port $Port) {
        Wait-PortReleased -Port $Port
    }

    if ($stoppedSomething) {
        Write-Host "$Name encerrado." -ForegroundColor Green
    }
    else {
        Write-Host "$Name nao estava em execucao." -ForegroundColor DarkGray
    }
}

if ($BackendOnly -and $FrontendOnly) {
    throw "Use apenas um dos parametros: -BackendOnly ou -FrontendOnly."
}

if (-not $FrontendOnly) {
    Stop-Target -Name "backend" -Port 8080 -LauncherMarker "Reserva+ Backend"
}

if (-not $BackendOnly) {
    Stop-Target -Name "frontend" -Port 4200 -LauncherMarker "Reserva+ Frontend"
}

Write-Host ""
Write-Host "MySQL do XAMPP nao foi afetado." -ForegroundColor DarkGray
Write-Host "Para subir novamente: $repoRoot\\run-local.ps1" -ForegroundColor DarkGray
