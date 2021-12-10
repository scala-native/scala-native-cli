package scala.scalanative.cli.options

import caseapp.HelpMessage
import caseapp.Group

case class MiscOptions(
    @Group("Help")
    @HelpMessage("Print scala-native-cli version and exit")
    val version: Boolean = false
)
