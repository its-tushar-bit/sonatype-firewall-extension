/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.configuration.AutomaticSourceControlConfigurationDAO;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class AutomaticSourceControlConfigurationResourceTest
    extends AbstractResourceTest
{
  private AutomaticSourceControlConfigurationDAO automaticSourceControlConfigurationDAO;

  @Before
  public void setUp() {
    automaticSourceControlConfigurationDAO = lookup(AutomaticSourceControlConfigurationDAO.class);
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(AutomaticSourceControlConfigurationResource.RESOURCE_PATH);
  }

  @Test
  public void testGetAutomaticSourceControl() throws Exception {
    automaticSourceControlConfigurationDAO.setSourceControlConfigurationEnabled(false);

    HttpResponse response = restRequest().get();
    assertResponseStatus(HttpStatus.SC_OK, response);

    AutomaticSourceControlConfiguration configuration = response.getBody(AutomaticSourceControlConfiguration.class);
    assertThat(configuration.isEnabled()).isFalse();
  }

  @Test
  public void testUpdateAutomaticSourceControl() throws Exception {
    HttpResponse response = restRequest().body(new AutomaticSourceControlConfiguration(true)).put();
    assertResponseStatus(HttpStatus.SC_OK, response);

    AutomaticSourceControlConfiguration configuration = response.getBody(AutomaticSourceControlConfiguration.class);
    assertThat(configuration.isEnabled()).isTrue();

    assertThat(automaticSourceControlConfigurationDAO.isSourceControlConfigurationEnabled()).isTrue();
  }
}
