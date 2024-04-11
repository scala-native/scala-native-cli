package scala.scalanative.cli.options

import scopt.OptionParser

case class ConfigOptions(
    main: Option[String] = None,
    outpath: Option[String] = None,
    workdir: String = "."
)

object ConfigOptions {
  def set(parser: OptionParser[LinkerOptions]) = {
    parser.note("Config options:")
    parser
      .opt[String]("main")
      .valueName("<main>")
      .optional()
      .action((x, c) => c.copy(config = c.config.copy(main = Some(x))))
      .text("Main class - entry point for application")
    parser
      .opt[String]('o', "outpath")
      .valueName("<output-path>")
      .optional()
      .action((x, c) => c.copy(config = c.config.copy(outpath = Some(x))))
      .text("Path of the resulting output binary.")
    parser
      .opt[String]("workdir")
      .valueName("<path-to-directory>")
      .optional()
      .action((x, c) => c.copy(config = c.config.copy(workdir = x)))
      .text("Scala Native working directory. [.]")
  }
}
