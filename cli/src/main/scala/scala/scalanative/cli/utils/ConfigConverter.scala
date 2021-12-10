package scala.scalanative.cli.utils

import scala.scalanative.build.Config
import scala.scalanative.build.NativeConfig
import scala.scalanative.build.Discover
import java.nio.file.Paths
import java.nio.file.Path
import scala.util.Try
import scala.scalanative.cli.options.CliOptions
import caseapp.Tag

case class BuildOptions(
    config: Config,
    outpath: Path
)

object ConfigConverter {

  def convert(
      options: CliOptions,
      main: String,
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
      options: CliOptions
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
      maybeNativeConfig <- VersionSpecificOptionsIncluder
        .withVersionSpecificOptions(
          options,
          NativeConfig.empty
            .withMode(options.nativeConfig.mode)
            .withLTO(options.nativeConfig.lto)
            .withGC(options.nativeConfig.gc)
            .withLinkStubs(options.nativeConfig.linkStubs)
            .withCheck(options.nativeConfig.check)
            .withDump(options.nativeConfig.dump)
            .withOptimize(!options.nativeConfig.noOptimize)
            .withTargetTriple(options.nativeConfig.targetTriple)
            .withClang(clang)
            .withClangPP(clangPP)
            .withCompileOptions(options.nativeConfig.compileOption)
            .withLinkingOptions(options.nativeConfig.linkingOption)
        )
    } yield maybeNativeConfig
  }

  private def generateConfig(
      options: CliOptions,
      main: String,
      classPath: Seq[String]
  ): Either[Throwable, Config] = {
    for {
      nativeConfig <- generateNativeConfig(options)
      classPath <- Try(parseClassPath(classPath)).toEither
    } yield {
      val config = Config.empty
        .withWorkdir(Paths.get(options.config.workdir).toAbsolutePath())
        .withCompilerConfig(nativeConfig)
        .withClassPath(classPath)
        .withMainClass(main)

      val verbosity = Tag.unwrap(options.logger.verbose)
      val logger = new FilteredLogger(verbosity)
      config.withLogger(logger)
    }
  }

  private def parseClassPath(classPath: Seq[String]): Seq[Path] =
    classPath.map(Paths.get(_))
}
