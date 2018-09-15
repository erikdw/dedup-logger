package org.deduplogger.filter;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.message.Message;

import java.util.LinkedHashMap;
import java.util.Map;


/**
 * This filter returns the onMatch result if the message appears a number of time that is less or equal to the user
 * defined threshold value
 *
 * By default the cache can store 500 messages. The threshold by default is set to 1
 */
@Plugin(name = "DedupFilter", category = Node.CATEGORY, elementType = Filter.ELEMENT_TYPE, printObject = true)
public class DedupFilter extends AbstractFilter {

  private static final int DEFAULT_LOG_CACHE_SIZE = 500;

  private static final int DEFAULT_LOG_CACHE_THRESHOLD = 1;

  private final int logCacheSize;

  private final int logCacheThreshold;

  private final Map<String, Integer> LruCache;

  /**
   * @param cacheSize  The maximum number of messages can be stored into the cache
   * @param threshold  Number of times the same message get accepted before being rejected by the filter
   * @param onMatch    The action to take on a match
   * @param onMismatch The action to take on a mismatch
   */

  public DedupFilter(final int cacheSize, final int threshold, final Result onMatch, final Result onMismatch) {
    super(onMatch, onMismatch);
    this.logCacheSize = cacheSize;
    this.logCacheThreshold = threshold;
    this.LruCache = new LinkedHashMap<String, Integer>(logCacheSize, 0.75f, true) {
      @Override
      protected boolean removeEldestEntry(Map.Entry<String, Integer> eldest) {
        return size() > logCacheSize;
      }
    };
  }

  private Result checkIfDuplicate(String key) {
    synchronized (LruCache) {
      if (!LruCache.containsKey(key)) {
        LruCache.put(key, 0);
      } else if (LruCache.get(key) < this.logCacheThreshold) {
        LruCache.put(key, LruCache.get(key) + 1);
      }

      if (LruCache.get(key) >= this.logCacheThreshold) {
        return onMismatch;
      }
      return onMatch;
    }
  }

  @Override
  public Result filter(LogEvent event) {
    return checkIfDuplicate(event.getMessage().getFormattedMessage());
  }

  @Override
  public Result filter(Logger logger, Level level, Marker marker,
                       Message msg, Throwable t) {
    return checkIfDuplicate(msg.getFormattedMessage());
  }

  @Override
  public Result filter(Logger logger, Level level, Marker marker,
                       Object msg, Throwable t) {
    return checkIfDuplicate(msg.toString());
  }

  @Override
  public Result filter(Logger logger, Level level, Marker marker,
                       String msg, Object... params) {
    return checkIfDuplicate(msg);
  }

  @Override
  public Result filter(Logger logger, Level level, Marker marker,
                       String msg, Object p0) {
    return filter(logger, level, marker, msg, new Object[]{p0});
  }

  @Override
  public Result filter(Logger logger, Level level, Marker marker,
                       String msg, Object p0, Object p1) {
    return filter(logger, level, marker, msg, new Object[]{p0, p1});
  }

  @Override
  public Result filter(Logger logger, Level level, Marker marker,
                       String msg, Object p0, Object p1, Object p2) {
    return filter(logger, level, marker, msg, new Object[]{p0, p1, p2});
  }

  @Override
  public Result filter(Logger logger, Level level, Marker marker,
                       String msg, Object p0, Object p1, Object p2,
                       Object p3) {
    return filter(logger, level, marker, msg, new Object[]{p0, p1, p2, p3});
  }

  @Override
  public Result filter(Logger logger, Level level, Marker marker,
                       String msg, Object p0, Object p1, Object p2,
                       Object p3, Object p4) {
    return filter(logger, level, marker, msg, new Object[]{p0, p1, p2, p3, p4});
  }

  @Override
  public Result filter(Logger logger, Level level, Marker marker,
                       String msg, Object p0, Object p1, Object p2,
                       Object p3, Object p4, Object p5) {
    return filter(logger, level, marker, msg, new Object[]{p0, p1, p2, p3, p4, p5});
  }

  @Override
  public Result filter(Logger logger, Level level, Marker marker,
                       String msg, Object p0, Object p1, Object p2,
                       Object p3, Object p4, Object p5, Object p6) {
    return filter(logger, level, marker, msg, new Object[]{p0, p1, p2, p3, p4, p5, p6});
  }

  @Override
  public Result filter(Logger logger, Level level, Marker marker,
                       String msg, Object p0, Object p1, Object p2,
                       Object p3, Object p4, Object p5, Object p6,
                       Object p7) {
    return filter(logger, level, marker, msg, new Object[]{p0, p1, p2, p3, p4, p5, p6, p7});
  }

  @Override
  public Result filter(Logger logger, Level level, Marker marker,
                       String msg, Object p0, Object p1, Object p2,
                       Object p3, Object p4, Object p5, Object p6,
                       Object p7, Object p8) {
    return filter(logger, level, marker, msg, new Object[]{p0, p1, p2, p3, p4, p5, p6, p7, p8});
  }

  @Override
  public Result filter(Logger logger, Level level, Marker marker,
                       String msg, Object p0, Object p1, Object p2,
                       Object p3, Object p4, Object p5, Object p6,
                       Object p7, Object p8, Object p9) {
    return filter(logger, level, marker, msg, new Object[]{p0, p1, p2, p3, p4, p5, p6, p7, p8, p9});
  }

  @Override
  public String toString() {
    return "cacheSize = " + this.logCacheSize + " threshold " + this.logCacheThreshold;
  }

  /**
   * Create a DedupFilter.
   *
   * @param cacheSize The size for message cache
   * @param threshold The number of time that the same message will be accepted before getting rejected by the filter
   * @param match     Action to perform if the input message is not consider to be duplicated.
   * @param mismatch  Action to perform if the message appears number of time less or equal to threshold value
   * @return A DedupFilter.
   */
  @PluginFactory
  public static DedupFilter createFilter(
      @PluginAttribute("cacheSize") final Integer cacheSize,
      @PluginAttribute("threshold") final Integer threshold,
      @PluginAttribute("onMatch") final Result match,
      @PluginAttribute("onMismatch") final Result mismatch) {
    final int logCacheSize = cacheSize != null && cacheSize > 0 ? cacheSize : DEFAULT_LOG_CACHE_SIZE;
    final int logCacheThreshold = threshold != null && threshold >= 1 ? threshold : DEFAULT_LOG_CACHE_THRESHOLD;
    final Result onMatch = match == null ? Result.ACCEPT : match;
    final Result onMismatch = mismatch == null ? Result.DENY : mismatch;
    return new DedupFilter(logCacheSize, logCacheThreshold, onMatch, onMismatch);
  }
}


