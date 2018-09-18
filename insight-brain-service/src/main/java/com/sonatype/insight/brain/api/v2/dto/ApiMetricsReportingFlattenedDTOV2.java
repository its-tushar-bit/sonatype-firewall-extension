/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

/**
 * This DTO holds the same information as ApiMetricsReportingDTOV2 and ApiMetricsReportingAggregationDTOV2 but holds
 * it in a flattened structure suitable for CSV serialization.
 *
 * @since 1.52
 */
public class ApiMetricsReportingFlattenedDTOV2
{
  public String applicationId;

  public String applicationPublicId;

  public String applicationName;

  public String organizationId;

  public String organizationName;

  public String timePeriodStart;

  public Long mttrLowThreat;

  public Long mttrModerateThreat;

  public Long mttrSevereThreat;

  public Long mttrCriticalThreat;

  public int evaluationCount;

  public int openCountSecurity;

  public int openCountLicense;

  public int openCountQuality;

  public int openCountOther;

  public int discoveredCountSecurityLow;

  public int discoveredCountSecurityModerate;

  public int discoveredCountSecuritySevere;

  public int discoveredCountSecurityCritical;

  public int discoveredCountLicenseLow;

  public int discoveredCountLicenseModerate;

  public int discoveredCountLicenseSevere;

  public int discoveredCountLicenseCritical;

  public int discoveredCountQualityLow;

  public int discoveredCountQualityModerate;

  public int discoveredCountQualitySevere;

  public int discoveredCountQualityCritical;

  public int discoveredCountOtherLow;

  public int discoveredCountOtherModerate;

  public int discoveredCountOtherSevere;

  public int discoveredCountOtherCritical;

  public int fixedCountSecurityLow;

  public int fixedCountSecurityModerate;

  public int fixedCountSecuritySevere;

  public int fixedCountSecurityCritical;

  public int fixedCountLicenseLow;

  public int fixedCountLicenseModerate;

  public int fixedCountLicenseSevere;

  public int fixedCountLicenseCritical;

  public int fixedCountQualityLow;

  public int fixedCountQualityModerate;

  public int fixedCountQualitySevere;

  public int fixedCountQualityCritical;

  public int fixedCountOtherLow;

  public int fixedCountOtherModerate;

  public int fixedCountOtherSevere;

  public int fixedCountOtherCritical;

  public int waivedCountSecurityLow;

  public int waivedCountSecurityModerate;

  public int waivedCountSecuritySevere;

  public int waivedCountSecurityCritical;

  public int waivedCountLicenseLow;

  public int waivedCountLicenseModerate;

  public int waivedCountLicenseSevere;

  public int waivedCountLicenseCritical;

  public int waivedCountQualityLow;

  public int waivedCountQualityModerate;

  public int waivedCountQualitySevere;

  public int waivedCountQualityCritical;

  public int waivedCountOtherLow;

  public int waivedCountOtherModerate;

  public int waivedCountOtherSevere;

  public int waivedCountOtherCritical;

