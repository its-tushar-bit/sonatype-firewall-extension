/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;

import com.sonatype.clm.dto.model.application.ApplicationSummary;
import com.sonatype.clm.dto.model.application.ApplicationSummaryList;
import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import com.ning.http.client.Response;
import com.yammer.dropwizard.testing.JsonHelpers;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class ApplicationSummaryResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testGetApplication() throws Exception {
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());

    Response response = AuthedRestAccess.get(getServicePath());
    assertResponseStatus(200, response);

    ApplicationSummaryList applicationListDTO = JsonHelpers.fromJson(response.getResponseBody(),
        ApplicationSummaryList.class);
    assertThat(applicationListDTO, notNullValue());
    assertThat(applicationListDTO.getApplicationSummaries(), hasSize(1));
    ApplicationSummary applicationDTO = applicationListDTO.getApplicationSummaries().get(0);
    assertThat(applicationDTO.getId(), is(application.getId()));
    assertThat(applicationDTO.getPublicId(), is(application.getPublicId()));
    assertThat(applicationDTO.getName(), is(application.getName()));
  }

  private String getServicePath() {
    return getRestBaseUrl() + "/" + ApplicationSummaryResource.SERVICE_PATH;
  }
}
