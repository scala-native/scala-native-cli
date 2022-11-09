package scala.scalanative.cli.options

import scopt.OptionParser

case class OptimizerConfigOptions(
    maxInlineDepth: Option[Int] = None,
    maxCallerSize: Option[Int] = None,
    maxInlineSize: Option[Int] = None
)

object OptimizerConfigOptions {
  def set(parser: OptionParser[LinkerOptions]) = {
    def update(c: LinkerOptions)(fn: OptimizerConfigOptions => OptimizerConfigOptions) =
      c.copy(optimizerConifg = fn(c.optimizerConifg))
    parser.note("Optimizer options:")
    parser
      .opt[Int]("max-inline-depth")
      .optional()
      .action((x, c) => update(c)(_.copy(maxInlineDepth = Some(x))))
      .text("Maximal number of allowed nested inlines.")
    parser
      .opt[Int]("max-caller-size")
      .optional()
      .action((x, c) => update(c)(_.copy(maxCallerSize = Some(x))))
      .text(
        "Maximal size (number of instructions) of the caller method which can accept inlines."
      )
    parser
      .opt[Int]("max-inline-size")
      .optional()
      .action((x, c) => update(c)(_.copy(maxInlineSize = Some(x))))
      .text(
        "Maximal size (number of instructions) of the inlined method."
      )
  }
}
