/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.successmetrics.SuccessMetricsConfigurationDTO;
import com.sonatype.insight.brain.successmetrics.SuccessMetricsResource;
import com.sonatype.insight.brain.successmetrics.SuccessMetricsService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2SuccessMetricsResourceTest
{
  private IqTestContext ctx;

  private SystemConfigurationPropertyDAO dao;

  @BeforeEach
  void setUp() {
    dao = ctx.lookup(SystemConfigurationPropertyDAO.class);
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(SuccessMetricsResource.RESOURCE_PATH);
  }

  @Test
  void testGet() throws Exception {
    HttpResponse response = restRequest().get();
    ctx.assertResponseStatus(200, response);
    SuccessMetricsConfigurationDTO configuration = response.getBody(SuccessMetricsConfigurationDTO.class);
    assertThat(configuration.enabled).isTrue();
  }

  @Test
  void testUpdate() throws Exception {
    SuccessMetricsConfigurationDTO configuration = new SuccessMetricsConfigurationDTO();
    HttpResponse response = restRequest().body(configuration).put();
    ctx.assertResponseStatus(200, response);
    configuration = response.getBody(SuccessMetricsConfigurationDTO.class);
    assertThat(configuration.enabled).isFalse();
    assertThat(dao.getByName(SuccessMetricsService.PROPERTY_ENABLED).getValue()).isEqualTo("false");
  }
}
