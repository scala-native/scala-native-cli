import scala.util.Properties.envOrNone

val ScalaNativeVersion = envOrNone("CI_NATIVE_VERSION")
  .filterNot(_.isEmpty)
  .getOrElse("0.5.13")

val scala2_12 = "2.12.21"
val scala2_13 = "2.13.18"
val scala3PublishVersion = "3.1.3"

sonatypePublishSettings

val publishScalaVersions = Seq(scala2_12, scala2_13, scala3PublishVersion)

val scalaNativeVersion =
  settingKey[String]("Version of Scala Native for which to build to CLI")

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
    resolvers += Resolver.sonatypeCentralSnapshots,
    resolvers += Resolver.mavenCentral,
    resolvers += Resolver.defaultLocal
  )
)

lazy val cli = project
  .in(file("cli"))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    name := "scala-native-cli",
    crossScalaVersions := publishScalaVersions,
    Compile / run / mainClass :=
      Some("scala.scalanative.cli.ScalaNativeLd"),
    scalacOptions ++= Seq(
      "-release:8",
      "-Ywarn-unused:imports"
    ),
    libraryDependencies ++= Seq(
      "org.scala-native" %% "tools" % scalaNativeVersion.value,
      "com.github.scopt" %% "scopt" % "4.0.1",
      "org.scalatest" %% "scalatest" % "3.2.10" % Test
    ),
    buildInfoKeys := Seq[BuildInfoKey](
      "nativeVersion" -> scalaNativeVersion.value
    ),
    buildInfoPackage := "scala.scalanative.cli.options",
    publishSettings
  )

val cliClasspathFile =
  settingKey[File](
    "File listing runtime classpath of the CLI for scripted tests"
  )

lazy val cliScriptedTests = project
  .in(file("cliScriptedTests"))
  .enablePlugins(ScriptedPlugin)
  .settings(
    sbtTestDirectory := (cli / sourceDirectory).value / "sbt-test",
    scalaVersion := scala2_12,
    cliClasspathFile := target.value / "cli-classpath.txt",
    scriptedLaunchOpts ++= Seq(
      "-Xmx1024M",
      "-Dplugin.version=" + (cli / scalaNativeVersion).value,
      "-Dscala.version=" + (cli / scalaVersion).value,
      "-Dscala-native-cli.classpath-file=" + cliClasspathFile.value
    ),
    scriptedBufferLog := false,
    scriptedDependencies := {
      scriptedDependencies.value
      val cliClasspath = (cli / Runtime / fullClasspath).value
        .map(_.data.getAbsolutePath)
        .mkString(java.io.File.pathSeparator)
      IO.write(cliClasspathFile.value, cliClasspath)
    }
  )

lazy val publishSettings = Def.settings(
  Compile / publishArtifact := true,
  Test / publishArtifact := false,
  sonatypePublishSettings
)
lazy val sonatypePublishSettings = Def.settings(
  publishMavenStyle := true,
  pomIncludeRepository := (_ => false),
  publishTo := {
    if (isSnapshot.value) Some(Resolver.sonatypeCentralSnapshots)
    else localStaging.value
  },
  credentials ++= {
    for {
      user <- sys.env.get("SONATYPE_USER")
      password <- sys.env.get("SONATYPE_PASSWORD")
    } yield Credentials(
      realm = "",
      host = "central.sonatype.com",
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
