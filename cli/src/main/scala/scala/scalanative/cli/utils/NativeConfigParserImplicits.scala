package scala.scalanative.cli.utils

import scala.scalanative.build.LTO
import scala.scalanative.build.GC
import scala.scalanative.build.Mode

import caseapp.core.argparser.SimpleArgParser
import caseapp.core.argparser.ArgParser

object NativeConfigParserImplicits {

  implicit val ltoParser: ArgParser[LTO] =
    SimpleArgParser.from[LTO]("lto") {
      case "none" => Right(LTO.none)
      case "thin" => Right(LTO.thin)
      case "full" => Right(LTO.full)
      case other =>
        Left(
          caseapp.core.Error.UnrecognizedArgument(other)
        )
    }

  implicit val gcParser: ArgParser[GC] =
    SimpleArgParser.from[GC]("gc") {
      case "immix"  => Right(GC.immix)
      case "commix" => Right(GC.commix)
      case "boehm"  => Right(GC.boehm)
      case "none"   => Right(GC.none)
      case other =>
        Left(
          caseapp.core.Error.UnrecognizedArgument(other)
        )
    }

  implicit val modeParser: ArgParser[Mode] =
    SimpleArgParser.from[Mode]("mode") {
      case "debug"        => Right(Mode.debug)
      case "release-fast" => Right(Mode.releaseFast)
      case "release-full" => Right(Mode.releaseFull)
      case other =>
        Left(
          caseapp.core.Error.UnrecognizedArgument(other)
        )
    }
}
