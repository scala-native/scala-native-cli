package scala.scalanative.cli.utils

import scala.scalanative.build.Logger

class FilteredLogger(
    private val logDebug: Boolean,
    private val logInfo: Boolean,
    private val logWarn: Boolean,
    private val logError: Boolean
) extends Logger {

  private val underlying = Logger.default

  override def trace(msg: Throwable): Unit = underlying.trace(msg)
  override def debug(msg: String): Unit =
    if (logDebug) underlying.debug(msg)
  override def info(msg: String): Unit =
    if (logInfo) underlying.info(msg)
  override def warn(msg: String): Unit =
    if (logWarn) underlying.warn(msg)
  override def error(msg: String): Unit =
    if (logError) underlying.error(msg)

  override def equals(other: Any): Boolean = {
    if (other.isInstanceOf[FilteredLogger]) {
      val that = other.asInstanceOf[FilteredLogger]
      this.logDebug == that.logDebug && this.logInfo == that.logInfo && this.logWarn == that.logWarn && this.logError == that.logError
    } else {
      false
    }
  }

}
