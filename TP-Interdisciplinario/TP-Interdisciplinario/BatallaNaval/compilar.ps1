# Compila el proyecto y genera dist\BatallaNaval.jar (Windows)
Remove-Item -Recurse -Force out, dist -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force out, dist | Out-Null
$fuentes = Get-ChildItem -Recurse src -Filter *.java | ForEach-Object { $_.FullName }
javac --release 17 -encoding UTF-8 -d out $fuentes
jar cf dist\BatallaNaval.jar -C out .
Write-Host "Listo: dist\BatallaNaval.jar"
