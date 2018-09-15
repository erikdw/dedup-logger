package org.deduplogger.logger;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Marker;
import org.slf4j.helpers.MessageFormatter;

import java.util.List;


/**
 * Wrapper class around org.slf4j.Logger interface. TODO : 1. flush on kill signal 2. flush on exit // *  3. iterator on
 * lru cache is leading to ConcurrentModificationException 4. flush the message when the memory usage is 75% of the
 * configured memory threshold 5. Implement memory footprint guarantee and make memory usage configurable 6. Two
 * flavors: 6.1 : Thread Level Cache implies low latency, low deduplication result 6.2 : Application Level Cache implies
 * high latency, high deduplication result
 */

public class DedupLogger implements org.slf4j.Logger {

  private static final int DEFAULT_LOG_CACHE_SIZE = 500;

  private static final int DEFAULT_LOG_CACHE_THRESHOLD = 1;

  private static final long DEFAULT_TIME_EXPIRATION_THRESHOLD = 10000;

  private static final long DEFAULT_MEMORY_LIMIT = 50;  // 50MB

  public static final int timeStampMessageLengthThreshold = 65000;

  private static LogCache lru;

  public final org.slf4j.Logger innerLogger;

  enum Level {
    TRACE, DEBUG, INFO, WARN, ERROR
  }

  public DedupLogger(org.slf4j.Logger innerLogger) {
    this(innerLogger, DEFAULT_LOG_CACHE_SIZE, DEFAULT_LOG_CACHE_THRESHOLD, DEFAULT_TIME_EXPIRATION_THRESHOLD,
         DEFAULT_TIME_EXPIRATION_THRESHOLD);
  }

