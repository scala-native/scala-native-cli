package scala.scalanative.cli
package testplugin

import sbt._
import sbt.Keys._
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import scala.scalanative.cli.testplugin.ScalaNativeCliTestRunnerPlugin.autoImport._
import complete.DefaultParsers._
import scala.collection.JavaConverters._
import java.lang.ProcessBuilder.Redirect

// Adapted and adjusted from the Scala Native sbt plugin.
object TestRunnerPluginInternal {

  lazy val dependencies: Seq[Setting[_]] = Seq(
    libraryDependencies ++= Seq(
      "org.scala-native" %%% "nativelib_native0.4" % nativeVersion,
      "org.scala-native" %%% "javalib_native0.4" % nativeVersion,
      "org.scala-native" %%% "auxlib_native0.4" % nativeVersion,
      "org.scala-native" %%% "scalalib_native0.4" % nativeVersion
    ),
    addCompilerPlugin(
      "org.scala-native" % "nscplugin" % nativeVersion cross CrossVersion.full
    )
  )

  // No Test settings necessary
  lazy val projectSettings: Seq[Setting[_]] =
    dependencies ++
      inConfig(Compile)(configSettings)

  lazy val configSettings: Seq[Setting[_]] = Seq(
    runTest := {
      val args = spaceDelimited("<arg>").parsed

      val classpath = fullClasspath.value.map(_.data.toString())

      val cliPath = System.getProperty("scala-native-cli")

      val proc = new ProcessBuilder()
        .command(
          List("java", "-jar", cliPath)
            .++(args.toList)
            .++(List("--class-path"))
            .++(List(classpath.mkString(":")))
            .asJava
        )

      // Removes fake error notification - expected errors would be
      // reported as such, leading to failing CI and confusing output
      proc.redirectErrorStream(true)
      proc.inheritIO()

      proc.start().waitFor()
    }
  )

}
