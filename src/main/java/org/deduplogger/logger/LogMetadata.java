package org.deduplogger.logger;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// A java class to store all the metadata for the dedupLogger
public class LogMetadata {

  private List<Long> timestamps;

  public LogMetadata() {
    this.timestamps = new ArrayList<>();
  }

  public List<Long> getTimeStamp() {
    return this.timestamps;
  }

  public String getTimeStampInDateFormat(int idx) {
    SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss,SSS");
    Date current = new Date(this.timestamps.get(idx));
    return sdf.format(current);
  }
}