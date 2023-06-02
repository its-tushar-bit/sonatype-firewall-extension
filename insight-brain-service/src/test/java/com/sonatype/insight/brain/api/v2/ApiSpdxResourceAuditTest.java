/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.io.IOException;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ApiSpdxResourceAuditTest
    extends AbstractAuditTest
{
  private String scanId;

  private Application app;

  @Before
  public void setUp() {
    scanId = tempEntity.uuid();
    app = tempEntity.newApplicationWithParent();
    tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, scanId);

    SystemConfigurationPropertyFeature.SPDX_EXPORT.setEnabled(true);
  }

  @After
  public void teardown() {
    SystemConfigurationPropertyFeature.SPDX_EXPORT.setEnabled(false);
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.SPDX_RESOURCE_PATH);
  }

  private void createReportFile(String appId, String scanId) throws IOException {
    createReportFile(appId, scanId, "/ApiSpdxServiceTest/report");
  }

  @Test
  public void testGetLatestForStage() throws Exception {
    getHttpRequestLatestForStage().get();
    AuditDTO auditDTO = assertAuditLog(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT, null);
    assertApplicationData(auditDTO, app);
    assertCustomData(auditDTO, "scanId", scanId);
  }

  @Test
  public void testGetLatestForStage_Unauthorized() throws Exception {
    getHttpRequestLatestForStage().with(unauthorizedUser()).get();
    AuditDTO auditDTO = assertAuditLog(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT, "unauthorized");
    assertApplicationData(auditDTO, app);
  }

  @Test
  public void testGetByScanId() throws Exception {
    getHttpRequestByScanId().get();
    AuditDTO auditDTO = assertAuditLog(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT, null);
    assertApplicationData(auditDTO, app);
    assertCustomData(auditDTO, "scanId", scanId);
  }

  @Test
  public void testGetByScanId_Unauthorized() throws Exception {
    getHttpRequestByScanId().with(unauthorizedUser()).get();
    AuditDTO auditDTO = assertAuditLog(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT, "unauthorized");
    assertApplicationData(auditDTO, app);
  }

  private HttpRequest getHttpRequestLatestForStage() throws Exception {
    HttpRequest request = getHttpRequest(ApiSpdxResource.GET_BY_STAGE_PATH);
    request.parameter(app.getId(), Stage.ID_BUILD);
    return request;
  }

  private HttpRequest getHttpRequestByScanId() throws Exception {
    HttpRequest request = getHttpRequest(ApiSpdxResource.GET_BY_REPORT_PATH);
    request.parameter(app.getId(), scanId);
    return request;
  }

  private HttpRequest getHttpRequest(String path) throws IOException {
    createReportFile(app.getId(), scanId);
    return restRequest().path(path);
  }
}
