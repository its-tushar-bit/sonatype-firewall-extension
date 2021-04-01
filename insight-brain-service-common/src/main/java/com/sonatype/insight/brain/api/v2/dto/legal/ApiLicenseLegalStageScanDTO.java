/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.legal;

import java.util.Date;

public class ApiLicenseLegalStageScanDTO
{
  private String stageName;

  private String scanId;

  private Date scanDate;

  public ApiLicenseLegalStageScanDTO() {
    // for jackson
  }

  public ApiLicenseLegalStageScanDTO(String stageName, String scanId, Date scanDate) {
    this.stageName = stageName;
    this.scanId = scanId;
    this.scanDate = scanDate;
  }

  public String getStageName() {
    return stageName;
  }

  public void setStageName(String stageName) {
    this.stageName = stageName;
  }

  public String getScanId() {
    return scanId;
  }

  public void setScanId(String scanId) {
    this.scanId = scanId;
  }

  public Date getScanDate() {
    return scanDate;
  }

  public void setScanDate(Date scanDate) {
    this.scanDate = scanDate;
  }

  @Override
  public String toString() {
    return "ApiLicenseLegalStageScanDTO [stageName=" + stageName + ", scanId=" + scanId + ", scanDate=" + scanDate
        + "]";
  }
}
