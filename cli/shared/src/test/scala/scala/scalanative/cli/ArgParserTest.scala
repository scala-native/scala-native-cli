package scala.scalanative.cli

import org.scalatest.flatspec.AnyFlatSpec
import java.nio.file.Paths
import scala.scalanative.build.NativeConfig
import scala.scalanative.build.GC
import scala.scalanative.build.Mode
import scala.scalanative.build.LTO
import scala.scalanative.cli.utils.BuildOptionsParser
import scala.scalanative.cli.utils.FilteredLogger

class ArgParserTest extends AnyFlatSpec {
  val dummyClassPath = "/dir/file"
  val dummyOutpath = "."
  val dummyMain = ""

  val dummyOptions = Array(
    "--outpath",
    dummyOutpath,
    "--class-path",
    dummyClassPath,
    "--main",
    dummyMain
  )

  "ArgParser" should "parse default settings" in {
    val config = BuildOptionsParser(dummyOptions)
    assert(config.isRight)
  }

  it should "return default nativeConfig which lines up with empty nativeConfig" in {
    val internallyModified = Array(
      "--clang",
      "",
      "--clang-pp",
      ""
    )
    val result = BuildOptionsParser(dummyOptions ++ internallyModified)
    assert(result.right.get.get.config.compilerConfig == NativeConfig.empty)
  }

  it should "parse classpath string correctly" in {
    val classPathString = "/home/dir/file:/home/dirfile2:/home/dir/with spaces/"
    val expected = Seq(
      Paths.get("/home/dir/file"),
      Paths.get("/home/dirfile2"),
      Paths.get("/home/dir/with spaces/")
    )

    val options = Array(
      "--outpath",
      dummyOutpath,
      "--class-path",
      classPathString,
      "--main",
      dummyMain
    )
    val config = BuildOptionsParser(options)

    assert(config != None)
    assert(config.right.get.get.config.classPath.sameElements(expected))
  }

  it should "handle NativeConfig GC correctly" in {
    def gcAssertion(gcString: String, expectedGC: GC) = {
      val options = dummyOptions ++ Array(
        "--gc",
        gcString
      )
      val config = BuildOptionsParser(options).right.get.get.config
      assert(config.compilerConfig.gc == expectedGC)
    }
    gcAssertion("immix", GC.immix)
    gcAssertion("commix", GC.commix)
    gcAssertion("none", GC.none)
    gcAssertion("boehm", GC.boehm)
  }

  it should "handle NativeConfig Mode correctly" in {
    def modeAssertion(modeString: String, expectedMode: Mode) = {
      val options = dummyOptions ++ Array(
        "--native-mode",
        modeString
      )
      val config = BuildOptionsParser(options).right.get.get.config
      assert(config.compilerConfig.mode == expectedMode)
    }
    modeAssertion("debug", Mode.debug)
    modeAssertion("release-fast", Mode.releaseFast)
    modeAssertion("release-full", Mode.releaseFull)
  }

  it should "handle NativeConfig LTO correctly" in {
    def ltoAssertion(ltoString: String, expectedLto: LTO) = {
      val options = dummyOptions ++ Array(
        "--lto",
        ltoString
      )
      val config = BuildOptionsParser(options).right.get.get.config
      assert(config.compilerConfig.lto == expectedLto)
    }
    ltoAssertion("none", LTO.none)
    ltoAssertion("thin", LTO.thin)
    ltoAssertion("full", LTO.full)
  }

  it should "parse log level options correctly" in {
    def logLevelAssertion(
        logLevelOptions: Array[String],
        expectedLogger: FilteredLogger
    ) = {
      val options = dummyOptions ++ logLevelOptions

      val obtained = BuildOptionsParser(options).right.get.get.config
      assert(obtained.logger == expectedLogger)
    }

    logLevelAssertion(
      Array("--disable-error"),
      new FilteredLogger(
        logDebug = true,
        logInfo = true,
        logWarn = true,
        logError = false
      )
    )
    logLevelAssertion(
      Array("--disable-warn"),
      new FilteredLogger(
        logDebug = true,
        logInfo = true,
        logWarn = false,
        logError = true
      )
    )
    logLevelAssertion(
      Array("--disable-info"),
      new FilteredLogger(
        logDebug = true,
        logInfo = false,
        logWarn = true,
        logError = true
      )
    )
    logLevelAssertion(
      Array("--disable-debug"),
      new FilteredLogger(
        logDebug = false,
        logInfo = true,
        logWarn = true,
        logError = true
      )
    )
  }

  it should "handle boolean options as opposite of default" in {
    val booleanOptions = dummyOptions ++ Array(
      "--check",
      "--no-optimize",
      "--link-stubs",
      "--dump"
    )
    val defaultNativeConfig =
      BuildOptionsParser(dummyOptions).right.get.get.config.compilerConfig
    val booleanNativeConfig =
      BuildOptionsParser(booleanOptions).right.get.get.config.compilerConfig

    assert(booleanNativeConfig.check == !defaultNativeConfig.check)
    assert(booleanNativeConfig.optimize == !defaultNativeConfig.optimize)
    assert(booleanNativeConfig.linkStubs == !defaultNativeConfig.linkStubs)
    assert(booleanNativeConfig.dump == !defaultNativeConfig.dump)
  }

  it should "set clang and clang++ correctly" in {
    val clangString = "/tmp/clang"
    val clangPPString = "tmp/clang++"

    val expectedClangPath = Paths.get(clangString)
    val expectedClangPPPath = Paths.get(clangPPString)

    val options = dummyOptions ++ Array(
      "--clang",
      clangString,
      "--clang-pp",
      clangPPString
    )

    val nativeConfig = BuildOptionsParser(options).right.get.get

    assert(nativeConfig.config.compilerConfig.clang == expectedClangPath)
    assert(nativeConfig.config.compilerConfig.clangPP == expectedClangPPPath)
  }
}
