@ECHO OFF
set SCALA_BIN_VER=@SCALA_BIN_VER@
set SCALANATIVE_VER=@SCALANATIVE_VER@

for /F "tokens=5" %%i in (' scala -version 2^>^&1 1^>nul ') do set SCALA_VER=%%i

if NOT "%SCALA_VER:~0,4%" == "%SCALA_BIN_VER%" (
  echo "This bundle of Scala Native CLI is for %SCALA_BIN_VER%. Your scala version is %SCALA_VER%!" 1>&2
) else (
  set PLUGIN=%~dp0\..\lib\nscplugin_%SCALA_VER%-%SCALANATIVE_VER%.jar
  set CLASSPATH="."
  set libs=nativelib clib posixlib windowslib auxlib javalib scalalib
  (for %%lib in (%libs%) do ( 
    set CLASSPATH="%CLASSPATH%:%~dp0\..\lib\%%lib_%SCALA_BIN_VER%-%SCALANATIVE_VER%.jar"
  ))

  scalac -classpath "%CLASSPATH%" "-Xplugin:%PLUGIN%" %*
)
