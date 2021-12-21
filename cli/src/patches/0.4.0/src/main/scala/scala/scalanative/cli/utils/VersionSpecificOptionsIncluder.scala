package scala.scalanative.cli.utils

import scala.scalanative.build.NativeConfig

import scala.scalanative.cli.options.LinkerOptions

private[utils] object VersionSpecificOptionsIncluder {
  def withVersionSpecificOptions(
      options: LinkerOptions,
      nativeConfig: NativeConfig
  ): Either[Throwable, NativeConfig] = {
    Right(nativeConfig)
  }
}
