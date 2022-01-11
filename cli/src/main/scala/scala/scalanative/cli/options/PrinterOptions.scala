package scala.scalanative.cli.options

import scopt.OptionParser

case class PrinterOptions(
    classNames: List[String] = Nil,
    classpath: List[String] = "." :: Nil,
    usingDefaultClassPath: Boolean = true,
    fromPath: Boolean = false,
    verbose: Boolean = false
)

object PrinterOptions {
  def set(parser: OptionParser[PrinterOptions]) = {
    parser
      .opt[String]("classpath")
      .abbr("-cp")
      .valueName("<path>")
      .optional()
      .unbounded()
      .action((x, c) =>
        if (c.usingDefaultClassPath)
          c.copy(classpath = x :: Nil, usingDefaultClassPath = false)
        else
          c.copy(classpath = c.classpath :+ x)
      )
      .text("Specify where to find user class files.")
    parser
      .opt[Unit]("from-path")
      .optional()
      .action((x, c) => c.copy(fromPath = true))
      .text("Instead of passing class/object names, pass NIR file paths.")
    parser
      .opt[Unit]("verbose")
      .abbr("v")
      .optional()
      .action((_, c) => c.copy(verbose = true))
      .text("Print all informations about NIR, including method definitions.")

  }
}
