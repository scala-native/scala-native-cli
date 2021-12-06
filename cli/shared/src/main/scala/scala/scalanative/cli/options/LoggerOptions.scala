package scala.scalanative.cli.options

import caseapp._

case class LoggerOptions(
    @Group("Logger")
    @HelpMessage(
      "Filter out `debug` logs from the default Scala Native logger. [false]"
    )
    disableDebug: Boolean = false,
    @Group("Logger")
    @HelpMessage(
      "Filter out `info` logs from the default Scala Native logger. [false]"
    )
    disableInfo: Boolean = false,
    @Group("Logger")
    @HelpMessage(
      "Filter out `warn` logs from the default Scala Native logger. [false]"
    )
    disableWarn: Boolean = false,
    @Group("Logger")
    @HelpMessage(
      "Filter out `error` logs from the default Scala Native logger. [false]"
    )
    disableError: Boolean = false
)
