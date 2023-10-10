package scala.scalanative.cli.utils

import scala.scalanative.build._
import java.nio.file.Paths
import java.nio.file.Path
import scala.util.Try
import scala.scalanative.cli.options._
import java.io.File

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

    def resolveBaseName: Either[Throwable, String] =
      options.nativeConfig.baseName match {
        case Some(name) => Right(name)
        case _ =>
          Paths
            .get(options.config.outpath.replaceAll(raw"[/\\\\]", File.separator))
            .getFileName()
            .toString()
            .split('.')
            .headOption match {
            case Some(name) => Right(name)
            case None =>
              Left(
                new IllegalArgumentException(
                  s"Invalid output path, failed to resolve base name of output file for path '${options.config.outpath}'"
                )
              )
          }
      }
    for {
      clang <- toPathOrDiscover(options.nativeConfig.clang)(Discover.clang())
      clangPP <- toPathOrDiscover(options.nativeConfig.clangPP)(
        Discover.clangpp()
      )
      ltp <- LinktimePropertyParser.parseAll(options.nativeConfig.ltp)
      baseName <- resolveBaseName
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
      .withIncrementalCompilation(options.nativeConfig.incrementalCompilation)
      .withOptimizerConfig(generateOptimizerConfig(options.optimizerConifg))
      .withBaseName(baseName)
      .withMultithreadingSupport(options.nativeConfig.multithreadingSupport)
      .withDebugMetadata(options.nativeConfig.debugMetadata)
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
    } yield Config.empty
      .withBaseDir(Paths.get(options.config.workdir).toAbsolutePath())
      .withCompilerConfig(nativeConfig)
      .withClassPath(classPath)
      .withLogger(new FilteredLogger(options.verbose))
      .withMainClass(main)

  }

  private def parseClassPath(classPath: Seq[String]): Seq[Path] =
    classPath.map(Paths.get(_))
}
