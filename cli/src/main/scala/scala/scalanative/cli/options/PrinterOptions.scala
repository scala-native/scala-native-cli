package scala.scalanative.cli.options

case class PrinterOptions(
    classNames: List[String] = Nil,
    classpath: List[String] = "." :: Nil,
    usingDefaultClassPath: Boolean = true,
    fromPath: Boolean = false,
    misc: MiscOptions = MiscOptions()
)
