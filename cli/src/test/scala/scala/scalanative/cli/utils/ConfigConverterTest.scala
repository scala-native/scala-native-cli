package scala.scalanative.cli

import org.scalatest.flatspec.AnyFlatSpec
import java.nio.file.Paths
import scala.scalanative.cli.utils.ConfigConverter
import scala.scalanative.cli.options.LinkerOptions
import scala.scalanative.cli.options.NativeConfigOptions
import scala.scalanative.cli.options.ConfigOptions
import scala.scalanative.build.GC
import scala.scalanative.cli.utils.NativeConfigParserImplicits
import scala.scalanative.build.Mode
import scala.scalanative.build.LTO
import scala.scalanative.build.BuildTarget

class ConfigConverterTest extends AnyFlatSpec {
  val dummyConfigOptions = ConfigOptions(main = Some("Main"))

  val dummyArguments =
    Seq("A.jar", "B.jar")
  val dummyMain = "Main"

  val dummyLinkerOptions = LinkerOptions(
    classpath = dummyArguments.toList,
    config = dummyConfigOptions
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
        classpath = dummyArguments.toList,
        config = dummyConfigOptions,
        nativeConfig = NativeConfigOptions(gc = gc)
      )
      val parsed =
        ConfigConverter
          .convert(options, dummyMain, dummyArguments)
          .map(_.config.gc)

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
        classpath = dummyArguments.toList,
        config = dummyConfigOptions,
        nativeConfig = NativeConfigOptions(mode = mode)
      )
      val parsed =
        ConfigConverter
          .convert(options, dummyMain, dummyArguments)
          .map(_.config.compilerConfig.mode)
      assert(parsed.contains(expectedMode))
    }
    modeAssertion("debug", Mode.debug)
    modeAssertion("release-fast", Mode.releaseFast)
    modeAssertion("release-full", Mode.releaseFull)
    modeAssertion("release-size", Mode.releaseSize)
  }

  it should "handle NativeConfig LTO correctly" in {
    def ltoAssertion(ltoString: String, expectedLto: LTO) = {
      val lto = NativeConfigParserImplicits.ltoRead.reads(ltoString)
      val options = LinkerOptions(
        classpath = dummyArguments.toList,
        config = dummyConfigOptions,
        nativeConfig = NativeConfigOptions(lto = lto)
      )
      val parsed =
        ConfigConverter
          .convert(options, dummyMain, dummyArguments)
          .map(_.config.compilerConfig.lto)

      assert(parsed.contains(expectedLto))
    }
    ltoAssertion("none", LTO.none)
    ltoAssertion("thin", LTO.thin)
    ltoAssertion("full", LTO.full)
  }

  it should "handle NativeConfig Buildtarget correctly" in {
    def checkParser(stringValue: String, expected: BuildTarget) = {
      import NativeConfigParserImplicits.buildTargetRead
      val target = implicitly[scopt.Read[BuildTarget]].reads(stringValue)
      val options = LinkerOptions(
        classpath = dummyArguments.toList,
        config = dummyConfigOptions,
        nativeConfig = NativeConfigOptions(buildTarget = target)
      )
      val parsed =
        ConfigConverter
          .convert(options, dummyMain, dummyArguments)
          .map(_.config.compilerConfig.buildTarget)

      assert(parsed.contains(expected))
    }
    checkParser("application", BuildTarget.application)
    checkParser("app", BuildTarget.application)
    checkParser("library-dynamic", BuildTarget.libraryDynamic)
    checkParser("library-static", BuildTarget.libraryStatic)
  }

  it should "set clang and clang++ correctly" in {
    val clangString = "/tmp/clang"
    val clangPPString = "tmp/clang++"

    val expectedClangPath = Paths.get(clangString)
    val expectedClangPPPath = Paths.get(clangPPString)

    val options = LinkerOptions(
      classpath = dummyArguments.toList,
      config = dummyConfigOptions,
      nativeConfig = NativeConfigOptions(
        clang = Some(clangString),
        clangPP = Some(clangPPString)
      )
    )

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
      config = dummyConfigOptions,
      nativeConfig = NativeConfigOptions(
        check = true,
        dump = true,
        noOptimize = true,
        linkStubs = true,
        checkFatalWarnings = true
      )
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
        assert(nonDefault.checkFatalWarnings != default.checkFatalWarnings)
      }
    )
  }
}
