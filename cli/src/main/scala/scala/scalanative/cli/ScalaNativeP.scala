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
import scala.annotation.tailrec
import java.nio.ByteBuffer

object ScalaNativeP extends CaseApp[PrinterOptions] {

  def run(options: PrinterOptions, args: RemainingArgs): Unit = {
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
      val cp = classpath.toStream.map(VirtualDirectory.real(_))
      def virtualDirPathMatches(
          virtualPath: Path,
          regularPath: Path
      ): Boolean = {
        // Paths in jars are always using Unix path denotation
        val relativeInJar = virtualPath.toString().stripPrefix("/")
        relativeInJar == regularPath.toString()
      }
      @tailrec
      def findAndRead(
          classpath: Stream[VirtualDirectory],
          path: Path
      ): Option[ByteBuffer] = {
        classpath match {
          case Stream.Empty => None
          case dir #:: tail =>
            val found = dir.files
              .find(virtualDirPathMatches(_, path))
              .map(dir.read(_))
            if (found.isEmpty) findAndRead(tail, path)
            else found
        }
      }
      for {
        fileName <- args
        path = Paths.get(fileName).normalize()
        content <- findAndRead(cp, path)
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
