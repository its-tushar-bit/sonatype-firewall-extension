/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.ingestion;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.PolicyEvaluationHelper;
import com.sonatype.insight.brain.api.v2.dto.ApiThirdPartyScanTicketDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractAuditTest;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.mock.hds.HdsMockServer.RestHandler;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class SbomImportResourceAuditTest
    extends AbstractAuditTest
{
  private Application app;

  private PolicyEvaluationHelper policyEvaluationHelper;

  @Before
  public void before() {
    app = tempEntity.newApplicationWithParent();
    policyEvaluationHelper = lookup(PolicyEvaluationHelper.class);
  }

  @Test
  public void testImportDetectedSbom() throws Exception {
    setFeatures(LicensedFeature.SBOM_MANAGER);
    mockReport(RestHandler.SCAN_ID, "/AbstractAuditTest/report");
    URL resource = SbomImportResourceTest.class.getResource("/SbomImportResourceTest/valid-spdx-bom.json");
    File sbom = new File(Objects.requireNonNull(resource).getFile());
    HttpResponse responseDetect = restRequest()
        .parameter(app.getId())
        .part("file", sbom.getName(), Files.readAllBytes(sbom.toPath()))
        .path(SbomImportResource.RESOURCE_PATH, SbomImportResource.DETECT_PATH)
        .post();
    SbomDetectionResultDTO actual = responseDetect.getBody(SbomDetectionResultDTO.class);

    HttpResponse responseCommit = restRequest()
        .path(SbomImportResource.RESOURCE_PATH, SbomImportResource.COMMIT_PATH)
        .parameter(app.getId(), actual.getSbomSummary().applicationVersion)
        .post();
    assertResponseStatus(202, responseCommit);
    List<AuditDTO> auditDTOs = assertAuditLogs(AuditEvent.CREATE_SBOM_VERSION, 1, null);
    assertCustomData(auditDTOs.get(0), "applicationId", app.getId());
    assertCustomData(auditDTOs.get(0), "sbomVersion", "a140fd3c3ded4bb0a640dc31e2904dc9");
    assertCustomData(auditDTOs.get(0), "status", "UPLOADED");
    assertCustomData(auditDTOs.get(0), "operation", "CREATE");
    assertCustomData(auditDTOs.get(0), "stageId", "compliance");

    ApiThirdPartyScanTicketDTO responseCommitBody = responseCommit.getBody(ApiThirdPartyScanTicketDTO.class);
    policyEvaluationHelper.awaitEvaluationFinished(app.getId(), getStatusId(responseCommitBody.statusUrl));
  }

  private String getStatusId(String statusUrl) {
    return statusUrl.substring(statusUrl.lastIndexOf("/") + 1);
  }
}
