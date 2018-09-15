package org.deduplogger.logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


public class LogCache {

  // cache is an customized LRU cache used to keep track of the duplicated log messages
  // pq is an priorityQueue used to check if any LogEntry is expired based on the time expiration thresholds
  public LruCache cache;

  private LinkedHashMap<String, Long> pq;

  private final Long LOG_TIME_THRESHOLD;

  private final int LOG_CACHE_THRESHOLD;

  private final int LOG_CACHE_SIZE;

  private final long MEMORY_LIMIT;

  public static final int timeStampMessageLengthThreshold = 65000;

  private long LRU_MEMORY_USAGE = 0;

  private long PQ_MEMORY_USAGE = 0;

  private final int LONG_SIZE = 8;

  private final int STRING_HEADER_SIZE = 36;

  enum MESSAGE {
    SIZE_EVICTION {
      public String toString() {
        return "Cache Full";
      }
    },
    TIME_EVICTION {
      public String toString() {
        return "Time Expiration";
      }
    },
    EXIT {
      public String toString() {
        return "Program Exit";
      }
    },
    MEMORY_EVICTION {
      public String toString() {
        return "Reach Memory Limit";
      }
    }

  }

  public LogCache(int logCacheSize, int logCacheThreshold, Long timeExpireThreshold, long memoryThreshold) {
    cache = new LruCache(logCacheSize);
    pq = new LinkedHashMap<>();

    this.MEMORY_LIMIT = memoryThreshold;
    this.LOG_CACHE_SIZE = logCacheSize;
    this.LOG_CACHE_THRESHOLD = logCacheThreshold;
    this.LOG_TIME_THRESHOLD = timeExpireThreshold;
  }

  public LruCache getLruCache() {
    return this.cache;
  }

  /*
   * PriorityQueue put
   */
  public void registerTimeStamp(String key, Long val) {
    pq.put(key, val);
  }

  /*
  * PriorityQueue Peek
  */
  public Entry<String, Long> getEarliestTimestamp() {
    if (pq.size() > 0) {
      Entry<String, Long> e = pq.entrySet().iterator().next();
      return e;
    }
    return null;
  }

  /*
   * Priority Queue Poll
   */
  public Entry<String, Long> removeEarliestTimestamp() {
    if (pq.size() > 0) {
      Entry<String, Long> e = pq.entrySet().iterator().next();
      if (e != null) {
        //FIXME remove later
        assert (pq.size() != 0);
        pq.remove(e.getKey());
      }
      return e;
    }
    return null;
  }

  /*
   * Calculate the size of string for estimating the memory consumption of the cache
   * 64 bit reference : sizeof(string) = 36 + string.length() * 2
   */
  private long calculateStringSize(String msg) {
    if (msg == null) {
      return 0;
    }
    return msg.length() * 2 + STRING_HEADER_SIZE;
  }

  /**
   * Check if the current message is duplicate
   * @param msg the log message to check
   * @return true if the input message is duplicate, false otherwise
   */
  public boolean checkIfDuplicate(String msg) {
    return cache.get(msg) != null && cache.get(msg).getTimeStamp().size() >= this.LOG_CACHE_THRESHOLD;
  }

  /*
   * if cache memory usage exceeds the allocated limit, flush some messages to reduce memory consumption to 50%
   */
  private List<String> flushMessageIfOutOfMemory(List<String> evictionMessages) {
    if (PQ_MEMORY_USAGE + LRU_MEMORY_USAGE >= MEMORY_LIMIT) {
//      System.out.printf("Reach Memory Limit, Starting to flush the messages LRU : %d kb, PQ : %d kb\n",
//                              LRU_MEMORY_USAGE / 1024, PQ_MEMORY_USAGE / 1024);
      int targetSize = pq.size() / 2;
      String msg;
      Iterator<Entry<String, Long>> itr = pq.entrySet().iterator();

      while (pq.size() > 0 && pq.size() > targetSize) {
        Map.Entry<String, Long> e = itr.next();
        msg = e.getKey();

        // Update the memory counter for LRU and cache
        PQ_MEMORY_USAGE = PQ_MEMORY_USAGE - calculateStringSize(msg) - LONG_SIZE;
        LRU_MEMORY_USAGE = LRU_MEMORY_USAGE - calculateStringSize(msg) - cache.get(msg).getTimeStamp().size() * LONG_SIZE;
        // Remove from cache and pq
        LogMetadata metadata = cache.get(msg);
        if (metadata.getTimeStamp().size() > LOG_CACHE_THRESHOLD) {
          evictionMessages.add(generateSingleEvictionSummary(msg, MESSAGE.MEMORY_EVICTION.toString(), metadata));
        }
        cache.remove(msg);
        itr.remove();
      }
    }
    return evictionMessages;
  }

