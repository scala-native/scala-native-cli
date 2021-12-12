@ECHO OFF
SetLocal

set SCALA_BIN_VER=@SCALA_BIN_VER@
set SCALANATIVE_VER=@SCALANATIVE_VER@
set SCALANATIVE_BIN_VER=@SCALANATIVE_BIN_VER@
echo %*
for /F "tokens=5" %%i in (' scala -version 2^>^&1 1^>nul ') do set SCALA_VER=%%i

if NOT "%SCALA_VER:~0,4%" == "%SCALA_BIN_VER%" (
  echo "This bundle of Scala Native CLI is for %SCALA_BIN_VER%. Your scala version is %SCALA_VER%!" 1>&2
  EXIT 1
)

set BASE=%~dp0\..\lib
set SUFFIX=_native%SCALANATIVE_BIN_VER%_%SCALA_BIN_VER%-%SCALANATIVE_VER%.jar

set PLUGIN=%BASE%\nscplugin_%SCALA_VER%-%SCALANATIVE_VER%.jar
set NATIVELIB=%BASE%\nativelib%SUFFIX%;%BASE%\clib%SUFFIX%;%BASE%\posixlib%SUFFIX%;%BASE%\windowslib%SUFFIX%;%BASE%\auxlib%SUFFIX%;%BASE%\javalib%SUFFIX%;%BASE%\scalalib%SUFFIX%
  
scalac -classpath .;%NATIVELIB% -Xplugin:%PLUGIN% "%*"
