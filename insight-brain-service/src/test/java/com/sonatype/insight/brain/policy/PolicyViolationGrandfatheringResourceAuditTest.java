/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.Date;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Before;
import org.junit.Test;

public class PolicyViolationGrandfatheringResourceAuditTest
    extends AbstractAuditTest
{
  private Application application;

  private PolicyEvaluation policyEvaluation;

  @Before
  public void before() {
    application = tempEntity.newApplicationWithParent();
    policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_BUILD, "scanId");
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PolicyViolationGrandfatheringResource.RESOURCE_PATH);
  }

  @Test
  public void testGrandfather() throws Exception {
    Policy policyGrandfatheringAllowed = tempEntity.newPolicy("policyGrandfatheringAllowed");
    policyGrandfatheringAllowed.setPolicyViolationGrandfatheringAllowed(true);
    new PolicyDAO().update(policyGrandfatheringAllowed);

    // Policy violation that is already grandfathered - is not counted
    PolicyViolation policyViolationAlreadyGrandfathered = tempEntity
        .newPolicyViolation(policyEvaluation, policyGrandfatheringAllowed);
    policyViolationAlreadyGrandfathered.setGrandfatherTime(new Date());
    new PolicyViolationDAO().update(policyViolationAlreadyGrandfathered);

    // Policy violation for a policy that cannot be found - is counted
    tempEntity.newPolicyViolation(policyEvaluation, new Policy("doesNotExistId", "doesNotExistName"));

    // Policy violation for a policy that does not allow grandfathering - is not counted
    tempEntity.newPolicyViolation(policyEvaluation, tempEntity.newPolicy("policyGrandfatheringNotAllowed"));

    // Policy violation that is not grandfathered - is counted
    tempEntity.newPolicyViolation(policyEvaluation, policyGrandfatheringAllowed);

    restRequest().path(PolicyViolationGrandfatheringResource.GRANDFATHER_PATH).parameter(application.getPublicId())
        .put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.APPLY_GRANDFATHERING, null);
    assertApplicationData(auditDTO, application);
    assertGrandfatheringData(auditDTO, 2);
  }

  @Test
  public void testGrandfather_Unauthorized() throws Exception {
    restRequest().auth(unauthorizedUser.getUsername(), unauthorizedUser.getPassword())
        .path(PolicyViolationGrandfatheringResource.GRANDFATHER_PATH).parameter(application.getPublicId()).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.APPLY_GRANDFATHERING, "unauthorized");
    assertApplicationData(auditDTO, application);
    assertGrandfatheringData(auditDTO, null);
  }

  @Test
  public void testRevokeGrandfathering() throws Exception {
    tempEntity.newGrandfatheredPolicyViolation(policyEvaluation, tempEntity.newPolicy("policy"));

    restRequest().path(PolicyViolationGrandfatheringResource.REVOKE_PATH).parameter(application.getPublicId()).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.REVOKE_GRANDFATHERING, null);
    assertApplicationData(auditDTO, application);
    assertGrandfatheringData(auditDTO, 1);
  }

  private AuditDTO assertAuditLog(AuditEvent auditEvent, String error) {
    AuditDTO auditDTO = awaitLogEntries(auditEvent, 1).get(0);
    assertStandardData(auditDTO, auditEvent, error);
    return auditDTO;
  }

  private void assertGrandfatheringData(AuditDTO auditDTO, Integer changedPolicyViolationCount) {
    assertCustomData(auditDTO, "changedPolicyViolationCount", changedPolicyViolationCount);
  }
}
