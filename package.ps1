$ErrorActionPreference = 'Stop'

# MazeRunner Windows release packager
# Requires JDK 21 or newer (javac, jar, and jpackage) only on the machine performing the build.
# The resulting application includes its own Java runtime, so end users do not need
# to install Java, Maven, or the JDK.

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$Target = Join-Path $Root 'target'
$Classes = Join-Path $Target 'classes'
$Jar = Join-Path $Target 'maze-runner.jar'
$Dist = Join-Path $Root 'dist'
$Input = Join-Path $Dist 'input'
$Output = Join-Path $Dist 'windows'
$AppName = 'MazeRunner'
$MainClass = 'com.mazerunner.Main'

function Require-Command([string] $Name) {
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Required command '$Name' was not found. Install a complete JDK 21 or newer before building the release."
    }
}

Require-Command 'javac'
Require-Command 'jar'
Require-Command 'jpackage'

if (-not (Test-Path $Classes)) {
    New-Item -ItemType Directory -Force -Path $Classes | Out-Null
}

Write-Host 'Compiling MazeRunner source...'
$Sources = Get-ChildItem -Path (Join-Path $Root 'src/main') -Recurse -Filter '*.java' | Select-Object -ExpandProperty FullName
if (-not $Sources) {
    throw 'No Java source files were found under src/main.'
}
& javac -encoding UTF-8 -d $Classes $Sources
if ($LASTEXITCODE -ne 0) {
    throw 'Java compilation failed.'
}

$Resources = Join-Path $Root 'src/main/resources'
if (Test-Path $Resources) {
    Copy-Item -Path (Join-Path $Resources '*') -Destination $Classes -Recurse -Force -ErrorAction SilentlyContinue
}

Write-Host 'Creating application JAR...'
if (Test-Path $Jar) { Remove-Item $Jar -Force }
& jar --create --file $Jar --main-class $MainClass -C $Classes .
if ($LASTEXITCODE -ne 0) {
    throw 'JAR creation failed.'
}

if (Test-Path $Dist) { Remove-Item $Dist -Recurse -Force }
New-Item -ItemType Directory -Force -Path $Input, $Output | Out-Null
Copy-Item $Jar (Join-Path $Input 'maze-runner.jar')

Write-Host 'Creating a self-contained Windows application image...'
& jpackage `
    --type app-image `
    --name $AppName `
    --input $Input `
    --main-jar 'maze-runner.jar' `
    --main-class $MainClass `
    --dest $Output `
    --app-version '1.0.0' `
    --vendor 'MazeRunner Project' `
    --description 'Maze Runner: Kinetic Gravity Engine'
if ($LASTEXITCODE -ne 0) {
    throw 'jpackage app-image creation failed.'
}

$AppImage = Join-Path $Output $AppName
$Zip = Join-Path $Dist 'MazeRunner-Windows.zip'
if (Test-Path $Zip) { Remove-Item $Zip -Force }
Compress-Archive -Path $AppImage -DestinationPath $Zip -CompressionLevel Optimal

Write-Host ''
Write-Host 'Release created successfully:' -ForegroundColor Green
Write-Host "  $Zip"
Write-Host "  $AppImage"
Write-Host ''
Write-Host 'The ZIP contains a bundled Java runtime. End users only need to extract it and run MazeRunner.exe.'
Write-Host 'For a signed installer, use the GitHub Actions release workflow or add WiX and switch jpackage to --type exe.'
