/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.configuration.AutomaticApplicationsConfiguration;
import com.sonatype.insight.brain.configuration.AutomaticApplicationsConfigurationResource;
import com.sonatype.insight.brain.dataaccess.configuration.AutomaticApplicationsConfigurationDAO;
import com.sonatype.insight.brain.model.Organization;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2AutomaticApplicationsConfigurationResourceTest
{
  private IqTestContext ctx;

  private AutomaticApplicationsConfigurationDAO automaticApplicationsConfigurationDAO;

  @BeforeEach
  void setUp() {
    automaticApplicationsConfigurationDAO = ctx.lookup(AutomaticApplicationsConfigurationDAO.class);
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(AutomaticApplicationsConfigurationResource.RESOURCE_PATH);
  }

  @Test
  void testGet() throws Exception {
    HttpResponse response = restRequest().get();
    ctx.assertResponseStatus(HttpStatus.SC_OK, response);

    AutomaticApplicationsConfiguration configuration = response.getBody(AutomaticApplicationsConfiguration.class);
    assertThat(configuration.isEnabled()).isFalse();
    assertThat(configuration.getParentOrganizationId()).isEqualTo("");
  }

  @Test
  void testUpdate() throws Exception {
    Organization organization = ctx.tempEntity().newOrganization();

    HttpResponse response = restRequest().body(new AutomaticApplicationsConfiguration(true, organization.getId()))
        .put();
    ctx.assertResponseStatus(HttpStatus.SC_OK, response);

    AutomaticApplicationsConfiguration configuration = response.getBody(AutomaticApplicationsConfiguration.class);
    assertThat(configuration.isEnabled()).isTrue();
    assertThat(configuration.getParentOrganizationId()).isEqualTo(organization.getId());

    assertThat(automaticApplicationsConfigurationDAO.isEnabled()).isTrue();
    assertThat(automaticApplicationsConfigurationDAO.getOrganizationId()).isEqualTo(organization.getId());
  }
}
