/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.io.IOException;

import jakarta.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.cyclonedx.Version;
import org.junit.Before;
import org.junit.Test;

public class ApiCycloneDxResourceV2AuditTest
    extends AbstractAuditTest
{
  private String scanId;

  Application app;

  @Before
  public void setUp() {
    scanId = TemporaryEntity.uuid();
    app = tempEntity.newApplicationWithParent();
    tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, scanId);
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.CYCLONE_DX_RESOURCE_PATH);
  }

  private void createReportFile(String appId, String scanId) throws IOException {
    createReportFile(appId, scanId, "/" + getClass().getSimpleName() + "/report");
  }

  @Test
  public void testGetLatest() throws Exception {
    getHttpRequestLatest(ApiCycloneDxResourceV2.GET_BY_STAGE_PATH).get();
    AuditDTO auditDTO = assertAuditLog(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT, null);
    assertApplicationData(auditDTO, app);
    assertCustomData(auditDTO, "reportId", scanId);
  }

  @Test
  public void testGetLatest_With_Version_1_1() throws Exception {
    getHttpRequestLatest("1.1/" + ApiCycloneDxResourceV2.GET_BY_STAGE_PATH, Version.VERSION_11,
        MediaType.APPLICATION_XML).get();
    AuditDTO auditDTO = assertAuditLog(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT, null);
    assertApplicationData(auditDTO, app);
    assertCustomData(auditDTO, "reportId", scanId);
  }

  @Test
  public void testGetLatest_With_Version_1_5() throws Exception {
    getHttpRequestLatest("1.5/" + ApiCycloneDxResourceV2.GET_BY_STAGE_PATH, Version.VERSION_15,
        MediaType.APPLICATION_XML).get();
    AuditDTO auditDTO = assertAuditLog(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT, null);
    assertApplicationData(auditDTO, app);
    assertCustomData(auditDTO, "reportId", scanId);
  }

  @Test
  public void testGetLatest_Unauthorized() throws Exception {
    getHttpRequestLatest(ApiCycloneDxResourceV2.GET_BY_STAGE_PATH).with(unauthorizedUser()).get();
    AuditDTO auditDTO = assertAuditLog(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT, "unauthorized");
    assertApplicationData(auditDTO, app);
  }

  @Test
  public void testGetByReportId() throws Exception {
    getHttpRequestByReportId(ApiCycloneDxResourceV2.GET_BY_REPORT_PATH).get();
    AuditDTO auditDTO = assertAuditLog(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT, null);
    assertApplicationData(auditDTO, app);
    assertCustomData(auditDTO, "reportId", scanId);
  }

  @Test
  public void testGetByReportId_With_Version_1_1() throws Exception {
    getHttpRequestByReportId("1.1/" + ApiCycloneDxResourceV2.GET_BY_REPORT_PATH, Version.VERSION_11,
        MediaType.APPLICATION_XML).get();
    AuditDTO auditDTO = assertAuditLog(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT, null);
    assertApplicationData(auditDTO, app);
    assertCustomData(auditDTO, "reportId", scanId);
  }

  @Test
  public void testGetByReportId_With_Version_1_5() throws Exception {
    getHttpRequestByReportId("1.5/" + ApiCycloneDxResourceV2.GET_BY_REPORT_PATH, Version.VERSION_15,
        MediaType.APPLICATION_XML).get();
    AuditDTO auditDTO = assertAuditLog(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT, null);
    assertApplicationData(auditDTO, app);
    assertCustomData(auditDTO, "reportId", scanId);
  }

  @Test
  public void testGetByReportId_Unauthorized() throws Exception {
    getHttpRequestByReportId(ApiCycloneDxResourceV2.GET_BY_REPORT_PATH).with(unauthorizedUser()).get();
    AuditDTO auditDTO = assertAuditLog(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT, "unauthorized");
    assertApplicationData(auditDTO, app);
  }

  private HttpRequest getHttpRequestLatest(String path) throws Exception {
    return getHttpRequestLatest(path, null, null);
  }

  private HttpRequest getHttpRequestLatest(String path, Version version, String mediaType) throws Exception {
    HttpRequest request = getHttpRequest(path, mediaType);
    if (version != null) {
      request.parameter(app.getId(), Stage.ID_BUILD, version.getVersionString());
    }
    else {
      request.parameter(app.getId(), Stage.ID_BUILD);
    }
    return request;
  }

  private HttpRequest getHttpRequestByReportId(String path) throws Exception {
    return getHttpRequestByReportId(path, null, null);
  }

  private HttpRequest getHttpRequestByReportId(String path, Version version, String mediaType) throws Exception {
    HttpRequest request = getHttpRequest(path, mediaType);
    if (version != null) {
      request.parameter(app.getId(), scanId, version.getVersionString());
    }
    else {
      request.parameter(app.getId(), scanId);
    }
    return request;
  }

  private HttpRequest getHttpRequest(final String path, final String mediaType) throws IOException {
    createReportFile(app.getId(), scanId);

    HttpRequest request = restRequest().path(path);
    if (mediaType != null) {
      request.header("Accept", mediaType);
    }
    return request;
  }
}
