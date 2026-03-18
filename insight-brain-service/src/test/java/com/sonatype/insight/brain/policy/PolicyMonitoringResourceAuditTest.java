/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Before;
import org.junit.Test;

@Category(SlowTest.class)
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

  private HttpRequest restRequest(Owner owner) {
    return restRequest(owner.getType(), owner.getPublicId());
  }

  private HttpRequest restRequest(OwnerType ownerType, String ownerId) {
    return restRequest().path(PolicyMonitoringResource.RESOURCE_PATH).parameter(ownerType, ownerId);
  }

  @Test
  public void testSet_Application() throws Exception {
    PolicyMonitoring policyMonitoring = new PolicyMonitoring(app.getId(), Stage.ID_RELEASE);
    restRequest(app).body(policyMonitoring).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_CONTINUOUS_MONITORING, null);
    assertApplicationData(auditDTO, app);
    assertCustomData(auditDTO, "stageId", Stage.ID_RELEASE);
  }

  @Test
  public void testSet_Organization() throws Exception {
    PolicyMonitoring policyMonitoring = new PolicyMonitoring(org.getId(), Stage.ID_RELEASE);
    restRequest(org).body(policyMonitoring).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_CONTINUOUS_MONITORING, null);
    assertOrganizationData(auditDTO, org);
    assertCustomData(auditDTO, "stageId", Stage.ID_RELEASE);
  }

  @Test
  public void testSet_Unauthorized() throws Exception {
    PolicyMonitoring policyMonitoring = new PolicyMonitoring(org.getId(), Stage.ID_RELEASE);
    restRequest(org).with(unauthorizedUser()).body(policyMonitoring).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_CONTINUOUS_MONITORING, "unauthorized");
    assertOrganizationData(auditDTO, org);
  }

  @Test
  public void testDelete_Application() throws Exception {
    PolicyMonitoring policyMonitoring = tempEntity.newPolicyMonitoring(app.getId(), Stage.ID_RELEASE);
    restRequest(app).query("stageTypeId", policyMonitoring.getStageTypeId()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_CONTINUOUS_MONITORING, null);
    assertApplicationData(auditDTO, app);
    assertCustomData(auditDTO, "stageId", "inherited");
  }

  @Test
  public void testDelete_Organization() throws Exception {
    PolicyMonitoring policyMonitoring = tempEntity.newPolicyMonitoring(org.getId(), Stage.ID_RELEASE);
    restRequest(org).query("stageTypeId", policyMonitoring.getStageTypeId()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_CONTINUOUS_MONITORING, null);
    assertOrganizationData(auditDTO, org);
    assertCustomData(auditDTO, "stageId", "inherited");
  }

  @Test
  public void testDelete_RootOrganization() throws Exception {
    PolicyMonitoring policyMonitoring =
        tempEntity.newPolicyMonitoring(Organization.ROOT_ORGANIZATION_ID, Stage.ID_RELEASE);
    restRequest(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID).query("stageTypeId",
        policyMonitoring.getStageTypeId()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_CONTINUOUS_MONITORING, null);
    assertOrganizationData(auditDTO, org.getParentOrganizationId(), "Root Organization");
    assertCustomData(auditDTO, "stageId", "none");
  }

  @Test
  public void testDelete_Unauthorized() throws Exception {
    restRequest(org).with(unauthorizedUser()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_CONTINUOUS_MONITORING, "unauthorized");
    assertOrganizationData(auditDTO, org);
  }
}
