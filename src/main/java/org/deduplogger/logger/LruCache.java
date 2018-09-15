package org.deduplogger.logger;

import java.util.LinkedHashMap;
import java.util.Map;

// A LRU cache to store the log message as well as the corresponding
//A customized LRU cache implementation using the LinkedHashMap
public class LruCache extends LinkedHashMap<String, LogMetadata> {

  private final int capacity;
  private String msg;
  private LogMetadata evictedEntry;
  private boolean evicted;

  public LruCache(int size) {
    super(size, 0.75f, true);
    this.capacity = size;
  }

  public LogMetadata get(String key) {
    return super.get(key);
  }

  public boolean put(String key, long timestamp) {
    this.evicted = false;
    LogMetadata val = get(key);
    if (val == null) {
      val = new LogMetadata();
    }
    val.getTimeStamp().add(timestamp);
    super.put(key, val);
    return evicted;
  }

  public String getEvictedMsg() {
    return this.msg;
  }

  public LogMetadata getEvictedMsgData() {
    return evictedEntry;
  }

  public int getCapacity() {
    return this.capacity;
  }

  public void clear() {
    super.clear();
  }

  /*
   * Overwrite the removeEldestEntry function, so the cache will automatically delete the oldest
   * element when the it is full
   */
  @Override
  protected boolean removeEldestEntry(Map.Entry<String, LogMetadata> eldest) {
    if (size() > capacity) {
      this.msg = eldest.getKey();
      this.evictedEntry = eldest.getValue();
      this.evicted = true;
      return true;
    }
    this.evicted = false;
    return false;
  }
}
