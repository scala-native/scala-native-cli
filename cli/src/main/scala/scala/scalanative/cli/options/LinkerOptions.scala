package scala.scalanative.cli.options

import caseapp._

@AppName("ScalaNativeLd")
@ProgName("scala-native-ld")
@ArgsName("classpath")
case class LinkerOptions(
    @Recurse
    config: ConfigOptions,
    @Recurse
    nativeConfig: NativeConfigOptions,
    @Recurse
    logger: LoggerOptions,
    @Recurse
    misc: MiscOptions
)
