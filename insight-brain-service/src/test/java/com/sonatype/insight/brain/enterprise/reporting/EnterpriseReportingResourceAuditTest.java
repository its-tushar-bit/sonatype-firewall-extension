/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.enterprise.reporting;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class EnterpriseReportingResourceAuditTest
    extends AbstractAuditTest
{
  @Before
  @After
  public void clearLookerConfigCache() {
    getCLMServer().getInstance(EnterpriseReportingService.class).clearLookerConfigCacheForTests();
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(EnterpriseReportingResource.RESOURCE_PATH);
  }

  @Test
  public void testCreateSSOEmbedUrl() throws Exception {
    String lookerSSOUrl = "looker.someurl.com";
    String baseUrl = "https://looker.example.com";
    hdsMockServer.respondWith("{\"url\":\"" + lookerSSOUrl + "\"}").atUri("rest/enterpriseReporting/ssoEmbedUrl");
    hdsMockServer.respondWith("{\"baseUrl\":\"" + baseUrl + "\"}").atUri("rest/enterpriseReporting/config");

    DashboardRequestDTO request = new DashboardRequestDTO("test");
    restRequest().path(EnterpriseReportingResource.SSO_EMBED_URL_PATH).body(request).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_INTEGRATED_ENTERPRISE_REPORTING_DASHBOARD, null);
    assertCustomData(auditDTO, "dashboard", "test");
  }
}
