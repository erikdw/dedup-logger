package org.deduplogger.test;

import org.deduplogger.logger.DedupLogger;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import static org.junit.Assert.assertEquals;

public class DedupLoggerTest {

  private Logger logger;
  private DedupLogger testLogger;

  private Marker small;
  private Marker middle;
  private Marker large;

  @Before
  public void setUp() throws Exception {
    logger = LoggerFactory.getLogger(DedupLoggerTest.class);

    small = MarkerFactory.getMarker("small");
    middle = MarkerFactory.getMarker("middle");
    large = MarkerFactory.getMarker("large");

    testLogger = new DedupLogger(logger, 3, 1, 1000L, 50);
  }

  @Test
  public void testBasicSetup() {
    assertEquals(3, testLogger.getLogCache().getLruCache().getCapacity());
  }

  @Test
  public synchronized void testTimeExpirationBasic() {
    testLogger.getLogCache().getLruCache().clear();
    long startTime = System.currentTimeMillis();

    try {
      boolean first = true;
      while (System.currentTimeMillis() - startTime < 10000) {
        testLogger.trace("test time expiration");
        if (first == false) {
          assertEquals(testLogger.getLogCache().getLruCache().get("test time expiration"), null);
          first = true;
        }
        wait(1001);
      }
    } catch (InterruptedException ex) {
      System.err.println("InterruptedException");
    }
  }


  @Test
  public synchronized void testTimeExpirationAdvance() {
    testLogger.getLogCache().getLruCache().clear();
    long startTime = System.currentTimeMillis();

    try {
      boolean first = true;
      while (System.currentTimeMillis() - startTime < 10000) {
        testLogger.trace("test time expiration A");
        testLogger.trace("test time expiration B");
        testLogger.trace("test time expiration B");
        testLogger.trace("test time expiration C");
        testLogger.trace("test time expiration C");
        testLogger.trace("test time expiration D");
        testLogger.trace("test time expiration D");
        wait(1001);
      }
    } catch (InterruptedException ex) {
      System.err.println("InterruptedException");
    }
  }

  @Test
  public void testLRUCacheBasicFunctionWithThreshold() {
    testLogger.getLogCache().getLruCache().clear();
    testLogger.trace("A");
    testLogger.trace("B");
    testLogger.trace("B");
    testLogger.trace("A");
    testLogger.trace("A");
    testLogger.trace("A");
    testLogger.trace("A");
    testLogger.trace("C");
    testLogger.trace("A");
    testLogger.trace("D");
    assertEquals(null, testLogger.getLogCache().getLruCache().get("B"));
    testLogger.trace("C");
    testLogger.trace("E");
    testLogger.trace("E");

    assertEquals(3, testLogger.getLogCache().getLruCache().size());
    assertEquals(null, testLogger.getLogCache().getLruCache().get("A"));
    assertEquals(null, testLogger.getLogCache().getLruCache().get("B"));
    assertEquals(2, testLogger.getLogCache().getLruCache().get("E").getTimeStamp().size());
    assertEquals(1, testLogger.getLogCache().getLruCache().get("D").getTimeStamp().size());
    assertEquals(2, testLogger.getLogCache().getLruCache().get("C").getTimeStamp().size());
  }

  //
  @Test
  public void testTraceBasic() {
    testLogger.getLogCache().getLruCache().clear();
    testLogger.trace("TRACE");
    testLogger.trace("a");
    testLogger.trace("b");
    testLogger.trace("{}", "a");
    testLogger.trace("{} {}", "a", "b");
    testLogger.trace("one two three : {} {} {}", "a", "b", "c");
    testLogger.trace("one two three four : {} {} {} {}", "a", "b", "c", "d");
    testLogger.trace("one two three", new Exception("Something goes wrong"));
    testLogger.trace("one two three", new Throwable("Something goes wrong"));

    Marker marker = MarkerFactory.getMarker("test");

    testLogger.trace(marker, "a");
    testLogger.trace(marker, "b");
    testLogger.trace(marker, "{}", "a");
    testLogger.trace(marker, "{} {}", "a", "b");
    testLogger.trace(marker, "one two three : {} {} {}", "a", "b", "c");
    testLogger.trace(marker, "one two three four : {} {} {} {}", "a", "b", "c", "d");
    testLogger.trace(marker, "one two three", new Exception("Something goes wrong"));
    testLogger.trace(marker, "one two three", new Throwable("Something goes wrong"));

  }

