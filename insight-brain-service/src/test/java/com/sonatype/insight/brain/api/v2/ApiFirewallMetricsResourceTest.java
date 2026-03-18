/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.HashMap;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.roi.dto.RoiFirewallMetricsDTO;
import com.sonatype.insight.brain.model.roi.CurrencyTypes;
import com.sonatype.insight.brain.model.successmetrics.ApiFirewallMetricsResultDTO;
import com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName.NAMESPACE_ATTACKS_BLOCKED;
import static com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName.SAFE_VERSIONS_SELECTED_AUTOMATICALLY;
import static com.sonatype.insight.brain.utils.DateConverter.toDate;
import static org.assertj.core.api.Assertions.assertThat;

public class ApiFirewallMetricsResourceTest
    extends AbstractResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(ApiFirewallMetricsResource.RESOURCE_PATH);
  }

  @Test
  public void testGetFirewallMetrics() throws Exception {
    LocalDate fiveDaysAgoLocalDate = LocalDate.now().minusDays(5);
    tempEntity.newFirewallMetrics(FirewallMetricsName.WAIVED_COMPONENTS, 10,
        toDate(fiveDaysAgoLocalDate), fiveDaysAgoLocalDate);
    HttpResponse response = restRequest().get();
    assertResponseStatus(200, response);
    HashMap<FirewallMetricsName, ApiFirewallMetricsResultDTO> firewallMetricsNameValueMap = response
        .getBody(HashMap.class);
    assertThat(firewallMetricsNameValueMap).hasSize(6);
    ObjectMapper objectMapper = new ObjectMapper();
    ApiFirewallMetricsResultDTO apiFirewallMetricsResultDTO =
        objectMapper.convertValue(firewallMetricsNameValueMap.get("WAIVED_COMPONENTS"),
            ApiFirewallMetricsResultDTO.class);
    assertThat(apiFirewallMetricsResultDTO.getFirewallMetricsValue()).isEqualTo(10);
    assertThat(apiFirewallMetricsResultDTO.getLatestUpdatedTime()).isEqualTo(toDate(fiveDaysAgoLocalDate));
  }

  @Test
  public void testGetRoiFirewallMetrics_RoiConfigurationExist() throws Exception {
    tempEntity.createRoiConfiguration(
        CurrencyTypes.USD,
        BigDecimal.valueOf(50000),
        BigDecimal.valueOf(60000),
        BigDecimal.valueOf(70000),
        15,
        BigDecimal.valueOf(400));

    tempEntity.newFirewallMetrics(FirewallMetricsName.SUPPLY_CHAIN_ATTACKS_BLOCKED, 10, new Date());
    tempEntity.newFirewallMetrics(NAMESPACE_ATTACKS_BLOCKED, 10, new Date());
    tempEntity.newFirewallMetrics(SAFE_VERSIONS_SELECTED_AUTOMATICALLY, 10, new Date(), LocalDate.now());
    HttpResponse response = restRequest().path(ApiFirewallMetricsResource.ROI_FIREWALL_METRICS_PATH)
        .parameter(CurrencyTypes.USD)
        .get();
    assertResponseStatus(200, response);
    RoiFirewallMetricsDTO roiMetricsDTO = response.getBody(RoiFirewallMetricsDTO.class);
    assertThat(roiMetricsDTO).isNotNull();
    assertThat(roiMetricsDTO.getCurrency()).isEqualTo(CurrencyTypes.USD);
    assertThat(roiMetricsDTO.getMalwareAttacksPrevented()).isEqualTo(BigDecimal.valueOf(500000));
    assertThat(roiMetricsDTO.getSafeComponentsAutoSelected()).isEqualTo(BigDecimal.valueOf(700000));
    assertThat(roiMetricsDTO.getNamespaceAttacksPrevented()).isEqualTo(BigDecimal.valueOf(600000));
  }
}
