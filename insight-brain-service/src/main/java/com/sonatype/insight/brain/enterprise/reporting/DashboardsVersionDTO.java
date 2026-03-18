/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.enterprise.reporting;

public class DashboardsVersionDTO
{
  public int version;

  public DashboardsVersionDTO() {
    // for jackson;
  }

  public DashboardsVersionDTO(final int version) {
    this.version = version;
  }
}
