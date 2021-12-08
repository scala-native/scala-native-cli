package scala.scalanative.cli

import org.scalatest.flatspec.AnyFlatSpec
import scala.scalanative.cli.utils.LinktimePropertyParser

class LinktimePropertyParserTest extends AnyFlatSpec {

  "LinktimePropertyParser" should "handle boolean value types correctly" in {
    val input = List("isTesting=[Boolean]true")
    val expected = Map[String, Any]("isTesting" -> true)
    val obtained = LinktimePropertyParser.toMap(input)
    assert(obtained.right.get == expected)
  }

  it should "return error on undefined string patterns" in {
    val noEquals = List("key-[String]value")
    val obtainedNoEquals = LinktimePropertyParser.toMap(noEquals)
    assert(obtainedNoEquals.isLeft)

    val noType = List("key=value")
    val obtainedNoType = LinktimePropertyParser.toMap(noEquals)
    assert(obtainedNoType.isLeft)
  }

  it should "handle numeric value types correctly" in {
    val input = List(
      "testKey0=[Short]1",
      "testKey1=[Int]10",
      "testKey2=[Long]10000",
      "testKey3=[Float]1.20",
      "testKey4=[Double]20.20",
      "testKey5=[Char]35"
    )
    val expected = Map[String, Any](
      "testKey0" -> 1.asInstanceOf[Short],
      "testKey1" -> 10.asInstanceOf[Int],
      "testKey2" -> 10000.asInstanceOf[Long],
      "testKey3" -> (1.20).asInstanceOf[Float],
      "testKey4" -> (20.20).asInstanceOf[Double],
      "testKey5" -> 35.asInstanceOf[Char]
    )
    val obtained = LinktimePropertyParser.toMap(input)
    assert(obtained.right.get == expected)
  }

  it should "handle string value types correctly" in {
    val input = List(
      "testKey0=[String]1",
      "testKey1=[String]windows",
      "testKey2=[String]",
      "testKey3=[String]with space",
      "testKey4=[String]20.20"
    )
    val expected = Map[String, Any](
      "testKey0" -> "1",
      "testKey1" -> "windows",
      "testKey2" -> "",
      "testKey3" -> "with space",
      "testKey4" -> "20.20"
    )
    val obtained = LinktimePropertyParser.toMap(input)
    assert(obtained.right.get == expected)
  }

  it should "fail on unhandled value types" in {
    val inputObject = List("testKey=[Object]1")
    assert(LinktimePropertyParser.toMap(inputObject).isLeft)

    val inputOption = List("testKey=[Option[Int]]Some(1)")
    assert(LinktimePropertyParser.toMap(inputOption).isLeft)

    val inputMessy = List("testKey=[[Int]]1")
    assert(LinktimePropertyParser.toMap(inputMessy).isLeft)
  }

  it should "fail on incorect value against their defined types" in {
    val inputIntString = List("testKey=[Int]stringvalue")
    assert(LinktimePropertyParser.toMap(inputIntString).isLeft)

    val inputBooleanNumber = List("testKey=[Boolean]20")
    assert(LinktimePropertyParser.toMap(inputBooleanNumber).isLeft)

    val inputDoubleEmpty = List("testKey=[Double]")
    assert(LinktimePropertyParser.toMap(inputDoubleEmpty).isLeft)
  }

  it should "handle square brackets and equals in string value correctly" in {
    val inputString = List(
      "testKey0=[String]string[]value",
      "testKey1=[String][] after brackets",
      "testKey2=[String]with=equals"
    )

    val expected = Map[String, Any](
      "testKey0" -> "string[]value",
      "testKey1" -> "[] after brackets",
      "testKey2" -> "with=equals"
    )

    assert(LinktimePropertyParser.toMap(inputString).right.get == expected)
  }
}
