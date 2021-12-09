package scala.scalanative.cli.utils

import org.scalatest.flatspec.AnyFlatSpec

class LinktimePropertyParserTest extends AnyFlatSpec {

  "LinktimePropertyParser" should "handle boolean value types correctly" in {
    val input = List("isTesting=true", "isNotTesting=False")
    val expected =
      Map[String, Any]("isTesting" -> "true", "isNotTesting" -> "False")
    val obtained = LinktimePropertyParser.parseAll(input)
    assert(obtained.right.get == expected)
  }

  it should "return error on undefined string patterns" in {
    val noEquals = List("key-true")
    val obtainedNoEquals = LinktimePropertyParser.parseAll(noEquals)
    assert(obtainedNoEquals.isLeft)

    val noKey = List("=true")
    val obtainedNoKey = LinktimePropertyParser.parseAll(noKey)
    assert(obtainedNoKey.isLeft)
  }

}
