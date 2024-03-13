import sbt._
import sbt.Keys._
import complete.DefaultParsers._
import scala.collection.JavaConverters._
import java.util.Locale
import java.nio.file.Paths
import java.io.File

val runScript = inputKey[Unit](
  "Runs scala-native-cli pack script"
)
val runExec = inputKey[Unit](
  "Runs given executable (due to problems with exec on Windows)"
)

scalaVersion := {
  val scalaVersion = System.getProperty("scala.version")
  if (scalaVersion == null)
    throw new RuntimeException(
      """|The system property 'scala.version' is not defined.
         |Specify this property using the scriptedLaunchOpts -D.""".stripMargin
    )
  else scalaVersion
}
resolvers ++= sys.props
  .get("scalanative.build.staging.resolvers")
  .toList
  .flatMap(_.split(","))
  .flatMap(Resolver.sonatypeOssRepos(_))

runScript := {
  val scriptName +: rawArgs = spaceDelimited("<arg>").parsed.toSeq
  val cliPackDir = System.getProperty("scala-native-cli-pack")
  val isWindows = System
    .getProperty("os.name", "unknown")
    .toLowerCase(Locale.ROOT)
    .startsWith("windows")

  val ver = scalaVersion.value
  val cacheDir =
    new File(cliPackDir).getParentFile.getParentFile / s"fetchScala-$ver"
  val scalaDir =
    if (ver.startsWith("3.")) s"scala3-$ver"
    else s"scala-$ver"
  val scalaBinDir = cacheDir / scalaDir / "bin"

  FileFunction.cached(
    cacheDir / s"fetchScala-$ver",
    FilesInfo.lastModified,
    FilesInfo.exists
  ) { _ =>
    if (!scalaBinDir.exists) {
      val downloadUrl =
        if (ver.startsWith("3."))
          s"https://github.com/lampepfl/dotty/releases/download/$ver/$scalaDir.zip"
        else
          s"https://downloads.lightbend.com/scala/${ver}/$scalaDir.zip"
      IO.unzipURL(url(downloadUrl), cacheDir)
    }
    // Make sure we can execute scala/scalac from downloaded distro
    scalaBinDir
      .listFiles()
      .foreach(f =>
        f.setExecutable( /* executable = */ true, /* ownerOnly = */ false)
      )

    Set(scalaBinDir)
  }(Set(scalaBinDir))

  val script = Paths.get(cliPackDir, "bin", scriptName).toString
  val args = rawArgs.map {
    case "@NATIVE_LIB@" | "\"@NATIVE_LIB@\"" =>
      "\"" + Paths
        .get(cliPackDir, "lib")
        .toFile()
        .listFiles()
        .mkString(File.pathSeparator) + "\""
    case arg => arg
  }
  val proc =
    if (isWindows)
      new ProcessBuilder(
        (Seq("cmd", "/c", script) ++ args): _*
      )
    else new ProcessBuilder((Seq("sh", script) ++ args): _*)

  // Removes fake error notification - expected errors would be
  // reported as such, leading to failing CI and confusing output
  proc.redirectErrorStream(true)
  proc.inheritIO()

  // Find name of Path environment, depending on OS/env it might differ in casing
  // Update existing path or create a new env variable
  proc.environment.keySet.toArray.collectFirst {
    case key: String if key.toUpperCase(Locale.ROOT) == "PATH" => key
  } match {
    case Some(pathKey) =>
      val prevPath = proc.environment.get(pathKey)
      proc.environment.put(
        pathKey,
        s"${scalaBinDir}${File.pathSeparator}${prevPath}"
      )
    case _ =>
      proc.environment.put("PATH", scalaBinDir.toString)
  }

  proc.start().waitFor() match {
    case 0 => ()
    case exitCode =>
      throw new RuntimeException(
        s"Execution of command failed with code $exitCode"
      ) with scala.util.control.NoStackTrace
  }
}

runExec := {
  val args = spaceDelimited("<arg>").parsed
  new ProcessBuilder(args: _*)
    .inheritIO()
    .start()
    .waitFor() match {
    case 0 => ()
    case exitCode =>
      throw new RuntimeException(
        s"Execution of command failed with code $exitCode"
      ) with scala.util.control.NoStackTrace
  }
}
