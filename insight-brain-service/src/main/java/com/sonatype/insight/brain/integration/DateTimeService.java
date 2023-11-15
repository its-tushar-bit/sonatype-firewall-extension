/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.integration;

import java.time.Instant;
import java.util.Date;
import javax.inject.Named;

@Named
class DateTimeService
{
  public Date getCurrentDate() {
    return new Date();
  }

  public long getCurrentTimeMs() {
    return Instant.now().toEpochMilli();
  }
}
