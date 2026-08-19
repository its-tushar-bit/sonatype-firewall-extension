/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.sbom.SbomSpecification;
import com.sonatype.insight.brain.scan.datastore.ScanEntity;
import com.sonatype.insight.scan.model.ItemContentType;

import org.apache.commons.lang3.StringUtils;

public class ThirdPartyScanContext
{
  private final String scanRequestId;

  private final String applicationId;

  private final SbomScanType scanType;

  private ScanEntity scanEntity;

  private boolean sbomSavedForScan;

  private String sbomFileName;

  private String applicationVersion;

  private String thirdPartyFileId;

  private String thirdPartyScanId;

  private String stageType;

  private String sbomMetadataId;

  private Boolean isValid;

  private final List<String> containerUriPaths = new ArrayList<>();

  private ItemContentType containerItemContentType;

  private SbomSpecification containerImageSbomSpecification;

  public ThirdPartyScanContext(
      final String scanRequestId,
      final String applicationId,
      final SbomScanType scanType,
      final ScanEntity scanEntity,
      final String stageType)
  {
    this.scanRequestId = scanRequestId;
    this.applicationId = applicationId;
    this.scanType = scanType;
    this.scanEntity = scanEntity;
    this.stageType = stageType;
  }

  public String getScanRequestId() {
    return scanRequestId;
  }

  public String getApplicationId() {
    return applicationId;
  }

  public ScanEntity getScanEntity() {
    return scanEntity;
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

  public String getApplicationVersion() {
    return applicationVersion;
  }

  public void setApplicationVersion(final String applicationVersion) {
    this.applicationVersion = applicationVersion;
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

  public SbomScanType getScanType() {
    return scanType;
  }

  public Boolean isValid() {
    return isValid;
  }

  public void setIsValid(final Boolean isValid) {
    this.isValid = isValid;
  }

  public List<String> getContainerUriPaths() {
    return containerUriPaths;
  }

  public void addContainerUriPath(String path) {
    if (StringUtils.isNotEmpty(path)) {
      this.containerUriPaths.add(path);
    }
  }

  public ItemContentType getContainerItemContentType() {
    return containerItemContentType;
  }

  public void setContainerItemContentType(ItemContentType containerItemContentType) {
    this.containerItemContentType = containerItemContentType;
  }

  public SbomSpecification getContainerImageSbomSpecification() {
    return containerImageSbomSpecification;
  }

  public void setContainerImageSbomSpecification(final SbomSpecification containerImageSbomSpecification) {
    this.containerImageSbomSpecification = containerImageSbomSpecification;
  }
}
