package org.deduplogger.test;

import org.deduplogger.logger.DedupLogger;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

public class DedupFilterTest {

    private Logger logger;
    private DedupLogger dedupLogger;

    private Marker small;
    private Marker middle;
    private Marker large;

    @Before
    public void setUp() throws Exception {
        logger = LoggerFactory.getLogger(DedupFilterTest.class);
    }

    @Test
    public void testDedupFilter() {
        logger.trace("a");
        logger.trace("a");
        logger.trace("a");
        logger.trace("a");

        logger.trace("b");
        logger.trace("b");
        logger.trace("c");
        logger.trace("d");
        logger.trace("a");
        logger.trace("b");

        logger.trace("{} {}", "a", "b");
        logger.trace("{} {}", "a", "b");
        logger.trace("{} {}", "a", "b");
        logger.trace("{} {}", "a", "b");
        logger.trace("b");
        logger.trace("one two three four : {} {} {} {}", "a", "b", "c", "d");
        logger.trace("one two three four : {} {} {} {}", "a", "b", "c", "d");
        logger.trace("one two three", new Exception("Something goes wrong"));
        logger.trace("{} {}", "a", "b");
    }

}