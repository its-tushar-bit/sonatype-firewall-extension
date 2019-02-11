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
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.policy.PolicyViolationGrandfatheringService.PolicyViolationGrandfatheringDTO;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Before;
import org.junit.Test;

public class PolicyViolationGrandfatheringResourceAuditTest
    extends AbstractAuditTest
{
  private Organization organization;

  private Application application;

  private PolicyEvaluation policyEvaluation;

  @Before
  public void before() {
    organization = tempEntity.newOrganization();
    application = tempEntity.newApplicationWithParent();
    policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_BUILD, "scanId");
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PolicyViolationGrandfatheringResource.RESOURCE_PATH);
  }

  @Test
  public void testGrandfather() throws Exception {
    application.setPolicyViolationGrandfatheringEnabled(true);
    new ApplicationDAO().update(application);

    Policy policyGrandfatheringAllowed = tempEntity.newPolicy();
    policyGrandfatheringAllowed.setPolicyViolationGrandfatheringAllowed(true);
    new PolicyDAO().update(policyGrandfatheringAllowed);

    // Policy violation that is already grandfathered - is not counted
    PolicyViolation policyViolationAlreadyGrandfathered = tempEntity
        .newPolicyViolation(policyEvaluation, policyGrandfatheringAllowed);
    policyViolationAlreadyGrandfathered.setGrandfatherTime(new Date());
    new PolicyViolationDAO().update(policyViolationAlreadyGrandfathered);

    // Policy violation for a policy that cannot be found - is counted
    Policy policyDoesNotExist = tempEntity.newPolicy();
    tempEntity.newPolicyViolation(policyEvaluation, policyDoesNotExist);
    new PolicyDAO().delete(policyDoesNotExist);

    // Policy violation for a policy that does not allow grandfathering - is not counted
    tempEntity.newPolicyViolation(policyEvaluation, tempEntity.newPolicy());

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
    restRequest().with(unauthorizedUser()).path(PolicyViolationGrandfatheringResource.GRANDFATHER_PATH)
        .parameter(application.getPublicId()).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.APPLY_GRANDFATHERING, "unauthorized");
    assertApplicationData(auditDTO, application);
    assertGrandfatheringData(auditDTO, null);
  }

  @Test
  public void testRevokeGrandfathering() throws Exception {
    tempEntity.newGrandfatheredPolicyViolation(policyEvaluation, tempEntity.newPolicy());

    restRequest().path(PolicyViolationGrandfatheringResource.REVOKE_PATH).parameter(application.getPublicId()).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.REVOKE_GRANDFATHERING, null);
    assertApplicationData(auditDTO, application);
    assertGrandfatheringData(auditDTO, 1);
  }

  @Test
  public void testSetGrandfathering_NoOverrideByChild_InheritLocalSetting() throws Exception {
    PolicyViolationGrandfatheringDTO policyViolationGrandfatheringDTO = new PolicyViolationGrandfatheringDTO();
    policyViolationGrandfatheringDTO.enabled = null;

    restRequest().path(PolicyViolationGrandfatheringResource.GET_PATH)
        .parameter(OwnerType.APPLICATION, application.getPublicId()).body(policyViolationGrandfatheringDTO).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_GRANDFATHERING, null);
    assertApplicationData(auditDTO, application);
    assertGrandfatheringConfigurationData(auditDTO, null, "inherit");
  }

  @Test
  public void testSetGrandfathering_AllowOverrideByChild_EnableLocalSetting() throws Exception {
    PolicyViolationGrandfatheringDTO policyViolationGrandfatheringDTO = new PolicyViolationGrandfatheringDTO();
    policyViolationGrandfatheringDTO.enabled = true;
    policyViolationGrandfatheringDTO.allowOverride = true;

    restRequest().path(PolicyViolationGrandfatheringResource.GET_PATH)
        .parameter(OwnerType.ORGANIZATION, organization.getId()).body(policyViolationGrandfatheringDTO).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_GRANDFATHERING, null);
    assertOrganizationData(auditDTO, organization);
    assertGrandfatheringConfigurationData(auditDTO, "allow", "enable");
  }

  @Test
  public void testSetGrandfathering_DisallowOverrideByChild_DisableLocalSetting() throws Exception {
    PolicyViolationGrandfatheringDTO policyViolationGrandfatheringDTO = new PolicyViolationGrandfatheringDTO();
    policyViolationGrandfatheringDTO.enabled = false;

    restRequest().path(PolicyViolationGrandfatheringResource.GET_PATH)
        .parameter(OwnerType.ORGANIZATION, organization.getId()).body(policyViolationGrandfatheringDTO).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_GRANDFATHERING, null);
    assertOrganizationData(auditDTO, organization);
    assertGrandfatheringConfigurationData(auditDTO, "disallow", "disable");
  }

  private void assertGrandfatheringData(AuditDTO auditDTO, Integer changedPolicyViolationCount) {
    assertCustomData(auditDTO, "changedPolicyViolationCount", changedPolicyViolationCount);
  }

  private void assertGrandfatheringConfigurationData(AuditDTO auditDTO, String overrideByChild, String localSetting) {
    assertCustomData(auditDTO, "overrideByChild", overrideByChild);
    assertCustomData(auditDTO, "localSetting", localSetting);
  }
}
