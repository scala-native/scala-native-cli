package scala.scalanative.cli.options

import caseapp._

@AppName("Scala Native Cli")
@ProgName("scala-native-cli")
case class CliOptions(
    @Recurse
    config: ConfigOptions,
    @Recurse
    nativeConfig: NativeConfigOptions,
    @Recurse
    logger: LoggerOptions
)
