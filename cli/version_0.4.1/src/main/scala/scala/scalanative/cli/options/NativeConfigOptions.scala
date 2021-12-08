package scala.scalanative.cli.options

import caseapp._
import scala.scalanative.build.LTO
import scala.scalanative.build.Mode
import scala.scalanative.build.GC

case class NativeConfigOptions(
    @Group("Native Config")
    @HelpMessage("Scala Native compilation mode. [debug]")
    @ValueDescription("<mode> (debug, release-fast or release-full)")
    mode: Mode = Mode.debug,
    @Group("Native Config")
    @HelpMessage("Link-time optimisation mode. [none]")
    @ValueDescription("<mode> (none, thin or full)")
    lto: LTO = LTO.none,
    @Group("Native Config")
    @HelpMessage("Used garbage collector. [immix]")
    @ValueDescription("<gc> (immix. commix, boehm, or none)")
    gc: GC = GC.immix,
    @Group("Native Config")
    @HelpMessage("Should stubs be linked? [false]")
    linkStubs: Boolean = false,
    @Group("Native Config")
    @HelpMessage(
      "Shall linker check that NIR is well-formed after every phase? [false]"
    )
    check: Boolean = false,
    @Group("Native Config")
    @HelpMessage("Shall linker NIR check treat warnings as errors? [false]")
    checkFatalWarnings: Boolean = false,
    @Group("Native Config")
    @HelpMessage(
      "Shall linker dump intermediate NIR after every phase? [false]"
    )
    dump: Boolean = false,
    @Group("Native Config")
    @HelpMessage("Should the resulting NIR code be not optimized? [false]")
    noOptimize: Boolean = false,
    @Group("Native Config")
    @HelpMessage(
      "User defined properties resolved at link-time. Multiple can be defined. Example: \"isCli=[Boolean]true\""
    )
    @ValueDescription("<keystring=[type]property>")
    ltp: List[String] = List.empty,
    @Group("Native Config")
    @HelpMessage("Linking options passed to LLVM. Multiple can be defined.")
    @ValueDescription("<passed-option>")
    linkingOption: List[String] = List.empty,
    @Group("Native Config")
    @HelpMessage("Compilation options passed to LLVM. Multiple can be defined.")
    @ValueDescription("<passed-option>")
    compileOption: List[String] = List.empty,
    @Group("Native Config")
    @HelpMessage("Target triple. Defines OS, ABI and CPU architectures.")
    @ValueDescription("<config-string>")
    targetTriple: Option[String] = None,
    @Group("Native Config")
    @HelpMessage(
      "Path to the `clang` executable. Internally discovered if not specified."
    )
    @ValueDescription("<path-to-clang>")
    clang: Option[String] = None,
    @Group("Native Config")
    @Name("clang++")
    @HelpMessage(
      "Path to the `clang++` executable. Internally discovered if not specified."
    )
    @ValueDescription("<path-to-clang++>")
    clangPP: Option[String] = None
)
