/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.looker;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.looker.LookerResource.SSO_EMBED_URL_PATH;

public class LookerResourceAuditTest
    extends AbstractAuditTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(LookerResource.RESOURCE_PATH);
  }

  @Before
  public void before() {
    ApiConfigFeaturesService.SystemConfigurationPropertyFeature
        .LOOKER_INTEGRATED_ENTERPRISE_REPORTING.setEnabled(true);
  }

  @After
  public void after() {
    ApiConfigFeaturesService.SystemConfigurationPropertyFeature
        .LOOKER_INTEGRATED_ENTERPRISE_REPORTING.setEnabled(false);
  }

  @Test
  public void testCreateSSOEmbedUrl() throws Exception {
    String lookerSSOUrl = "looker.someurl.com";
    String baseUrl = "https://looker.example.com";
    hdsMockServer.respondWith("{\"url\":\"" + lookerSSOUrl + "\"}").atUri("rest/looker/ssoEmbedUrl");
    hdsMockServer.respondWith("{\"baseUrl\":\"" + baseUrl + "\"}").atUri("rest/looker/config");

    LookerDashboardDTO request = new LookerDashboardDTO("test");
    restRequest().path(SSO_EMBED_URL_PATH).body(request).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_LOOKER_DASHBOARD, null);
    assertCustomData(auditDTO, "dashboard", "test");
  }
}
