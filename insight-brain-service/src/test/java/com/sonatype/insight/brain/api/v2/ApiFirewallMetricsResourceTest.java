/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.time.LocalDate;
import java.util.HashMap;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.successmetrics.ApiFirewallMetricsResultDTO;
import com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

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
    LocalDate fiveDaysAgoLocalDate =  LocalDate.now().minusDays(5);
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
}
