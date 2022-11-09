#/bin/bash

Version=0.4.8
Scala3Version=3.1.3
ReleaseDir="${PWD}/release"
TargetDir="${PWD}/cli/target"

mkdir -p ${ReleaseDir}
cd $ReleaseDir

sbt "clean;+cli/cliPack"

for ScalaBinVer in 2.12 2.13 3; do
  ScalaVersion=$ScalaBinVer
  if [ "$ScalaBinVer" = "3" ]; then
    ScalaVersion="${Scala3Version}"
  fi
  cd "${TargetDir}/scala-${ScalaVersion}/pack"

  dirName="scala-native-cli_${ScalaBinVer}-${Version}"
  tarName="$dirName.tgz"
  zipName="$dirName.zip"
  tar cfz $tarName $dirName
  zip -r $zipName -r $dirName/
  mv $tarName $zipName $ReleaseDir
done
