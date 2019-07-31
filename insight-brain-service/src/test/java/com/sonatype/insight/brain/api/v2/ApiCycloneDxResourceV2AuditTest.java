/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.io.File;
import java.io.IOException;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractAuditTest;
import com.sonatype.insight.brain.service.InsightWork;

import org.codehaus.plexus.util.FileUtils;
import org.junit.Test;

public class ApiCycloneDxResourceV2AuditTest
    extends AbstractAuditTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.CYCLONE_DX_RESOURCE_PATH);
  }

  private void createReportFile(String appId, String scanId) throws IOException {
    File reportFile = new InsightWork(getCLMServer().getConfiguration()).getReportFile(appId, scanId);
    FileUtils.copyURLToFile(getClass().getResource("/ApiCycloneDxResourceV2AuditTest/report.zip"), reportFile);
  }

  @Test
  public void testGetLatest() throws Exception {
    String scanId = tempEntity.uuid();
    Application app = tempEntity.newApplicationWithParent();
    tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, scanId);
    createReportFile(app.getId(), scanId);

    restRequest().path(ApiCycloneDxResourceV2.GET_BY_STAGE_PATH).parameter(app.getId(), Stage.ID_BUILD).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT, null);
    assertApplicationData(auditDTO, app);
    assertCustomData(auditDTO, "reportId", scanId);
  }

  @Test
  public void testGetLatest_Unauthorized() throws Exception {
    String scanId = tempEntity.uuid();
    Application app = tempEntity.newApplicationWithParent();
    tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, scanId);
    createReportFile(app.getId(), scanId);

    restRequest().path(ApiCycloneDxResourceV2.GET_BY_STAGE_PATH).parameter(app.getId(), Stage.ID_BUILD)
        .with(unauthorizedUser()).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT, "unauthorized");
    assertApplicationData(auditDTO, app);
  }

  @Test
  public void testGetByReportId() throws Exception {
    String scanId = tempEntity.uuid();
    Application app = tempEntity.newApplicationWithParent();
    tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, scanId);
    createReportFile(app.getId(), scanId);

    restRequest().path(ApiCycloneDxResourceV2.GET_BY_REPORT_PATH).parameter(app.getId(), scanId).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT, null);
    assertApplicationData(auditDTO, app);
    assertCustomData(auditDTO, "reportId", scanId);
  }

  @Test
  public void testGetByReportId_Unauthorized() throws Exception {
    String scanId = tempEntity.uuid();
    Application app = tempEntity.newApplicationWithParent();
    tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, scanId);
    createReportFile(app.getId(), scanId);

    restRequest().path(ApiCycloneDxResourceV2.GET_BY_REPORT_PATH).parameter(app.getId(), scanId)
        .with(unauthorizedUser()).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT, "unauthorized");
    assertApplicationData(auditDTO, app);
  }
}
