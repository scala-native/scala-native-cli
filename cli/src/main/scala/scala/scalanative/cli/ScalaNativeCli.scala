package scala.scalanative.cli

import scala.scalanative.build.Build
import scala.scalanative.util.Scope
import scala.scalanative.cli.utils.ConfigConverter
import scala.scalanative.cli.utils.NativeConfigParserImplicits._
import scala.scalanative.cli.options.CliOptions
import caseapp.core.app.CaseApp
import caseapp.core.RemainingArgs
import scala.scalanative.cli.options.BuildInfo

object ScalaNativeCli extends CaseApp[CliOptions] {

  def run(options: CliOptions, args: RemainingArgs) = {
    if (options.misc.version) {
      println(BuildInfo.nativeVersion)
    } else {
      println(scala.scalanative.cli.options.BuildInfo.nativeVersion)
      val positionalArgs = args.all
      val buildOptionsMaybe = ConfigConverter.convert(options, positionalArgs)

      buildOptionsMaybe match {
        case Left(thrown) => 
          System.err.println(thrown.getMessage()) 
        case Right(buildOptions) => 
          Scope { implicit scope =>
            Build.build(buildOptions.config, buildOptions.outpath)
          }
      }
    }
  }
}
