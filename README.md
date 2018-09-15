
# Dedup-logger

## Overview: 
This tool comes with two parts.

1. The `DedupFilter` is a customized deduplication filter for the Log4J2 library. It will reject all the messages that beyond the user-defined threshold. 

2. The `DedupLogger` is a wrapper class on top of the SLF4J's `Logger` interface. It can be used to 
deduplicate the log message from various logging framework supported by SLF4J, such as 
java.util.logging, logback, and log4j2. The general goal of this project is to reduce as much as 
the duplicated messages get logged into the disk and send to Splunk as possible while still preserving good readability 
of the original log file, so the user can use it to diagnose the problem in their code easily.

## Implementation detail: 

## DedupFilter: 
Two Parameters are configurable for DedupFilter. One is cacheSize, and another one is the threshold.

* **Cache size**: the size of LRU cache. Larger cache size typically indicates better performance, 
but it also consumes more memory. By default, the LRU cache can store 50 messages before getting full.

* **Threshold**: the number of times that messages with the same content get logged before it gets rejected by the DedupFilter. 

Example usage inside Log4J2 configuration file: 
```
<DedupFilter cacheSize= "3" threshold = "1" onMatch="ACCEPT" onMismatch="DENY"/>
```

## DedupLogger
When the `DedupLogger` detects the repeated message, instead of to log the message directly, it will store the message into an in-memory cache along with the corresponding message timestamp. 

When the cache is full,  the DedupLogger will evict the least recently used message and print its number of occurrence and timestamps.

Four parameters are configurable by the user:

* **Cache size**: the size of LRU cache. Larger cache size typically indicates better performance, 
but it also indicates more memory consumption. By default, the LRU cache can store 500 messages before getting full.

* **Threshold**: the number of times that messages with the same content get logged before it gets deduplicated by the Deduplogger. The default threshold value is 1.

* **Time expiration**: the maximum amount of time that a message can stay in the cache before it gets rejected. Expiration time is calculated based on the earliest timestamps associated with the message inside the cache. The default value is 10 seconds.

* **Memory Usage**: the maximum memory that the cache can use. If the cache reaches the memory limit, then
reduce the memory consumption to 50% of the memory usage limit. The default value is 50Mb.

For example, a user can create a DedupLogger with cache size 500, threshold 1, time expiration 10s and memory usage of 50Mb using:
```$xslt
        DedupLogger dedupLogger = new DedupLogger(logger, 500, 1, 10000L, 50);
```

*Note: the DedupLogger by default will only print the number of occurrence of the messages beyond 
the threshold. For example, if a message only appears 2 times in total but the threshold is 3, 
then the dedup-logger will not print the eviction message during cache eviction.*

## Build 
```$xslt
mvn clean install
```

The application jar can be found in target directory, named `dedup-logger-1.0-SNAPSHOT.jar`



