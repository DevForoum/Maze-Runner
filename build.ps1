$files = Get-ChildItem -Path src/main -Recurse -Filter *.java | ForEach-Object { $_.FullName }
javac -d target/classes $files
if ($LASTEXITCODE -eq 0) {
    Copy-Item -Recurse -Force src/main/resources/* target/classes/ -ErrorAction SilentlyContinue
    Write-Host "BUILD SUCCESS"
} else {
    Write-Host "BUILD FAILED"
}
