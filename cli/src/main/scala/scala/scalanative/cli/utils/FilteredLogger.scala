package scala.scalanative.cli.utils

import java.lang.System.err

import scala.scalanative.build.Logger

class FilteredLogger(
    private val verbosity: Int
) extends Logger {

  private val underlying: Logger =
    // Like Logger.default, but sending everything to stderr
    new Logger {
      def trace(msg: Throwable): Unit = err.println(s"[trace] $msg")
      def debug(msg: String): Unit = err.println(s"[debug] $msg")
      def info(msg: String): Unit = err.println(s"[info] $msg")
      def warn(msg: String): Unit = err.println(s"[warn] $msg")
      def error(msg: String): Unit = err.println(s"[error] $msg")
    }

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
