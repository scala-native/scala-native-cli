val crossScalaVersions212 = (14 to 18).map("2.12." + _)
val crossScalaVersions213 = (8 to 12).map("2.13." + _)
val crossScalaVersions3 =
  (2 to 3).map("3.1." + _) ++
    (0 to 2).map("3.2." + _) ++
    (0 to 1).map("3.3." + _)

val publishScalaVersions =
  Seq(crossScalaVersions212, crossScalaVersions213).map(_.last) ++ Seq("3.1.3")

def scalaReleasesForBinaryVersion(v: String): Seq[String] = v match {
  case "2.12" => crossScalaVersions212
  case "2.13" => crossScalaVersions213
  case "3"    => crossScalaVersions3
  case ver =>
    throw new IllegalArgumentException(
      s"Unsupported binary scala version `${ver}`"
    )
}

def scalaStdlibForBinaryVersion(
    nativeBinVer: String,
    scalaBinVer: String
): Seq[String] = {
  def depPattern(lib: String, v: String) =
    s"${lib}_native${nativeBinVer}_${v}"
  val scalalib = "scalalib"
  val scala3lib = "scala3lib"
  val commonLibs = Seq(
    "nativelib",
    "clib",
    "posixlib",
    "windowslib",
    "javalib",
    "auxlib"
  )
  scalaBinVer match {
    case "2.12" | "2.13" =>
      (commonLibs :+ scalalib).map(depPattern(_, scalaBinVer))
    case "3" =>
      (commonLibs :+ scala3lib).map(depPattern(_, scalaBinVer)) :+
        depPattern(scalalib, "2.13")
    case ver =>
      throw new IllegalArgumentException(
        s"Unsupported binary scala version `${ver}`"
      )
  }
}

val scalaNativeVersion =
  settingKey[String]("Version of Scala Native for which to build to CLI")

val cliAssemblyJarName = settingKey[String]("Name of created assembly jar")

inThisBuild(
  Def.settings(
    organization := "org.scala-native",
    scalaNativeVersion := "0.5.0-SNAPSHOT",
    version := scalaNativeVersion.value,
    scalaVersion := crossScalaVersions212.last,
    crossScalaVersions := publishScalaVersions,
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
    ),
    resolvers ++= Resolver.sonatypeOssRepos("snapshots"),
    resolvers += Resolver.mavenCentral,
    resolvers += Resolver.defaultLocal
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
    crossScalaVersions := publishScalaVersions,
    Compile / run / mainClass :=
      Some("scala.scalanative.cli.ScalaNativeLd"),
    scalacOptions += "-Ywarn-unused:imports",
    scalacOptions ++= CrossVersion.partialVersion(scalaVersion.value).collect {
      case (2, _) => "-target:jvm-1.8"
      case (3, _) => "-Xtarget:8"
    },
    libraryDependencies ++= Seq(
      "org.scala-native" %% "tools" % scalaNativeVersion.value,
      "com.github.scopt" %% "scopt" % "4.0.1",
      "org.scalatest" %% "scalatest" % "3.2.10" % Test
    ),
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

def nativeBinaryVersion(version: String): String = {
  val VersionPattern = raw"(\d+)\.(\d+)\.(\d+)(\-.*)?".r
  val VersionPattern(major, minor, patch, milestone) = version
  if (patch != null && milestone != null) version
  else s"$major.$minor"
}

val nativeSourceExtensions = Set(".c", ".cpp", ".cxx", ".h", ".hpp", ".S")
val DeduplicateOrRename = new sbtassembly.MergeStrategy {
  def name: String = "deduplicate-or-rename"
  def apply(
      tempDir: java.io.File,
      path: String,
      files: Seq[java.io.File]
  ): Either[String, Seq[(java.io.File, String)]] =
    MergeStrategy.deduplicate(tempDir, path, files) match {
      case v @ Right(_) => v
      case _            => MergeStrategy.rename(tempDir, path, files)
    }
}

lazy val cliPackSettings = Def.settings(
  assemblyMergeStrategy := {
    val default = assemblyMergeStrategy.value
    file =>
      if (nativeSourceExtensions.exists(file.endsWith)) DeduplicateOrRename
      else default(file)
  },
  cliPackLibJars := {
    val s = streams.value
    val log = s.log

    val scalaNativeOrg = organization.value
    val scalaBinVer = scalaBinaryVersion.value
    val snVer = scalaNativeVersion.value
    val nativeBinVer = nativeBinaryVersion(snVer)

    val scalaFullVers = scalaReleasesForBinaryVersion(scalaBinVer)
    val cliAssemblyJar = assembly.value

    val scalaStdLibraryModuleIDs =
      scalaStdlibForBinaryVersion(nativeBinVer, scalaBinVer)

    // Standard modules needed for linking of Scala Native
    val stdLibModuleIDs = scalaStdLibraryModuleIDs.map(
      scalaNativeOrg % _ % snVer
    )
    val compilerPluginModuleIDs =
      scalaFullVers.map(v => scalaNativeOrg % s"nscplugin_$v" % snVer)
    val allModuleIDs = (stdLibModuleIDs ++ compilerPluginModuleIDs).toVector
    val allModuleIDsIntransitive = allModuleIDs.map(_.intransitive())

    val resolvedLibJars = {
      val retrieveDir = s.cacheDirectory / "cli-lib-jars"
      val lm = {
        import sbt.librarymanagement.ivy._
        val ivyConfig = InlineIvyConfiguration()
          .withResolvers(resolvers.value.toVector)
          .withLog(log)
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
          nativeBinaryVersion(snVer)
        )
      val dest = trgBin / scriptFile.getName
      IO.write(dest, processedContent)
      if (scriptFile.canExecute)
        dest.setExecutable( /* executable = */ true, /* ownerOnly = */ false)
    }

    trg
  }
)

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
      user <- sys.env.get("MAVEN_USER")
      password <- sys.env.get("MAVEN_PASSWORD")
    } yield Credentials(
      realm = "Sonatype Nexus Repository Manager",
      host = "oss.sonatype.org",
      userName = user,
      passwd = password
    )
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
