/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.thirdpartyscans;

import java.util.Date;

import com.sonatype.insight.brain.model.thirdpartyscans.SbomPolicyViolationSummaryDTO;
import com.sonatype.insight.json.store.ISODateSerializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class SbomApplicationSummaryDTO
{
  private String applicationInternalId;

  private String sbomVersion;

  private String applicationPublicId;

  private String applicationName;

  @JsonSerialize(using = ISODateSerializer.class)
  private Date importDate;

  private SbomVulnerabilitiesSummaryDTO vulnerabilitySummary;

  private SbomPolicyViolationSummaryDTO policyViolationSummary;

  private Double releaseStatusPercentage;

  public SbomApplicationSummaryDTO(
      Object[] result,
      SbomPolicyViolationSummaryDTO policyViolationSummary)
  {
    applicationInternalId = String.valueOf(result[0]);
    sbomVersion = String.valueOf(result[1]);
    importDate = (Date) result[2];
    applicationPublicId = String.valueOf(result[3]);
    applicationName = String.valueOf(result[4]);
    SbomVulnerabilitiesSummaryDTO sbomVulnerabilitiesSummaryDTO =
        new SbomVulnerabilitiesSummaryDTO();
    sbomVulnerabilitiesSummaryDTO.setNone((Long) result[5]);
    sbomVulnerabilitiesSummaryDTO.setLow((Long) result[6]);
    sbomVulnerabilitiesSummaryDTO.setMedium((Long) result[7]);
    sbomVulnerabilitiesSummaryDTO.setHigh((Long) result[8]);
    sbomVulnerabilitiesSummaryDTO.setCritical((Long) result[9]);
    this.policyViolationSummary = policyViolationSummary;
    this.vulnerabilitySummary = sbomVulnerabilitiesSummaryDTO;
    releaseStatusPercentage = result[11] == null ? null : ((Number) result[11]).doubleValue();
  }

  // for Jackson
  public SbomApplicationSummaryDTO() {
  }

  public SbomApplicationSummaryDTO(final Object[] result) {
    applicationInternalId = String.valueOf(result[0]);
    sbomVersion = String.valueOf(result[1]);
    importDate = (Date) result[2];
    applicationPublicId = String.valueOf(result[3]);
    applicationName = String.valueOf(result[4]);
    SbomVulnerabilitiesSummaryDTO sbomVulnerabilitiesSummaryDTO =
        new SbomVulnerabilitiesSummaryDTO();
    sbomVulnerabilitiesSummaryDTO.setNone((Long) result[5]);
    sbomVulnerabilitiesSummaryDTO.setLow((Long) result[6]);
    sbomVulnerabilitiesSummaryDTO.setMedium((Long) result[7]);
    sbomVulnerabilitiesSummaryDTO.setHigh((Long) result[8]);
    sbomVulnerabilitiesSummaryDTO.setCritical((Long) result[9]);
    this.policyViolationSummary = null;
    this.vulnerabilitySummary = sbomVulnerabilitiesSummaryDTO;
    releaseStatusPercentage = result[11] == null ? null : ((Number) result[11]).doubleValue();
  }

  public Double getReleaseStatusPercentage() {
    return releaseStatusPercentage;
  }

  public void setReleaseStatusPercentage(final Double releaseStatusPercentage) {
    this.releaseStatusPercentage = releaseStatusPercentage;
  }

  public SbomVulnerabilitiesSummaryDTO getVulnerabilitySummary() {
    return vulnerabilitySummary;
  }

  public SbomPolicyViolationSummaryDTO getPolicyViolationSummary() {
    return policyViolationSummary;
  }

  public void setVulnerabilitySummary(
      final SbomVulnerabilitiesSummaryDTO sbomVulnerabilitiesSummaryDTO)
  {
    this.vulnerabilitySummary = sbomVulnerabilitiesSummaryDTO;
  }

  public void setPolicyViolationSummary(
      final SbomPolicyViolationSummaryDTO policyViolationSummary)
  {
    this.policyViolationSummary = policyViolationSummary;
  }

  public String getApplicationInternalId() {
    return applicationInternalId;
  }

  public String getApplicationPublicId() {
    return applicationPublicId;
  }

  public String getApplicationName() {
    return applicationName;
  }

  public Date getImportDate() {
    return importDate;
  }

  public void setApplicationInternalId(final String applicationInternalId) {
    this.applicationInternalId = applicationInternalId;
  }

  public void setApplicationPublicId(final String applicationPublicId) {
    this.applicationPublicId = applicationPublicId;
  }

  public void setApplicationName(final String applicationName) {
    this.applicationName = applicationName;
  }

  public void setImportDate(final Date importDate) {
    this.importDate = importDate;
  }

  public String getSbomVersion() {
    return sbomVersion;
  }

  public void setSbomVersion(final String sbomVersion) {
    this.sbomVersion = sbomVersion;
  }
}
