package scala.scalanative.cli.utils

import scala.scalanative.build._

object NativeConfigParserImplicits {

  implicit val ltoRead: scopt.Read[LTO] =
    scopt.Read.reads {
      case "none" => LTO.none
      case "thin" => LTO.thin
      case "full" => LTO.full
      case other  => throw new IllegalArgumentException(other)
    }

  implicit val gcRead: scopt.Read[GC] =
    scopt.Read.reads {
      case "immix"  => GC.immix
      case "commix" => GC.commix
      case "boehm"  => GC.boehm
      case "none"   => GC.none
      case other    => throw new IllegalArgumentException(other)
    }

  implicit val modeRead: scopt.Read[Mode] =
    scopt.Read.reads {
      case "debug"        => Mode.debug
      case "release-fast" => Mode.releaseFast
      case "release-size" => Mode.releaseSize
      case "release-full" => Mode.releaseFull
      case other          => throw new IllegalArgumentException(other)
    }

  implicit val buildTargetRead: scopt.Read[BuildTarget] =
    scopt.Read.reads {
      case "application" | "app" | "default"    => BuildTarget.application
      case "library-dynamic" | "library-shared" => BuildTarget.libraryDynamic
      case "library-static"                     => BuildTarget.libraryStatic
      case other => throw new IllegalArgumentException(other)
    }
}
