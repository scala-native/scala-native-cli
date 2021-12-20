val crossScalaVersions212 = (13 to 15).map(v => s"2.12.$v")
val crossScalaVersions213 = (4 to 7).map(v => s"2.13.$v")
val latestsScalaVersions =
  Seq(crossScalaVersions212.last, crossScalaVersions213.last)

def scalaReleasesForBinaryVersion(v: String): Seq[String] = v match {
  case "2.12" => crossScalaVersions212
  case "2.13" => crossScalaVersions213
  case ver =>
    throw new IllegalArgumentException(
      s"Unsupported binary scala version `${ver}`"
    )
}

val scalaNativeVersion =
  settingKey[String]("Version of Scala Native for which to build to CLI")

val cliAssemblyJarName = settingKey[String]("Name of created assembly jar")

inThisBuild(
  Def.settings(
    organization := "org.scala-native",
    scalaNativeVersion := "0.4.2",
    version := scalaNativeVersion.value,
    scalaVersion := crossScalaVersions212.last,
    crossScalaVersions := latestsScalaVersions,
    homepage := Some(url("http://www.scala-native.org")),
    startYear := Some(2021),
    licenses := Seq(
      "BSD-like" -> url(
        "https://github.com/scala-native/scala-native-cli/blob/main/LICENSE"
      )
    ),
    scmInfo := Some(
      ScmInfo(
        browseUrl = url("https://github.com/scala-native/scala-native-cli"),
        connection = "scm:git:git@github.com:scala-native/scala-native-cli.git",
        devConnection =
          Some("scm:git:git@github.com:scala-native/scala-native-cli.git")
      )
    )
  )
)
val cliPackLibJars =
  taskKey[Seq[File]]("All libraries packed with packed in cliPack")
val cliPack = taskKey[File]("Pack the CLI for the current configuration")

lazy val cli = project
  .in(file("cli"))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    name := "scala-native-cli",
    crossScalaVersions := {
      scalaNativeVersion.value match {
        // No Scala 2.13 artifacts until 0.4.2
        case "0.4.0" | "0.4.1" => Seq(crossScalaVersions212.last)
        case _                 => latestsScalaVersions
      }
    },
    Compile / run / mainClass :=
      Some("scala.scalanative.cli.ScalaNativeCli"),
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
    assembly / mainClass := (Compile / run / mainClass).value,
    cliPackSettings,
    publishSettings
  )

lazy val cliScriptedTests = project
  .in(file("cliScriptedTests"))
  .enablePlugins(ScriptedPlugin)
  .settings(
    sbtTestDirectory := (cli / sourceDirectory).value / "sbt-test",
    scalaVersion := crossScalaVersions212.last,
    scriptedLaunchOpts ++= {
      val jarName = (cli / cliAssemblyJarName).value
      val cliPath = (cli / Compile / crossTarget).value / jarName
      val packDir = (cli / cliPack / crossTarget).value
      Seq(
        "-Xmx1024M",
        "-Dplugin.version=" + (cli / scalaNativeVersion).value,
        "-Dscala.version=" + (cli / scalaVersion).value,
        "-Dscala-native-cli=" + cliPath,
        "-Dscala-native-cli-pack=" + packDir
      )
    },
    scriptedBufferLog := false,
    scriptedDependencies := {
      scriptedDependencies
        .dependsOn(
          cli / assembly,
          cli / cliPack
        )
        .value
    }
  )

lazy val cliPackSettings = Def.settings(
  cliPackLibJars := {
    val s = streams.value
    val log = s.log

    val scalaNativeOrg = organization.value
    val scalaBinVer = scalaBinaryVersion.value
    val snVer = scalaNativeVersion.value

    val scalaFullVers = scalaReleasesForBinaryVersion(scalaBinVer)
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

lazy val publishSettings = Def.settings(
  Compile / publishArtifact := true,
  Test / publishArtifact := false,
  publishMavenStyle := true,
  pomIncludeRepository := (_ => false),
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  credentials ++= {
    for {
      realm <- sys.env.get("MAVEN_REALM")
      domain <- sys.env.get("MAVEN_DOMAIN")
      user <- sys.env.get("MAVEN_USER")
      password <- sys.env.get("MAVEN_PASSWORD")
    } yield Credentials(realm, domain, user, password)
  }.toSeq,
  developers ++= List(
    Developer(
      email = "wmazur@virtuslab.com",
      id = "wmazur",
      name = "Wojciech Mazur",
      url = url("https://github.com/WojciechMazur")
    ),
    Developer(
      email = "jchyb@virtuslab.com",
      id = "jchyb",
      name = "Jan Chyb",
      url = url("https://github.com/jchyb")
    )
  ),
  pomExtra := (
    <issueManagement>
        <system>GitHub Issues</system>
        <url>https://github.com/scala-native/scala-native-cli/issues</url>
      </issueManagement>
  )
)
