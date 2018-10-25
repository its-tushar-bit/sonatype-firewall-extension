/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Before;
import org.junit.Test;

public class PolicyMonitoringResourceAuditTest
    extends AbstractAuditTest
{
  private Application app;

  private Organization org;

  @Before
  public void before() {
    org = tempEntity.newOrganization();
    app = tempEntity.newApplicationWithParent();
  }

  private HttpRequest restRequest(User user, Owner owner) {
    return (user == null ? restRequest() : restRequest().auth(user.getUsername(), user.getPassword()))
        .path(PolicyMonitoringResource.RESOURCE_PATH).parameter(owner.getType(), owner.getPublicId());
  }

  @Test
  public void testSet_Application() throws Exception {
    PolicyMonitoring policyMonitoring = new PolicyMonitoring(app.getId(), Stage.ID_RELEASE);
    restRequest(null, app).body(policyMonitoring).put();

    AuditDTO auditDTO = assertAuditLog(null);
    assertApplicationData(auditDTO, app);
    assertCustomData(auditDTO, "stageId", Stage.ID_RELEASE);
  }

  @Test
  public void testSet_Organization() throws Exception {
    PolicyMonitoring policyMonitoring = new PolicyMonitoring(org.getId(), Stage.ID_RELEASE);
    restRequest(null, org).body(policyMonitoring).put();

    AuditDTO auditDTO = assertAuditLog(null);
    assertOrganizationData(auditDTO, org);
    assertCustomData(auditDTO, "stageId", Stage.ID_RELEASE);
  }

  @Test
  public void testSet_Unauthorized() throws Exception {
    PolicyMonitoring policyMonitoring = new PolicyMonitoring(org.getId(), Stage.ID_RELEASE);
    restRequest(unauthorizedUser, org).body(policyMonitoring).put();

    AuditDTO auditDTO = assertAuditLog("unauthorized");
    assertOrganizationData(auditDTO, org);
  }

  private AuditDTO assertAuditLog(String error) {
    AuditDTO auditDTO = awaitLogEntries(AuditEvent.CONFIGURE_CONTINUOUS_MONITORING, 1).get(0);
    assertStandardData(auditDTO, AuditEvent.CONFIGURE_CONTINUOUS_MONITORING, error);
    return auditDTO;
  }
}
