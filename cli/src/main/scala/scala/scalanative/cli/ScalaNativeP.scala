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
import java.nio.file.Path
import scala.scalanative.io.VirtualDirectory
import scala.scalanative.nir.serialization.deserializeBinary
import scala.scalanative.nir.Defn

object ScalaNativeP extends CaseApp[POptions] {

  def run(options: POptions, args: RemainingArgs): Unit = {
    if (options.misc.version) {
      println(BuildInfo.nativeVersion)
      exit(0)
    }

    if (args.all.isEmpty) {
      if (options.fromPath)
        System.err.println("Required NIR file not specified.")
      else
        System.err.println("Required class/object not specified.")
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

    if (options.fromPath) printFromFiles(classpath, args.all)
    else printFromNames(classpath, args.all)
  }

  private def printFromNames(
      classpath: List[Path],
      args: Seq[String]
  ): Unit = {
    Scope { implicit scope =>
      val classLoader =
        ClassLoader.fromDisk {
          Config.empty.withClassPath(classpath)
        }

      for {
        className <- args
      } {
        classLoader.load(Global.Top(className)) match {
          case Some(defns) => printNIR(defns)
          case None => fail(s"Not found class/object with name `${className}`")
        }
      }
    }
  }

  private def printFromFiles(
      classpath: List[Path],
      args: Seq[String]
  ): Unit = {

    Scope { implicit scope =>
      val cp = classpath.map(VirtualDirectory.real(_))
      def isVirtualDirPathEqual(
          virtualPath: Path,
          regularPath: Path
      ): Boolean = {
        // Paths in jars are always using Unix path denotation
        val relativeInJar = virtualPath.toString().stripPrefix("/")
        relativeInJar == regularPath.toString()
      }
      def virtualDirHasPath(dir: VirtualDirectory, path: Path): Boolean = {
        dir.files.exists(isVirtualDirPathEqual(_, path))
      }
      for {
        fileName <- args
        path = Paths.get(fileName).normalize()
        content <- cp
          .collectFirst {
            case d if virtualDirHasPath(d, path) =>
              // Paths in VirtualDirectories have a seperate, incomparable FileSystem
              // Correct path has to be chosen from the read ones
              val correctedPath = d.files.find { p =>
                val relativeInJar = p.toString().stripPrefix("/")
                relativeInJar == path.toString()
              }
              d.read(correctedPath.get)
          }
          .orElse(fail(s"Not found file with name `${fileName}`"))
      } {
        val defns = deserializeBinary(content, fileName)
        printNIR(defns)
      }
    }
  }

  private def printNIR(defns: Seq[Defn]) =
    defns
      .sortBy(_.name.mangle)
      .foreach { d =>
        println(d.show)
        println()
      }

  private def fail(msg: String): Nothing = {
    Console.err.println(msg)
    exit(1)
  }

}
