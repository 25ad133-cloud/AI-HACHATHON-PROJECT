$libDir = Join-Path $PSScriptRoot "lib"
if (!(Test-Path $libDir)) {
    New-Item -ItemType Directory -Force -Path $libDir
}

$dependencies = @{
    "sqlite-jdbc-3.42.0.0.jar" = "https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.42.0.0/sqlite-jdbc-3.42.0.0.jar"
    "slf4j-api-1.7.36.jar"     = "https://repo1.maven.org/maven2/org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar"
    "slf4j-simple-1.7.36.jar"  = "https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/1.7.36/slf4j-simple-1.7.36.jar"
    "core-3.5.1.jar"           = "https://repo1.maven.org/maven2/com/google/zxing/core/3.5.1/core-3.5.1.jar"
    "javase-3.5.1.jar"         = "https://repo1.maven.org/maven2/com/google/zxing/javase/3.5.1/javase-3.5.1.jar"
    "gson-2.10.1.jar"          = "https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar"
    "jbcrypt-0.4.jar"          = "https://repo1.maven.org/maven2/org/mindrot/jbcrypt/0.4/jbcrypt-0.4.jar"
}

foreach ($fileName in $dependencies.Keys) {
    $targetPath = Join-Path $libDir $fileName
    $url = $dependencies[$fileName]
    if (Test-Path $targetPath) {
        Write-Host "Dependency $fileName already exists. Skipping."
    } else {
        Write-Host "Downloading $fileName from $url ..."
        try {
            [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
            Invoke-WebRequest -Uri $url -OutFile $targetPath -UseBasicParsing
            Write-Host "Successfully downloaded $fileName."
        } catch {
            Write-Error "Failed to download $fileName. Error: $_"
            exit 1
        }
    }
}

Write-Host "All dependencies downloaded successfully."
