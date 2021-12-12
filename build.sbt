val crossScalaNative212 = Seq("2.12.13", "2.12.14", "2.12.15")

val scalaNativeVersion =
  settingKey[String]("Version of Scala Native for which to build to CLI")

val cliAssemblyJarName = settingKey[String]("Name of created assembly jar")

inThisBuild(
  Def.settings(
    organization := "org.scala-native",
    scalaVersion := crossScalaNative212.last,
    scalaNativeVersion := "0.4.0",
    version := scalaNativeVersion.value
  )
)
val cliPackLibJars =
  taskKey[Seq[File]]("All libraries packed with packed in cliPack")
val cliPack = taskKey[File]("Pack the CLI for the current configuration")

lazy val cli = project
  .in(file("cli"))
  .enablePlugins(BuildInfoPlugin, ScriptedPlugin)
  .settings(
    name := "scala-native-cli",
    scalacOptions += "-Ywarn-unused:imports",
    libraryDependencies ++= Seq(
      "org.scala-native" %% "tools" % scalaNativeVersion.value,
      "org.scalatest" %% "scalatest" % "3.1.1" % Test,
      "com.github.alexarchambault" %% "case-app" % "2.1.0-M10"
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
        "-Dscala-native-cli=" + cliPath,
        "-Dscala-native-cli-pack=" + (cliPack / crossTarget).value
      )
    },
    scriptedBufferLog := false,
    scriptedDependencies := {
      scriptedDependencies
        .dependsOn(assembly, cliPack)
        .value
    },
    cliPackSettings
  )

lazy val cliPackSettings = Def.settings(
  cliPackLibJars := {
    val s = streams.value
    val log = s.log

    val scalaNativeOrg = "org.scala-native"
    val scalaBinVer = scalaBinaryVersion.value
    val snVer = scalaNativeVersion.value

    val scalaFullVers = crossScalaNative212
    val cliAssemblyJar = assembly.value

    // Standard modules needed for linking of Scala Native
    val optLib = snVer match {
      case "0.4.0" => Nil
      case v       => "windowslib" :: Nil
    }
    val stdLibModuleIDs = Seq(
      "nativelib",
      "clib",
      "posixlib",
      "javalib",
      "auxlib",
      "scalalib"
    ).++(optLib).map { lib =>
      val nativeBinVersion = ScalaNativeCrossVersion.binaryVersion(snVer)
      scalaNativeOrg % s"${lib}_native${nativeBinVersion}_${scalaBinVer}" % snVer
    }
    val compilerPluginModuleIDs =
      scalaFullVers.map(v => scalaNativeOrg % s"nscplugin_$v" % snVer)
    val allModuleIDs = (stdLibModuleIDs ++ compilerPluginModuleIDs).toVector
    val allModuleIDsIntransitive = allModuleIDs.map(_.intransitive())

    val resolvedLibJars = {
      val retrieveDir = s.cacheDirectory / "cli-lib-jars"
      val lm = {
        import sbt.librarymanagement.ivy._
        val ivyConfig = InlineIvyConfiguration().withLog(log)
        IvyDependencyResolution(ivyConfig)
      }
      val dummyModuleName =
        s"clilibjars-$snVer-$scalaBinVer-" + scalaFullVers.mkString("-")
      val dummyModuleID = scalaNativeOrg % dummyModuleName % version.value
      val descriptor =
        lm.moduleDescriptor(
          dummyModuleID,
          allModuleIDsIntransitive,
          scalaModuleInfo = None
        )
      lm
        .retrieve(descriptor, retrieveDir, log)
        .fold(
          { unresolvedWarn => throw unresolvedWarn.resolveException },
          identity
        )
        .distinct
    }

    cliAssemblyJar +: resolvedLibJars
  },
  cliPack / target := crossTarget.value / "pack",
  cliPack / moduleName :=
    s"scala-native-cli_${scalaBinaryVersion.value}-${scalaNativeVersion.value}",
  cliPack / crossTarget := (cliPack / target).value / (cliPack / moduleName).value,
  cliPack := {
    val scalaBinVer = scalaBinaryVersion.value
    val snVer = scalaNativeVersion.value

    val trg = (cliPack / crossTarget).value
    val trgLib = trg / "lib"
    val trgBin = trg / "bin"

    if (trg.exists)
      IO.delete(trg)

    IO.createDirectory(trgLib)
    val libJars = cliPackLibJars.value
    for (libJar <- libJars) {
      IO.copyFile(libJar, trgLib / libJar.getName)
    }

    IO.createDirectory(trgBin)
    val scriptDir = (Compile / sourceDirectory).value.getParentFile / "script"
    println(scriptDir)
    for {
      scriptFile <- IO.listFiles(scriptDir)
      if !scriptFile.getPath.endsWith("~")
    } {
      val content = IO.read(scriptFile)
      val processedContent = content
        .replaceAllLiterally("@SCALA_BIN_VER@", scalaBinVer)
        .replaceAllLiterally("@SCALANATIVE_VER@", snVer)
        .replaceAllLiterally(
          "@SCALANATIVE_BIN_VER@",
          ScalaNativeCrossVersion.binaryVersion(snVer)
        )
      val dest = trgBin / scriptFile.getName
      IO.write(dest, processedContent)
      if (scriptFile.canExecute)
        dest.setExecutable( /* executable = */ true, /* ownerOnly = */ false)
    }

    trg
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
