/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.releasegraph;

import com.sonatype.insight.brain.report.ReportDownloader;
import com.sonatype.insight.brain.service.InsightWork;

public class ReportItemKey
{
  private String licenseFingerprint;

  private String applicationPublicId;

  private String scanId;

  private InsightWork work;

  private ReportDownloader reportDownloader;

  public ReportItemKey(ReportDownloader reportDownloader, String licenseFingerprint, String applicationPublicId,
      String scanId, InsightWork work)
  {
    this.reportDownloader = reportDownloader;
    this.licenseFingerprint = licenseFingerprint;
    this.applicationPublicId = applicationPublicId;
    this.scanId = scanId;
    this.work = work;
  }

  public InsightWork getWork() {
    return work;
  }

  public String getApplicationPublicId() {
    return applicationPublicId;
  }

  public String getLicenseFingerprint() {
    return licenseFingerprint;
  }

  public String getScanId() {
    return scanId;
  }

  public ReportDownloader getReportDownloader() {
    return reportDownloader;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((applicationPublicId == null) ? 0 : applicationPublicId.hashCode());
    result = prime * result + ((scanId == null) ? 0 : scanId.hashCode());
    result = prime * result + ((licenseFingerprint == null) ? 0 : licenseFingerprint.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    ReportItemKey other = (ReportItemKey) obj;
    if (applicationPublicId == null) {
      if (other.applicationPublicId != null)
        return false;
    }
    else if (!applicationPublicId.equals(other.applicationPublicId))
      return false;
    if (scanId == null) {
      if (other.scanId != null)
        return false;
    }
    else if (!scanId.equals(other.scanId))
      return false;
    if (licenseFingerprint == null) {
      if (other.licenseFingerprint != null)
        return false;
    }
    else if (!licenseFingerprint.equals(other.licenseFingerprint))
      return false;
    return true;
  }

}