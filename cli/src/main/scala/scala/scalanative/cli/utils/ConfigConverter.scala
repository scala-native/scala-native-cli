package scala.scalanative.cli.utils

import scala.scalanative.build._
import java.nio.file.Paths
import java.nio.file.Path
import scala.util.Try
import scala.scalanative.cli.options._
import java.io.File

case class BuildOptions(
    config: Config,
    outpath: Option[Path]
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
        Try(options.config.outpath.map(Paths.get(_))).toEither.map(outpath =>
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
          options.config.outpath
            .map(
              _.replace('/', File.separatorChar)
                .replace('\\', File.separatorChar)
            )
            .fold[Either[Throwable, String]](Right("scala-native")) {
              Paths
                .get(_)
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
      }
    for {
      clang <- toPathOrDiscover(options.nativeConfig.clang)(Discover.clang())
      clangPP <- toPathOrDiscover(options.nativeConfig.clangPP)(
        Discover.clangpp()
      )
      ltp <- LinktimePropertyParser.parseAll(options.nativeConfig.ltp)
      baseName <- resolveBaseName
      default = NativeConfig.empty
    } yield default
      .withMode(options.nativeConfig.mode)
      .withLTO(options.nativeConfig.lto)
      .withGC(options.nativeConfig.gc)
      .withLinkStubs(options.nativeConfig.linkStubs)
      .withCheck(options.nativeConfig.check)
      .withCheckFatalWarnings(options.nativeConfig.checkFatalWarnings)
      .withCheckFeatures(
        options.nativeConfig.checkFeatures.getOrElse(default.checkFeatures)
      )
      .withDump(options.nativeConfig.dump)
      .withOptimize(!options.nativeConfig.noOptimize)
      .withEmbedResources(options.nativeConfig.embedResources)
      .withResourceIncludePatterns(
        options.nativeConfig.resourceIncludePatterns match {
          case Nil      => default.resourceIncludePatterns
          case patterns => patterns
        }
      )
      .withResourceExcludePatterns(
        options.nativeConfig.resourceExcludePatterns match {
          case Nil      => default.resourceExcludePatterns
          case patterns => patterns
        }
      )
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
      .withMultithreading(options.nativeConfig.multithreading)
      .withSemanticsConfig(generateSemanticsConfig(options.semanticsConfig))
      .withSourceLevelDebuggingConfig(
        generateSourceLevelDebuggingConfig(options.sourceLevelDebuggingConfig)
      )
      .withServiceProviders(
        options.nativeConfig.serviceProviders.groupBy(_._1).map {
          case (key, values) => (key, values.map(_._2))
        }
      )
      .withSanitizer(
        options.nativeConfig.sanitizer.flatMap(sanitizerFromString)
      )
  }

  private def sanitizerFromString(v: String): Option[Sanitizer] = Seq(
    Sanitizer.ThreadSanitizer,
    Sanitizer.AddressSanitizer,
    Sanitizer.UndefinedBehaviourSanitizer
  ).find(_.name == v)

  private def generateOptimizerConfig(
      options: OptimizerConfigOptions
  ): OptimizerConfig = {
    val c0 = OptimizerConfig.empty
    val c1 = options.maxInlineDepth.foldLeft(c0)(_.withMaxInlineDepth(_))
    val c2 = options.maxCallerSize.foldLeft(c1)(_.withMaxCallerSize(_))
    val c3 = options.maxCalleeSize.foldLeft(c2)(_.withMaxCalleeSize(_))
    val c4 = options.maxInlineSize.foldLeft(c3)(_.withSmallFunctionSize(_))
    c4
  }

  private def generateSemanticsConfig(
      options: SemanticsConfigOptions
  ): SemanticsConfig = {
    val c0 = SemanticsConfig.default
    val c1 =
      options.finalFields.map(_.convert).foldLeft(c0)(_.withFinalFields(_))
    val c2 =
      options.strictExternCalls.foldLeft(c1)(_.withStrictExternCallSemantics(_))
    c2
  }

  private def generateSourceLevelDebuggingConfig(
      options: SourceLevelDebuggingConfigOptions
  ): SourceLevelDebuggingConfig = {
    val c0 = SourceLevelDebuggingConfig.disabled.withCustomSourceRoots(
      options.customSourceRoots
    )
    val c1 = options.enabled.foldLeft(c0)(_.enabled(_))
    val c2 = options.genFunctionSourcePositions.foldLeft(c1)(
      _.generateFunctionSourcePositions(_)
    )
    val c3 = options.genLocalVariables.foldLeft(c2)(_.generateLocalVariables(_))
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