  public ApiMetricsReportingFlattenedDTOV2(String applicationId,
                                           String applicationPublicId,
                                           String applicationName,
                                           String organizationId,
                                           String organizationName,
                                           String timePeriodStart,
                                           Long mttrLowThreat,
                                           Long mttrModerateThreat,
                                           Long mttrSevereThreat,
                                           Long mttrCriticalThreat,
                                           int evaluationCount,
                                           int openCountSecurity,
                                           int openCountLicense,
                                           int openCountQuality,
                                           int openCountOther,
                                           int discoveredCountSecurityLow,
                                           int discoveredCountSecurityModerate,
                                           int discoveredCountSecuritySevere,
                                           int discoveredCountSecurityCritical,
                                           int discoveredCountLicenseLow,
                                           int discoveredCountLicenseModerate,
                                           int discoveredCountLicenseSevere,
                                           int discoveredCountLicenseCritical,
                                           int discoveredCountQualityLow,
                                           int discoveredCountQualityModerate,
                                           int discoveredCountQualitySevere,
                                           int discoveredCountQualityCritical,
                                           int discoveredCountOtherLow,
                                           int discoveredCountOtherModerate,
                                           int discoveredCountOtherSevere,
                                           int discoveredCountOtherCritical,
                                           int fixedCountSecurityLow,
                                           int fixedCountSecurityModerate,
                                           int fixedCountSecuritySevere,
                                           int fixedCountSecurityCritical,
                                           int fixedCountLicenseLow,
                                           int fixedCountLicenseModerate,
                                           int fixedCountLicenseSevere,
                                           int fixedCountLicenseCritical,
                                           int fixedCountQualityLow,
                                           int fixedCountQualityModerate,
                                           int fixedCountQualitySevere,
                                           int fixedCountQualityCritical,
                                           int fixedCountOtherLow,
                                           int fixedCountOtherModerate,
                                           int fixedCountOtherSevere,
                                           int fixedCountOtherCritical,
                                           int waivedCountSecurityLow,
                                           int waivedCountSecurityModerate,
                                           int waivedCountSecuritySevere,
                                           int waivedCountSecurityCritical,
                                           int waivedCountLicenseLow,
                                           int waivedCountLicenseModerate,
                                           int waivedCountLicenseSevere,
                                           int waivedCountLicenseCritical,
                                           int waivedCountQualityLow,
                                           int waivedCountQualityModerate,
                                           int waivedCountQualitySevere,
                                           int waivedCountQualityCritical,
                                           int waivedCountOtherLow,
                                           int waivedCountOtherModerate,
                                           int waivedCountOtherSevere,
                                           int waivedCountOtherCritical)
  {
    this.applicationId = applicationId;
    this.applicationPublicId = applicationPublicId;
    this.applicationName = applicationName;
    this.organizationId = organizationId;
    this.organizationName = organizationName;
    this.timePeriodStart = timePeriodStart;
    this.mttrLowThreat = mttrLowThreat;
    this.mttrModerateThreat = mttrModerateThreat;
    this.mttrSevereThreat = mttrSevereThreat;
    this.mttrCriticalThreat = mttrCriticalThreat;
    this.evaluationCount = evaluationCount;
    this.openCountSecurity = openCountSecurity;
    this.openCountLicense = openCountLicense;
    this.openCountQuality = openCountQuality;
    this.openCountOther = openCountOther;
    this.discoveredCountSecurityLow = discoveredCountSecurityLow;
    this.discoveredCountSecurityModerate = discoveredCountSecurityModerate;
    this.discoveredCountSecuritySevere = discoveredCountSecuritySevere;
    this.discoveredCountSecurityCritical = discoveredCountSecurityCritical;
    this.discoveredCountLicenseLow = discoveredCountLicenseLow;
    this.discoveredCountLicenseModerate = discoveredCountLicenseModerate;
    this.discoveredCountLicenseSevere = discoveredCountLicenseSevere;
    this.discoveredCountLicenseCritical = discoveredCountLicenseCritical;
    this.discoveredCountQualityLow = discoveredCountQualityLow;
    this.discoveredCountQualityModerate = discoveredCountQualityModerate;
    this.discoveredCountQualitySevere = discoveredCountQualitySevere;
    this.discoveredCountQualityCritical = discoveredCountQualityCritical;
    this.discoveredCountOtherLow = discoveredCountOtherLow;
    this.discoveredCountOtherModerate = discoveredCountOtherModerate;
    this.discoveredCountOtherSevere = discoveredCountOtherSevere;
    this.discoveredCountOtherCritical = discoveredCountOtherCritical;
    this.fixedCountSecurityLow = fixedCountSecurityLow;
    this.fixedCountSecurityModerate = fixedCountSecurityModerate;
    this.fixedCountSecuritySevere = fixedCountSecuritySevere;
    this.fixedCountSecurityCritical = fixedCountSecurityCritical;
    this.fixedCountLicenseLow = fixedCountLicenseLow;
    this.fixedCountLicenseModerate = fixedCountLicenseModerate;
    this.fixedCountLicenseSevere = fixedCountLicenseSevere;
    this.fixedCountLicenseCritical = fixedCountLicenseCritical;
    this.fixedCountQualityLow = fixedCountQualityLow;
    this.fixedCountQualityModerate = fixedCountQualityModerate;
    this.fixedCountQualitySevere = fixedCountQualitySevere;
    this.fixedCountQualityCritical = fixedCountQualityCritical;
    this.fixedCountOtherLow = fixedCountOtherLow;
    this.fixedCountOtherModerate = fixedCountOtherModerate;
    this.fixedCountOtherSevere = fixedCountOtherSevere;
    this.fixedCountOtherCritical = fixedCountOtherCritical;
    this.waivedCountSecurityLow = waivedCountSecurityLow;
    this.waivedCountSecurityModerate = waivedCountSecurityModerate;
    this.waivedCountSecuritySevere = waivedCountSecuritySevere;
    this.waivedCountSecurityCritical = waivedCountSecurityCritical;
    this.waivedCountLicenseLow = waivedCountLicenseLow;
    this.waivedCountLicenseModerate = waivedCountLicenseModerate;
    this.waivedCountLicenseSevere = waivedCountLicenseSevere;
    this.waivedCountLicenseCritical = waivedCountLicenseCritical;
    this.waivedCountQualityLow = waivedCountQualityLow;
    this.waivedCountQualityModerate = waivedCountQualityModerate;
    this.waivedCountQualitySevere = waivedCountQualitySevere;
    this.waivedCountQualityCritical = waivedCountQualityCritical;
    this.waivedCountOtherLow = waivedCountOtherLow;
    this.waivedCountOtherModerate = waivedCountOtherModerate;
    this.waivedCountOtherSevere = waivedCountOtherSevere;
    this.waivedCountOtherCritical = waivedCountOtherCritical;
  }
}
