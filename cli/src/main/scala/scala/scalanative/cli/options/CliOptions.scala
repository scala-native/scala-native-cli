package scala.scalanative.cli.options

import caseapp._

@AppName("ScalaNativeCli")
@ProgName("scala-native-cli")
@ArgsName("classpath")
case class CliOptions(
    @Recurse
    config: ConfigOptions,
    @Recurse
    nativeConfig: NativeConfigOptions,
    @Recurse
    logger: LoggerOptions,
    @Recurse
    misc: MiscOptions
)
