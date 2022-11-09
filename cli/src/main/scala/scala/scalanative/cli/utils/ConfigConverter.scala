package scala.scalanative.cli.utils

import scala.scalanative.build._
import java.nio.file.Paths
import java.nio.file.Path
import scala.util.Try
import scala.scalanative.cli.options._

case class BuildOptions(
    config: Config,
    outpath: Path
)

object ConfigConverter {
  def convert(
      options: LinkerOptions,
      main: String,
      classpath: Seq[String]
  ): Either[Throwable, BuildOptions] = convert(options, Some(main), classpath)

  def convert(
      options: LinkerOptions,
      main: Option[String],
      classpath: Seq[String]
  ): Either[Throwable, BuildOptions] = {
    if (classpath.isEmpty) {
      Left(
        new IllegalArgumentException(
          "Classpath not specified. Pass classpath files as positional arguments."
        )
      )
    } else {
      generateConfig(options, main, classpath).flatMap(config =>
        Try(Paths.get(options.config.outpath)).toEither.map(outpath =>
          BuildOptions(config, outpath)
        )
      )
    }
  }

  private def generateNativeConfig(
      options: LinkerOptions
  ): Either[Throwable, NativeConfig] = {

    def toPathOrDiscover(
        optPath: Option[String]
    )(discover: => Path): Either[Throwable, Path] =
      Try {
        optPath.map(Paths.get(_)).getOrElse(discover)
      }.toEither

    for {
      clang <- toPathOrDiscover(options.nativeConfig.clang)(Discover.clang())
      clangPP <- toPathOrDiscover(options.nativeConfig.clangPP)(
        Discover.clangpp()
      )
      ltp <- LinktimePropertyParser.parseAll(options.nativeConfig.ltp)
    } yield NativeConfig.empty
      .withMode(options.nativeConfig.mode)
      .withLTO(options.nativeConfig.lto)
      .withGC(options.nativeConfig.gc)
      .withLinkStubs(options.nativeConfig.linkStubs)
      .withCheck(options.nativeConfig.check)
      .withCheckFatalWarnings(options.nativeConfig.checkFatalWarnings)
      .withDump(options.nativeConfig.dump)
      .withOptimize(!options.nativeConfig.noOptimize)
      .withEmbedResources(options.nativeConfig.embedResources)
      .withTargetTriple(options.nativeConfig.targetTriple)
      .withClang(clang)
      .withClangPP(clangPP)
      .withCompileOptions(options.nativeConfig.compileOption)
      .withLinkingOptions(options.nativeConfig.linkingOption)
      .withLinktimeProperties(ltp)
      .withBuildTarget(options.nativeConfig.buildTarget)
      .withOptimizerConfig(generateOptimizerConfig(options.optimizerConifg))
  }

  private def generateOptimizerConfig(
      options: OptimizerConfigOptions
  ): OptimizerConfig = {
    val c0 = OptimizerConfig.empty
    val c1 = options.maxInlineDepth.foldLeft(c0)(_.withMaxInlineDepth(_))
    val c2 = options.maxCallerSize.foldLeft(c1)(_.withMaxCallerSize(_))
    val c3 = options.maxInlineSize.foldLeft(c2)(_.withMaxInlineSize(_))
    c3
  }

  private def generateConfig(
      options: LinkerOptions,
      main: Option[String],
      classPath: Seq[String]
  ): Either[Throwable, Config] = {
    for {
      nativeConfig <- generateNativeConfig(options)
      classPath <- Try(parseClassPath(classPath)).toEither
    } yield {
      val baseConfig = Config.empty
        .withWorkdir(Paths.get(options.config.workdir).toAbsolutePath())
        .withCompilerConfig(nativeConfig)
        .withClassPath(classPath)
        .withLogger(new FilteredLogger(options.verbose))

      main.foldLeft(baseConfig)(_.withMainClass(_))
    }
  }

  private def parseClassPath(classPath: Seq[String]): Seq[Path] =
    classPath.map(Paths.get(_))
}