  /**
   * Generate the summary message
   * @param msg the msg to be checked against the cache
   * @return A list of eviction message
   */
  public List<String> generateSummaryMessage(String msg) {
    List<String> evictionMessages = new ArrayList<>();
    Long currentTime;
    currentTime = System.currentTimeMillis();

    LRU_MEMORY_USAGE =
        cache.containsKey(msg) ? LRU_MEMORY_USAGE + LONG_SIZE : LRU_MEMORY_USAGE + calculateStringSize(msg) + LONG_SIZE;
    boolean evicted = cache.put(msg, currentTime);

    String evictedMsg = cache.getEvictedMsg();
    LogMetadata metadata = cache.getEvictedMsgData();

    // add the timestamp of the first occurrence of the message to the priorityQueue
    if (!pq.containsKey(msg)) {
      pq.put(msg, currentTime);
      PQ_MEMORY_USAGE += calculateStringSize(msg) + LONG_SIZE;
    }

    // Cache Full, evict the message from the cache and update the priorityQueue
    if (evicted) {
      if (metadata.getTimeStamp().size() > this.LOG_CACHE_THRESHOLD) {
        String
            msgToLog =
            generateSingleEvictionSummary(evictedMsg, MESSAGE.SIZE_EVICTION.toString(), metadata);
        assert (msgToLog != null);
        evictionMessages.add(msgToLog);
      }
      LRU_MEMORY_USAGE -= metadata.getTimeStamp().size() * LONG_SIZE + calculateStringSize(evictedMsg) ;
      PQ_MEMORY_USAGE -= calculateStringSize(evictedMsg) + LONG_SIZE;

      // update the priorityQueue;
      pq.remove(evictedMsg);
    }

    // Time Eviction, expire the old entries
    currentTime = System.currentTimeMillis();
    Entry<String, Long> top = getEarliestTimestamp();
    while (true) {
      if (top == null || currentTime - top.getValue() < LOG_TIME_THRESHOLD || pq.size() == 0) {
        break;
      }
      LogMetadata metadata1 = cache.get(top.getKey());

      if (metadata1.getTimeStamp().size() > this.LOG_CACHE_THRESHOLD) {
        String
            msgToLog1 =
            generateSingleEvictionSummary(top.getKey(), MESSAGE.TIME_EVICTION.toString(), metadata1);
        assert (msgToLog1 != null);
        evictionMessages.add(msgToLog1);
      }

      // Remove the evicted message from the cache and pq, update the memory count
      PQ_MEMORY_USAGE -= LONG_SIZE + calculateStringSize(top.getKey());
      LRU_MEMORY_USAGE -= cache.get(top.getKey()).getTimeStamp().size() * LONG_SIZE +  calculateStringSize(top.getKey());

      removeEarliestTimestamp();
      cache.remove(top.getKey());
      top = getEarliestTimestamp();
    }

    // check the memory limit and flush the messages if necessary
    List<String> result = flushMessageIfOutOfMemory(evictionMessages);
    return result;
  }

  private String generateSingleEvictionSummary(String msg, String header, LogMetadata logMetadata) {
    //append all the timestamps to a string
    StringBuffer timestampCollection = new StringBuffer();

    for (int i = 0; i < logMetadata.getTimeStamp().size() - 1; i++) {
      //The limit for messages bing logged into log4j can no longer large than 65446
      //characters. Here the timeStampMessageLengthThreshold is set to 65000 characters.
      //"..." will be printed when there are too many timestamps
      if (timestampCollection.length() + msg.length()
          <= timeStampMessageLengthThreshold) {
        timestampCollection.append(logMetadata.getTimeStampInDateFormat(i) + ", ");
      } else {
        timestampCollection.append("...");
        break;
      }
    }

    //Append the last timestamps and return the Evicted Message as a String
    timestampCollection.append(logMetadata.getTimeStampInDateFormat(logMetadata.getTimeStamp().size() - 1));
    String msgToLog = String.format(
        "%s : Evict Msg \'%s\'. This Message Appears %d Time(s) In Total and Was Logged %d Time(s) Before"
        + "\nTimestamps at %s", header, msg, logMetadata.getTimeStamp().size(), this.LOG_CACHE_THRESHOLD,
        timestampCollection);
    return msgToLog;
  }

  /**
   * Flush all the messages inside the cache
   * @return  a list of evicted messages
   */
  public List<String> flushAllMessages() {
    synchronized (cache) {
      List<String> result = new ArrayList<>();
      long now = System.currentTimeMillis();
      Iterator<Entry<String, LogMetadata>> itr = cache.entrySet().iterator();
      while (itr.hasNext()) {
        Map.Entry<String, LogMetadata> e = itr.next();
        LogMetadata metadata = e.getValue();
        if (e.getKey() != null) {
          //Print the log summary with time expiration info for the message if it stays in the cache more than
          //LOG_TIME_THRESHOLD and it appears more than 'LOG_CACHE_THRESHOLD' times
          if (metadata.getTimeStamp().size() > LOG_CACHE_THRESHOLD) {
            result.add(generateSingleEvictionSummary(e.getKey(), MESSAGE.EXIT.toString(), metadata));
            itr.remove();
          }
        }
      }
      return result;
    }
  }
}




