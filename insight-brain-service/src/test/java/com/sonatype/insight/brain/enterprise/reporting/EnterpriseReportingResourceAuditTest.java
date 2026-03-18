/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.enterprise.reporting;

import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import com.sonatype.clm.dto.model.looker.EmbedCookielessSessionAcquire;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@Category(SlowTest.class)
public class EnterpriseReportingResourceAuditTest
    extends AbstractAuditTest
{
  @Before
  @After
  public void clearLookerConfigCache() {
    getCLMServer().getInstance(EnterpriseReportingService.class)
        .clearEnterpriseReportingConfigDTOBaseUrlSupplierForTests();
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(EnterpriseReportingResource.RESOURCE_PATH);
  }

  @Test
  public void testAcquireEmbedSession() throws Exception {
    EmbedCookielessSessionAcquire expectedResponse =
        new EmbedCookielessSessionAcquire("authTokenResponse", 300, "navTokenResponse", 400, "apiTokenResponse", 500,
            "sessionTokenResponse", 600);
    hdsMockServer.respondWith(expectedResponse).atUri("rest/enterpriseReporting/acquireEmbedSession");
    String encodedEmbedDomain = "http%3A%2F%2Flocalhost%3A8070";

    restRequest().path(EnterpriseReportingResource.ACQUIRE_EMBED_SESSION)
        .query("dashboardId", "dashboardIdParam")
        .query("embedDomain", encodedEmbedDomain)
        .get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_INTEGRATED_ENTERPRISE_REPORTING_DASHBOARD, null);
    assertCustomData(auditDTO, "dashboard", "dashboardIdParam");
  }
}
