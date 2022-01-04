package scala.scalanative.cli.options

case class LinkerOptions(
    classpath: List[String] = Nil,
    config: ConfigOptions = ConfigOptions(),
    nativeConfig: NativeConfigOptions = NativeConfigOptions(),
    verbose: Int = 0
)
