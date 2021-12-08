package scala.scalanative.cli

import org.scalatest.flatspec.AnyFlatSpec
import java.nio.file.Paths
import scala.scalanative.cli.utils.ConfigConverter
import scala.scalanative.cli.options.CliOptions
import scala.scalanative.cli.options.LoggerOptions
import scala.scalanative.cli.options.NativeConfigOptions
import scala.scalanative.cli.options.ConfigOptions
import scala.scalanative.build.GC
import scala.scalanative.cli.utils.NativeConfigParserImplicits
import scala.scalanative.build.Mode
import scala.scalanative.build.LTO

class ConfigConverterTest extends AnyFlatSpec {
  val dummyLoggerOptions = LoggerOptions()
  val dummyNativeConfigOptions = NativeConfigOptions()
  val dummyConfigOptions = ConfigOptions()

  val dummyArguments = Seq("$Main", "A.nir", "B.nir") // TODO incomplete arguments

  val dummyCliOptions: CliOptions = CliOptions(
    config = dummyConfigOptions,
    nativeConfig = dummyNativeConfigOptions,
    logger = dummyLoggerOptions
  )

  "ArgParser" should "parse default options" in {
    val config = ConfigConverter.convert(dummyCliOptions, dummyArguments)
    assert(config.isRight)
  }

  //TODO incomplete arguments check reporting

  // it should "return default nativeConfig which lines up with empty nativeConfig" in {
  //   val withInternallyDiscovered = 
  //     CliOptions(
  //       dummyConfigOptions,
  //       NativeConfigOptions(clang = Some(""), clangPP = Some("")),
  //       logger = dummyLoggerOptions
  //     )
  //   val result = ConfigConverter.convert(withInternallyDiscovered, dummyArguments)
  //   assert(result.right.get.config.compilerConfig == NativeConfig.empty)
  // }

  it should "parse classpath strings correctly" in {
    val classPathStrings = Seq("/home/dir/file", "/home/dirfile2", "/home/dir/with spaces/") // check case app passing with spaces
    val expected = Seq(
      Paths.get("/home/dir/file"),
      Paths.get("/home/dirfile2"),
      Paths.get("/home/dir/with spaces/")
    )

    val config = ConfigConverter.convert(dummyCliOptions, Seq("$Main") ++ classPathStrings)

    assert(config != None)
    assert(config.right.get.config.classPath.sameElements(expected))
  }

  it should "handle NativeConfig GC correctly" in {
    def gcAssertion(gcString: String, expectedGC: GC) = {
      val options = CliOptions(
        dummyConfigOptions,
        NativeConfigOptions(gc = NativeConfigParserImplicits.gcParser(None, gcString).right.get),
        dummyLoggerOptions
      )
      val config = ConfigConverter.convert(options, dummyArguments).right.get.config
      assert(config.compilerConfig.gc == expectedGC)
    }
    gcAssertion("immix", GC.immix)
    gcAssertion("commix", GC.commix)
    gcAssertion("none", GC.none)
    gcAssertion("boehm", GC.boehm)
  }

  it should "handle NativeConfig Mode correctly" in {
    def modeAssertion(modeString: String, expectedMode: Mode) = {
      val options = CliOptions(
        dummyConfigOptions,
        NativeConfigOptions(mode = NativeConfigParserImplicits.modeParser(None, modeString).right.get),
        dummyLoggerOptions
      )
      val config = ConfigConverter.convert(options, dummyArguments).right.get.config
      assert(config.compilerConfig.mode == expectedMode)
    }
    modeAssertion("debug", Mode.debug)
    modeAssertion("release-fast", Mode.releaseFast)
    modeAssertion("release-full", Mode.releaseFull)
  }

  it should "handle NativeConfig LTO correctly" in {
    def ltoAssertion(ltoString: String, expectedLto: LTO) = {
      val options = CliOptions(
        dummyConfigOptions,
        NativeConfigOptions(lto = NativeConfigParserImplicits.ltoParser(None, ltoString).right.get),
        dummyLoggerOptions
      )
      val config = ConfigConverter.convert(options, dummyArguments).right.get.config
      assert(config.compilerConfig.lto == expectedLto)
    }
    ltoAssertion("none", LTO.none)
    ltoAssertion("thin", LTO.thin)
    ltoAssertion("full", LTO.full)
  }

  // it should "parse log level options correctly" in {
  //   def logLevelAssertion(
  //       logLevelOptions: Array[String],
  //       expectedLogger: FilteredLogger
  //   ) = {
  //     val options = dummyOptions ++ logLevelOptions

  //     val obtained = BuildOptionsParser(options).right.get.get.config
  //     assert(obtained.logger == expectedLogger)
  //   }

  //   logLevelAssertion(
  //     Array("--disable-error"),
  //     new FilteredLogger(
  //       logDebug = true,
  //       logInfo = true,
  //       logWarn = true,
  //       logError = false
  //     )
  //   )
  //   logLevelAssertion(
  //     Array("--disable-warn"),
  //     new FilteredLogger(
  //       logDebug = true,
  //       logInfo = true,
  //       logWarn = false,
  //       logError = true
  //     )
  //   )
  //   logLevelAssertion(
  //     Array("--disable-info"),
  //     new FilteredLogger(
  //       logDebug = true,
  //       logInfo = false,
  //       logWarn = true,
  //       logError = true
  //     )
  //   )
  //   logLevelAssertion(
  //     Array("--disable-debug"),
  //     new FilteredLogger(
  //       logDebug = false,
  //       logInfo = true,
  //       logWarn = true,
  //       logError = true
  //     )
  //   )
  // }

  it should "set clang and clang++ correctly" in {
    val clangString = "/tmp/clang"
    val clangPPString = "tmp/clang++"

    val expectedClangPath = Paths.get(clangString)
    val expectedClangPPPath = Paths.get(clangPPString)

    val options = CliOptions(
      dummyConfigOptions, 
      NativeConfigOptions(clang=Some(clangString), clangPP = Some(clangPPString)),
      dummyLoggerOptions
    )

    val nativeConfig = ConfigConverter.convert(options, dummyArguments).right.get

    assert(nativeConfig.config.compilerConfig.clang == expectedClangPath)
    assert(nativeConfig.config.compilerConfig.clangPP == expectedClangPPPath)
  }
}
