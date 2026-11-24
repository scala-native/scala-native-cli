package scala.scalanative.cli.options

case class LinkerOptions(
    classpath: List[String] = Nil,
    config: ConfigOptions = ConfigOptions(),
    nativeConfig: NativeConfigOptions = NativeConfigOptions(),
    optimizerConifg: OptimizerConfigOptions = OptimizerConfigOptions(),
    semanticsConfig: SemanticsConfigOptions = SemanticsConfigOptions(),
    sourceLevelDebuggingConfig: SourceLevelDebuggingConfigOptions =
      SourceLevelDebuggingConfigOptions(),
    gcConfig: GarbageCollectorConfigOptions = GarbageCollectorConfigOptions(),
    verbose: Int = 0
)
