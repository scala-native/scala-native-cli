package scala.scalanative.cli.utils

import scala.scalanative.build.NativeConfig

import scala.scalanative.cli.options.CliOptions

object VersionSpecificOptionsIncluder {
  def withVersionSpecificOptions(
      options: CliOptions,
      nativeConfig: NativeConfig
  ): Either[Throwable, NativeConfig] = {
    Right(nativeConfig)
  }
}
