$ErrorActionPreference = 'Stop'
if (-not $env:JAVA_HOME) {
    $candidatos = @('C:/Program Files/Java/jdk-21') + @(Get-ChildItem (Join-Path $env:USERPROFILE '.gradle/jdks') -Directory -ErrorAction SilentlyContinue | Where-Object { $_.Name -match '21' } | Select-Object -ExpandProperty FullName)
    $jdk = $candidatos | Where-Object { Test-Path (Join-Path $_ 'bin/java.exe') } | Select-Object -First 1
    if ($jdk) { $env:JAVA_HOME = $jdk }
}
Push-Location $PSScriptRoot
try { & ./gradlew.bat bootRun; if ($LASTEXITCODE -ne 0) { throw 'No se pudo iniciar AgroAPI' } }
finally { Pop-Location }
