@ECHO OFF
SetLocal

set SCALA_BIN_VER=@SCALA_BIN_VER@
set SCALANATIVE_VER=@SCALANATIVE_VER@
set SCALANATIVE_BIN_VER=@SCALANATIVE_BIN_VER@
set SCALALIB_2_13_FOR_3_VER=@SCALALIB_2_13_FOR_3_VER@

for /F "tokens=5" %%i in (' scala -version 2^>^&1 1^>nul ') do set SCALA_VER=%%i

if NOT "%SCALA_BIN_VER%" == "3" (
  if NOT "%SCALA_VER:~0,4%" == "%SCALA_BIN_VER%" (
    echo "This bundle of Scala Native CLI is for %SCALA_BIN_VER%. Your scala version is %SCALA_VER%!" 1>&2
    EXIT 1
  )
)

set BASE=%~dp0\..\lib
set PLUGIN="%BASE%\nscplugin_%SCALA_VER%-%SCALANATIVE_VER%.jar"

set NATIVELIB=.
set NATIVELIB=%NATIVELIB%;"%BASE%\nativelib%SUFFIX%"
set NATIVELIB=%NATIVELIB%;"%BASE%\clib%SUFFIX%"
set NATIVELIB=%NATIVELIB%;"%BASE%\posixlib%SUFFIX%"
set NATIVELIB=%NATIVELIB%;"%BASE%\windowslib%SUFFIX%"
set NATIVELIB=%NATIVELIB%;"%BASE%\auxlib%SUFFIX%"
set NATIVELIB=%NATIVELIB%;"%BASE%\javalib%SUFFIX%"
set NATIVELIB=%NATIVELIB%;"%BASE%\javalib-intf-%SCALANATIVE_VER%.jar"

if "%SCALA_BIN_VER%" == "3" (
  set NATIVELIB=%NATIVELIB%;"%BASE%\scalalib_native%SCALANATIVE_BIN_VER%_2.13-%SCALALIB_2_13_FOR_3_VER%+%SCALANATIVE_VER%.jar"
  set NATIVELIB=%NATIVELIB%;"%BASE%\scala3lib_native%SCALANATIVE_BIN_VER%_3-%SCALA_VER%+%SCALANATIVE_VER%.jar"
) else (
  set NATIVELIB=%NATIVELIB%;"%BASE%\scalalib_native%SCALANATIVE_BIN_VER%_%SCALA_BIN_VER%-%SCALA_VER%+%SCALANATIVE_VER%.jar"
)

scalac -classpath %NATIVELIB% -Xplugin:%PLUGIN% "%*"
