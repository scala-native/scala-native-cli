package scala.scalanative.cli

import scala.scalanative.build.Build
import scala.scalanative.util.Scope
import scala.scalanative.cli.utils.ConfigConverter
import scala.scalanative.cli.utils.NativeConfigParserImplicits._
import scala.scalanative.cli.options.LinkerOptions
import scala.scalanative.cli.options.BuildInfo
import scala.scalanative.build.Mode
import scala.scalanative.build.LTO
import scala.scalanative.build.GC

object ScalaNativeLd {

  def main(args: Array[String]): Unit = {
    val parser = new scopt.OptionParser[LinkerOptions]("scala-native-p") {
      override def errorOnUnknownArgument = false
      head("scala-native-ld", BuildInfo.nativeVersion)
      arg[String]("classpath")
        .hidden()
        .optional()
        .unbounded()
        .action((x, c) => c.copy(classpath = c.classpath :+ x))
      
      note("Config options:")
      opt[String]("main")
        .valueName("<main>")
        .optional()
        .action((x, c) => c.copy(config = c.config.copy(main = Some(x))))
        .text("Required main class.")
      opt[String]('o', "outpath")
        .valueName("<output-path>")
        .optional()
        .action((x, c) => c.copy(config = c.config.copy(outpath = x)))
        .text("Path of the resulting output binary. [./scala-native-out]")
      opt[String]("workdir")
        .valueName("<path-to-directory>")
        .optional()
        .action((x, c) => c.copy(config = c.config.copy(workdir = x)))
        .text("Scala Native working directory. [.]")

      note("Native Config options:")
      opt[Mode]("mode")
        .valueName("<mode> (debug, release-fast or release-full)")
        .optional()
        .action((x, c) => c.copy(nativeConfig = c.nativeConfig.copy(mode = x)))
        .text("Scala Native compilation mode. [debug]")
      opt[LTO]("lto")
        .valueName("<mode> (none, thin or full)")
        .optional()
        .action((x, c) => c.copy(nativeConfig = c.nativeConfig.copy(lto = x)))
        .text("Link-time optimisation mode. [none]")
      opt[GC]("gc")
        .valueName("<gc> (immix. commix, boehm, or none)")
        .optional()
        .action((x, c) => c.copy(nativeConfig = c.nativeConfig.copy(gc = x)))
        .text("Used garbage collector. [immix]")
      opt[Unit]("link-stubs")
        .optional()
        .action((x, c) => c.copy(nativeConfig = c.nativeConfig.copy(linkStubs = true)))
        .text("Should stubs be linked? [false]")
      opt[Unit]("check")
        .optional()
        .action((x, c) => c.copy(nativeConfig = c.nativeConfig.copy(check = true)))
        .text("Shall linker check that NIR is well-formed after every phase? [false]")
      opt[Unit]("check-fatal-warnings")
        .optional()
        .action((x, c) => c.copy(nativeConfig = c.nativeConfig.copy(checkFatalWarnings = true)))
        .text("Shall linker NIR check treat warnings as errors? [false]")
      opt[Unit]("dump")
        .optional()
        .action((x, c) => c.copy(nativeConfig = c.nativeConfig.copy(dump = true)))
        .text("Shall linker dump intermediate NIR after every phase? [false]")
      opt[Unit]("no-optimize")
        .optional()
        .action((x, c) => c.copy(nativeConfig = c.nativeConfig.copy(noOptimize = true)))
        .text("Should the resulting NIR code be not optimized? [false]")
      opt[String]("ltp")
        .valueName("<keystring=value>")
        .optional()
        .action((x, c) => c.copy(nativeConfig = c.nativeConfig.copy(ltp = c.nativeConfig.ltp :+ x)))
        .text("User defined properties resolved at link-time. Multiple can be defined. Example: \"isCli=true\"")
      opt[String]("linking-option")
        .valueName("<passed-option>")
        .optional()
        .unbounded()
        .action((x, c) => c.copy(nativeConfig = c.nativeConfig.copy(linkingOption = c.nativeConfig.linkingOption :+ x)))
        .text("Linking options passed to LLVM. Multiple can be defined.")
      opt[String]("compile-option")
        .valueName("<passed-option>")
        .optional()
        .unbounded()
        .action((x, c) => c.copy(nativeConfig = c.nativeConfig.copy(compileOption = c.nativeConfig.compileOption :+ x)))
        .text("Compilation options passed to LLVM. Multiple can be defined.")
      opt[String]("target-triple")
        .valueName("<config-string>")
        .optional()
        .action((x, c) => c.copy(nativeConfig = c.nativeConfig.copy(targetTriple = Some(x))))
        .text("Target triple. Defines OS, ABI and CPU architectures.")
      opt[String]("clang")
        .valueName("<path-to-clang>")
        .optional()
        .action((x, c) => c.copy(nativeConfig = c.nativeConfig.copy(clang = Some(x))))
        .text("Path to the `clang` executable. Internally discovered if not specified.")
      opt[String]("clang++")
        .valueName("<path-to-clang++>")
        .optional()
        .action((x, c) => c.copy(nativeConfig = c.nativeConfig.copy(clangPP = Some(x))))
        .text("Path to the `clang++` executable. Internally discovered if not specified.")
      
      note("Logger options:")
      opt[Unit]("verbose")
        .abbr("v")
        .optional()
        .unbounded()
        .action((x, c) => c.copy(logger = c.logger.copy(verbose = c.logger.verbose + 1)))
      
      note("Help options:")
      help("help")
        .text("Print this usage text and exit.")
      version("version")
        .text("Print scala-native-cli version and exit.")
    }

    parser.parse(args, LinkerOptions()) match {
      case Some(config) =>
        runLd(config)
      case _ =>
        // arguments are of bad format, scopt will have displayed errors automatically
        sys.exit(1)
    }
  }

  def runLd(options: LinkerOptions) = {
    if (options.misc.version) {
      println(BuildInfo.nativeVersion)
    } else if (options.config.main.isEmpty) {
      println("Required option not specified: --main")
      sys.exit(1)
    } else {
      val (ignoredArgs, classpath) = options.classpath.partition(_.startsWith("-"))
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
