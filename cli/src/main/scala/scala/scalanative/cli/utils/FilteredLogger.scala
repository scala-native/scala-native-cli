package scala.scalanative.cli.utils

import scala.scalanative.build.Logger

class FilteredLogger(
    private val verbosity: Int
) extends Logger {

  private val underlying = Logger.default

  override def trace(msg: Throwable): Unit = underlying.trace(msg)
  override def debug(msg: String): Unit =
    if (verbosity >= 3) underlying.debug(msg)
  override def info(msg: String): Unit =
    if (verbosity >= 2) underlying.info(msg)
  override def warn(msg: String): Unit =
    if (verbosity >= 1) underlying.warn(msg)
  override def error(msg: String): Unit =
    underlying.error(msg)

  override def equals(other: Any): Boolean = {
    if (other.isInstanceOf[FilteredLogger]) {
      val that = other.asInstanceOf[FilteredLogger]
      this.verbosity == that.verbosity
    } else {
      false
    }
  }

}
