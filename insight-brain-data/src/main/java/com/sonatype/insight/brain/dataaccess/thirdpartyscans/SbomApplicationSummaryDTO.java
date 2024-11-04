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

  private SbomVulnerabilitiesSummaryDTO sbomVulnerabilitiesSummaryDTO;

  private SbomPolicyViolationSummaryDTO applicationPagePolicyViolationSummary;

  private Double annotatedPercentage;

  public SbomApplicationSummaryDTO(Object[] result,
                                   SbomPolicyViolationSummaryDTO policyViolationSummary)
  {
    applicationInternalId =  String.valueOf(result[0]);
    sbomVersion = String.valueOf(result[1]);
    importDate = (Date) result[2];
    applicationPublicId = String.valueOf(result[3]);
    applicationName = String.valueOf(result[4]);
    SbomVulnerabilitiesSummaryDTO sbomVulnerabilitiesSummaryDTO =
        new SbomVulnerabilitiesSummaryDTO();
    sbomVulnerabilitiesSummaryDTO.setVulnerabilityNone((Long)result[5]);
    sbomVulnerabilitiesSummaryDTO.setVulnerabilityLow((Long)result[6]);
    sbomVulnerabilitiesSummaryDTO.setVulnerabilityMedium((Long)result[7]);
    sbomVulnerabilitiesSummaryDTO.setVulnerabilityHigh((Long)result[8]);
    sbomVulnerabilitiesSummaryDTO.setVulnerabilityCritical((Long)result[9]);
    this.applicationPagePolicyViolationSummary = policyViolationSummary;
    this.sbomVulnerabilitiesSummaryDTO = sbomVulnerabilitiesSummaryDTO;
    annotatedPercentage = result[10] == null ? null : ((Number)result[10]).doubleValue();
  }

  //for Jackson
  public SbomApplicationSummaryDTO() {
  }

  public SbomApplicationSummaryDTO(final Object[] result) {
    applicationInternalId =  String.valueOf(result[0]);
    sbomVersion = String.valueOf(result[1]);
    importDate = (Date) result[2];
    applicationPublicId = String.valueOf(result[3]);
    applicationName = String.valueOf(result[4]);
    SbomVulnerabilitiesSummaryDTO sbomVulnerabilitiesSummaryDTO =
        new SbomVulnerabilitiesSummaryDTO();
    sbomVulnerabilitiesSummaryDTO.setVulnerabilityNone((Long)result[5]);
    sbomVulnerabilitiesSummaryDTO.setVulnerabilityLow((Long)result[6]);
    sbomVulnerabilitiesSummaryDTO.setVulnerabilityMedium((Long)result[7]);
    sbomVulnerabilitiesSummaryDTO.setVulnerabilityHigh((Long)result[8]);
    sbomVulnerabilitiesSummaryDTO.setVulnerabilityCritical((Long)result[9]);
    this.applicationPagePolicyViolationSummary = null;
    this.sbomVulnerabilitiesSummaryDTO = sbomVulnerabilitiesSummaryDTO;
    annotatedPercentage = result[10] == null ? null : ((Number)result[10]).doubleValue();
  }

  public SbomVulnerabilitiesSummaryDTO getApplicationPageVulnerabilitySummary() {
    return sbomVulnerabilitiesSummaryDTO;
  }

  public SbomPolicyViolationSummaryDTO getApplicationPagePolicyViolationSummary() {
    return applicationPagePolicyViolationSummary;
  }

  public void setApplicationPageVulnerabilitySummary(
      final SbomVulnerabilitiesSummaryDTO sbomVulnerabilitiesSummaryDTO)
  {
    this.sbomVulnerabilitiesSummaryDTO = sbomVulnerabilitiesSummaryDTO;
  }

  public void setApplicationPagePolicyViolationSummary(
      final SbomPolicyViolationSummaryDTO applicationPagePolicyViolationSummary)
  {
    this.applicationPagePolicyViolationSummary = applicationPagePolicyViolationSummary;
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

  public Double getAnnotatedPercentage() {
    return annotatedPercentage;
  }

  public void setAnnotatedPercentage(final Double annotatedPercentage) {
    this.annotatedPercentage = annotatedPercentage;
  }
}
