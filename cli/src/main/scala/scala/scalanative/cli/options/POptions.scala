package scala.scalanative.cli.options

import caseapp._

@ProgName("scala-native-p")
@ArgsName("NIR files")
case class POptions(
  @HelpMessage("Where to look for NIR files. Can include jar files.")
  @ExtraName("cp")
  @ValueDescription("<path>")
  classpath: Option[String] = None,
  @Recurse
  misc: MiscOptions
)