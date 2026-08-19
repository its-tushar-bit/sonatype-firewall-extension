/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.configuration.AutomaticSourceControlConfiguration;
import com.sonatype.insight.brain.configuration.AutomaticSourceControlConfigurationResource;
import com.sonatype.insight.brain.dataaccess.configuration.AutomaticSourceControlConfigurationDAO;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2AutomaticSourceControlConfigurationResourceTest
{
  private IqTestContext ctx;

  private AutomaticSourceControlConfigurationDAO automaticSourceControlConfigurationDAO;

  @BeforeEach
  void setUp() {
    automaticSourceControlConfigurationDAO = ctx.lookup(AutomaticSourceControlConfigurationDAO.class);
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(AutomaticSourceControlConfigurationResource.RESOURCE_PATH);
  }

  @Test
  void testGetAutomaticSourceControl() throws Exception {
    automaticSourceControlConfigurationDAO.setSourceControlConfigurationEnabled(false);

    HttpResponse response = restRequest().get();
    ctx.assertResponseStatus(HttpStatus.SC_OK, response);

    AutomaticSourceControlConfiguration configuration = response.getBody(AutomaticSourceControlConfiguration.class);
    assertThat(configuration.isEnabled()).isFalse();
  }

  @Test
  void testUpdateAutomaticSourceControl() throws Exception {
    HttpResponse response = restRequest().body(new AutomaticSourceControlConfiguration(true)).put();
    ctx.assertResponseStatus(HttpStatus.SC_OK, response);

    AutomaticSourceControlConfiguration configuration = response.getBody(AutomaticSourceControlConfiguration.class);
    assertThat(configuration.isEnabled()).isTrue();

    assertThat(automaticSourceControlConfigurationDAO.isSourceControlConfigurationEnabled()).isTrue();
  }
}
