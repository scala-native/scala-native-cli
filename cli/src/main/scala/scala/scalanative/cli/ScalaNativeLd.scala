package scala.scalanative.cli

import scala.scalanative.build.Build
import scala.scalanative.util.Scope
import scala.scalanative.cli.utils.ConfigConverter
import scala.scalanative.cli.options.LinkerOptions
import scala.scalanative.cli.options.BuildInfo
import scala.scalanative.cli.options.ConfigOptions
import scala.scalanative.cli.options.NativeConfigOptions

object ScalaNativeLd {

  def main(args: Array[String]): Unit = {
    val parser = new scopt.OptionParser[LinkerOptions]("scala-native-ld") {
      override def errorOnUnknownArgument = false
      head("scala-native-ld", BuildInfo.nativeVersion)
      arg[String]("classpath")
        .hidden()
        .optional()
        .unbounded()
        .action((x, c) => c.copy(classpath = c.classpath :+ x))

      ConfigOptions.set(this)
      NativeConfigOptions.set(this)

      note("Logger options:")
      opt[Unit]("verbose")
        .abbr("v")
        .optional()
        .unbounded()
        .action((x, c) => c.copy(verbose = c.verbose + 1))
        .text(
          "Increase verbosity of internal logger. Can be specified multiple times."
        )

      note("Help options:")
      help('h', "help")
        .text("Print this usage text and exit.")
      version("version")
        .text("Print scala-native-cli version and exit.")
    }
    parser.parse(args, LinkerOptions()) match {
      case Some(config) =>
        runLd(config)
        sys.exit(0)
      case _ =>
        // arguments are of bad format, scopt will have displayed errors automatically
        sys.exit(1)
    }
  }

  def runLd(options: LinkerOptions) = {
    if (options.config.main.isEmpty) {
      println("Required option not specified: --main")
      sys.exit(1)
    } else {
      val (ignoredArgs, classpath) =
        options.classpath.partition(_.startsWith("-"))
      ignoredArgs.foreach { arg =>
        println(s"Unrecognised argument: ${arg}")
      }
      val main = options.config.main.get
      val buildOptionsMaybe = ConfigConverter.convert(options, main, classpath)

      buildOptionsMaybe match {
        case Left(thrown) =>
          System.err.println(thrown.getMessage())
          sys.exit(1)
        case Right(buildOptions) =>
          Scope { implicit scope =>
            Build.build(buildOptions.config, buildOptions.outpath)
          }
      }
    }
  }
}
