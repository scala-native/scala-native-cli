package scala.scalanative.cli

import org.scalatest.flatspec.AnyFlatSpec
import java.nio.file.Paths
import scala.scalanative.cli.utils.ConfigConverter
import scala.scalanative.cli.options.LinkerOptions
import scala.scalanative.cli.options.LoggerOptions
import scala.scalanative.cli.options.NativeConfigOptions
import scala.scalanative.cli.options.ConfigOptions
import scala.scalanative.build.GC
import scala.scalanative.cli.utils.NativeConfigParserImplicits
import scala.scalanative.build.Mode
import scala.scalanative.build.LTO
import scala.scalanative.cli.options.MiscOptions

class ConfigConverterTest extends AnyFlatSpec {
  val dummyLoggerOptions = LoggerOptions()
  val dummyNativeConfigOptions = NativeConfigOptions()
  val dummyConfigOptions = ConfigOptions(main = Some("Main"))
  val dummyMiscOptions = MiscOptions()

  val dummyArguments =
    Seq("A.jar", "B.jar")
  val dummyMain = "Main"

  val dummyLinkerOptions = LinkerOptions(
    config = dummyConfigOptions,
    nativeConfig = dummyNativeConfigOptions,
    logger = dummyLoggerOptions,
    misc = dummyMiscOptions
  )

  "ArgParser" should "parse default options" in {
    val config =
      ConfigConverter.convert(dummyLinkerOptions, dummyMain, dummyArguments)
    assert(config.isRight)
  }

  it should "report incomplete arguments" in {
    val noArgs = Seq()
    val mainOnlyResult =
      ConfigConverter.convert(dummyLinkerOptions, dummyMain, noArgs)
    assert(mainOnlyResult.left.exists(_.isInstanceOf[IllegalArgumentException]))
  }

  it should "parse classpath strings correctly" in {
    val classPathStrings = Seq(
      "/home/dir/file",
      "/home/dirfile2",
      "/home/dir/with spaces/"
    )
    val expected = Seq(
      Paths.get("/home/dir/file"),
      Paths.get("/home/dirfile2"),
      Paths.get("/home/dir/with spaces/")
    )

    val config =
      ConfigConverter.convert(dummyLinkerOptions, dummyMain, classPathStrings)

    assert(config.exists(_.config.classPath.sameElements(expected)))
  }

  it should "handle NativeConfig GC correctly" in {
    def gcAssertion(gcString: String, expectedGC: GC) = {
      val gc = NativeConfigParserImplicits.gcRead.reads(gcString)
      val options = LinkerOptions(
          dummyArguments.toList,
          dummyConfigOptions,
          NativeConfigOptions(gc = gc),
          dummyLoggerOptions,
          dummyMiscOptions
        )
      val parsed = for {
        converted <- ConfigConverter.convert(options, dummyMain, dummyArguments)
      } yield converted.config.gc
      assert(parsed.contains(expectedGC))
    }
    gcAssertion("immix", GC.immix)
    gcAssertion("commix", GC.commix)
    gcAssertion("none", GC.none)
    gcAssertion("boehm", GC.boehm)
  }

  it should "handle NativeConfig Mode correctly" in {
    def modeAssertion(modeString: String, expectedMode: Mode) = {
      val mode = NativeConfigParserImplicits.modeRead.reads(modeString)
      val options = LinkerOptions(
          dummyArguments.toList,
          dummyConfigOptions,
          NativeConfigOptions(mode = mode),
          dummyLoggerOptions,
          dummyMiscOptions
        )
      val parsed = for {
        converted <- ConfigConverter.convert(options, dummyMain, dummyArguments)
      } yield converted.config.compilerConfig.mode
      assert(parsed.contains(expectedMode))
    }
    modeAssertion("debug", Mode.debug)
    modeAssertion("release-fast", Mode.releaseFast)
    modeAssertion("release-full", Mode.releaseFull)
  }

  it should "handle NativeConfig LTO correctly" in {
    def ltoAssertion(ltoString: String, expectedLto: LTO) = {
      val lto = NativeConfigParserImplicits.ltoRead.reads(ltoString)
      val options = LinkerOptions(
          dummyArguments.toList,
          dummyConfigOptions,
          NativeConfigOptions(lto = lto),
          dummyLoggerOptions,
          dummyMiscOptions
        )
      val parsed = for {
        opts <- ConfigConverter
          .convert(options, dummyMain, dummyArguments)
      } yield opts.config.compilerConfig.lto

      assert(parsed.contains(expectedLto))
    }
    ltoAssertion("none", LTO.none)
    ltoAssertion("thin", LTO.thin)
    ltoAssertion("full", LTO.full)
  }

  it should "set clang and clang++ correctly" in {
    val clangString = "/tmp/clang"
    val clangPPString = "tmp/clang++"

    val expectedClangPath = Paths.get(clangString)
    val expectedClangPPPath = Paths.get(clangPPString)

    val options = LinkerOptions(
      dummyArguments.toList,
      dummyConfigOptions,
      NativeConfigOptions(
        clang = Some(clangString),
        clangPP = Some(clangPPString)
      ),
      dummyLoggerOptions,
      dummyMiscOptions
    )

    val nativeConfig =
      ConfigConverter
        .convert(options, dummyMain, dummyArguments)
        .map(_.config.compilerConfig)
        .fold(
          fail(_),
          { config =>
            assert(config.clang == expectedClangPath)
            assert(config.clangPP == expectedClangPPPath)
          }
        )

  }

  it should "parse boolean options as opposite of default" in {
    val options = LinkerOptions(
      classpath = dummyArguments.toList,
      dummyConfigOptions,
      NativeConfigOptions(
        check = true,
        dump = true,
        noOptimize = true,
        linkStubs = true
      ),
      dummyLoggerOptions,
      dummyMiscOptions
    )
    val parsed = for {
      default <- ConfigConverter
        .convert(dummyLinkerOptions, dummyMain, dummyArguments)
        .map(_.config.compilerConfig)
      nonDefault <- ConfigConverter
        .convert(options, dummyMain, dummyArguments)
        .map(_.config.compilerConfig)
    } yield (default, nonDefault)

    parsed.fold(
      fail(_),
      { case (default, nonDefault) =>
        assert(nonDefault.check != default.check)
        assert(nonDefault.dump != default.dump)
        assert(nonDefault.optimize != default.optimize)
        assert(nonDefault.linkStubs != default.linkStubs)
      }
    )
  }
}
