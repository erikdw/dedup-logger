package org.deduplogger.test;

import org.deduplogger.logger.LogCache;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class LogLruCacheTest {

  private LogCache smallCache;
  private LogCache middleCache;
  private LogCache largeCache;

  //SetUp
  @Before
  public void setUp() throws Exception {
    smallCache = new LogCache(3, 1, 10000000L, 50 * 1024 * 1024);
    middleCache = new LogCache(50, 1, 1000L, 50 * 1024 * 1024);
    largeCache = new LogCache(500, 1, 100000000L, 50 * 1024 * 1024);
  }

    @Test
    public void testTimeBasedPriorityQueue() {
        LogCache cache = new LogCache(50, 3, 100000000L, 50 * 1024 * 1024);
        cache.registerTimeStamp("msg1 ", 1L);
        cache.registerTimeStamp("msg2 ", 2L);
        cache.registerTimeStamp("msg3 ", 3L);
        cache.registerTimeStamp("msg4 ", 4L);
        cache.registerTimeStamp("msg5 ", 5L);

        assert (cache.getEarliestTimestamp().getKey().equals("msg1 "));
        assert (cache.getEarliestTimestamp().getValue() == 1);
        cache.removeEarliestTimestamp();
        assert (cache.getEarliestTimestamp().getKey().equals("msg2 "));
        assert (cache.getEarliestTimestamp().getValue() == 2);
        cache.removeEarliestTimestamp();
        cache.registerTimeStamp("msg5 ", 6L);
        cache.removeEarliestTimestamp();
        assert (cache.getEarliestTimestamp().getKey().equals("msg4 "));
        assert (cache.getEarliestTimestamp().getValue() == 4);
    }

    @Test
    public void testGetCapacity() {
        assertEquals(3, smallCache.getLruCache().getCapacity());
        assertEquals(50, middleCache.getLruCache().getCapacity());
        assertEquals(500, largeCache.getLruCache().getCapacity());
    }

    @Test
    public void testLRUCachePutAndGetSmallCache() {
        smallCache.getLruCache().put("A", System.currentTimeMillis());
        smallCache.getLruCache().put("B", System.currentTimeMillis());
        smallCache.getLruCache().put("A", System.currentTimeMillis());
        smallCache.getLruCache().put("A", System.currentTimeMillis());
        smallCache.getLruCache().put("C", System.currentTimeMillis());
        smallCache.getLruCache().put("D", System.currentTimeMillis());

        assertEquals(null, smallCache.getLruCache().get("B"));
        assertEquals(3, smallCache.getLruCache().get("A").getTimeStamp().size());
        assertEquals(1, smallCache.getLruCache().get("C").getTimeStamp().size());
        assertEquals(1, smallCache.getLruCache().get("D").getTimeStamp().size());

        smallCache.getLruCache().put("D", System.currentTimeMillis());
        smallCache.getLruCache().put("E", System.currentTimeMillis());
        smallCache.getLruCache().put("C", System.currentTimeMillis());
        smallCache.getLruCache().put("E", System.currentTimeMillis());

        assertEquals(2, smallCache.getLruCache().get("C").getTimeStamp().size());
        assertEquals(2, smallCache.getLruCache().get("E").getTimeStamp().size());
        assertEquals(2, smallCache.getLruCache().get("D").getTimeStamp().size());
        assertEquals(null, smallCache.getLruCache().get("A"));
        assertEquals(3, smallCache.getLruCache().size());
    }

    @Test
    public void testLRUCachePutAndGetForMiddleCache() {
        int i;
        for (i = 0; i < 50; i++) {
            assertFalse(middleCache.getLruCache().put(String.valueOf(i), System.currentTimeMillis()));
        }
        //Evict when Cache is Full
        assertTrue(middleCache.getLruCache().put(String.valueOf(i), System.currentTimeMillis()));

    }

    @Test
    public void testLRUCachePutAndGetForLargeCache() {
        largeCache.getLruCache().clear();

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 501; j++) {
                boolean evicted = largeCache.getLruCache().put(String.valueOf(j), (long) i);
            }
        }
        //0 got evicted when we put 500 to the cache at the end
        assertEquals(null, largeCache.getLruCache().get(String.valueOf(0)));

        for (int k = 1; k < 501; k++) {
            assertEquals(1, largeCache.getLruCache().get(String.valueOf(k)).getTimeStamp().size());
        }
    }

    @Test
    public void testLRUCacheEvictionForLargeCache() {
        smallCache.getLruCache().clear();
        List<String> result = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 4; j++) {
                 result = smallCache.generateSummaryMessage(String.valueOf(j));
            }
            for (String s : result) {
                System.out.println(s);
            }
        }

        //0 got evicted when we put 500 to the cache at the end
        assertEquals(null, smallCache.getLruCache().get(String.valueOf(0)));

        for (int k = 1; k < 4; k++) {
            assertEquals(1, smallCache.getLruCache().get(String.valueOf(k)).getTimeStamp().size());
        }
    }

  @Test
  public synchronized void testTimeExpirationBasic() {
    middleCache.getLruCache().clear();
    long startTime = System.currentTimeMillis();
    List<String> result = new ArrayList<>();

    try {
      boolean first = true;
      while (System.currentTimeMillis() - startTime < 10000L) {
        result = middleCache.generateSummaryMessage("test time expiration");
        for (String s : result) {
          System.out.println(s);
        }
        if (!first) {
          assertEquals(middleCache.getLruCache().get("test time expiration"), null);
          first = true;
        }
        wait(1001L);
      }
    } catch (InterruptedException ex) {
      System.err.println("InterruptedException");
    }
  }
}