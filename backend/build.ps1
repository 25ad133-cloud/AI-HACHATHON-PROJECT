$binDir = Join-Path $PSScriptRoot "bin"
$libDir = Join-Path $PSScriptRoot "lib"
$srcDir = Join-Path $PSScriptRoot "src"

if (!(Test-Path $binDir)) {
    New-Item -ItemType Directory -Force -Path $binDir
}

# Find all Java source files recursively
$javaFiles = Get-ChildItem -Path $srcDir -Filter *.java -Recurse | Select-Object -ExpandProperty FullName

if (!$javaFiles) {
    Write-Host "No Java files found to compile."
    exit 0
}

Write-Host "Compiling Java files..."
# Build the classpath matching all jars in lib directory
$classpath = "$binDir;${libDir}/*"

$compileCmd = "javac -d `"$binDir`" -cp `"$classpath`" " + ($javaFiles | ForEach-Object { "`"$_`"" }) -join " "
Invoke-Expression $compileCmd

if ($LASTEXITCODE -eq 0) {
    Write-Host "Compilation successful."
} else {
    Write-Error "Compilation failed."
    exit 1
}
