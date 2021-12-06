package scala.scalanative.cli.utils

import scala.scalanative.build.Config
import scala.scalanative.build.NativeConfig
import scala.scalanative.build.Discover
import caseapp.core.app.CaseApp
import java.nio.file.Paths
import java.nio.file.Path
import scala.util.Try
import scala.scalanative.cli.options.NativeVersion
import scala.scalanative.cli.options.CliOptions

import scala.scalanative.cli.utils.NativeConfigParserImplicits._

case class BuildOptions(
    config: Config,
    outpath: Path
)

object BuildOptionsParser {

  def apply(options: Array[String]): Either[Throwable, Option[BuildOptions]] = {
    if (options.contains("--help") || options.contains("-h")) {
      CaseApp.printHelp[CliOptions]()
      Right(None)
    } else if (options.contains("--version")) {
      println(s"Version: ${NativeVersion.value}")
      Right(None)
    } else {
      val remappedOptions = CaseApp
        .parse[CliOptions](options)
        .left
        .map(error => new IllegalArgumentException(error.message))

      remappedOptions.flatMap { case (options, _) =>
        val cliOptions = options
        val outpath = Paths.get(cliOptions.config.outpath)

        generateConfig(cliOptions).map(config =>
          Some(BuildOptions(config, outpath))
        )
      }
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

    val clangEither = Try {
      options.nativeConfig.clang match {
        case Some(value) => Paths.get(value)
        case None        => Discover.clang()
      }
    }.toEither

    val clangPPEither = Try {
      options.nativeConfig.clangPP match {
        case Some(value) => Paths.get(value)
        case None        => Discover.clangpp()
      }
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
            .withMode(options.nativeConfig.nativeMode)
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

  private def generateConfig(options: CliOptions): Either[Throwable, Config] = {
    for {
      nativeConfig <- generateNativeConfig(options)
      classPath <- Try(parseClassPath(options.config.classPath)).toEither
    } yield {
      val config = Config.empty
        .withClassPath(classPath)
        .withMainClass(options.config.main)
        .withWorkdir(Paths.get(options.config.workdir).toAbsolutePath())
        .withCompilerConfig(nativeConfig)
      val logger =
        new FilteredLogger(
          logDebug = !options.logger.disableDebug,
          logInfo = !options.logger.disableInfo,
          logWarn = !options.logger.disableWarn,
          logError = !options.logger.disableError
        )
      config.withLogger(logger)
    }

  }

  private def parseClassPath(classPath: String): Seq[Path] =
    classPath.split(":").map(Paths.get(_))
}
