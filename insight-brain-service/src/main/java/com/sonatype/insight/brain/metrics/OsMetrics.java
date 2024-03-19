/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.metrics;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.SystemInfo;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HardwareAbstractionLayer;

public class OsMetrics
{
  private static final Logger log = LoggerFactory.getLogger(OsMetrics.class);

  public static long getBytesRead() {
    long bytesRead = 0;
    SystemInfo si = new SystemInfo();

    HardwareAbstractionLayer hal = si.getHardware();
    List<HWDiskStore> diskStores = hal.getDiskStores();
    for (HWDiskStore disk : diskStores) {
      long readBytes = disk.getReadBytes();
      log.debug("Read bytes: {}: {} ", disk.getName(), readBytes);
      bytesRead += readBytes;
    }
    return bytesRead;
  }
}
