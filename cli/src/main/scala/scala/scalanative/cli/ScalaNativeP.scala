package scala.scalanative.cli

import caseapp.core.app.CaseApp
import scala.scalanative.cli.options.POptions
import caseapp.core.RemainingArgs
import scala.scalanative.io.VirtualDirectory
import scala.scalanative.util.Scope
import scala.scalanative.nir.serialization.deserializeBinary
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files
import java.nio.ByteBuffer
import scala.scalanative.cli.options.BuildInfo
import scala.collection.mutable
import java.io.File

object ScalaNativeP extends CaseApp[POptions] {

  def run(options: POptions, args: RemainingArgs): Unit = {
    if (options.misc.version) {
      println(BuildInfo.nativeVersion)
    } else {
      if (args.all.isEmpty) {
        System.err.println("Required NIR file not specified.")
        exit(1)
      } else {
        val allPaths = args.all.map(Paths.get(_))
        val classpathMaybe = options.classpath.map(_.split(File.pathSeparator))
        Scope { implicit scope =>
          findAndPrintDefns(allPaths, classpathMaybe)
        }
      }
    }
  }

  private def findAndPrintDefns(
      filePaths: Seq[Path],
      classpathMaybe: Option[Array[String]]
  )(implicit scope: Scope) =
    classpathMaybe match {
      case None => // find directly
        var notFound = false
        filePaths.foreach { filePath =>
          if (Files.exists(filePath)) {
            printDefns(filePath, None)
          } else {
            notFound = true
            System.err.println(s"File $filePath not found.\n")
          }
        }
        if (notFound) exit(1)

      case Some(classpath) => // find in classpath
        val notYetFound = mutable.Set(filePaths.map(_.toString()): _*)

        classpath.map { sourceString =>
          val sourcePath = Paths.get(sourceString)
          val usingVirtualDirectory =
            Files.isDirectory(sourcePath) || sourceString.endsWith(".jar")

          val (virtDirMaybe, foundFiles) =
            if (usingVirtualDirectory) {
              val virtDir = VirtualDirectory.real(sourcePath)
              (Some(virtDir), virtDir.files)
            } else {
              (None, Seq(sourcePath))
            }

          foundFiles
            .filter { file =>
              val compString = normalizedVirtDirPathString(file)

              !Files.isDirectory(file) && notYetFound.contains(compString)
            }
            .map { file =>
              printDefns(file, virtDirMaybe)
              notYetFound.remove(normalizedVirtDirPathString(file))
            }
        }

        notYetFound.foreach { filePath =>
          System.err.println(s"File $filePath not found.\n")
        }
        if (!notYetFound.isEmpty) exit(1)
    }

  private def printDefns(file: Path, sourceDirMaybe: Option[VirtualDirectory])(
      implicit scope: Scope
  ) = {
    val fileByteBuffer = sourceDirMaybe match {
      case Some(virtDir) => virtDir.read(file)
      case None          => byteBufferFromRegularFile(file)
    }

    // Used for an internal NIR compatibility assertion message
    val bufferName = file.getFileName().toString()

    val defns = deserializeBinary(fileByteBuffer, bufferName)

    defns
      .filter(_ != null)
      .sortBy(_.name)
      .foreach { defn =>
        println(defn.show)
      }
    println()
  }

  // Paths obtained from VirtualDirectory.files() are coded as absolute, 
  // but actually are relative to the classpath.
  // Users are expected to pass relative paths, like in javap and scalajsp.
  // This is a workaround to standardize the two to allow comparisons.
  // It is worth noting that it's impossible to pass actual absolute directory here
  // and lose that data as it should be impossible for a child of a directory to be root.
  private def normalizedVirtDirPathString(path: Path) = {
    val fileString = path.toString()
    if (path.isAbsolute()) fileString.substring(1)
    else fileString
  }

  private def byteBufferFromRegularFile(file: Path): ByteBuffer = {
    val bytes = Files.readAllBytes(file)
    val buffer = ByteBuffer.wrap(bytes)
    buffer
  }

}
