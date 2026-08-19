/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.developer.integrationdashboard;

import java.time.Instant;
import java.util.Date;
import jakarta.inject.Named;

@Named
public class DateTimeService
{
  public Date getCurrentDate() {
    return new Date();
  }

  public long getCurrentTimeMs() {
    return Instant.now().toEpochMilli();
  }
}
