# fetch-ck.ps1
# Downloads the CK Tool executable JAR (with dependencies) used by the
# Software Metrics Analysis module (Part A).
#
# Trusted source: Maven Central (repo1.maven.org), groupId com.github.mauricioaniche.
#   https://repo1.maven.org/maven2/com/github/mauricioaniche/ck/
# The released artifact is the official CK build from https://github.com/mauricioaniche/ck
# published under the Apache 2.0 license. The SHA1 published by Maven Central is
# verified after the download before the file is kept.
#
# Usage (from the project root):
#   powershell -ExecutionPolicy Bypass -File tools/fetch-ck.ps1
#   powershell -ExecutionPolicy Bypass -File tools/fetch-ck.ps1 -Version 0.7.0
#
# The JAR is placed next to this script (tools/) which is the default location the
# application looks for when neither -Dck.jar nor the CK_JAR environment variable is set.

param(
    [string]$Version = "0.7.0",
    [string]$OutputDirectory = $PSScriptRoot
)

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

$fileName = "ck-$Version-jar-with-dependencies.jar"
$baseUrl  = "https://repo1.maven.org/maven2/com/github/mauricioaniche/ck/$Version"
$jarUrl   = "$baseUrl/$fileName"
$sha1Url  = "$jarUrl.sha1"

if ([string]::IsNullOrWhiteSpace($OutputDirectory)) {
    $OutputDirectory = (Get-Location).Path
}

New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null
$target = Join-Path $OutputDirectory $fileName

Write-Host "Downloading CK $Version from Maven Central"
Write-Host "  $jarUrl"
Invoke-WebRequest -UseBasicParsing -Uri $jarUrl -OutFile $target

# Verify the published SHA1 before trusting the downloaded binary.
$expected = (Invoke-WebRequest -UseBasicParsing -Uri $sha1Url).Content.Trim()
$actual = (Get-FileHash -Algorithm SHA1 -Path $target).Hash.ToLower()
if ($actual -ne $expected.ToLower()) {
    Remove-Item -Force $target
    throw "SHA1 verification failed for $fileName. Expected $expected but got $actual. File removed."
}

Write-Host "SHA1 verified: $actual"
Write-Host "Saved to: $target"