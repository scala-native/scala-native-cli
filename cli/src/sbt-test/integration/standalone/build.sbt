import sbt._
import sbt.Keys._
import complete.DefaultParsers._
import scala.collection.JavaConverters._
import java.util.Locale
import java.nio.file.Paths

val runScript = inputKey[Unit](
  "Runs scala-native-cli pack script"
)

runScript := {
val scriptName +: args = spaceDelimited("<arg>").parsed.toSeq
val cliPackDir = System.getProperty("scala-native-cli-pack")
val isWindows = System
  .getProperty("os.name", "unknown")
  .toLowerCase(Locale.ROOT)
  .startsWith("windows")

  val ver = scalaVersion.value
  val cacheDir = new File(cliPackDir).getParentFile.getParentFile / s"fetchScala-$ver"
  val scalaBinDir = cacheDir / s"scala-$ver" / "bin"

  FileFunction.cached(
    cacheDir / s"fetchScala-$ver",
    FilesInfo.lastModified,
    FilesInfo.exists
  ) { dependencies =>
    if (!scalaBinDir.exists) {
      IO.unzipURL(
        url(s"https://downloads.lightbend.com/scala/${ver}/scala-$ver.zip"),
        cacheDir
      )
    }
    Set(scalaBinDir)
  }(Set(scalaBinDir))

  val script = Paths.get(cliPackDir, "bin", scriptName).toString
  val proc =
    if (isWindows)
      new ProcessBuilder(
        (Seq("cmd", "/c", s"${script}.bat") ++ args.tail): _*
      )
    else new ProcessBuilder((Seq("sh", script) ++ args): _*)

  // Removes fake error notification - expected errors would be
  // reported as such, leading to failing CI and confusing output
  proc.redirectErrorStream(true)
  proc.inheritIO()
  scalaBinDir.listFiles().foreach(f => f.setExecutable( /* executable = */ true, /* ownerOnly = */ false))
  val prevPath = proc.environment.get("PATH")
  val newPath = prevPath.split(":").filter(!_.contains("scala")).mkString(":")
  proc.environment.put("PATH", s"$scalaBinDir:${newPath}")
  proc.environment.put("SCALA_HOME", s"$scalaBinDir")
  println(proc.environment.get("PATH"))

  proc.start().waitFor() match {
    case 0 => ()
    case exitCode =>
      throw new RuntimeException(
        s"Execution of command failed with code $exitCode"
      ) with scala.util.control.NoStackTrace
  }
}
