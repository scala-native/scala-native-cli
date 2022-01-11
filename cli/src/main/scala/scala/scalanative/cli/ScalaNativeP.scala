package scala.scalanative.cli

import java.nio.file.Paths
import java.io.File
import scala.scalanative.util.Scope
import scala.scalanative.cli.options._
import scala.scalanative.nir._
import scala.scalanative.build.Config
import scala.scalanative.linker.ClassLoader
import java.nio.file.Path
import scala.scalanative.io.VirtualDirectory
import scala.scalanative.nir.serialization.deserializeBinary
import scala.annotation.tailrec
import java.nio.ByteBuffer

object ScalaNativeP {

  def main(args: Array[String]): Unit = {

    val parser = new scopt.OptionParser[PrinterOptions]("scala-native-p") {
      override def errorOnUnknownArgument = false

      head("scala-native-p", BuildInfo.nativeVersion)
      arg[String]("Class names")
        .hidden()
        .optional()
        .unbounded()
        .action((x, c) => c.copy(classNames = c.classNames :+ x))

      note("Help options:")
      help('h', "help")
        .text("Print this usage text and exit.")
      version("version")
        .text("Print scala-native-cli version and exit.")

      note("Other options:")
      PrinterOptions.set(this)
    }

    parser.parse(args, PrinterOptions()) match {
      case Some(config) =>
        runPrinter(config)
        sys.exit(0)
      case _ =>
        // arguments are of bad format, scopt will have displayed errors automatically
        sys.exit(1)
    }
  }

  private def runPrinter(options: PrinterOptions): Unit = {
    if (options.classNames.isEmpty) {
      if (options.fromPath)
        System.err.println("Required NIR file not specified.")
      else
        System.err.println("Required class/object not specified.")
      sys.exit(1)
    }

    val (classpath, ignoredPaths) =
      options.classpath
        .flatMap(_.split(File.pathSeparator))
        .map(Paths.get(_))
        .partition(_.toFile().exists())
    ignoredPaths.foreach { path =>
      System.err.println(s"Ignoring non existing path: $path")
    }

    if (options.fromPath) printFromFiles(classpath, options)
    else printFromNames(classpath, options)
  }

  private def printFromNames(
      classpath: List[Path],
      options: PrinterOptions
  ): Unit = {
    Scope { implicit scope =>
      val classLoader =
        ClassLoader.fromDisk {
          Config.empty.withClassPath(classpath)
        }

      for {
        className <- options.classNames
      } {
        classLoader.load(Global.Top(className)) match {
          case Some(defns) => printNIR(defns, options.verbose)
          case None => fail(s"Not found class/object with name `${className}`")
        }
      }
    }
  }

  private def printFromFiles(
      classpath: List[Path],
      options: PrinterOptions
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
      def findInClasspathAndRead(
          classpath: Stream[VirtualDirectory],
          path: Path
      ): Option[ByteBuffer] = {
        classpath match {
          case Stream.Empty => None
          case dir #:: tail =>
            val found = dir.files
              .find(virtualDirPathMatches(_, path))
              .map(dir.read(_))
            if (found.isEmpty) findInClasspathAndRead(tail, path)
            else found
        }
      }

      def tryReadFromPath(path: Path): Option[ByteBuffer] = {
        val file = path.toFile()
        val absPath = path.toAbsolutePath()
        // When classpath is explicitly provided don't try to read directly
        if (!options.usingDefaultClassPath || !file.exists()) None
        else
          util.Try {
            VirtualDirectory
              .real(absPath.getParent())
              .read(absPath.getFileName())
          }.toOption
      }

      for {
        fileName <- options.classNames
        path = Paths.get(fileName).normalize()
        content <-
          tryReadFromPath(path)
            .orElse(findInClasspathAndRead(cp, path))
            .orElse(fail(s"Not found file with name `${fileName}`"))
      } {
        val defns = deserializeBinary(content, fileName)
        printNIR(defns, options.verbose)
      }
    }
  }

  private def printNIR(defns: Seq[Defn], verbose: Boolean) =
    defns
      .map {
        case defn @ Defn.Define(attrs, name, ty, _) if !verbose =>
          Defn.Declare(attrs, name, ty)(defn.pos)
        case defn => defn
      }
      .sortBy(_.name.mangle)
      .foreach { d =>
        println(d.show)
        println()
      }

  private def fail(msg: String): Nothing = {
    Console.err.println(msg)
    sys.exit(1)
  }

}
