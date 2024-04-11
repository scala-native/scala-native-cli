package scala.scalanative.cli.options

import scopt.OptionParser
import scala.scalanative.build

case class SemanticsConfigOptions(
    finalFields: Option[JVMMemoryModelCompliance] = None,
    strictExternCalls: Option[Boolean] = None
)

sealed abstract class JVMMemoryModelCompliance {
  import JVMMemoryModelCompliance._
  def convert: build.JVMMemoryModelCompliance = this match {
    case None    => build.JVMMemoryModelCompliance.None
    case Relaxed => build.JVMMemoryModelCompliance.Relaxed
    case Strict  => build.JVMMemoryModelCompliance.Strict
  }
}
object JVMMemoryModelCompliance {
  case object None extends JVMMemoryModelCompliance
  case object Relaxed extends JVMMemoryModelCompliance
  case object Strict extends JVMMemoryModelCompliance

  implicit val read: scopt.Read[JVMMemoryModelCompliance] =
    scopt.Read.reads {
      case "none"    => JVMMemoryModelCompliance.None
      case "relaxed" => JVMMemoryModelCompliance.Relaxed
      case "strict"  => JVMMemoryModelCompliance.Strict
    }
}

object SemanticsConfigOptions {
  def set(parser: OptionParser[LinkerOptions]) = {
    def update(c: LinkerOptions)(
        fn: SemanticsConfigOptions => SemanticsConfigOptions
    ) =
      c.copy(semanticsConfig = fn(c.semanticsConfig))
    parser.note("Semantics options:")
    parser
      .opt[JVMMemoryModelCompliance]("final-fields-semantics")
      .valueName("<final-fields-semantics> (none, relaxed, or stricts)")
      .optional()
      .action((x, c) => update(c)(_.copy(finalFields = Some(x))))
      .text(
        "JVM memory model compliance for final, either 'none' (no special handling), 'relaxed' (default, synchronization of annotated fiedls), `strict` (synchronization of all final fields)"
      )
    parser
      .opt[Boolean]("strict-extern-call-semantics")
      .optional()
      .action((x, c) => update(c)(_.copy(strictExternCalls = Some(x))))
      .text(
        "Should runtime notify GC when entering unmanaged (foreign, native) code. When enabled every extern call would notify GC, otherwise (default) only annotated blocking methods notify GC."
      )
  }
}
