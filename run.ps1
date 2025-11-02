# run.ps1 — compila y ejecuta ExternalMavic con Webots Java

$ErrorActionPreference = "Stop"

# 1) Localiza Webots
$wb = ${env:WEBOTS_HOME}
if (-not $wb) { throw "WEBOTS_HOME no está definido. Abre Webots una vez o define la variable de entorno." }

# 2) Rutas de librerías Java de Webots
$ctrlJar = Join-Path $wb "lib\controller\java\Controller.jar"
$vehJar  = Join-Path $wb "lib\controller\java\vehicle.jar"
$javaDllDir = Join-Path $wb "lib\controller\java"
$ctrlDir    = Join-Path $wb "lib\controller"
$libDir     = Join-Path $wb "lib"

# 3) DLLs de MinGW (dependencias nativas del JavaController.dll)
$mingwBin    = Join-Path $wb "msys64\mingw64\bin"
$mingwBinCpp = Join-Path $wb "msys64\mingw64\bin\cpp"

# 4) Comprobaciones mínimas
foreach ($p in @($ctrlJar,$vehJar,$javaDllDir,$ctrlDir,$libDir,$mingwBin,$mingwBinCpp)) {
  if (-not (Test-Path $p)) { throw "Ruta no encontrada: $p" }
}

# 5) Compilar
$src = "src\main\java\es\upm\sarl\controller\ExternalMavic.java"
if (-not (Test-Path $src)) { throw "No existe el fuente: $src" }

New-Item -ItemType Directory -Force -Path build | Out-Null
$jarClasspath = "$ctrlJar;$vehJar"
Write-Host "Compilando $src ..." -ForegroundColor Cyan
& javac -encoding UTF-8 -cp "$jarClasspath" -d build "$src"

# 6) Preparar classpath y paths nativos
$cp = "build;$jarClasspath"
$nat = "$javaDllDir;$ctrlDir;$libDir;$mingwBin;$mingwBinCpp"

# IMPORTANTÍSIMO: extender PATH para que la JVM resuelva dependencias transitivas (libstdc++, libgcc, winpthread)
$env:PATH = "$nat;$($env:PATH)"

# 7) Ejecutar
$main = "es.upm.sarl.controller.ExternalMavic"
Write-Host "Ejecutando $main ..." -ForegroundColor Green
Write-Host "java -Djava.library.path=$nat -cp $cp $main" -ForegroundColor DarkGray
& java "-Djava.library.path=$nat" -cp "$cp" $main
