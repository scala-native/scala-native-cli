package scala.scalanative.cli.options

import caseapp._

@AppName("ScalaNativeCli")
@ProgName("scala-native-cli")
@ArgsName("main] [nir-files")
case class CliOptions(
    @Recurse
    config: ConfigOptions,
    @Recurse
    nativeConfig: NativeConfigOptions,
    @Recurse
    logger: LoggerOptions
)
