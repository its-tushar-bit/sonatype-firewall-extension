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

  private String sbomFileName;

  private String thirdPartyFileId;

  private String thirdPartyScanId;

  private String stageType;

  private String sbomMetadataId;

  public ThirdPartyScanContext(final String scanRequestId,
                               final String applicationId,
                               final File scanFile,
                               final String stageType)
  {
    this.scanRequestId = scanRequestId;
    this.applicationId = applicationId;
    this.scanFile = scanFile;
    this.stageType = stageType;
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

  public String getSbomFileName() {
    return sbomFileName;
  }

  public void setSbomFileName(final String sbomFileName) {
    this.sbomFileName = sbomFileName;
  }

  public String getThirdPartyFileId() {
    return thirdPartyFileId;
  }

  public void setThirdPartyFileId(final String thirdPartyFileId) {
    this.thirdPartyFileId = thirdPartyFileId;
  }

  public String getStageType() {
    return stageType;
  }

  public void setStageType(final String stageType) {
    this.stageType = stageType;
  }

  public String getSbomMetadataId() {
    return sbomMetadataId;
  }

  public void setSbomMetadataId(final String sbomMetadataId) {
    this.sbomMetadataId = sbomMetadataId;
  }

  public String getThirdPartyScanId() {
    return thirdPartyScanId;
  }

  public void setThirdPartyScanId(final String thirdPartyScanId) {
    this.thirdPartyScanId = thirdPartyScanId;
  }
}
