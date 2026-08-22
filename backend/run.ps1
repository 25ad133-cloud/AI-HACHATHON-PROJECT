$binDir = Join-Path $PSScriptRoot "bin"
$libDir = Join-Path $PSScriptRoot "lib"

$classpath = "$binDir;${libDir}/*"

Write-Host "Running Main..."
java -cp "$classpath" Main
