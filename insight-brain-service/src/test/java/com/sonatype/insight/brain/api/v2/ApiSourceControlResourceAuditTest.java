/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.v2.dto.ApiSourceControlDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlProvider;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.api.PublicApiPaths.SOURCE_CONTROL_PATH_V2;

public class ApiSourceControlResourceAuditTest
    extends AbstractAuditTest
{
  private Application app;

  @Before
  public void setup() {
    app = tempEntity.newApplicationWithParent();
  }

  @Test
  public void testAuditForCRUD() throws Exception {
    //CREATE
    String repositoryUrl = ApiSourceControlResourceTest.VALID_URL;
    SourceControl sourceControl = new SourceControl(app.getId(), repositoryUrl, "token", SourceControlProvider.GITHUB);
    HttpResponse response = restRequest().path(SOURCE_CONTROL_PATH_V2).path(app.getId()).body(sourceControl).post();
    assertResponseStatus(200, response);
    ApiSourceControlDTO result = response.getBody(ApiSourceControlDTO.class);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_SOURCE_CONTROL, null);
    assertCustomData(auditDTO, "repositoryUrl", repositoryUrl);
    assertCustomData(auditDTO, "sourceControlId", result.id);
    assertCustomData(auditDTO, "provider", result.provider.toString());
    assertApplicationData(auditDTO, app);

    //UPDATE
    String updatedUrl = sourceControl.getRepositoryUrl() + ".1";
    result.repositoryUrl = updatedUrl;
    response = restRequest().path(SOURCE_CONTROL_PATH_V2).path(app.getId()).body(result).put();
    assertResponseStatus(200, response);

    auditDTO = assertAuditLog(AuditEvent.UPDATE_SOURCE_CONTROL, null);
    assertCustomData(auditDTO, "repositoryUrl", updatedUrl);
    assertCustomData(auditDTO, "sourceControlId", result.id);
    assertCustomData(auditDTO, "provider", result.provider.toString());
    assertApplicationData(auditDTO, app);

    //DELETE
    response = restRequest().path(SOURCE_CONTROL_PATH_V2).path(app.getId()).path(result.id).delete();
    assertResponseStatus(204, response);

    auditDTO = assertAuditLog(AuditEvent.DELETE_SOURCE_CONTROL, null);
    assertCustomData(auditDTO, "sourceControlId", result.id);
    assertApplicationData(auditDTO, app);
  }

  @Test
  public void testAuditForAddOrUpdate() throws Exception {
    //CREATE
    String repositoryUrl = ApiSourceControlResourceTest.VALID_URL;
    SourceControl sourceControl = new SourceControl(app.getId(), repositoryUrl, "token", SourceControlProvider.GITHUB);
    HttpResponse response = restRequest().path(SOURCE_CONTROL_PATH_V2).path(app.getId()).body(sourceControl).post();
    assertResponseStatus(200, response);
    ApiSourceControlDTO result = response.getBody(ApiSourceControlDTO.class);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_SOURCE_CONTROL, null);
    assertCustomData(auditDTO, "repositoryUrl", repositoryUrl);
    assertCustomData(auditDTO, "sourceControlId", result.id);
    assertCustomData(auditDTO, "provider", result.provider.toString());
    assertApplicationData(auditDTO, app);

    //UPDATE
    String updatedUrl = sourceControl.getRepositoryUrl() + ".1";
    result.repositoryUrl = updatedUrl;
    response = restRequest().path(SOURCE_CONTROL_PATH_V2).path(app.getId()).body(result).put();
    assertResponseStatus(200, response);

    auditDTO = assertAuditLog(AuditEvent.UPDATE_SOURCE_CONTROL, null);
    assertCustomData(auditDTO, "repositoryUrl", updatedUrl);
    assertCustomData(auditDTO, "sourceControlId", result.id);
    assertCustomData(auditDTO, "provider", result.provider.toString());
    assertApplicationData(auditDTO, app);

    //DELETE
    response = restRequest().path(SOURCE_CONTROL_PATH_V2).path(app.getId()).path(result.id).delete();
    assertResponseStatus(204, response);

    auditDTO = assertAuditLog(AuditEvent.DELETE_SOURCE_CONTROL, null);
    assertCustomData(auditDTO, "sourceControlId", result.id);
    assertApplicationData(auditDTO, app);
  }
}
