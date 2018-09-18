
/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ApiMetricsReportingFlattenedDTOV2Test
{
  @Test
  public void testConstructor() {
    ApiMetricsReportingFlattenedDTOV2 dto = new ApiMetricsReportingFlattenedDTOV2( //
        "appId", "appPublicId", "appName", "orgId", "orgName",
        "2017-10-01", //
        20L, 500L, 2L, 6L, //
        10, // evaluationCount
        1, 2, 3, 4, // openCounts
        1, 0, 0, 0, // discovered security
        0, 5, 0, 0, // discovered license
        0, 0, 0, 0, // discovered quality
        0, 0, 0, 0, // discovered other
        0, 0, 0, 0, // fixed security
        0, 0, 0, 0, // fixed license
        0, 0, 2, 0, // fixed quality
        0, 0, 0, 0, // fixed other
        0, 0, 0, 0, // waived security
        0, 0, 0, 0, // waived license
        0, 0, 0, 0, // waived quality
        0, 0, 0, 100);  // waived other

    assertThat(dto.timePeriodStart, is("2017-10-01"));

    assertThat(dto.applicationId, is("appId"));
    assertThat(dto.applicationPublicId, is("appPublicId"));
    assertThat(dto.applicationName, is("appName"));
    assertThat(dto.organizationId, is("orgId"));
    assertThat(dto.organizationName, is("orgName"));

    assertThat(dto.mttrLowThreat, is(20L));
    assertThat(dto.mttrModerateThreat, is(500L));
    assertThat(dto.mttrSevereThreat, is(2L));
    assertThat(dto.mttrCriticalThreat, is(6L));

    assertThat(dto.discoveredCountSecurityLow, is(1));
    assertThat(dto.discoveredCountSecurityModerate, is(0));
    assertThat(dto.discoveredCountSecuritySevere, is(0));
    assertThat(dto.discoveredCountSecurityCritical, is(0));
    assertThat(dto.discoveredCountLicenseLow, is(0));
    assertThat(dto.discoveredCountLicenseModerate, is(5));
    assertThat(dto.discoveredCountLicenseSevere, is(0));
    assertThat(dto.discoveredCountLicenseCritical, is(0));
    assertThat(dto.discoveredCountQualityLow, is(0));
    assertThat(dto.discoveredCountQualityModerate, is(0));
    assertThat(dto.discoveredCountQualitySevere, is(0));
    assertThat(dto.discoveredCountQualityCritical, is(0));
    assertThat(dto.discoveredCountOtherLow, is(0));
    assertThat(dto.discoveredCountOtherModerate, is(0));
    assertThat(dto.discoveredCountOtherSevere, is(0));
    assertThat(dto.discoveredCountOtherCritical, is(0));

    assertThat(dto.fixedCountSecurityLow, is(0));
    assertThat(dto.fixedCountSecurityModerate, is(0));
    assertThat(dto.fixedCountSecuritySevere, is(0));
    assertThat(dto.fixedCountSecurityCritical, is(0));
    assertThat(dto.fixedCountLicenseLow, is(0));
    assertThat(dto.fixedCountLicenseModerate, is(0));
    assertThat(dto.fixedCountLicenseSevere, is(0));
    assertThat(dto.fixedCountLicenseCritical, is(0));
    assertThat(dto.fixedCountQualityLow, is(0));
    assertThat(dto.fixedCountQualityModerate, is(0));
    assertThat(dto.fixedCountQualitySevere, is(2));
    assertThat(dto.fixedCountQualityCritical, is(0));
    assertThat(dto.fixedCountOtherLow, is(0));
    assertThat(dto.fixedCountOtherModerate, is(0));
    assertThat(dto.fixedCountOtherSevere, is(0));
    assertThat(dto.fixedCountOtherCritical, is(0));

    assertThat(dto.waivedCountSecurityLow, is(0));
    assertThat(dto.waivedCountSecurityModerate, is(0));
    assertThat(dto.waivedCountSecuritySevere, is(0));
    assertThat(dto.waivedCountSecurityCritical, is(0));
    assertThat(dto.waivedCountLicenseLow, is(0));
    assertThat(dto.waivedCountLicenseModerate, is(0));
    assertThat(dto.waivedCountLicenseSevere, is(0));
    assertThat(dto.waivedCountLicenseCritical, is(0));
    assertThat(dto.waivedCountQualityLow, is(0));
    assertThat(dto.waivedCountQualityModerate, is(0));
    assertThat(dto.waivedCountQualitySevere, is(0));
    assertThat(dto.waivedCountQualityCritical, is(0));
    assertThat(dto.waivedCountOtherLow, is(0));
    assertThat(dto.waivedCountOtherModerate, is(0));
    assertThat(dto.waivedCountOtherSevere, is(0));
    assertThat(dto.waivedCountOtherCritical, is(100));

    assertThat(dto.openCountSecurity, is(1));
    assertThat(dto.openCountLicense, is(2));
    assertThat(dto.openCountQuality, is(3));
    assertThat(dto.openCountOther, is(4));

    assertThat(dto.evaluationCount, is(10));
  }
}
