scalaVersion := "2.12.15"

val scalaNativeVersion =
  settingKey[String]("Version of Scala Native linker to use")

inThisBuild(
  Def.settings(
    organization := "org.scala-native",
    scalaNativeVersion := "0.4.0",
    version := scalaNativeVersion.value
  )
)

lazy val cli = project
  .in(file("cli"))
  .enablePlugins(BuildInfoPlugin, ScriptedPlugin)
  .settings(
    name := "scala-native-cli",
    scalacOptions += "-Ywarn-unused:imports",
    libraryDependencies ++= Seq(
      "org.scala-native" %% "tools" % scalaNativeVersion.value,
      "com.github.alexarchambault" %% "case-app" % "2.1.0-M10",
      "org.scalatest" %% "scalatest" % "3.1.1" % Test
    ),
    patchSourcesSettings,
    buildInfoKeys := Seq[BuildInfoKey](
      "nativeVersion" -> scalaNativeVersion.value
    ),
    buildInfoPackage := "scala.scalanative.cli.options",
    assembly / assemblyJarName :=
      genAssemblyJarName(
        normalizedName.value,
        scalaBinaryVersion.value,
        scalaNativeVersion.value
      ),
    scriptedLaunchOpts ++= {
      val jarName = genAssemblyJarName(
        normalizedName.value,
        scalaBinaryVersion.value,
        scalaNativeVersion.value
      )
      val cliPath = (Compile / packageBin / artifactPath).value.getParentFile / jarName
      Seq(
        "-Xmx1024M",
        "-Dplugin.version=" + scalaNativeVersion.value,
        "-Dscala-native-cli=" + cliPath,
      )
    },
    scriptedDependencies := {
      scriptedDependencies
        .dependsOn(assembly)
        .value
    },
    scriptedBufferLog := false
  )

def genAssemblyJarName(
    normalizedName: String,
    scalaBinaryVersion: String,
    scalaNativeVersion: String
): String = {
  s"${normalizedName}-assembly_${scalaBinaryVersion}-${scalaNativeVersion}.jar"
}

lazy val patchSourcesSettings = {
  def patchSources(base: File, version: String, subdir: String) = {
    val directory = version match {
      case v @ "0.4.0" => v
      case _           => "current"
    }
    base / "patches" / directory / "src" / subdir / "scala"
  }

  Def.settings(
    Compile / unmanagedSourceDirectories += patchSources(
      sourceDirectory.value,
      scalaNativeVersion.value,
      "main"
    ),
    Test / unmanagedSourceDirectories += patchSources(
      sourceDirectory.value,
      scalaNativeVersion.value,
      "test"
    )
  )
}
