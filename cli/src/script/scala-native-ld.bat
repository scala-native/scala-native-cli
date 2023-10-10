@ECHO OFF
SetLocal 

set SCALA_BIN_VER=@SCALA_BIN_VER@
set SCALANATIVE_VER=@SCALANATIVE_VER@
set SCALANATIVE_BIN_VER=@SCALANATIVE_BIN_VER@
set SCALALIB_2_13_FOR_3_VER=@SCALALIB_2_13_FOR_3_VER@

for /F "tokens=5" %%i in (' scala -version 2^>^&1 ^| findstr /i scala ^| findstr /o [0-9]*\.[0-9]*\.[0-9]*') do set SCALA_VER=%%i

set BASE=%~dp0\..
set SUFFIX=_native%SCALANATIVE_BIN_VER%_%SCALA_BIN_VER%-%SCALANATIVE_VER%.jar

set CLILIB=%BASE%\lib\scala-native-cli-assembly_%SCALA_BIN_VER%-%SCALANATIVE_VER%.jar
set NATIVELIB=

for %%i in (nativelib clib posixlib windowslib auxlib javalib) do (
  set NATIVELIB=!NATIVELIB! %BASE%\lib\%%i%SUFFIX%
)

set NATIVELIB=%NATIVELIB% %BASE%\lib\javalib-intf-%SCALANATIVE_VER%.jar

if "%SCALA_BIN_VER%" == "3" (
  set NATIVELIB=%NATIVELIB% %BASE%\lib\scalalib_native%SCALANATIVE_BIN_VER%_2.13-%SCALALIB_2_13_FOR_3_VER%+%SCALANATIVE_VER%.jar %BASE%\lib\scala3lib%SUFFIX%
)

scala -classpath %CLILIB% scala.scalanative.cli.ScalaNativeLd %* %NATIVELIB%
