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
import java.nio.file.Files

object ScalaNativeP extends CaseApp[POptions] {

  def run(options: POptions, args: RemainingArgs): Unit = {
    if (options.misc.version) {
      println(BuildInfo.nativeVersion)
      exit(0)
    }

    if (args.all.isEmpty) {
      if (options.nirFiles)
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

    if (options.nirFiles) deserializeFiles(classpath, args.all)
    else deserializeClassesOrObjects(classpath, args.all)
  }

  private def deserializeClassesOrObjects(
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
          case Some(defns) =>
            defns
              .sortBy(_.name.mangle)
              .foreach { d =>
                println(d.show)
                println()
              }
          case None => fail(s"Not found class/object with name `${className}`")
        }
      }
    }
  }

  private def deserializeFiles(
      classpath: List[Path],
      args: Seq[String]
  ): Unit = {

    case class VirtDirFile(file: Path, virtDir: VirtualDirectory)

    // Paths obtained from VirtualDirectory.files() are decoded as absolute,
    // but actually are relative to the classpath.
    // Users are expected to pass relative paths, like in javap and scalajsp.
    // This is a workaround to standardize the two to allow comparisons.
    // It is worth noting that it's impossible to pass actual absolute directory here
    // and lose that data as it should be impossible for a child of a directory to be root.
    def normalizedVirtDirPathString(path: Path) = {
      val fileString = path.toString()
      if (path.isAbsolute()) fileString.substring(1)
      else fileString
    }

    Scope { implicit scope =>
      val allVirtDirFiles =
        for {
          path <- classpath
          virtDir = VirtualDirectory.real(path)
          file <- virtDir.files
        } yield VirtDirFile(file, virtDir)

      for {
        fileName <- args
      } {

        val foundFileMaybe = allVirtDirFiles.find {
          case VirtDirFile(file, virtDir) =>
            val compString = normalizedVirtDirPathString(file)
            !Files.isDirectory(file) && compString == fileName
        }

        foundFileMaybe match {
          case Some(VirtDirFile(file, virtDir)) =>
            // Used for an internal NIR compatibility assertion message
            val bufferName = file.getFileName().toString()

            val fileByteBuffer = virtDir.read(file)
            val defns = deserializeBinary(fileByteBuffer, bufferName)

            defns
              .sortBy(_.name.mangle)
              .foreach { d =>
                println(d.show)
                println()
              }
          case None => fail(s"Not found file with name `${fileName}`")
        }
      }
    }
  }

  private def fail(msg: String): Nothing = {
    Console.err.println(msg)
    exit(1)
  }

}
