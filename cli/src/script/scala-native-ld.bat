@ECHO OFF
set SCALA_BIN_VER=@SCALA_BIN_VER@
set SCALANATIVE_VER=@SCALANATIVE_VER@
set SCALANATIVE_BIN_VER=@SCALANATIVE_BIN_VER@

set CLILIB="%~dp0\..\lib\scala-native-cli-assembly_%SCALA_BIN_VER%-%SCALANATIVE_VER%.jar"
set NATIVELIB=""
set libs=nativelib clib posixlib windowslib auxlib javalib scalalib
(for %%lib in (%libs%) do ( 
   set NATIVELIB="%NATIVELIB% %~dp0\..\lib\%%lib_%SCALA_BIN_VER%-%SCALANATIVE_VER%.jar"
))

scala -classpath %$CLILIB% scala.scalanative.cli.ScalaNativeCli %* %NATIVELIB%
