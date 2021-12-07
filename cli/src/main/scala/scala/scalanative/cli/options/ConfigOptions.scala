package scala.scalanative.cli.options

import caseapp._

case class ConfigOptions(
    @Group("Config")
    @HelpMessage(
      "Required path of the resulting output binary. [./scala-native-out]"
    )
    @ValueDescription("<output-path>")
    outpath: String = "scala-native-out",
    @Group("Config")
    @HelpMessage("Scala Native working directory. [.]")
    @ValueDescription("<path-to-directory>")
    workdir: String = "."
)
