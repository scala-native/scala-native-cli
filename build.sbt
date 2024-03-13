val ScalaNativeVersion = "0.5.0-SNAPSHOT"
// Update during release procedure to provide access to staged, but not published artifacts
val StagingRepoIds = Nil
val StagingRepoNames = StagingRepoIds.map(id => s"orgscala-native-$id").toSeq

val crossScalaVersions212 = (14 to 19).map("2.12." + _)
val crossScalaVersions213 = (8 to 13).map("2.13." + _)
val crossScalaVersions3 =
  (2 to 3).map("3.1." + _) ++
    (0 to 2).map("3.2." + _) ++
    (0 to 3).map("3.3." + _) ++
    (0 to 0).map("3.4." + _)

val scala2_12 = crossScalaVersions212.last
val scala2_13 = crossScalaVersions213.last
val scala3 = crossScalaVersions3.last
val scala3PublishVersion = "3.1.3"

val publishScalaVersions = Seq(scala2_12, scala2_13, scala3PublishVersion)

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
    organization: String,
    nativeVersion: String,
    nativeBinaryVersion: String,
    scalaBinaryVersion: String
): Seq[ModuleID] = {
  def artifact(module: String, binV: String, version: String = nativeVersion) =
    organization % s"${module}_native${nativeBinaryVersion}_$binV" % version

  def crossScalaLibVersion(scalaVersion: String) =
    s"$scalaVersion+$nativeVersion"
  def scalalibVersion(scalaBinVersion: String): String = {
    val scalaVersion = scalaReleasesForBinaryVersion(scalaBinVersion).last
    crossScalaLibVersion(scalaVersion)
  }
  def scalalib(binV: String) = artifact("scalalib", binV, scalalibVersion(binV))
  val scala3lib =
    artifact("scala3lib", "3", crossScalaLibVersion(scala3PublishVersion))
  val crossRuntimeLibraries = List(
    "nativelib",
    "clib",
    "posixlib",
    "windowslib",
    "javalib",
    "auxlib"
  ).map(artifact(_, scalaBinaryVersion))

  val nonCrossRuntimeLibraries = List("javalib-intf")
    .map(organization % _ % nativeVersion)

  val runtimeLibraries = crossRuntimeLibraries ++ nonCrossRuntimeLibraries

  scalaBinaryVersion match {
    case "2.12" | "2.13" => scalalib(scalaBinaryVersion) :: runtimeLibraries
    case "3"             => scala3lib :: scalalib("2.13") :: runtimeLibraries
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
    scalaNativeVersion := ScalaNativeVersion,
    version := scalaNativeVersion.value,
    scalaVersion := scala3PublishVersion,
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
    // Used during the releases
    resolvers ++= StagingRepoNames.flatMap(Resolver.sonatypeOssRepos(_)),
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
        "-Dscala-native-cli-pack=" + packDir,
        "-Dscalanative.build.staging.resolvers=" + StagingRepoNames
          .mkString(",")
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
      else if (file.endsWith("scala-native.properties")) MergeStrategy.concat
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

    // Standard modules needed for linking of Scala Native
    val stdLibModuleIDs =
      scalaStdlibForBinaryVersion(
        organization = scalaNativeOrg,
        nativeVersion = snVer,
        nativeBinaryVersion = nativeBinVer,
        scalaBinaryVersion = scalaBinVer
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
        .replaceAllLiterally(
          "@SCALALIB_2_13_FOR_3_VER@",
          crossScalaVersions213.last
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
