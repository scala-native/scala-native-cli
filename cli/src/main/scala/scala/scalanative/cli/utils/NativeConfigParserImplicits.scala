package scala.scalanative.cli.utils

import scala.scalanative.build.LTO
import scala.scalanative.build.GC
import scala.scalanative.build.Mode

object NativeConfigParserImplicits {

  implicit val ltoRead: scopt.Read[LTO] =
    scopt.Read.reads{
      case "none" => LTO.none
      case "thin" => LTO.thin
      case "full" => LTO.full
      case other => throw new IllegalArgumentException(other)
    }

  implicit val gcRead: scopt.Read[GC] =
    scopt.Read.reads {
      case "immix"  => GC.immix
      case "commix" => GC.commix
      case "boehm"  => GC.boehm
      case "none"   => GC.none
      case other => throw new IllegalArgumentException(other)
    }

  implicit val modeRead: scopt.Read[Mode] =
    scopt.Read.reads {
      case "debug"        => Mode.debug
      case "release-fast" => Mode.releaseFast
      case "release-full" => Mode.releaseFull
      case other => throw new IllegalArgumentException(other)
    }
}
