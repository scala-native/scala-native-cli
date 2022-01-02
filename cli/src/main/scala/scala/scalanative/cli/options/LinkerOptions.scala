package scala.scalanative.cli.options

// import caseapp._

// @AppName("ScalaNativeLd")
// @ProgName("scala-native-ld")
// @ArgsName("classpath")
case class LinkerOptions(
    classpath: List[String] = Nil,
    // @Recurse
    config: ConfigOptions = ConfigOptions(),
    // @Recurse
    nativeConfig: NativeConfigOptions = NativeConfigOptions(),
    // @Recurse
    logger: LoggerOptions = LoggerOptions(),
    // @Recurse
    misc: MiscOptions = MiscOptions()
)
