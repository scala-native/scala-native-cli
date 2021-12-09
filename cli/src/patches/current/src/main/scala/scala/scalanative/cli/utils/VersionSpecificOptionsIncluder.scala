package scala.scalanative.cli.utils

import scala.scalanative.build.NativeConfig

import _root_.scala.scalanative.cli.options.CliOptions
import scala.util.Try

private[utils] object VersionSpecificOptionsIncluder {
  def withVersionSpecificOptions(
      options: CliOptions,
      baseNativeConfig: NativeConfig
  ): Either[Throwable, NativeConfig] = {
    generateNativeConfigWithLTP(options, baseNativeConfig).map {
      _.withCheckFatalWarnings(options.nativeConfig.checkFatalWarnings)
    }
  }
  private def generateNativeConfigWithLTP(
      options: CliOptions,
      baseNativeConfig: NativeConfig
  ): Either[Throwable, NativeConfig] = {
    LinktimePropertyParser
      .parseAll(options.nativeConfig.ltp)
      .flatMap { ltpMap =>
        Try(
          baseNativeConfig.withLinktimeProperties(ltpMap)
        ).toEither
      }
  }
}
