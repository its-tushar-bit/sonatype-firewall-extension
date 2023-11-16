/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;
import javax.inject.Inject;

import com.sonatype.insight.brain.model.successmetrics.ApiFirewallMetricsResultDTO;
import com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ApiFirewallMetricsServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApiFirewallMetricsService firewallMetricsService;

  @Inject
  private TestProductLicense testProductLicense;

  @Test
  public void testGetFirewallMetrics() {
    Date testDate1 = new GregorianCalendar(2023, Calendar.OCTOBER, 1).getTime();
    Date testDate2 = new GregorianCalendar(2023, Calendar.OCTOBER, 2).getTime();
    tempEntity.newFirewallMetrics(FirewallMetricsName.WAIVED_COMPONENTS, 20, testDate1);
    tempEntity.newFirewallMetrics(FirewallMetricsName.COMPONENTS_QUARANTINED, 10, testDate2);

    Map<FirewallMetricsName, ApiFirewallMetricsResultDTO> firewallMetricsNameValueMap =
        firewallMetricsService.getFirewallMetrics();
    assertThat(firewallMetricsNameValueMap.size()).isEqualTo(6);
    ApiFirewallMetricsResultDTO apiFirewallMetricsResultDTO1 =
        firewallMetricsNameValueMap.get(FirewallMetricsName.WAIVED_COMPONENTS);
    ApiFirewallMetricsResultDTO apiFirewallMetricsResultDTO2 =
        firewallMetricsNameValueMap.get(FirewallMetricsName.COMPONENTS_QUARANTINED);
    ApiFirewallMetricsResultDTO apiFirewallMetricsResultDTO3 =
        firewallMetricsNameValueMap.get(FirewallMetricsName.NAMESPACE_ATTACKS_BLOCKED);
    ApiFirewallMetricsResultDTO apiFirewallMetricsResultDTO4 =
        firewallMetricsNameValueMap.get(FirewallMetricsName.COMPONENTS_AUTO_RELEASED);
    ApiFirewallMetricsResultDTO apiFirewallMetricsResultDTO5 =
        firewallMetricsNameValueMap.get(FirewallMetricsName.SUPPLY_CHAIN_ATTACKS_BLOCKED);
    ApiFirewallMetricsResultDTO apiFirewallMetricsResultDTO6 =
        firewallMetricsNameValueMap.get(FirewallMetricsName.SAFE_VERSIONS_SELECTED_AUTOMATICALLY);
    assertThat(apiFirewallMetricsResultDTO1.getFirewallMetricsValue()).isEqualTo(20);
    assertThat(apiFirewallMetricsResultDTO1.getLatestUpdatedTime()).isEqualTo(testDate1);
    assertThat(apiFirewallMetricsResultDTO2.getFirewallMetricsValue()).isEqualTo(10);
    assertThat(apiFirewallMetricsResultDTO2.getLatestUpdatedTime()).isEqualTo(testDate2);
    assertThat(apiFirewallMetricsResultDTO3.getFirewallMetricsValue()).isEqualTo(0);
    assertThat(apiFirewallMetricsResultDTO3.getLatestUpdatedTime()).isNull();
    assertThat(apiFirewallMetricsResultDTO4.getFirewallMetricsValue()).isEqualTo(0);
    assertThat(apiFirewallMetricsResultDTO4.getLatestUpdatedTime()).isNull();
    assertThat(apiFirewallMetricsResultDTO5.getFirewallMetricsValue()).isEqualTo(0);
    assertThat(apiFirewallMetricsResultDTO5.getLatestUpdatedTime()).isNull();
    assertThat(apiFirewallMetricsResultDTO6.getFirewallMetricsValue()).isEqualTo(0);
    assertThat(apiFirewallMetricsResultDTO6.getLatestUpdatedTime()).isNull();
  }

  @Test
  public void testGetFirewallMetrics_NoReleaseIntegrityFeature() {
    testProductLicense.setMissingFeatures(LicensedFeature.RELEASE_INTEGRITY);

    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(
        () -> firewallMetricsService.getFirewallMetrics());
  }

  @Test
  public void testGetFirewallMetrics_NoFirewallAutoUnquarantineFeature() {
    testProductLicense.setMissingFeatures(LicensedFeature.FIREWALL_AUTO_UNQUARANTINE);

    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> firewallMetricsService.getFirewallMetrics());
  }
}
