/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Test;

public class ApiStaleWaiversReportingResourceAuditTest
    extends AbstractAuditTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest()
        .path(PublicApiPaths.REPORTS_RESOURCE_PATH_V2 + ApiStaleWaiversReportingResource.PATH);
  }

  @Test
  public void testGetStaleWaivers_NoValues() throws Exception {
    restRequest().get();
    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_STALE_WAIVERS, null);
    assertCustomData(auditDTO, "numberOfStaleWaivers", 0);
  }

  @Test
  public void testGetStaleWaivers() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    Policy policy = tempEntity.newPolicy();
    Policy expiredWaiverPolicy = tempEntity.newPolicy();

    tempEntity.newWaiver("hash1", policy.getId(), app.getId(), "stale waiver comment1");

    Repository repo = tempEntity.newRepository();

    tempEntity.newWaiver("hash2", policy.getId(), repo.getId(), null, "stale waiver comment2");

    Date expiredTime = Date.from(Instant.now().minus(Duration.ofHours(10)));
    tempEntity.newWaiver("hash3", expiredWaiverPolicy.getId(), app.getId(), null, "expired waiver", null,
        expiredTime);

    restRequest().get();
    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_STALE_WAIVERS, null);
    assertCustomData(auditDTO, "numberOfStaleWaivers", 2);
  }
}
