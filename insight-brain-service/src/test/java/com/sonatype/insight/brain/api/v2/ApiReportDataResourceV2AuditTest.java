/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluateResource;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Before;
import org.junit.Test;

public class ApiReportDataResourceV2AuditTest
    extends AbstractAuditTest
{
  private Application app;

  private static final String SCAN_ID = "ApiReportResourceAuditTest_ScanId";

  @Before
  public void before() {
    app = tempEntity.newApplicationWithParent("ApiReportResourceAuditTest_AppId");
  }

  @Test
  public void testGetData() throws Exception {
    mockReport(SCAN_ID, "/ReportResourceTest/report");
    restRequest().path(PolicyEvaluateResource.RESOURCE_PATH).parameter(app.getPublicId())
        .query("scanId", SCAN_ID).body(new Stage(Stage.ID_BUILD)).post();

    reportDataRequest(app.getPublicId(), SCAN_ID).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT, null);
    assertApplicationData(auditDTO, app);
    assertCustomData(auditDTO, "reportId", SCAN_ID);
  }

  @Test
  public void testGetData_Unauthorized() throws Exception {
    reportDataRequest(app.getPublicId(), SCAN_ID).with(unauthorizedUser()).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT, "unauthorized");
    assertApplicationData(auditDTO, app);
  }

  private HttpRequest reportDataRequest(String appId, String scanId) {
    return restRequest().path(PublicApiPaths.REPORT_DATA_RESOURCE_PATH_V2).path(ApiReportDataResourceV2.RAW_DATA_PATH)
        .parameter(appId, scanId);
  }
}
