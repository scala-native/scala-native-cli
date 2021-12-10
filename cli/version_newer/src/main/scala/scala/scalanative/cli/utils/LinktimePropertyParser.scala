package scala.scalanative.cli.utils

import java.util.regex.Pattern
import scala.util.Try

object LinktimePropertyParser {

  val ltpPattern = Pattern.compile("(.*?)=(.*)")

  @inline
  def getLtpPatternException(incorrectPattern: String) =
    new IllegalArgumentException(
      s"""|Link-time resolved properties must be of pattern keystring=boolean
          |\"${incorrectPattern}\" is incorrect.""".stripMargin
    )

  def toMap(ltpStrings: List[String]): Either[Throwable, Map[String, Any]] = {
    val eitherList =
      ltpStrings
        .map { inputPattern =>
          val matcher = ltpPattern.matcher(inputPattern)
          if (
            !matcher
              .find() || matcher.groupCount() != 2 || matcher.group(1).isEmpty()
          ) {
            Left(getLtpPatternException(inputPattern))
          } else {
            val (key, value) = (matcher.group(1), matcher.group(2))
            Right((key, value))
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
}