  public DedupLogger(org.slf4j.Logger innerLogger, int logCacheSize, int logCacheThreshold,
                     long timeExpireThreshold, long memoryThreshold) {
    this.innerLogger = innerLogger;
    lru = new LogCache(logCacheSize, logCacheThreshold, timeExpireThreshold, memoryThreshold * 1024 * 1024);

    // TODO : Implement a shutdown hook in the future to gracefully eviction the cache contents when program is interrupted
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      flushMessageOnExit();
    }));
  }

  public LogCache getLogCache() {
    return lru;
  }

  private boolean checkIfLogIsDup(String msg) {
    return lru.checkIfDuplicate(msg);
  }

  private void logMessage(String msg, Level level, Marker marker) {
    switch (level) {
      case TRACE:
        if (isTraceEnabled()) {
          if (marker != null) {
            innerLogger.trace(marker, msg);
          } else {
            innerLogger.trace(msg);
          }
        }
        break;
      case DEBUG:
        if (isDebugEnabled()) {
          if (marker != null) {
            innerLogger.debug(marker, msg);
          } else {
            innerLogger.debug(msg);
          }
        }
        break;
      case INFO:
        if (isInfoEnabled()) {
          if (marker != null) {
            innerLogger.info(marker, msg);
          } else {
            innerLogger.info(msg);
          }
        }
        break;
      case WARN:
        if (isWarnEnabled()) {
          if (marker != null) {
            innerLogger.warn(marker, msg);
          } else {
            innerLogger.warn(msg);
          }
        }
        break;
      case ERROR:
        if (isErrorEnabled()) {
          if (marker != null) {
            innerLogger.error(marker, msg);
          } else {
            innerLogger.error(msg);
          }
        }
        break;
    }
  }

  /*
   * Flush all the messages when the program exits or being killed.
   */
  private void flushMessageOnExit() {
    //TODO : Replace the print with log
    System.out.println("Flush all the messages");
    List<String> evictionMessages = lru.flushAllMessages();
    for (String s : evictionMessages) {
      System.out.println(s);
    }
  }

  /*
   * Logic to handle cache eviction when cache size is full or when the message exceeds the time expiration limit
   */
  private void updateCache(String msg, Level level, Marker marker) {
    // Print all the evicted message
    List<String> evictionSummary = lru.generateSummaryMessage(msg);
    for (String str : evictionSummary) {
      logMessage(str, level, marker);
    }
  }

  /**
   * Return the name of this <code>Logger</code> instance.
   *
   * @return name of this logger instance
   */
  public String getName() {
    return innerLogger.getName();
  }

  /**
   * Is the logger instance enabled for the TRACE level?
   *
   * @return True if this Logger is enabled for the TRACE level, false otherwise.
   * @since 1.4
   */
  public boolean isTraceEnabled() {
    return innerLogger.isTraceEnabled();
  }

  /**
   * Log a message at the TRACE level.
   *
   * @param msg the message string to be logged
   * @since 1.4
   */
  public void trace(String msg) {
    synchronized (this) {
      boolean dup = checkIfLogIsDup(msg);
      if (dup) {
        // Do Nothing
      } else {
        logMessage(msg, Level.TRACE, null);
      }
      updateCache(msg, Level.TRACE, null);
    }
  }

  /**
   * Log a message at the TRACE level according to the specified format and argument.
   * <p/>
   * <p>This form avoids superfluous object creation when the logger
   * is disabled for the TRACE level. </p>
   *
   * @param format the format string
   * @param arg    the argument
   * @since 1.4
   */
  public void trace(String format, Object arg) {
    trace(MessageFormatter.format(format, arg).getMessage());
  }

  /**
   * Log a message at the TRACE level according to the specified format and arguments.
   * <p/>
   * <p>This form avoids superfluous object creation when the logger
   * is disabled for the TRACE level. </p>
   *
   * @param format the format string
   * @param arg1   the first argument
   * @param arg2   the second argument
   * @since 1.4
   */
  public void trace(String format, Object arg1, Object arg2) {
    trace(MessageFormatter.format(format, arg1, arg2).getMessage());
  }

  /**
   * Log a message at the TRACE level according to the specified format and arguments.
   * <p/>
   * <p>This form avoids superfluous string concatenation when the logger
   * is disabled for the TRACE level. However, this variant incurs the hidden (and relatively small) cost of creating
   * an
   * <code>Object[]</code> before invoking the method, even if this logger is disabled for TRACE. The variants taking
   * {@link #trace(String, Object) one} and {@link #trace(String, Object, Object) two} arguments exist solely in order
   * to avoid this hidden cost.</p>
   *
   * @param format    the format string
   * @param arguments a list of 3 or more arguments
   * @since 1.4
   */
  public void trace(String format, Object... arguments) {
    trace(MessageFormatter.arrayFormat(format, arguments).getMessage());
  }

  /**
   * Log an exception (throwable) at the TRACE level with an accompanying message.
   *
   * @param msg the message accompanying the exception
   * @param t   the exception (throwable) to log
   * @since 1.4
   */
  public void trace(String msg, Throwable t) {
    trace(String.format("%s\n%s", msg, ExceptionUtils.getStackTrace(t)));
  }

  /**
   * Similar to {@link #isTraceEnabled()} method except that the marker data is also taken into account.
   *
   * @param marker The marker data to take into consideration
   * @return True if this Logger is enabled for the TRACE level, false otherwise.
   * @since 1.4
   */
  public boolean isTraceEnabled(Marker marker) {
    return innerLogger.isTraceEnabled(marker);
  }

  /**
   * Log a message with the specific Marker at the TRACE level.
   *
   * @param marker the marker data specific to this log statement
   * @param msg    the message string to be logged
   * @since 1.4
   */
  public void trace(Marker marker, String msg) {
    synchronized (this) {
      boolean dup = checkIfLogIsDup(msg);
      if (dup) {
        // Do Nothing
      } else {
        logMessage(msg, Level.TRACE, marker);
      }
      updateCache(msg, Level.TRACE, marker);
    }
  }

  /**
   * This method is similar to {@link #trace(String, Object)} method except that the marker data is also taken into
   * consideration.
   *
   * @param marker the marker data specific to this log statement
   * @param format the format string
   * @param arg    the argument
   * @since 1.4
   */
  public void trace(Marker marker, String format, Object arg) {
    trace(marker, MessageFormatter.format(format, arg).getMessage());
  }

  /**
   * This method is similar to {@link #trace(String, Object, Object)} method except that the marker data is also taken
   * into consideration.
   *
   * @param marker the marker data specific to this log statement
   * @param format the format string
   * @param arg1   the first argument
   * @param arg2   the second argument
   * @since 1.4
   */
  public void trace(Marker marker, String format, Object arg1, Object arg2) {
    trace(marker, MessageFormatter.format(format, arg1, arg2).getMessage());
  }

  /**
   * This method is similar to {@link #trace(String, Object...)} method except that the marker data is also taken into
   * consideration.
   *
   * @param marker   the marker data specific to this log statement
   * @param format   the format string
   * @param argArray an array of arguments
   * @since 1.4
   */
  public void trace(Marker marker, String format, Object... argArray) {
    trace(marker, MessageFormatter.arrayFormat(format, argArray).getMessage());
  }

  /**
   * This method is similar to {@link #trace(String, Throwable)} method except that the marker data is also taken into
   * consideration.
   *
   * @param marker the marker data specific to this log statement
   * @param msg    the message accompanying the exception
   * @param t      the exception (throwable) to log
   * @since 1.4
   */
  public void trace(Marker marker, String msg, Throwable t) {
    trace(marker, String.format("%s\n%s", msg, ExceptionUtils.getStackTrace(t)));
  }

  /**
   * Is the logger instance enabled for the DEBUG level?
   *
   * @return True if this Logger is enabled for the DEBUG level, false otherwise.
   */
  public boolean isDebugEnabled() {
    return innerLogger.isDebugEnabled();
  }

  /**
   * Log a message at the DEBUG level.
   *
   * @param msg the message string to be logged
   */
  public void debug(String msg) {
    synchronized (this) {
      boolean dup = checkIfLogIsDup(msg);
      if (dup) {
        // Do Nothing
      } else {
        logMessage(msg, Level.DEBUG, null);
      }
      updateCache(msg, Level.DEBUG, null);
    }
  }

  /**
   * Log a message at the DEBUG level according to the specified format and argument.
   * <p/>
   * <p>This form avoids superfluous object creation when the logger
   * is disabled for the DEBUG level. </p>
   *
   * @param format the format string
   * @param arg    the argument
   */
  public void debug(String format, Object arg) {
    debug(MessageFormatter.format(format, arg).getMessage());
  }

  /**
   * Log a message at the DEBUG level according to the specified format and arguments.
   * <p/>
   * <p>This form avoids superfluous object creation when the logger
   * is disabled for the DEBUG level. </p>
   *
   * @param format the format string
   * @param arg1   the first argument
   * @param arg2   the second argument
   */
  public void debug(String format, Object arg1, Object arg2) {
    debug(MessageFormatter.format(format, arg1, arg2).getMessage());
  }

  /**
   * Log a message at the DEBUG level according to the specified format and arguments.
   * <p/>
   * <p>This form avoids superfluous string concatenation when the logger
   * is disabled for the DEBUG level. However, this variant incurs the hidden (and relatively small) cost of creating
   * an
   * <code>Object[]</code> before invoking the method, even if this logger is disabled for DEBUG. The variants taking
   * {@link #debug(String, Object) one} and {@link #debug(String, Object, Object) two} arguments exist solely in order
   * to avoid this hidden cost.</p>
   *
   * @param format    the format string
   * @param arguments a list of 3 or more arguments
   */
  public void debug(String format, Object... arguments) {
    debug(MessageFormatter.arrayFormat(format, arguments).getMessage());
  }

  /**
   * Log an exception (throwable) at the DEBUG level with an accompanying message.
   *
   * @param msg the message accompanying the exception
   * @param t   the exception (throwable) to log
   */
  public void debug(String msg, Throwable t) {
    debug(String.format("%s\n%s", msg, ExceptionUtils.getStackTrace(t)));
  }

  /**
   * Similar to {@link #isDebugEnabled()} method except that the marker data is also taken into account.
   *
   * @param marker The marker data to take into consideration
   * @return True if this Logger is enabled for the DEBUG level, false otherwise.
   */
  public boolean isDebugEnabled(Marker marker) {
    return innerLogger.isDebugEnabled();
  }

  /**
   * Log a message with the specific Marker at the DEBUG level.
   *
   * @param marker the marker data specific to this log statement
   * @param msg    the message string to be logged
   */
  public void debug(Marker marker, String msg) {
    synchronized (this) {
      boolean dup = checkIfLogIsDup(msg);
      if (dup) {
        // Do Nothing
      } else {
        logMessage(msg, Level.DEBUG, marker);
      }
      updateCache(msg, Level.DEBUG, marker);
    }
  }

  /**
   * This method is similar to {@link #debug(String, Object)} method except that the marker data is also taken into
   * consideration.
   *
   * @param marker the marker data specific to this log statement
   * @param format the format string
   * @param arg    the argument
   */
  public void debug(Marker marker, String format, Object arg) {
    debug(marker, MessageFormatter.format(format, arg).getMessage());
  }

  /**
   * This method is similar to {@link #debug(String, Object, Object)} method except that the marker data is also taken
   * into consideration.
   *
   * @param marker the marker data specific to this log statement
   * @param format the format string
   * @param arg1   the first argument
   * @param arg2   the second argument
   */
  public void debug(Marker marker, String format, Object arg1, Object arg2) {
    debug(marker, MessageFormatter.format(format, arg1, arg2).getMessage());
  }

  /**
   * This method is similar to {@link #debug(String, Object...)} method except that the marker data is also taken into
   * consideration.
   *
   * @param marker    the marker data specific to this log statement
   * @param format    the format string
   * @param arguments a list of 3 or more arguments
   */
  public void debug(Marker marker, String format, Object... arguments) {
    debug(marker, MessageFormatter.arrayFormat(format, arguments).getMessage());
  }

  /**
   * This method is similar to {@link #debug(String, Throwable)} method except that the marker data is also taken into
   * consideration.
   *
   * @param marker the marker data specific to this log statement
   * @param msg    the message accompanying the exception
   * @param t      the exception (throwable) to log
   */
  public void debug(Marker marker, String msg, Throwable t) {
    debug(marker, String.format("%s\n%s", msg, ExceptionUtils.getStackTrace(t)));
  }

  /**
   * Is the logger instance enabled for the INFO level?
   *
   * @return True if this Logger is enabled for the INFO level, false otherwise.
   */
  public boolean isInfoEnabled() {
    return innerLogger.isInfoEnabled();
  }

  /**
   * Log a message at the INFO level.
   *
   * @param msg the message string to be logged
   */
  public void info(String msg) {
    synchronized (this) {
      boolean dup = checkIfLogIsDup(msg);
      if (dup) {
        // Do Nothing
      } else {
        logMessage(msg, Level.INFO, null);
      }
      updateCache(msg, Level.INFO, null);
    }
  }

  /**
   * Log a message at the INFO level according to the specified format and argument.
   * <p/>
   * <p>This form avoids superfluous object creation when the logger
   * is disabled for the INFO level. </p>
   *
   * @param format the format string
   * @param arg    the argument
   */
  public void info(String format, Object arg) {
    info(MessageFormatter.format(format, arg).getMessage());
  }

  /**
   * Log a message at the INFO level according to the specified format and arguments.
   * <p/>
   * <p>This form avoids superfluous object creation when the logger
   * is disabled for the INFO level. </p>
   *
   * @param format the format string
   * @param arg1   the first argument
   * @param arg2   the second argument
   */
  public void info(String format, Object arg1, Object arg2) {
    info(MessageFormatter.format(format, arg1, arg2).getMessage());
  }

  /**
   * Log a message at the INFO level according to the specified format and arguments.
   * <p/>
   * <p>This form avoids superfluous string concatenation when the logger
   * is disabled for the INFO level. However, this variant incurs the hidden (and relatively small) cost of creating an
   * <code>Object[]</code> before invoking the method, even if this logger is disabled for INFO. The variants taking
   * {@link #info(String, Object) one} and {@link #info(String, Object, Object) two} arguments exist solely in order to
   * avoid this hidden cost.</p>
   *
   * @param format    the format string
   * @param arguments a list of 3 or more arguments
   */
  public void info(String format, Object... arguments) {
    info(MessageFormatter.arrayFormat(format, arguments).getMessage());
  }

  /**
   * Log an exception (throwable) at the INFO level with an accompanying message.
   *
   * @param msg the message accompanying the exception
   * @param t   the exception (throwable) to log
   */
  public void info(String msg, Throwable t) {
    info(String.format("%s\n%s", msg, ExceptionUtils.getStackTrace(t)));
  }

  /**
   * Similar to {@link #isInfoEnabled()} method except that the marker data is also taken into consideration.
   *
   * @param marker The marker data to take into consideration
   * @return true if this logger is warn enabled, false otherwise
   */
  public boolean isInfoEnabled(Marker marker) {
    return innerLogger.isInfoEnabled(marker);
  }

  /**
   * Log a message with the specific Marker at the INFO level.
   *
   * @param marker The marker specific to this log statement
   * @param msg    the message string to be logged
   */
  public void info(Marker marker, String msg) {
    synchronized (this) {
      boolean dup = checkIfLogIsDup(msg);
      if (dup) {
        // Do Nothing
      } else {
        logMessage(msg, Level.INFO, marker);
      }
      updateCache(msg, Level.INFO, marker);
    }
  }

  /**
   * This method is similar to {@link #info(String, Object)} method except that the marker data is also taken into
   * consideration.
   *
   * @param marker the marker data specific to this log statement
   * @param format the format string
   * @param arg    the argument
   */
  public void info(Marker marker, String format, Object arg) {
    info(marker, MessageFormatter.format(format, arg).getMessage());
  }

  /**
   * This method is similar to {@link #info(String, Object, Object)} method except that the marker data is also taken
   * into consideration.
   *
   * @param marker the marker data specific to this log statement
   * @param format the format string
   * @param arg1   the first argument
   * @param arg2   the second argument
   */
  public void info(Marker marker, String format, Object arg1, Object arg2) {
    info(marker, MessageFormatter.format(format, arg1, arg2).getMessage());
  }

  /**
   * This method is similar to {@link #info(String, Object...)} method except that the marker data is also taken into
   * consideration.
   *
   * @param marker    the marker data specific to this log statement
   * @param format    the format string
   * @param arguments a list of 3 or more arguments
   */
  public void info(Marker marker, String format, Object... arguments) {
    info(marker, MessageFormatter.arrayFormat(format, arguments).getMessage());
  }

  /**
   * This method is similar to {@link #info(String, Throwable)} method except that the marker data is also taken into
   * consideration.
   *
   * @param marker the marker data for this log statement
   * @param msg    the message accompanying the exception
   * @param t      the exception (throwable) to log
   */
  public void info(Marker marker, String msg, Throwable t) {
    info(marker, String.format("%s\n%s", msg, ExceptionUtils.getStackTrace(t)));
  }

  /**
   * Is the logger instance enabled for the WARN level?
   *
   * @return True if this Logger is enabled for the WARN level, false otherwise.
   */
  public boolean isWarnEnabled() {
    return innerLogger.isWarnEnabled();
  }

  /**
   * Log a message at the WARN level.
   *
   * @param msg the message string to be logged
   */
  public void warn(String msg) {
    synchronized (this) {
      boolean dup = checkIfLogIsDup(msg);
      if (dup) {
        // Do Nothing
      } else {
        logMessage(msg, Level.WARN, null);
      }
      updateCache(msg, Level.WARN, null);
    }
  }

  /**
   * Log a message at the WARN level according to the specified format and argument.
   * <p/>
   * <p>This form avoids superfluous object creation when the logger
   * is disabled for the WARN level. </p>
   *
   * @param format the format string
   * @param arg    the argument
   */
  public void warn(String format, Object arg) {
    warn(MessageFormatter.format(format, arg).getMessage());
  }

  /**
   * Log a message at the WARN level according to the specified format and arguments.
   * <p/>
   * <p>This form avoids superfluous string concatenation when the logger
   * is disabled for the WARN level. However, this variant incurs the hidden (and relatively small) cost of creating an
   * <code>Object[]</code> before invoking the method, even if this logger is disabled for WARN. The variants taking
   * {@link #warn(String, Object) one} and {@link #warn(String, Object, Object) two} arguments exist solely in order to
   * avoid this hidden cost.</p>
   *
   * @param format    the format string
   * @param arguments a list of 3 or more arguments
   */
  public void warn(String format, Object... arguments) {
    warn(MessageFormatter.arrayFormat(format, arguments).getMessage());
  }

  /**
   * Log a message at the WARN level according to the specified format and arguments.
   * <p/>
   * <p>This form avoids superfluous object creation when the logger
   * is disabled for the WARN level. </p>
   *
   * @param format the format string
   * @param arg1   the first argument
   * @param arg2   the second argument
   */
  public void warn(String format, Object arg1, Object arg2) {
    warn(MessageFormatter.format(format, arg1, arg2).getMessage());
  }

  /**
   * Log an exception (throwable) at the WARN level with an accompanying message.
   *
   * @param msg the message accompanying the exception
   * @param t   the exception (throwable) to log
   */
  public void warn(String msg, Throwable t) {
    warn(String.format("%s\n%s", msg, ExceptionUtils.getStackTrace(t)));
  }

  /**
   * Similar to {@link #isWarnEnabled()} method except that the marker data is also taken into consideration.
   *
   * @param marker The marker data to take into consideration
   * @return True if this Logger is enabled for the WARN level, false otherwise.
   */
  public boolean isWarnEnabled(Marker marker) {
    return innerLogger.isWarnEnabled(marker);
  }

  /**
   * Log a message with the specific Marker at the WARN level.
   *
   * @param marker The marker specific to this log statement
   * @param msg    the message string to be logged
   */
  public void warn(Marker marker, String msg) {
    synchronized (this) {
      boolean dup = checkIfLogIsDup(msg);
      if (dup) {
        // Do Nothing
      } else {
        logMessage(msg, Level.WARN, marker);
      }
      updateCache(msg, Level.WARN, marker);
    }
  }

  /**
   * This method is similar to {@link #warn(String, Object)} method except that the marker data is also taken into
   * consideration.
   *
   * @param marker the marker data specific to this log statement
   * @param format the format string
   * @param arg    the argument
   */
  public void warn(Marker marker, String format, Object arg) {
    warn(marker, MessageFormatter.format(format, arg).getMessage());
  }

  /**
   * This method is similar to {@link #warn(String, Object, Object)} method except that the marker data is also taken
   * into consideration.
   *
   * @param marker the marker data specific to this log statement
   * @param format the format string
   * @param arg1   the first argument
   * @param arg2   the second argument
   */
  public void warn(Marker marker, String format, Object arg1, Object arg2) {
    warn(marker, MessageFormatter.format(format, arg1, arg2).getMessage());
//    warn(marker, String.format(format, arg1, arg2));
  }

  /**
   * This method is similar to {@link #warn(String, Object...)} method except that the marker data is also taken into
   * consideration.
   *
   * @param marker    the marker data specific to this log statement
   * @param format    the format string
   * @param arguments a list of 3 or more arguments
   */
  public void warn(Marker marker, String format, Object... arguments) {
    warn(marker, MessageFormatter.arrayFormat(format, arguments).getMessage());
  }

  /**
   * This method is similar to {@link #warn(String, Throwable)} method except that the marker data is also taken into
   * consideration.
   *
   * @param marker the marker data for this log statement
   * @param msg    the message accompanying the exception
   * @param t      the exception (throwable) to log
   */
  public void warn(Marker marker, String msg, Throwable t) {
    warn(marker, String.format("%s\n%s", msg, ExceptionUtils.getStackTrace(t)));
  }

  /**
   * Is the logger instance enabled for the ERROR level?
   *
   * @return True if this Logger is enabled for the ERROR level, false otherwise.
   */
  public boolean isErrorEnabled() {
    return innerLogger.isErrorEnabled();
  }

  /**
   * Log a message at the ERROR level.
   *
   * @param msg the message string to be logged
   */
  public void error(String msg) {
    synchronized (this) {
      boolean dup = checkIfLogIsDup(msg);
      if (dup) {
        // Do Nothing
      } else {
        logMessage(msg, Level.ERROR, null);
      }
      updateCache(msg, Level.ERROR, null);
    }
  }

  /**
   * Log a message at the ERROR level according to the specified format and argument.
   * <p/>
   * <p>This form avoids superfluous object creation when the logger
   * is disabled for the ERROR level. </p>
   *
   * @param format the format string
   * @param arg    the argument
   */
  public void error(String format, Object arg) {
    error(MessageFormatter.format(format, arg).getMessage());
  }

  /**
   * Log a message at the ERROR level according to the specified format and arguments.
   * <p/>
   * <p>This form avoids superfluous object creation when the logger
   * is disabled for the ERROR level. </p>
   *
   * @param format the format string
   * @param arg1   the first argument
   * @param arg2   the second argument
   */
  public void error(String format, Object arg1, Object arg2) {
    error(MessageFormatter.format(format, arg1, arg2).getMessage());
  }

  /**
   * Log a message at the ERROR level according to the specified format and arguments.
   * <p/>
   * <p>This form avoids superfluous string concatenation when the logger
   * is disabled for the ERROR level. However, this variant incurs the hidden (and relatively small) cost of creating
   * an
   * <code>Object[]</code> before invoking the method, even if this logger is disabled for ERROR. The variants taking
   * {@link #error(String, Object) one} and {@link #error(String, Object, Object) two} arguments exist solely in order
   * to avoid this hidden cost.</p>
   *
   * @param format    the format string
   * @param arguments a list of 3 or more arguments
   */
  public void error(String format, Object... arguments) {
    error(MessageFormatter.arrayFormat(format, arguments).getMessage());
  }

  /**
   * Log an exception (throwable) at the ERROR level with an accompanying message.
   *
   * @param msg the message accompanying the exception
   * @param t   the exception (throwable) to log
   */
  public void error(String msg, Throwable t) {
    error(String.format("%s\n%s", msg, ExceptionUtils.getStackTrace(t)));
  }

  /**
   * Similar to {@link #isErrorEnabled()} method except that the marker data is also taken into consideration.
   *
   * @param marker The marker data to take into consideration
   * @return True if this Logger is enabled for the ERROR level, false otherwise.
   */
  public boolean isErrorEnabled(Marker marker) {
    return innerLogger.isErrorEnabled(marker);
  }

  /**
   * Log a message with the specific Marker at the ERROR level.
   *
   * @param marker The marker specific to this log statement
   * @param msg    the message string to be logged
   */
  public void error(Marker marker, String msg) {
    synchronized (this) {
      boolean dup = checkIfLogIsDup(msg);
      if (dup) {
        // Do Nothing
      } else {
        logMessage(msg, Level.ERROR, marker);
      }
      updateCache(msg, Level.ERROR, marker);
    }
  }

  /**
   * This method is similar to {@link #error(String, Object)} method except that the marker data is also taken into
   * consideration.
   *
   * @param marker the marker data specific to this log statement
   * @param format the format string
   * @param arg    the argument
   */
  public void error(Marker marker, String format, Object arg) {
    error(marker, MessageFormatter.format(format, arg).getMessage());
  }

  /**
   * This method is similar to {@link #error(String, Object, Object)} method except that the marker data is also taken
   * into consideration.
   *
   * @param marker the marker data specific to this log statement
   * @param format the format string
   * @param arg1   the first argument
   * @param arg2   the second argument
   */
  public void error(Marker marker, String format, Object arg1, Object arg2) {
    error(marker, MessageFormatter.format(format, arg1, arg2).getMessage());
  }

  /**
   * This method is similar to {@link #error(String, Object...)} method except that the marker data is also taken into
   * consideration.
   *
   * @param marker    the marker data specific to this log statement
   * @param format    the format string
   * @param arguments a list of 3 or more arguments
   */
  public void error(Marker marker, String format, Object... arguments) {
    error(marker, MessageFormatter.arrayFormat(format, arguments).getMessage());
  }

  /**
   * This method is similar to {@link #error(String, Throwable)} method except that the marker data is also taken into
   * consideration.
   *
   * @param marker the marker data specific to this log statement
   * @param msg    the message accompanying the exception
   * @param t      the exception (throwable) to log
   */
  public void error(Marker marker, String msg, Throwable t) {
    error(marker, String.format("%s\n%s", msg, ExceptionUtils.getStackTrace(t)));
  }

}
