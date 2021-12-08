package scala.scalanative.cli.options

import caseapp._

case class LoggerOptions(
    @Group("Logger")
    @ExtraName("v")
    @HelpMessage(
      "Increase verbosity of internal logger. Can be specified multiple times."
    )
    verbose: Int @@ Counter = Tag.of(0)
)
