# script_ejecucion_sarl_webots.ps1 — compila el proyecto y ejecuta MainRunSarlWebots con Webots Java
$ErrorActionPreference = "Stop"

# ------------------------------------------------------------
# 1) Localiza Webots
# ------------------------------------------------------------
$wb = ${env:WEBOTS_HOME}
if (-not $wb) {
  throw "WEBOTS_HOME no está definido. Abre Webots una vez o define la variable de entorno."
}

# ------------------------------------------------------------
# 2) Rutas de librerías Java de Webots
# ------------------------------------------------------------
$ctrlJar    = Join-Path $wb "lib\controller\java\Controller.jar"
$vehJar     = Join-Path $wb "lib\controller\java\vehicle.jar"
$javaDllDir = Join-Path $wb "lib\controller\java"
$ctrlDir    = Join-Path $wb "lib\controller"
$libDir     = Join-Path $wb "lib"

# ------------------------------------------------------------
# 3) Rutas a las DLLs de MinGW
# ------------------------------------------------------------
$mingwBin    = Join-Path $wb "msys64\mingw64\bin"
$mingwBinCpp = Join-Path $wb "msys64\mingw64\bin\cpp"

# ------------------------------------------------------------
# 4) Comprobaciones mínimas
# ------------------------------------------------------------
foreach ($p in @($ctrlJar,$vehJar,$javaDllDir,$ctrlDir,$libDir,$mingwBin,$mingwBinCpp)) {
  if (-not (Test-Path $p)) { throw "Ruta no encontrada: $p" }
}

# ------------------------------------------------------------
# 5) Parámetros de ejecución
# ------------------------------------------------------------
$main = "es.upm.sarl.main.MainRunSarlWebots"
$sarl = "examples\11_modo_sincrono_sarl_webots_v0.sarl"

if ($args.Length -ge 1) {
  $sarl = $args[0]
}

if (-not (Test-Path $sarl)) {
  throw "No existe el script SARL: $sarl"
}

# ------------------------------------------------------------
# 6) Compilar TODO el proyecto con Maven
# ------------------------------------------------------------
Write-Host "Compilando proyecto con Maven..." -ForegroundColor Cyan
& mvn clean compile

# ------------------------------------------------------------
# 7) Generar classpath de dependencias Maven (ANTLR, etc.)
# ------------------------------------------------------------
Write-Host "Generando classpath de dependencias Maven..." -ForegroundColor Cyan
& mvn -q dependency:build-classpath "-Dmdep.outputFile=target\maven_classpath.txt"

if (-not (Test-Path "target\maven_classpath.txt")) {
  throw "No se pudo generar target\maven_classpath.txt"
}

$mvnCp = Get-Content "target\maven_classpath.txt" -Raw
$mvnCp = $mvnCp.Trim()

# ------------------------------------------------------------
# 8) Preparar classpath y paths nativos
# ------------------------------------------------------------
$jarClasspath = "$ctrlJar;$vehJar"
$cp  = "target\classes;$jarClasspath;$mvnCp"
$nat = "$javaDllDir;$ctrlDir;$libDir;$mingwBin;$mingwBinCpp"

# extender PATH para que la JVM resuelva dependencias transitivas
$env:PATH = "$nat;$($env:PATH)"

# ------------------------------------------------------------
# 9) Ejecutar el main SARL sobre Webots
# ------------------------------------------------------------
Write-Host "Ejecutando $main con $sarl ..." -ForegroundColor Green
Write-Host "java -Djava.library.path=$nat -cp $cp $main $sarl" -ForegroundColor DarkGray

& java "-Djava.library.path=$nat" -cp "$cp" $main $sarl