  @Test
  public void testInfoBasic() {
    testLogger.getLogCache().getLruCache().clear();
    testLogger.info("INFO");
    testLogger.info("a");
    testLogger.info("b");
    testLogger.info("{}", "a");
    testLogger.info("{} {}", "a", "b");
    testLogger.info("one two three : {} {} {}", "a", "b", "c");
    testLogger.info("one two three : a b c");
    testLogger.info("one two three four : {} {} {} {}", "a", "b", "c", "d");
    testLogger.info("one two three", new Exception("Something goes wrong"));
    //this line should evict "one two three : a b c" from cache
    testLogger.info("one two three", new Throwable("Something goes wrong"));

    Marker marker = MarkerFactory.getMarker("test");

    testLogger.info(marker, "a");
    testLogger.info(marker, "b");
    testLogger.info(marker, "{}", "a");
    testLogger.info(marker, "{} {}", "a", "b");
    testLogger.info(marker, "one two three : {} {} {}", "a", "b", "c");
    testLogger.info(marker, "one two three four : {} {} {} {}", "a", "b", "c", "d");
    testLogger.info(marker, "one two three", new Exception("Something goes wrong"));
    testLogger.info(marker, "one two three", new Throwable("Something goes wrong"));

  }

  @Test
  public void testDebugBasic() {
    testLogger.getLogCache().getLruCache().clear();
    testLogger.debug("DEBUG");
    testLogger.debug("a");
    testLogger.debug("b");
    testLogger.debug("{}", "a");
    testLogger.debug("{} {}", "a", "b");
    testLogger.debug("one two three : {} {} {}", "a", "b", "c");
    testLogger.debug("one two three four : {} {} {} {}", "a", "b", "c", "d");
    testLogger.debug("one two three", new Exception("Something goes wrong"));
    testLogger.debug("one two three", new Throwable("Something goes wrong"));

    Marker marker = MarkerFactory.getMarker("test");

    testLogger.debug(marker, "a");
    testLogger.debug(marker, "b");
    testLogger.debug(marker, "{}", "a");
    testLogger.debug(marker, "{} {}", "a", "b");
    testLogger.debug(marker, "one two three : {} {} {}", "a", "b", "c");
    testLogger.debug(marker, "one two three four : {} {} {} {}", "a", "b", "c", "d");
    testLogger.debug(marker, "one two three", new Exception("Something goes wrong"));
    testLogger.debug(marker, "one two three", new Throwable("Something goes wrong"));
  }

  //
//
  @Test
  public void testWarnBasic() {
    testLogger.getLogCache().getLruCache().clear();
    testLogger.warn("WARN");
    testLogger.warn("a");
    testLogger.warn("b");
    testLogger.warn("{}", "a");
    testLogger.warn("{} {}", "a", "b");
    testLogger.warn("one two three : {} {} {}", "a", "b", "c");
    testLogger.warn("one two three four : {} {} {} {}", "a", "b", "c", "d");
    testLogger.warn("one two three", new Exception("Something goes wrong"));
    testLogger.warn("one two three", new Throwable("Something goes wrong"));

    Marker marker = MarkerFactory.getMarker("test");

    testLogger.warn(marker, "a");
    testLogger.warn(marker, "b");
    testLogger.warn(marker, "{}", "a");
    testLogger.warn(marker, "{} {}", "a", "b");
    testLogger.warn(marker, "one two three : {} {} {}", "a", "b", "c");
    testLogger.warn(marker, "one two three four : {} {} {} {}", "a", "b", "c", "d");
    testLogger.warn(marker, "one two three", new Exception("Something goes wrong"));
    testLogger.warn(marker, "one two three", new Throwable("Something goes wrong"));
  }

  @Test
  public void testErrorBasic() {
    testLogger.getLogCache().getLruCache().clear();
    testLogger.error("ERROR");
    testLogger.error("a");
    testLogger.error("b");
    testLogger.error("{}", "a");
    testLogger.error("{} {}", "a", "b");
    testLogger.error("one two three : {} {} {}", "a", "b", "c");
    testLogger.error("one two three four : {} {} {} {}", "a", "b", "c", "d");
    testLogger.error("one two three", new Exception("Something goes wrong"));
    testLogger.error("one two three", new Throwable("Something goes wrong"));
    assertEquals(testLogger.getLogCache().getLruCache().get("a"), null);
    assertEquals(testLogger.getLogCache().getLruCache().get("one two three four : a b c d").getTimeStamp().size(), 1);

    Marker marker = MarkerFactory.getMarker("test");

    testLogger.error(marker, "a");
    testLogger.error(marker, "b");
    testLogger.error(marker, "{}", "a");
    testLogger.error(marker, "{} {}", "a", "b");
    testLogger.error(marker, "one two three : {} {} {}", "a", "b", "c");
    testLogger.error(marker, "one two three four : {} {} {} {}", "a", "b", "c", "d");
    testLogger.error(marker, "one two three", new Exception("Something goes wrong"));
    testLogger.error(marker, "one two three", new Throwable("Something goes wrong"));
  }


}