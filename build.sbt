val scalaNativeVersion =
  settingKey[String]("Version of Scala Native for which to build to CLI")

val cliAssemblyJarName = settingKey[String]("Name of created assembly jar")

inThisBuild(
  Def.settings(
    organization := "org.scala-native",
    scalaVersion := "2.12.15",
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
    cliAssemblyJarName := s"${normalizedName.value}-assembly_${scalaBinaryVersion.value}-${scalaNativeVersion.value}.jar",
    assembly / assemblyJarName := cliAssemblyJarName.value,
    scriptedLaunchOpts ++= {
      val jarName = cliAssemblyJarName.value
      val cliPath = (Compile / crossTarget).value / jarName
      Seq(
        "-Xmx1024M",
        "-Dplugin.version=" + scalaNativeVersion.value,
        "-Dscala-native-cli=" + cliPath
      )
    },
    scriptedDependencies := {
      scriptedDependencies
        .dependsOn(assembly)
        .value
    }
  )

// To be removed since 0.4.2
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
