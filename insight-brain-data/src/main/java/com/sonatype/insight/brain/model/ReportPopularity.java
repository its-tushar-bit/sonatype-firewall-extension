/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model;

import java.util.List;

public class ReportPopularity
{
  private long firstCatalog;

  private long lastCatalog;

  private List<GAVPopularity> popularity;

  public long getFirstCatalog() {
    return firstCatalog;
  }

  public long getLastCatalog() {
    return lastCatalog;
  }

  public List<GAVPopularity> getPopularity() {
    return popularity;
  }

  public void setFirstCatalog(long firstCatalog) {
    this.firstCatalog = firstCatalog;
  }

  public void setLastCatalog(long lastCatalog) {
    this.lastCatalog = lastCatalog;
  }

  public void setPopularity(List<GAVPopularity> popularity) {
    this.popularity = popularity;
  }
}
