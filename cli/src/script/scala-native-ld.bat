@ECHO OFF
SetLocal 

set SCALA_BIN_VER=@SCALA_BIN_VER@
set SCALANATIVE_VER=@SCALANATIVE_VER@
set SCALANATIVE_BIN_VER=@SCALANATIVE_BIN_VER@

set BASE=%~dp0\..\lib
set SUFFIX=_native%SCALANATIVE_BIN_VER%_%SCALA_BIN_VER%-%SCALANATIVE_VER%.jar

set CLILIB=%BASE%\scala-native-cli-assembly_%SCALA_BIN_VER%-%SCALANATIVE_VER%.jar
set PLUGIN=%BASE%\nscplugin_%SCALA_VER%-%SCALANATIVE_VER%.jar
set NATIVELIB=%BASE%\nativelib%SUFFIX%;%BASE%\clib%SUFFIX%;%BASE%\posixlib%SUFFIX%;%BASE%\windowslib%SUFFIX%;%BASE%\auxlib%SUFFIX%;%BASE%\javalib%SUFFIX%;%BASE%\scalalib%SUFFIX%

scala -classpath %CLILIB% scala.scalanative.cli.ScalaNativeCli  %* %NATIVELIB%