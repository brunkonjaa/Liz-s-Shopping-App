param(
    [switch]$CreateSigningIdentity
)

$ErrorActionPreference = 'Stop'
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$identityRoot = Join-Path $env:USERPROFILE '.lizshopping'
$keystorePath = Join-Path $identityRoot 'release-signing.p12'
$protectedPasswordPath = Join-Path $identityRoot 'release-password.dpapi'
$keyAlias = 'lizshopping-release'

if (-not (Test-Path $identityRoot)) {
    New-Item -ItemType Directory -Path $identityRoot | Out-Null
    $account = "$($env:USERDOMAIN)\$($env:USERNAME)"
    & icacls.exe $identityRoot /inheritance:r /grant:r "$account`:(OI)(CI)F" 'SYSTEM:(OI)(CI)F' | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Could not restrict signing-directory access.' }
}

if (-not (Test-Path $keystorePath) -or -not (Test-Path $protectedPasswordPath)) {
    if (-not $CreateSigningIdentity) {
        throw "No local release identity exists. Create one with: .\scripts\build-release.ps1 -CreateSigningIdentity"
    }
    if ((Test-Path $keystorePath) -or (Test-Path $protectedPasswordPath)) {
        throw 'A partial signing identity exists. Preserve it and inspect the files before proceeding.'
    }

    $bytes = New-Object byte[] 32
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    $password = [Convert]::ToBase64String($bytes)
    [Array]::Clear($bytes, 0, $bytes.Length)

    $env:LIZ_RELEASE_STORE_PASSWORD = $password
    $env:LIZ_RELEASE_KEY_PASSWORD = $password
    $keytool = (Get-Command keytool.exe -ErrorAction Stop).Source
    & $keytool -genkeypair -storetype PKCS12 -keystore $keystorePath -storepass:env LIZ_RELEASE_STORE_PASSWORD -keypass:env LIZ_RELEASE_KEY_PASSWORD -alias $keyAlias -keyalg RSA -keysize 3072 -validity 10000 -dname 'CN=Liz Shopping List Release, OU=Android, O=Liz Shopping List, C=IE' -noprompt
    if ($LASTEXITCODE -ne 0) { throw 'keytool could not create the release signing identity.' }

    $securePassword = ConvertTo-SecureString -String $password -AsPlainText -Force
    ConvertFrom-SecureString -SecureString $securePassword | Set-Content -LiteralPath $protectedPasswordPath -Encoding Ascii
    $password = $null
    $securePassword.Dispose()
}

$encryptedPassword = (Get-Content -LiteralPath $protectedPasswordPath -Raw).Trim()
$secureStoredPassword = ConvertTo-SecureString -String $encryptedPassword
$passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureStoredPassword)
try {
    $password = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
    $env:LIZ_RELEASE_STORE_FILE = $keystorePath
    $env:LIZ_RELEASE_STORE_PASSWORD = $password
    $env:LIZ_RELEASE_KEY_ALIAS = $keyAlias
    $env:LIZ_RELEASE_KEY_PASSWORD = $password
    Push-Location $repoRoot
    try {
        & (Join-Path $repoRoot 'gradlew.bat') clean testDebugUnitTest lintDebug assembleRelease --console=plain
        if ($LASTEXITCODE -ne 0) { throw "Gradle release build failed with exit code $LASTEXITCODE." }
    } finally {
        Pop-Location
    }
} finally {
    $env:LIZ_RELEASE_STORE_FILE = $null
    $env:LIZ_RELEASE_STORE_PASSWORD = $null
    $env:LIZ_RELEASE_KEY_ALIAS = $null
    $env:LIZ_RELEASE_KEY_PASSWORD = $null
    if ($passwordPointer -ne [IntPtr]::Zero) { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer) }
    $secureStoredPassword.Dispose()
}
