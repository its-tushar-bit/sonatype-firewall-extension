/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.configuration.AutomaticApplicationsConfigurationDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AutomaticApplicationsConfigurationResourceTest
    extends AbstractResourceTest
{
  private AutomaticApplicationsConfigurationDAO automaticApplicationsConfigurationDAO;

  @Before
  public void setUp() {
    automaticApplicationsConfigurationDAO = lookup(AutomaticApplicationsConfigurationDAO.class);
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(AutomaticApplicationsConfigurationResource.RESOURCE_PATH);
  }

  @Test
  public void testGet() throws Exception {
    HttpResponse response = restRequest().get();
    assertResponseStatus(HttpStatus.SC_OK, response);

    AutomaticApplicationsConfiguration configuration = response.getBody(AutomaticApplicationsConfiguration.class);
    assertThat(configuration.isEnabled()).isFalse();
    assertThat(configuration.getParentOrganizationId()).isEqualTo("");
  }

  @Test
  public void testUpdate() throws Exception {
    Organization organization = tempEntity.newOrganization();

    HttpResponse response = restRequest().body(new AutomaticApplicationsConfiguration(true, organization.getId()))
        .put();
    assertResponseStatus(HttpStatus.SC_OK, response);

    AutomaticApplicationsConfiguration configuration = response.getBody(AutomaticApplicationsConfiguration.class);
    assertThat(configuration.isEnabled()).isTrue();
    assertThat(configuration.getParentOrganizationId()).isEqualTo(organization.getId());

    assertThat(automaticApplicationsConfigurationDAO.isEnabled()).isTrue();
    assertThat(automaticApplicationsConfigurationDAO.getOrganizationId()).isEqualTo(organization.getId());
  }
}
