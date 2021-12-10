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

  override def ignoreUnrecognized: Boolean = true

  def run(options: CliOptions, args: RemainingArgs) = {
    if (options.misc.version) {
      println(BuildInfo.nativeVersion)
    } else if (options.config.main.isEmpty) {
      println("Required option not specified: --main")
      exit(1)
    } else {
      val (ignoredArgs, classpath) = args.all.partition(_.startsWith("-"))
      ignoredArgs.foreach { arg =>
        println(s"Unrecognised argument: ${arg}")
      }
      val main = options.config.main.get
      val buildOptionsMaybe = ConfigConverter.convert(options, main, classpath)

      buildOptionsMaybe match {
        case Left(thrown) =>
          System.err.println(thrown.getMessage())
          exit(1)
        case Right(buildOptions) =>
          Scope { implicit scope =>
            Build.build(buildOptions.config, buildOptions.outpath)
          }
      }
    }
  }
}
