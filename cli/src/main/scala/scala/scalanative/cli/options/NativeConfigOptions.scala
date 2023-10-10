package scala.scalanative.cli.options

import scopt.OptionParser

import scala.scalanative.build._
import scala.scalanative.cli.utils.NativeConfigParserImplicits._

case class NativeConfigOptions(
    mode: Mode = Mode.debug,
    buildTarget: BuildTarget = BuildTarget.application,
    lto: LTO = LTO.none,
    gc: GC = GC.immix,
    linkStubs: Boolean = false,
    check: Boolean = false,
    checkFatalWarnings: Boolean = false,
    dump: Boolean = false,
    noOptimize: Boolean = false,
    embedResources: Boolean = false,
    multithreadingSupport: Boolean = true,
    debugMetadata: Boolean = false,
    incrementalCompilation: Boolean = false,
    baseName: Option[String] = None,
    ltp: List[String] = List.empty,
    linkingOption: List[String] = List.empty,
    compileOption: List[String] = List.empty,
    targetTriple: Option[String] = None,
    clang: Option[String] = None,
    clangPP: Option[String] = None
)

object NativeConfigOptions {
  def set(parser: OptionParser[LinkerOptions]) = {
    parser.note("Native Config options:")
    parser
      .opt[Mode]("mode")
      .valueName("<mode> (debug, release-fast or release-full)")
      .optional()
      .action((x, c) => c.copy(nativeConfig = c.nativeConfig.copy(mode = x)))
      .text("Scala Native compilation mode. [debug]")
    parser
      .opt[BuildTarget]("build-target")
      .valueName(
        "<build-target> (application, library-dynamic or library-static)"
      )
      .optional()
      .action((x, c) =>
        c.copy(nativeConfig = c.nativeConfig.copy(buildTarget = x))
      )
      .text("Scala Native build target. [application]")
    parser
      .opt[LTO]("lto")
      .valueName("<mode> (none, thin or full)")
      .optional()
      .action((x, c) => c.copy(nativeConfig = c.nativeConfig.copy(lto = x)))
      .text("Link-time optimisation mode. [none]")
    parser
      .opt[GC]("gc")
      .valueName("<gc> (immix. commix, boehm, or none)")
      .optional()
      .action((x, c) => c.copy(nativeConfig = c.nativeConfig.copy(gc = x)))
      .text("Used garbage collector. [immix]")
    parser
      .opt[Unit]("link-stubs")
      .optional()
      .action((x, c) =>
        c.copy(nativeConfig = c.nativeConfig.copy(linkStubs = true))
      )
      .text("Should stubs be linked? [false]")
    parser
      .opt[Unit]("check")
      .optional()
      .action((x, c) =>
        c.copy(nativeConfig = c.nativeConfig.copy(check = true))
      )
      .text(
        "Shall linker check that NIR is well-formed after every phase? [false]"
      )
    parser
      .opt[Unit]("check-fatal-warnings")
      .optional()
      .action((x, c) =>
        c.copy(nativeConfig = c.nativeConfig.copy(checkFatalWarnings = true))
      )
      .text("Shall linker NIR check treat warnings as errors? [false]")
    parser
      .opt[Unit]("dump")
      .optional()
      .action((x, c) => c.copy(nativeConfig = c.nativeConfig.copy(dump = true)))
      .text("Shall linker dump intermediate NIR after every phase? [false]")
    parser
      .opt[Unit]("no-optimize")
      .optional()
      .action((x, c) =>
        c.copy(nativeConfig = c.nativeConfig.copy(noOptimize = true))
      )
      .text("Should the resulting NIR code be not optimized? [false]")
    parser
      .opt[Unit]("embed-resources")
      .optional()
      .action((x, c) =>
        c.copy(nativeConfig = c.nativeConfig.copy(embedResources = true))
      )
      .text("Shall resources file be embeded into executable? [false]")
    parser
      .opt[Unit]("incremental-compilation")
      .optional()
      .action((x, c) =>
        c.copy(nativeConfig =
          c.nativeConfig.copy(incrementalCompilation = true)
        )
      )
      .text(
        "Shall use incremental compilation mode for builds? (experimental) [false]"
      )
    parser
      .opt[Boolean]("multithreading")
      .abbr("-mt")
      .optional()
      .action((x, c) =>
        c.copy(nativeConfig = c.nativeConfig.copy(multithreadingSupport = x))
      )
      .text(
        "Should the target enable multihreading support for builds? [true]"
      )
    parser
      .opt[Boolean]("debug-info")
      .abbr("-g")
      .optional()
      .action((x, c) =>
        c.copy(nativeConfig = c.nativeConfig.copy(debugMetadata = x))
      )
      .text(
        "Should the build include additional debug information? These can be used for better stacktraces or debuging support [false]"
      )
    parser
      .opt[String]("base-name")
      .optional()
      .action((x, c) =>
        c.copy(nativeConfig =
          c.nativeConfig
            .copy(baseName = Some(x).map(_.trim()).filter(_.nonEmpty))
        )
      )
      .text(
        "Base name (without extension) used to generate names for build outputs. If empty `--base-name` would be resolved from `--outpath`"
      )
    parser
      .opt[String]("ltp")
      .valueName("<keystring=value>")
      .optional()
      .action((x, c) =>
        c.copy(nativeConfig =
          c.nativeConfig.copy(ltp = c.nativeConfig.ltp :+ x)
        )
      )
      .text(
        "User defined properties resolved at link-time. Multiple can be defined. Example: \"isCli=true\""
      )
    parser
      .opt[String]("linking-option")
      .valueName("<passed-option>")
      .optional()
      .unbounded()
      .action((x, c) =>
        c.copy(nativeConfig =
          c.nativeConfig.copy(linkingOption = c.nativeConfig.linkingOption :+ x)
        )
      )
      .text("Linking options passed to LLVM. Multiple can be defined.")
    parser
      .opt[String]("compile-option")
      .valueName("<passed-option>")
      .optional()
      .unbounded()
      .action((x, c) =>
        c.copy(nativeConfig =
          c.nativeConfig.copy(compileOption = c.nativeConfig.compileOption :+ x)
        )
      )
      .text("Compilation options passed to LLVM. Multiple can be defined.")
    parser
      .opt[String]("target-triple")
      .valueName("<config-string>")
      .optional()
      .action((x, c) =>
        c.copy(nativeConfig = c.nativeConfig.copy(targetTriple = Some(x)))
      )
      .text("Target triple. Defines OS, ABI and CPU architectures.")
    parser
      .opt[String]("clang")
      .valueName("<path-to-clang>")
      .optional()
      .action((x, c) =>
        c.copy(nativeConfig = c.nativeConfig.copy(clang = Some(x)))
      )
      .text(
        "Path to the `clang` executable. Internally discovered if not specified."
      )
    parser
      .opt[String]("clang-pp")
      .abbr("-clang++")
      .valueName("<path-to-clang++>")
      .optional()
      .action((x, c) =>
        c.copy(nativeConfig = c.nativeConfig.copy(clangPP = Some(x)))
      )
      .text(
        "Path to the `clang++` executable. Internally discovered if not specified."
      )
  }
}
