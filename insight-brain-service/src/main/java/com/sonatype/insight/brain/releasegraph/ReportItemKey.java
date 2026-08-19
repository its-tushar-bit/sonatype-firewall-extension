/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.releasegraph;

public class ReportItemKey
{
  private String applicationPublicId;

  private String scanId;

  public ReportItemKey(String applicationPublicId, String scanId) {
    this.applicationPublicId = applicationPublicId;
    this.scanId = scanId;
  }

  public String getApplicationPublicId() {
    return applicationPublicId;
  }

  public String getScanId() {
    return scanId;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((applicationPublicId == null) ? 0 : applicationPublicId.hashCode());
    result = prime * result + ((scanId == null) ? 0 : scanId.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    ReportItemKey other = (ReportItemKey) obj;
    if (applicationPublicId == null) {
      if (other.applicationPublicId != null) {
        return false;
      }
    }
    else if (!applicationPublicId.equals(other.applicationPublicId)) {
      return false;
    }
    if (scanId == null) {
      if (other.scanId != null) {
        return false;
      }
    }
    else if (!scanId.equals(other.scanId)) {
      return false;
    }
    return true;
  }
}
