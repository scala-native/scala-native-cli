package scala.scalanative.cli.options

import caseapp._

@ProgName("scala-native-p")
@ArgsName("Class names")
case class POptions(
    @HelpMessage("Specify where to find user class files")
    @ExtraName("cp")
    @ValueDescription("<path>")
    classpath: List[String] = "." :: Nil,
    @HelpMessage(
      "Instead of passing class/object names, pass NIR file paths."
    )
    fromPath: Boolean = false,
    @Recurse
    misc: MiscOptions
)
