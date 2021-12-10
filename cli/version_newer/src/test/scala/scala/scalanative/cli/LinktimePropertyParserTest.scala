package scala.scalanative.cli

import org.scalatest.flatspec.AnyFlatSpec
import scala.scalanative.cli.utils.LinktimePropertyParser

class LinktimePropertyParserTest extends AnyFlatSpec {

  "LinktimePropertyParser" should "handle boolean value types correctly" in {
    val input = List("isTesting=true", "isNotTesting=False")
    val expected =
      Map[String, Any]("isTesting" -> "true", "isNotTesting" -> "False")
    val obtained = LinktimePropertyParser.toMap(input)
    assert(obtained.right.get == expected)
  }

  it should "return error on undefined string patterns" in {
    val noEquals = List("key-true")
    val obtainedNoEquals = LinktimePropertyParser.toMap(noEquals)
    assert(obtainedNoEquals.isLeft)

    val noKey = List("=true")
    val obtainedNoKey = LinktimePropertyParser.toMap(noKey)
    assert(obtainedNoKey.isLeft)
  }

}
