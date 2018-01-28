/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.apache.http.HttpStatus;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class SystemConfigurationPropertyResourceTest
    extends AbstractResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(SystemConfigurationPropertyResource.RESOURCE_PATH);
  }

  @Test
  public void testGet() throws Exception {
    tempEntity.newSystemConfigurationProperty("TEST-NAME", "TEST-VALUE");
    HttpResponse response = restRequest().path("TEST-NAME").get();
    assertResponseStatus(HttpStatus.SC_OK, response);
    SystemConfigurationProperty property = response.getBody(SystemConfigurationProperty.class);
    assertThat(property.getValue(), is("TEST-VALUE"));
  }

  @Test
  public void testUpdate() throws Exception {
    tempEntity.newSystemConfigurationProperty("TEST-NAME", "TEST-VALUE");
    HttpResponse response = restRequest().body(new SystemConfigurationProperty("TEST-NAME", "UPDATED-VALUE")).put();
    assertResponseStatus(HttpStatus.SC_OK, response);
    SystemConfigurationProperty property = response.getBody(SystemConfigurationProperty.class);
    assertThat(property.getValue(), is("UPDATED-VALUE"));
  }
}
