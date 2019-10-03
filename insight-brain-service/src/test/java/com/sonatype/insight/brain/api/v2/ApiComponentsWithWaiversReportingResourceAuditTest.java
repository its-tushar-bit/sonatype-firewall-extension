/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Test;

public class ApiComponentsWithWaiversReportingResourceAuditTest
    extends AbstractAuditTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest()
        .path(PublicApiPaths.REPORTS_RESOURCE_PATH_V2 + ApiComponentsWithWaiversReportingResource.PATH);
  }

  @Test
  public void testGetComponentsWithWaivers() throws Exception {
    restRequest().get();
    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENTS_WITH_WAIVERS, null);
    assertCustomData(auditDTO, "numberOfApplicationComponents", 0);
    assertCustomData(auditDTO, "numberOfRepositoryComponents", 0);
  }
}
