package org.deduplogger.test;

import org.deduplogger.logger.DedupLogger;

import org.slf4j.*;

import java.util.ArrayList;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.concurrent.*;


class ReaderWriter implements Callable<Long> {

  private String inputFile;
  private Logger logger;

  ReaderWriter(String inputFile, Logger logger) {
    this.inputFile = inputFile;
    this.logger = logger;
  }

  @Override
  public Long call() {
    long startTime = System.currentTimeMillis();
    // TODO: Read file "line" including stacktrace if it exists wise and print output

    for (String line : new String[]{"hi", "there"}) {
//      System.out.println(line);
      logger.trace(line);
    }
//    try {
//      Thread.sleep(1000l);
//    } catch (InterruptedException e) {
//      e.printStackTrace();
//    }
    return (System.currentTimeMillis() - startTime) / 1000;
  }
}

public class DedupLoggerIntegrationTest {

  private static Logger
      DEDUP_LOGGER = new DedupLogger(LoggerFactory.getLogger(DedupLoggerIntegrationTest.class), 3, 1, 1000,50);

  public static void main(String[] args) {
//    DEDUP_LOGGER.info(format("len=%s", args.length));
    if (args.length < 1) {
      throw new RuntimeException("Bad Usage. pass input file cli");
    }

    String inputLogFile = args[0];
    ExecutorService executorService = Executors.newFixedThreadPool(4);
    List<Future<Long>> futures = new ArrayList<>();

    for (int i = 0; i < 100; i++) {
      ReaderWriter readerWriter = new ReaderWriter(inputLogFile, DEDUP_LOGGER);
      Future<Long> future = executorService.submit(readerWriter);
      futures.add(future);
    }

    List<Long> timeTaken = new ArrayList<>();
    for (Future f : futures) {
      try {
        timeTaken.add((Long) f.get());
      } catch (ExecutionException | InterruptedException e) {
        e.printStackTrace();
      }
    }

    executorService.shutdown();
    LongSummaryStatistics stats = timeTaken.stream()
        .mapToLong((x) -> x)
        .summaryStatistics();

    DEDUP_LOGGER.info(stats.toString());
  }
}
