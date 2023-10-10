/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.looker;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.model.security.Role.SYSTEM_ADMIN_ROLE_ID;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class LookerResourceTest
    extends AbstractResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(LookerResource.RESOURCE_PATH);
  }

  @Before
  public void before() {
    ApiConfigFeaturesService.SystemConfigurationPropertyFeature
        .INTEGRATED_ENTERPRISE_REPORTING.setEnabled(true);
  }

  @After
  public void cleanup() {
    ApiConfigFeaturesService.SystemConfigurationPropertyFeature
        .INTEGRATED_ENTERPRISE_REPORTING.setEnabled(false);
  }

  @Test
  public void testCreateSSOEmbedUrl_LookerError() throws Exception {
    hdsMockServer.respondWith("error").andStatus(409).atUri("rest/looker/ssoEmbedUrl");
    HttpResponse response =
        restRequest().path(LookerResource.SSO_EMBED_URL_PATH).body(new LookerDashboardDTO("rolling_recap")).post();
    assertResponseStatus(409, response);
  }

  @Test
  public void testCreateSSOEmbedUrl_Success() throws Exception {
    String username = "admin";
    //Set<String> membership = new HashSet<>(Arrays.asList("developers", "qa"));
    final Organization organization = tempEntity.newOrganization("Test Org");
    final Application application = tempEntity.newApplication("Some App", "SOME_APP", organization.getId());
    final Application application2 = tempEntity.newApplication("Some App 2", "SOME_APP2", organization.getId());
    final Application application3 = tempEntity.newApplication("Some App 3", "SOME_APP3", organization.getId());
    final Application application4 = tempEntity.newApplication("Some App 4", "SOME_APP4", organization.getId());
    tempEntity.newMembershipMapping(application.getId(), SYSTEM_ADMIN_ROLE_ID, username);
    tempEntity.newMembershipMapping(application2.getId(), SYSTEM_ADMIN_ROLE_ID, username);
    tempEntity.newMembershipMapping(application3.getId(), SYSTEM_ADMIN_ROLE_ID, username);
    tempEntity.newMembershipMapping(application4.getId(), SYSTEM_ADMIN_ROLE_ID, username);

    String lookerSSOUrl = "looker.someurl.com";
    String baseUrl = "https://looker.example.com";
    hdsMockServer.respondWith("{\"url\":\"" + lookerSSOUrl + "\"}").atUri("rest/looker/ssoEmbedUrl");
    hdsMockServer.respondWith("{\"baseUrl\":\"" + baseUrl + "\"}").atUri("rest/looker/config");
    LookerDashboardDTO lookerDashboardDTO = new LookerDashboardDTO("rolling_recap");

    HttpResponse response = restRequest().path(LookerResource.SSO_EMBED_URL_PATH).body(lookerDashboardDTO).post();
    assertResponseStatus(200, response);
    String expectedResponse = "{\"url\":\"" + lookerSSOUrl + "\",\"baseUrl\":\"" + baseUrl + "\"}";
    assertThat(response.getBodyText()).contains(expectedResponse);
  }
}
