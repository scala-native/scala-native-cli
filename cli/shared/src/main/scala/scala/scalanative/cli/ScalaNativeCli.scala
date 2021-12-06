package scala.scalanative.cli

import scala.scalanative.build.Build
import scala.scalanative.util.Scope
import System._
import scala.scalanative.cli.utils.{BuildOptionsParser, BuildOptions}

object ScalaNativeCli {
  def main(options: Array[String]): Unit = {
    val configOption = BuildOptionsParser(options)

    configOption match {
      case Right(Some(BuildOptions(config, outpath))) =>
        Scope { implicit scope =>
          Build.build(config, outpath)
        }
      case Left(exception) =>
        err.println(exception.getMessage())
        sys.exit(1)
      case Right(None) => // when --help or --version
    }
  }
}
