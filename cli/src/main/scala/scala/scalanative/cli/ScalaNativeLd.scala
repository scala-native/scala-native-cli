package scala.scalanative.cli

import java.nio.file._
import scala.scalanative.build._
import scala.scalanative.util.Scope
import scala.scalanative.cli.utils.ConfigConverter
import scala.scalanative.cli.options._
import scala.concurrent._
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

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
      OptimizerConfigOptions.set(this)
      SemanticsConfigOptions.set(this)
      SourceLevelDebuggingConfigOptions.set(this)

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
    val needsMain = options.nativeConfig.buildTarget match {
      case BuildTarget.Application => true
      case _: BuildTarget.Library  => false
    }
    if (needsMain && options.config.main.isEmpty) {
      System.err.println("Required option not specified: --main")
      sys.exit(1)
    } else {
      val (ignoredArgs, classpath) =
        options.classpath.partition(_.startsWith("-"))
      ignoredArgs.foreach { arg =>
        System.err.println(s"Unrecognised argument: ${arg}")
      }
      val main = options.config.main
      val buildOptionsMaybe = ConfigConverter.convert(options, main, classpath)

      buildOptionsMaybe match {
        case Left(thrown) =>
          System.err.println(thrown.getMessage())
          sys.exit(1)
        case Right(buildOptions) =>
          val outpath = Paths.get(options.config.outpath)
          val build = Scope { implicit scope =>
            Build
              .build(buildOptions.config)
              .map(
                Files.copy(
                  _,
                  outpath,
                  StandardCopyOption.REPLACE_EXISTING,
                  StandardCopyOption.COPY_ATTRIBUTES
                )
              )
          }
          Await.result(build, Duration.Inf)

      }
    }
  }
}
