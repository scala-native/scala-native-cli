package scala.scalanative.cli.utils

import java.util.regex.Pattern
import scala.util.Try

object LinktimePropertyParser {

  val ltpPattern = Pattern.compile("(.*?)=(.*)")
  val valueTypePattern = Pattern.compile("\\[(.*?)\\](.*)")

  @inline
  def getLtpPatternException(incorrectPattern: String) =
    new IllegalArgumentException(
      s"""|Link-time resolved properties must be of pattern keystring=[Type]value
              |\"${incorrectPattern}\" is incorrect.""".stripMargin
    )
  @inline
  def getLtpTypeException(incorrectType: String) =
    new IllegalArgumentException(
      s"""|Unrecognised link-time property value type:
          |\"${incorrectType}\" is incorrect.""".stripMargin
    )

  def toMap(ltpStrings: List[String]): Either[Throwable, Map[String, Any]] = {
    val eitherList =
      ltpStrings
        .map { inputPattern =>
          val matcher = ltpPattern.matcher(inputPattern)
          if (!matcher.find() || matcher.groupCount() != 2) {
            Left(getLtpPatternException(inputPattern))
          } else {
            val (key, typedValue) = (matcher.group(1), matcher.group(2))
            handleTypedValue(inputPattern, typedValue) match {
              case Right(value) => Right((key, value))
              case Left(value)  => Left(value)
            }
          }
        }

    flattenEither(eitherList)
  }

  def flattenEither(
      eitherList: List[Either[Throwable, (String, Any)]]
  ): Either[Throwable, Map[String, Any]] = {
    val errors = eitherList.collect { case Left(error) => error }
    if (!errors.isEmpty) {
      Left(errors.head)
    } else {
      Right(eitherList.collect { case Right(value) => value }.toMap)
    }
  }

  def handleTypedValue(
      pattern: String,
      typedValue: String
  ): Either[Throwable, Any] = {
    val matcher = valueTypePattern.matcher(typedValue)

    if (!matcher.find() || matcher.groupCount() != 2) {
      Left(getLtpPatternException(pattern))
    } else {
      val valueType = matcher.group(1)
      val valueContent = matcher.group(2)

      Try {
        valueType match {
          case "Boolean" => valueContent.toBoolean
          case "Byte"    => valueContent.toByte
          case "Char"    => valueContent.toInt.toChar
          case "Short"   => valueContent.toShort
          case "Int"     => valueContent.toInt
          case "Long"    => valueContent.toLong
          case "Float"   => valueContent.toFloat
          case "Double"  => valueContent.toDouble
          case "String"  => valueContent
          case _         => throw getLtpTypeException(valueType)
        }
      }.toEither
    }
  }
}
