package scala.scalanative.cli.options

import scopt.OptionParser
import scala.scalanative.build.MemorySize
import scala.scalanative.build.GarbageCollectorConfig.LogLevel

case class GarbageCollectorConfigOptions(
    initialHeapSize: Option[MemorySize] = None,
    maxHeapSize: Option[MemorySize] = None,
    threadHeapBlockSize: Option[MemorySize] = None,
    threads: Option[Int] = None,
    timeRatio: Option[Double] = None,
    freeRatio: Option[Double] = None,
    maxFreeRatio: Option[Double] = None,
    syncTimeoutMs: Option[Long] = None,
    syncWarningIntervalMs: Option[Long] = None,
    logLevel: Option[LogLevel] = None,
    logFile: Option[String] = None,
    statsFile: Option[String] = None,
    statsEnabled: Option[Boolean] = None,
    trapBasedYieldPoints: Option[Boolean] = None
)

object GarbageCollectorConfigOptions {
  implicit val memorySizeRead: scopt.Read[MemorySize] =
    scopt.Read.reads(MemorySize.parse)

  implicit val logLevelRead: scopt.Read[LogLevel] =
    scopt.Read.reads(LogLevel(_))

  def set(parser: OptionParser[LinkerOptions]) = {
    def update(c: LinkerOptions)(
        fn: GarbageCollectorConfigOptions => GarbageCollectorConfigOptions
    ) =
      c.copy(gcConfig = fn(c.gcConfig))
    val sizeFormat = "<size> (bytes with optional k, m or g suffix)"
    parser.note(
      "Garbage collector options (compiled-in defaults, can be overridden at runtime by environment variables):"
    )
    parser
      .opt[MemorySize]("gc-initial-heap-size")
      .valueName(sizeFormat)
      .optional()
      .action((x, c) => update(c)(_.copy(initialHeapSize = Some(x))))
      .text("Initial size of the heap. [GC_INITIAL_HEAP_SIZE]")
    parser
      .opt[MemorySize]("gc-max-heap-size")
      .valueName(sizeFormat)
      .optional()
      .action((x, c) => update(c)(_.copy(maxHeapSize = Some(x))))
      .text("Maximum size of the heap. [GC_MAXIMUM_HEAP_SIZE]")
    parser
      .opt[MemorySize]("gc-thread-heap-block-size")
      .valueName(sizeFormat)
      .optional()
      .action((x, c) => update(c)(_.copy(threadHeapBlockSize = Some(x))))
      .text(
        "Size of heap block allocated by each thread, multithreaded none GC only. [GC_THREAD_HEAP_BLOCK_SIZE]"
      )
    parser
      .opt[Int]("gc-threads")
      .optional()
      .action((x, c) => update(c)(_.copy(threads = Some(x))))
      .text("Number of GC worker threads, commix only. [GC_NPROCS]")
    parser
      .opt[Double]("gc-time-ratio")
      .optional()
      .action((x, c) => update(c)(_.copy(timeRatio = Some(x))))
      .text(
        "Ratio of time spent in GC that triggers heap growth, immix and commix only. [GC_TIME_RATIO]"
      )
    parser
      .opt[Double]("gc-free-ratio")
      .optional()
      .action((x, c) => update(c)(_.copy(freeRatio = Some(x))))
      .text(
        "Ratio of free heap triggering concurrent collection, commix only. [GC_FREE_RATIO]"
      )
    parser
      .opt[Double]("gc-max-free-ratio")
      .optional()
      .action((x, c) => update(c)(_.copy(maxFreeRatio = Some(x))))
      .text(
        "Ratio of free heap above which heap would not grow, immix and commix only. [GC_MAX_FREE_RATIO]"
      )
    parser
      .opt[Long]("gc-sync-timeout")
      .valueName("<milliseconds>")
      .optional()
      .action((x, c) => update(c)(_.copy(syncTimeoutMs = Some(x))))
      .text(
        "Timeout for threads synchronization before collection, immix and commix only. [SCALANATIVE_GC_SYNC_TIMEOUT_MS]"
      )
    parser
      .opt[Long]("gc-sync-warning-interval")
      .valueName("<milliseconds>")
      .optional()
      .action((x, c) => update(c)(_.copy(syncWarningIntervalMs = Some(x))))
      .text(
        "Interval of warnings about threads not reaching synchronization point, immix and commix only. [SCALANATIVE_GC_SYNC_WARNING_INTERVAL_MS]"
      )
    parser
      .opt[LogLevel]("gc-log-level")
      .valueName("<level> (debug, info, warn, error or none)")
      .optional()
      .action((x, c) => update(c)(_.copy(logLevel = Some(x))))
      .text("GC log level. [SCALANATIVE_GC_LOG_LEVEL]")
    parser
      .opt[String]("gc-log-file")
      .valueName("<path>")
      .optional()
      .action((x, c) => update(c)(_.copy(logFile = Some(x))))
      .text("File to which GC logs are written. [GC_LOG_FILE]")
    parser
      .opt[String]("gc-stats-file")
      .valueName("<path>")
      .optional()
      .action((x, c) => update(c)(_.copy(statsFile = Some(x))))
      .text(
        "File to which GC statistics are written, requires --gc-stats-enabled. [GC_STATS_FILE]"
      )
    parser
      .opt[Boolean]("gc-stats-enabled")
      .optional()
      .action((x, c) => update(c)(_.copy(statsEnabled = Some(x))))
      .text("Shall GC statistics be collected, immix and commix only.")
    parser
      .opt[Boolean]("gc-trap-based-yieldpoints")
      .optional()
      .action((x, c) => update(c)(_.copy(trapBasedYieldPoints = Some(x))))
      .text(
        "Shall GC use trap-based yieldpoints, requires multithreading. [SCALANATIVE_GC_TRAP_BASED_YIELDPOINTS]"
      )
  }
}
