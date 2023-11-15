/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.successmetrics.ApiFirewallMetricsResultDTO;
import com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

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
    Date testDate = new GregorianCalendar(2023, Calendar.OCTOBER, 1).getTime();
    tempEntity.newFirewallMetrics(FirewallMetricsName.WAIVED_COMPONENTS, 10, testDate);
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
    assertThat(apiFirewallMetricsResultDTO.getLatestUpdatedTime()).isEqualTo(testDate);
  }
}
