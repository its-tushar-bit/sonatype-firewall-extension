/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.io.File;

public class ThirdPartyScanContext
{
  private final String scanRequestId;

  private final String applicationId;

  private final File scanFile;

  private boolean sbomSavedForScan;

  public ThirdPartyScanContext(final String scanRequestId, final String applicationId, final File scanFile) {
    this.scanRequestId = scanRequestId;
    this.applicationId = applicationId;
    this.scanFile = scanFile;
  }

  public String getScanRequestId() {
    return scanRequestId;
  }

  public String getApplicationId() {
    return applicationId;
  }

  public File getScanFile() {
    return scanFile;
  }

  public boolean isSbomSavedForScan() {
    return sbomSavedForScan;
  }

  public void markSbomSavedForScan() {
    this.sbomSavedForScan = true;
  }
}
