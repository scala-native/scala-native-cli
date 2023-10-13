resolvers ++= Resolver.sonatypeOssRepos("snapshots")
enablePlugins(ScalaNativePlugin)

import sbt._
import sbt.Keys._
import complete.DefaultParsers._
import scala.collection.JavaConverters._

val runCli = inputKey[Unit](
  "Runs scala-native-cli with classpath and selected other options via cli arguments"
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

runCli := {
  val args = spaceDelimited("<arg>").parsed
  val classpath = (Compile / fullClasspath).value.map(_.data.toString())
  val cliPath = System.getProperty("scala-native-cli")
  val proc = new ProcessBuilder()
    .command(
      List("java", "-jar", cliPath)
        .++(args.toList)
        .++(classpath.toList)
        .asJava
    )

  // Removes fake error notification - expected errors would be
  // reported as such, leading to failing CI and confusing output
  proc.redirectErrorStream(true)
  proc.inheritIO()

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
