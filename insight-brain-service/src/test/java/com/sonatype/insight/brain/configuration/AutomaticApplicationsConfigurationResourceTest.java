/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.apache.http.HttpStatus;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class AutomaticApplicationsConfigurationResourceTest
    extends AbstractResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(AutomaticApplicationsConfigurationResource.RESOURCE_PATH);
  }

  @Test
  public void testGet() throws Exception {
    HttpResponse response = restRequest().get();
    assertResponseStatus(HttpStatus.SC_OK, response);

    AutomaticApplicationsConfiguration configuration = response.getBody(AutomaticApplicationsConfiguration.class);
    assertThat(configuration.isEnabled(), is(false));
    assertThat(configuration.getParentOrganizationId(), is(""));
  }

  @Test
  public void testUpdate() throws Exception {
    Organization organization = tempEntity.newOrganization();

    HttpResponse response = restRequest().body(new AutomaticApplicationsConfiguration(true, organization.getId()))
        .put();
    assertResponseStatus(HttpStatus.SC_OK, response);

    AutomaticApplicationsConfiguration configuration = response.getBody(AutomaticApplicationsConfiguration.class);
    assertThat(configuration.isEnabled(), is(true));
    assertThat(configuration.getParentOrganizationId(), is(organization.getId()));

    SystemConfigurationPropertyDAO dao = new SystemConfigurationPropertyDAO();
    SystemConfigurationProperty enabled = dao
        .getByNameNotNull(SystemConfigurationProperty.AUTOMATIC_APPLICATION_CREATION_ENABLED);
    SystemConfigurationProperty organizationId = dao
        .getByNameNotNull(SystemConfigurationProperty.AUTOMATIC_APPLICATION_CREATION_ORGANIZATION_ID);

    assertThat(enabled.getValue(), is("true"));
    assertThat(organizationId.getValue(), is(organization.getId()));
  }
}
