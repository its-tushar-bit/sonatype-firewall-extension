
/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiMetricsReportingFlattenedDTOV2Test
{
  @Test
  public void testConstructor() {
    ApiMetricsReportingFlattenedDTOV2 dto = new ApiMetricsReportingFlattenedDTOV2( //
        "appId", "appPublicId", "appName", "orgId", "orgName",
        "2017-10-01", //
        20L, 500L, 2L, 6L, //
        10, // evaluationCount
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
        0, 0, 0, 100, // waived other
        0, 0, 0, 0, // open security
        0, 0, 0, 0, // open license
        0, 0, 0, 0, // open quality
        0, 0, 99, 0); // open other

    assertThat(dto.timePeriodStart).isEqualTo("2017-10-01");

    assertThat(dto.applicationId).isEqualTo("appId");
    assertThat(dto.applicationPublicId).isEqualTo("appPublicId");
    assertThat(dto.applicationName).isEqualTo("appName");
    assertThat(dto.organizationId).isEqualTo("orgId");
    assertThat(dto.organizationName).isEqualTo("orgName");

    assertThat(dto.mttrLowThreat).isEqualTo(20);
    assertThat(dto.mttrModerateThreat).isEqualTo(500);
    assertThat(dto.mttrSevereThreat).isEqualTo(2);
    assertThat(dto.mttrCriticalThreat).isEqualTo(6);

    assertThat(dto.discoveredCountSecurityLow).isEqualTo(1);
    assertThat(dto.discoveredCountSecurityModerate).isEqualTo(0);
    assertThat(dto.discoveredCountSecuritySevere).isEqualTo(0);
    assertThat(dto.discoveredCountSecurityCritical).isEqualTo(0);
    assertThat(dto.discoveredCountLicenseLow).isEqualTo(0);
    assertThat(dto.discoveredCountLicenseModerate).isEqualTo(5);
    assertThat(dto.discoveredCountLicenseSevere).isEqualTo(0);
    assertThat(dto.discoveredCountLicenseCritical).isEqualTo(0);
    assertThat(dto.discoveredCountQualityLow).isEqualTo(0);
    assertThat(dto.discoveredCountQualityModerate).isEqualTo(0);
    assertThat(dto.discoveredCountQualitySevere).isEqualTo(0);
    assertThat(dto.discoveredCountQualityCritical).isEqualTo(0);
    assertThat(dto.discoveredCountOtherLow).isEqualTo(0);
    assertThat(dto.discoveredCountOtherModerate).isEqualTo(0);
    assertThat(dto.discoveredCountOtherSevere).isEqualTo(0);
    assertThat(dto.discoveredCountOtherCritical).isEqualTo(0);

    assertThat(dto.fixedCountSecurityLow).isEqualTo(0);
    assertThat(dto.fixedCountSecurityModerate).isEqualTo(0);
    assertThat(dto.fixedCountSecuritySevere).isEqualTo(0);
    assertThat(dto.fixedCountSecurityCritical).isEqualTo(0);
    assertThat(dto.fixedCountLicenseLow).isEqualTo(0);
    assertThat(dto.fixedCountLicenseModerate).isEqualTo(0);
    assertThat(dto.fixedCountLicenseSevere).isEqualTo(0);
    assertThat(dto.fixedCountLicenseCritical).isEqualTo(0);
    assertThat(dto.fixedCountQualityLow).isEqualTo(0);
    assertThat(dto.fixedCountQualityModerate).isEqualTo(0);
    assertThat(dto.fixedCountQualitySevere).isEqualTo(2);
    assertThat(dto.fixedCountQualityCritical).isEqualTo(0);
    assertThat(dto.fixedCountOtherLow).isEqualTo(0);
    assertThat(dto.fixedCountOtherModerate).isEqualTo(0);
    assertThat(dto.fixedCountOtherSevere).isEqualTo(0);
    assertThat(dto.fixedCountOtherCritical).isEqualTo(0);

    assertThat(dto.waivedCountSecurityLow).isEqualTo(0);
    assertThat(dto.waivedCountSecurityModerate).isEqualTo(0);
    assertThat(dto.waivedCountSecuritySevere).isEqualTo(0);
    assertThat(dto.waivedCountSecurityCritical).isEqualTo(0);
    assertThat(dto.waivedCountLicenseLow).isEqualTo(0);
    assertThat(dto.waivedCountLicenseModerate).isEqualTo(0);
    assertThat(dto.waivedCountLicenseSevere).isEqualTo(0);
    assertThat(dto.waivedCountLicenseCritical).isEqualTo(0);
    assertThat(dto.waivedCountQualityLow).isEqualTo(0);
    assertThat(dto.waivedCountQualityModerate).isEqualTo(0);
    assertThat(dto.waivedCountQualitySevere).isEqualTo(0);
    assertThat(dto.waivedCountQualityCritical).isEqualTo(0);
    assertThat(dto.waivedCountOtherLow).isEqualTo(0);
    assertThat(dto.waivedCountOtherModerate).isEqualTo(0);
    assertThat(dto.waivedCountOtherSevere).isEqualTo(0);
    assertThat(dto.waivedCountOtherCritical).isEqualTo(100);

    assertThat(dto.openCountAtTimePeriodEndSecurityLow).isEqualTo(0);
    assertThat(dto.openCountAtTimePeriodEndSecurityModerate).isEqualTo(0);
    assertThat(dto.openCountAtTimePeriodEndSecuritySevere).isEqualTo(0);
    assertThat(dto.openCountAtTimePeriodEndSecurityCritical).isEqualTo(0);
    assertThat(dto.openCountAtTimePeriodEndLicenseLow).isEqualTo(0);
    assertThat(dto.openCountAtTimePeriodEndLicenseModerate).isEqualTo(0);
    assertThat(dto.openCountAtTimePeriodEndLicenseSevere).isEqualTo(0);
    assertThat(dto.openCountAtTimePeriodEndLicenseCritical).isEqualTo(0);
    assertThat(dto.openCountAtTimePeriodEndQualityLow).isEqualTo(0);
    assertThat(dto.openCountAtTimePeriodEndQualityModerate).isEqualTo(0);
    assertThat(dto.openCountAtTimePeriodEndQualitySevere).isEqualTo(0);
    assertThat(dto.openCountAtTimePeriodEndQualityCritical).isEqualTo(0);
    assertThat(dto.openCountAtTimePeriodEndOtherLow).isEqualTo(0);
    assertThat(dto.openCountAtTimePeriodEndOtherModerate).isEqualTo(0);
    assertThat(dto.openCountAtTimePeriodEndOtherSevere).isEqualTo(99);
    assertThat(dto.openCountAtTimePeriodEndOtherCritical).isEqualTo(0);

    assertThat(dto.evaluationCount).isEqualTo(10);
  }
}
