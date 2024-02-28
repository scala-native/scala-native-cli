package scala.scalanative.cli.options

import java.nio.file.{Path, Paths}
import scopt.OptionParser

case class SourceLevelDebuggingConfigOptions(
    enabled: Option[Boolean] = None,
    genFunctionSourcePositions: Option[Boolean] = None,
    genLocalVariables: Option[Boolean] = None,
    customSourceRoots: Seq[Path] = Nil
)

object SourceLevelDebuggingConfigOptions {
  def set(parser: OptionParser[LinkerOptions]) = {
    def update(c: LinkerOptions)(
        fn: SourceLevelDebuggingConfigOptions => SourceLevelDebuggingConfigOptions
    ) =
      c.copy(sourceLevelDebuggingConfig = fn(c.sourceLevelDebuggingConfig))
    parser.note("Source Level Debugging options:")
    parser
      .opt[Boolean]("-debug-info")
      .optional()
      .action((x, c) => update(c)(_.copy(enabled = Some(x))))
      .text("Should enable generation of source level debug metadata")
    parser
      .opt[Unit]("debug-all")
      .abbr("g")
      .optional()
      .action((x, c) =>
        update(c)(
          _.copy(
            enabled = Some(true),
            genFunctionSourcePositions = Some(true),
            genLocalVariables = Some(true)
          )
        )
      )
      .text(
        "Should enable all debug metadata generation"
      )
    parser
      .opt[Boolean]("debug-function-source-positions")
      .optional()
      .action((x, c) => update(c)(_.copy(genFunctionSourcePositions = Some(x))))
      .text(
        "Should enable generation of function source position for stack traces"
      )
    parser
      .opt[Boolean]("debug-local-variables")
      .optional()
      .action((x, c) => update(c)(_.copy(genLocalVariables = Some(x))))
      .text("Should enable generation of localv variables metadata")
    parser
      .opt[String]("debug-source-root")
      .optional()
      .unbounded()
      .action((x, c) =>
        update(c)(cc =>
          cc.copy(customSourceRoots = Paths.get(x) +: cc.customSourceRoots)
        )
      )
      .text("Add custom sources root directory")
  }
}
