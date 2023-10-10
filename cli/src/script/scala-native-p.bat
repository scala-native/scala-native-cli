@ECHO OFF
SetLocal 

set SCALA_BIN_VER=@SCALA_BIN_VER@
set SCALANATIVE_VER=@SCALANATIVE_VER@

set BASE=%~dp0\..\lib
set CLILIB="%BASE%\scala-native-cli-assembly_%SCALA_BIN_VER%-%SCALANATIVE_VER%.jar"

scala -classpath %CLILIB% scala.scalanative.cli.ScalaNativeP  %*
