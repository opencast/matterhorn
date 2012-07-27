/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.ingest.impl.jmx;

import com.google.common.collect.MapMaker;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public class IngestStatistics implements IngestStatisticsMXBean {

  private long totalNumBytesRead = 0L;
  private int successful = 0;
  private int failed = 0;
  private ConcurrentMap<Long, Long> bytesCounter = new MapMaker().expireAfterWrite(15, TimeUnit.MINUTES).makeMap();

  /**
   * @see org.opencastproject.ingest.impl.jmx.IngestStatisticsMXBean#getSuccessfulIngestOperations()
   */
  @Override
  public int getSuccessfulIngestOperations() {
    return successful;
  }

  /**
   * @see org.opencastproject.ingest.impl.jmx.IngestStatisticsMXBean#getFailedIngestOperations()
   */
  @Override
  public int getFailedIngestOperations() {
    return failed;
  }

  /**
   * @see org.opencastproject.ingest.impl.jmx.IngestStatisticsMXBean#getTotalBytes()
   */
  @Override
  public long getTotalBytes() {
    return totalNumBytesRead;
  }

  /**
   * @see org.opencastproject.ingest.impl.jmx.IngestStatisticsMXBean#getBytesInLastMinute()
   */
  @Override
  public long getBytesInLastMinute() {
    long key = getKeyByTime(new DateTime().minusMinutes(1).getMillis());
    return key != 0 ? totalNumBytesRead - bytesCounter.get(key) : 0;
  }

  /**
   * @see org.opencastproject.ingest.impl.jmx.IngestStatisticsMXBean#getBytesInLastFiveMinutes()
   */
  @Override
  public long getBytesInLastFiveMinutes() {
    long key = getKeyByTime(new DateTime().minusMinutes(5).getMillis());
    return key != 0 ? totalNumBytesRead - bytesCounter.get(key) : 0;
  }

  /**
   * @see org.opencastproject.ingest.impl.jmx.IngestStatisticsMXBean#getBytesInLastFifteenMinutes()
   */
  @Override
  public long getBytesInLastFifteenMinutes() {
    long key = getKeyByTime(new DateTime().minusMinutes(15).getMillis());
    return key != 0 ? totalNumBytesRead - bytesCounter.get(key) : 0;
  }

  private long getKeyByTime(long timeBeforeFiveMinute) {
    long key = 0L;
    List<Long> bytes = new ArrayList<Long>(bytesCounter.keySet());
    Collections.sort(bytes);
    for (Long milis : bytes) {
      if (milis > timeBeforeFiveMinute) {
        key = milis;
        break;
      }
    }
    return key;
  }

  public void add(long bytes) {
    if (totalNumBytesRead == 0)
      bytesCounter.put(System.currentTimeMillis(), 0L);
    totalNumBytesRead += bytes;
    bytesCounter.put(System.currentTimeMillis(), totalNumBytesRead);
  }

  public void successful() {
    successful++;
  }

  public void failed() {
    failed++;
  }

}
