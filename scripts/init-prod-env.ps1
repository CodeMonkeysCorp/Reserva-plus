param(
    [string]$OutputPath = ".env.prod",
    [string]$BackendImage = "reserva-plus-backend:ci",
    [string]$FrontendImage = "reserva-plus-frontend:ci",
    [string]$AdminEmail = "admin@reserva.local",
    [string]$AdminName = "Administrador",
    [switch]$Force
)

$ErrorActionPreference = "Stop"

if ((Test-Path $OutputPath) -and -not $Force) {
    throw "Arquivo $OutputPath ja existe. Use -Force para recriar."
}

function New-HexSecret {
    param([int]$ByteCount = 32)

    $bytes = New-Object byte[] $ByteCount
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $rng.GetBytes($bytes)
        return -join ($bytes | ForEach-Object { $_.ToString("x2") })
    }
    finally {
        $rng.Dispose()
    }
}

$mysqlPassword = New-HexSecret 24
$mysqlRootPassword = New-HexSecret 24
$jwtSecret = New-HexSecret 48
$adminPassword = New-HexSecret 18

$content = @"
# Production Compose
FRONTEND_PORT=80
MYSQL_DATABASE=reserva_plus
MYSQL_USER=reserva_app
MYSQL_PASSWORD=$mysqlPassword
MYSQL_ROOT_PASSWORD=$mysqlRootPassword

# Images published by CD
BACKEND_IMAGE=$BackendImage
FRONTEND_IMAGE=$FrontendImage

# Backend / Spring Boot
JWT_SECRET=$jwtSecret
JWT_EXPIRATION_MS=86400000
APP_ADMIN_NAME=$AdminName
APP_ADMIN_EMAIL=$AdminEmail
APP_ADMIN_PASSWORD=$adminPassword
RESERVA_CONCLUSAO_CRON=0 0 * * * *
RESERVA_CONCLUSAO_ZONE=America/Sao_Paulo

# Cloudflare R2
APP_STORAGE_R2_ENABLED=false
APP_STORAGE_R2_ACCOUNT_ID=
APP_STORAGE_R2_ENDPOINT=
APP_STORAGE_R2_BUCKET=
APP_STORAGE_R2_ACCESS_KEY_ID=
APP_STORAGE_R2_SECRET_ACCESS_KEY=
APP_STORAGE_R2_PUBLIC_BASE_URL=
APP_STORAGE_R2_OBJECT_KEY_PREFIX=reserva-plus
APP_STORAGE_R2_MAX_FILE_SIZE=10MB
"@

Set-Content -LiteralPath $OutputPath -Value $content -Encoding UTF8

Write-Host "Arquivo $OutputPath criado com segredos fortes."
Write-Host "Guarde a senha inicial do admin em um cofre antes do primeiro deploy."
