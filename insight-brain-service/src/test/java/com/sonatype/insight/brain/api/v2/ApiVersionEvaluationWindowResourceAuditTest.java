/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiVersionEvaluationWindowDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Before;
import org.junit.Test;

public class ApiVersionEvaluationWindowResourceAuditTest
    extends AbstractAuditTest
{
  private Organization org;

  @Before
  public void setUp() {
    org = tempEntity.newOrganization();
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest()
        .path(PublicApiPaths.VERSION_EVALUATION_WINDOW_RESOURCE_PATH, ApiVersionEvaluationWindowResource.OWNER_PATH)
        .parameter(OwnerType.ORGANIZATION, org.getId());
  }

  @Test
  public void testSetConfiguration() throws Exception {
    ApiVersionEvaluationWindowDTO dto = new ApiVersionEvaluationWindowDTO("context1", 10, 30);

    restRequest().body(dto).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_VERSION_EVALUATION_WINDOW, null);
    assertOwnerData(auditDTO, org);
    assertCustomData(auditDTO, "contextId", "context1");
    assertCustomData(auditDTO, "maxVersions", 10);
    assertCustomData(auditDTO, "maxAgeInDays", 30);
  }

  @Test
  public void testSetConfiguration_Unauthorized() throws Exception {
    ApiVersionEvaluationWindowDTO dto = new ApiVersionEvaluationWindowDTO("context1", 10, 30);

    restRequest().with(unauthorizedUser()).body(dto).put();

    assertAuditLog(AuditEvent.CONFIGURE_VERSION_EVALUATION_WINDOW, "unauthorized");
  }

  @Test
  public void testDeleteConfiguration() throws Exception {
    tempEntity.newVersionEvaluationWindow(org.getId(), "context1", 10, 30);

    restRequest().query("contextId", "context1").delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_VERSION_EVALUATION_WINDOW, null);
    assertOwnerData(auditDTO, org);
    assertCustomData(auditDTO, "contextId", "context1");
  }

  @Test
  public void testDeleteConfiguration_Unauthorized() throws Exception {
    restRequest().with(unauthorizedUser()).query("contextId", "context1").delete();

    assertAuditLog(AuditEvent.DELETE_VERSION_EVALUATION_WINDOW, "unauthorized");
  }
}
