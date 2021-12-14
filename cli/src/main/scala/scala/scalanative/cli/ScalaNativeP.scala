package scala.scalanative.cli

import caseapp.core.app.CaseApp
import caseapp.core.RemainingArgs
import java.nio.file.Paths
import java.io.File
import scala.scalanative.util.Scope
import scala.scalanative.cli.options._
import scala.scalanative.nir.Global
import scala.scalanative.build.Config
import scala.scalanative.linker.ClassLoader

object ScalaNativeP extends CaseApp[POptions] {

  def run(options: POptions, args: RemainingArgs): Unit = {
    if (options.misc.version) {
      println(BuildInfo.nativeVersion)
      exit(0)
    }

    if (args.all.isEmpty) {
      System.err.println("Required NIR file not specified.")
      exit(1)
    }

    val (classpath, ignoredPaths) =
      options.classpath
        .flatMap(_.split(File.pathSeparator))
        .map(Paths.get(_))
        .partition(_.toFile().exists())
    ignoredPaths.foreach { path =>
      System.err.println(s"Ignoring non existing path: $path")
    }

    Scope { implicit scope =>
      val classLoader =
        ClassLoader.fromDisk {
          Config.empty.withClassPath(classpath)
        }

      for {
        fileName <- args.all
      } {
        classLoader.load(Global.Top(fileName)) match {
          case Some(defns) =>
            defns
              .sortBy(_.name.mangle)
              .foreach { d =>
                println(d.show)
                println()
              }
          case None => fail(s"Not found class/object with name `${fileName}`")
        }
      }
    }
  }

  private def fail(msg: String): Nothing = {
    Console.err.println(msg)
    exit(1)
  }

}
