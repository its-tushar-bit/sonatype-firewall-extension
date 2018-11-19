/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.successmetrics;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class SuccessMetricsResourceTest
    extends AbstractResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(SuccessMetricsResource.RESOURCE_PATH);
  }

  @Test
  public void testGet() throws Exception {
    HttpResponse response = restRequest().get();
    assertResponseStatus(200, response);
    SuccessMetricsConfigurationDTO configuration = response.getBody(SuccessMetricsConfigurationDTO.class);
    assertThat(configuration.enabled, is(true));
  }

  @Test
  public void testUpdate() throws Exception {
    SuccessMetricsConfigurationDTO configuration = new SuccessMetricsConfigurationDTO();
    HttpResponse response = restRequest().body(configuration).put();
    assertResponseStatus(200, response);
    configuration = response.getBody(SuccessMetricsConfigurationDTO.class);
    assertThat(configuration.enabled, is(false));
    assertThat(new SystemConfigurationPropertyDAO().getByName(SuccessMetricsService.PROPERTY_ENABLED).getValue(),
        is("false"));
  }
}
