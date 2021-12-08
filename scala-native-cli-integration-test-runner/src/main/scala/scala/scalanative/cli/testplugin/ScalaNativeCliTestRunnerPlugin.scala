package scala.scalanative.cli
package testplugin

import TestRunnerPluginInternal._

import sbt._

// Adapted and adjusted from the Scala Native sbt plugin.
// Meant to act as an interface for testing scala-native-cli.
// Resolves necessary dependencies, compiles scala to NIR and
// via sbt scripted tests provides environment for building.
object ScalaNativeCliTestRunnerPlugin extends AutoPlugin {

  override def requires: Plugins = plugins.JvmPlugin

  object autoImport {
    val nativeVersion = System.getProperty("scala-native-cli-native-version")

    val runCli = inputKey[Unit](
      "Runs scala-native-cli with classpath and selected other options via cli arguments"
    )
  }

  override def projectSettings: Seq[Setting[_]] =
    TestRunnerPluginInternal.projectSettings

}
