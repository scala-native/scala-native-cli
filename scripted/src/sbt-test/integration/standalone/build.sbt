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

runScript := {
  val scriptName +: args = spaceDelimited("<arg>").parsed.toSeq
  val cliPackDir = System.getProperty("scala-native-cli-pack")
  val isWindows = System
    .getProperty("os.name", "unknown")
    .toLowerCase(Locale.ROOT)
    .startsWith("windows")

  val ver = scalaVersion.value
  val cacheDir =
    new File(cliPackDir).getParentFile.getParentFile / s"fetchScala-$ver"
  val scalaBinDir = cacheDir / s"scala-$ver" / "bin"

  FileFunction.cached(
    cacheDir / s"fetchScala-$ver",
    FilesInfo.lastModified,
    FilesInfo.exists
  ) { _ =>
    if (!scalaBinDir.exists) {
      IO.unzipURL(
        url(s"https://downloads.lightbend.com/scala/${ver}/scala-$ver.zip"),
        cacheDir
      )
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
