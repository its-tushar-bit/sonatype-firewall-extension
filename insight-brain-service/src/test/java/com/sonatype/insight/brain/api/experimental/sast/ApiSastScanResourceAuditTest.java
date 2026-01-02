/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.sast;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Test;

import static com.sonatype.insight.brain.api.experimental.sast.SastTestUtil.buildTestSastScanRequestDTO;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class ApiSastScanResourceAuditTest
    extends AbstractAuditTest
{
  @Test
  public void testCreateSastScan_Unauthorized() throws Exception {
    final Application app = tempEntity.newApplicationWithParent();
    final HttpResponse response = restRequest()
        .path(PublicApiPaths.EXPERIMENTAL_SAST_SCAN_DATA_PATH)
        .with(unauthorizedUser())
        .parameter(app.getPublicId())
        .body(buildTestSastScanRequestDTO())
        .post();
    assertResponseStatus(403, response);
    assertAuditLog(AuditEvent.CREATE_SAST_SCAN, "unauthorized");
  }

  @Test
  public void testCreateSastScan_Authorized() throws Exception {
    final Application app = tempEntity.newApplicationWithParent();
    final HttpResponse response = restRequest()
        .path(PublicApiPaths.EXPERIMENTAL_SAST_SCAN_DATA_PATH)
        .parameter(app.getPublicId())
        .body(buildTestSastScanRequestDTO())
        .post();
    assertResponseStatus(200, response);
    final SastScanResponseDTO result = response.getBody(SastScanResponseDTO.class);

    final AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_SAST_SCAN, null);
    assertCustomData(auditDTO, "sastScanId", result.sastScanId);
  }
}
