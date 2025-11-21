# script_ejecucion.ps1 — compila y ejecuta Controlador con Webots Java
$ErrorActionPreference = "Stop" # Indica que cualquier error en el script debe detener la ejecución inmediatamente.

# 1) Localiza Webots
#Webots instala una variable del sistema llamada WEBOTS_HOME, que apunta a la carpeta principal de instalación (normalmente C:\Program Files\Webots)
$wb = ${env:WEBOTS_HOME} # Esta linea obtiene esa ruta.
if (-not $wb) { throw "WEBOTS_HOME no está definido. Abre Webots una vez o define la variable de entorno." } ##Si no existe, se lanza un error explicando al usuario cómo solucionarlo

# 2) Rutas de librerías Java de Webots
# Aquí construimos las rutas a las dependencias oficiales de Webots para Java:
$ctrlJar     = Join-Path $wb "lib\controller\java\Controller.jar" # contiene la API Java de los controladores
$vehJar      = Join-Path $wb "lib\controller\java\vehicle.jar" # incluye clases de soporte para vehículos   
$javaDllDir  = Join-Path $wb "lib\controller\java" # librerías nativas necesarias para que la JVM encuentre las dependencias C/C++ que usa Webots internamente
$ctrlDir     = Join-Path $wb "lib\controller"
$libDir      = Join-Path $wb "lib"

# 3) rutas a las DLLs de MinGW. Estas DLLs deben estar en el PATH cuando ejecutas un controlador en Java
# Webots incluye su propio compilador MinGW (versión de GCC) con DLLs requeridas por el motor físico
$mingwBin     = Join-Path $wb "msys64\mingw64\bin"
$mingwBinCpp  = Join-Path $wb "msys64\mingw64\bin\cpp"   

# 4) Comprobaciones mínimas 
# Antes de compilar, verificamos que todas las rutas necesarias existen
foreach ($p in @($ctrlJar,$vehJar,$javaDllDir,$ctrlDir,$libDir,$mingwBin,$mingwBinCpp)) {
  if (-not (Test-Path $p)) { throw "Ruta no encontrada: $p" }
}

# 5) Compilar el controlador
$src = "src\main\java\es\upm\sarl\controller\Controlador.java"
if (-not (Test-Path $src)) { throw "No existe el fuente: $src" } # Confirma que el archivo Java existe

New-Item -ItemType Directory -Force -Path build | Out-Null # Crea la carpeta build si no existe
$jarClasspath = "$ctrlJar;$vehJar" # Prepara el classpath con los JAR de Webots
Write-Host "Compilando $src ..." -ForegroundColor Cyan # Compila con javac, enviando los .class a la carpeta build/
& javac -encoding UTF-8 -cp "$jarClasspath" -d build "$src"

# 6) Preparar classpath y paths nativos (igual que el script que te funcionaba)
$cp  = "build;$jarClasspath" # $cp contiene todo lo que Java necesita para ejecutar la clase.
$nat = "$javaDllDir;$ctrlDir;$libDir;$mingwBin;$mingwBinCpp" # $nat contiene todas las rutas de DLLs que Webots utiliza

# extender PATH para que la JVM resuelva dependencias transitivas
$env:PATH = "$nat;$($env:PATH)" # añade nat al PATH

# 7) Ejecutar el controlador 
$main = "es.upm.sarl.controller.Controlador" # main es el nombre completo del paquete + clase principal
Write-Host "Ejecutando $main ..." -ForegroundColor Green
Write-Host "java -Djava.library.path=$nat -cp $cp $main" -ForegroundColor DarkGray # -cp para especificar el classpath
& java "-Djava.library.path=$nat" -cp "$cp" $main # -Djava.library.path para indicar dónde están las DLLs necesarias
