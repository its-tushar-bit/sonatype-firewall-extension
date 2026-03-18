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
import com.sonatype.insight.brain.policy.LegacyViolationService.LegacyViolationStatusDTO;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Before;
import org.junit.Test;

public class LegacyViolationResourceAuditTest
    extends AbstractAuditTest
{
  private PolicyDAO policyDAO;

  private ApplicationDAO applicationDAO;

  private PolicyViolationDAO policyViolationDAO;

  private Organization organization;

  private Application application;

  private PolicyEvaluation policyEvaluation;

  @Before
  public void before() {
    policyDAO = lookup(PolicyDAO.class);
    applicationDAO = lookup(ApplicationDAO.class);
    policyViolationDAO = lookup(PolicyViolationDAO.class);

    organization = tempEntity.newOrganization();
    application = tempEntity.newApplicationWithParent();
    policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_BUILD, "scanId");
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(LegacyViolationResource.RESOURCE_PATH);
  }

  @Test
  public void testGrantLegacyViolationStatus() throws Exception {
    application.setLegacyViolationEnabled(true);
    applicationDAO.update(application);

    Policy policyLegacyAllowed = tempEntity.newPolicy();
    policyLegacyAllowed.setLegacyViolationAllowed(true);
    policyDAO.update(policyLegacyAllowed);

    // Policy violation that is already in legacy status - is not counted
    PolicyViolation policyViolationAlreadyLegacy = tempEntity.newPolicyViolation(policyEvaluation, policyLegacyAllowed);
    policyViolationAlreadyLegacy.setLegacyViolationTime(new Date());
    policyViolationDAO.update(policyViolationAlreadyLegacy);

    // Policy violation for a policy that cannot be found - is counted
    Policy policyDoesNotExist = tempEntity.newPolicy();
    tempEntity.newPolicyViolation(policyEvaluation, policyDoesNotExist);
    policyDAO.delete(policyDoesNotExist);

    // Policy violation for a policy that does not allow legacy status - is not counted
    tempEntity.newPolicyViolation(policyEvaluation, tempEntity.newPolicy());

    // Policy violation that is not in legacy status - is counted
    tempEntity.newPolicyViolation(policyEvaluation, policyLegacyAllowed);

    restRequest().path(LegacyViolationResource.GRANT_PATH)
        .parameter(application.getPublicId())
        .put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.APPLY_LEGACY_VIOLATION_STATUS, null);
    assertApplicationData(auditDTO, application);
    assertLegacyViolationStatusData(auditDTO, 2);
  }

  @Test
  public void testGrantLegacyViolationStatus_Unauthorized() throws Exception {
    restRequest().with(unauthorizedUser())
        .path(LegacyViolationResource.GRANT_PATH)
        .parameter(application.getPublicId())
        .put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.APPLY_LEGACY_VIOLATION_STATUS, "unauthorized");
    assertApplicationData(auditDTO, application);
    assertLegacyViolationStatusData(auditDTO, null);
  }

  @Test
  public void testRevokeLegacyViolationStatus() throws Exception {
    tempEntity.newLegacyPolicyViolation(policyEvaluation, tempEntity.newPolicy());

    restRequest().path(LegacyViolationResource.REVOKE_PATH).parameter(application.getPublicId()).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.REVOKE_LEGACY_VIOLATION_STATUS, null);
    assertApplicationData(auditDTO, application);
    assertLegacyViolationStatusData(auditDTO, 1);
  }

  @Test
  public void testSetLegacyViolationStatus_NoOverrideByChild_InheritLocalSetting() throws Exception {
    LegacyViolationStatusDTO legacyViolationStatusDTO = new LegacyViolationStatusDTO();
    legacyViolationStatusDTO.enabled = null;

    restRequest().path(LegacyViolationResource.GET_PATH)
        .parameter(OwnerType.APPLICATION, application.getPublicId())
        .body(legacyViolationStatusDTO)
        .put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_LEGACY_VIOLATION_STATUS, null);
    assertApplicationData(auditDTO, application);
    assertLegacyViolationStatusConfigurationData(auditDTO, null, "inherit");
  }

  @Test
  public void testSetLegacyViolationStatus_AllowOverrideByChild_EnableLocalSetting() throws Exception {
    LegacyViolationStatusDTO legacyViolationStatusDTO = new LegacyViolationStatusDTO();
    legacyViolationStatusDTO.enabled = true;
    legacyViolationStatusDTO.allowOverride = true;

    restRequest().path(LegacyViolationResource.GET_PATH)
        .parameter(OwnerType.ORGANIZATION, organization.getId())
        .body(legacyViolationStatusDTO)
        .put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_LEGACY_VIOLATION_STATUS, null);
    assertOrganizationData(auditDTO, organization);
    assertLegacyViolationStatusConfigurationData(auditDTO, "allow", "enable");
  }

  @Test
  public void testSetLegacyViolationStatus_DisallowOverrideByChild_DisableLocalSetting() throws Exception {
    LegacyViolationStatusDTO legacyViolationStatusDTO = new LegacyViolationStatusDTO();
    legacyViolationStatusDTO.enabled = false;

    restRequest().path(LegacyViolationResource.GET_PATH)
        .parameter(OwnerType.ORGANIZATION, organization.getId())
        .body(legacyViolationStatusDTO)
        .put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_LEGACY_VIOLATION_STATUS, null);
    assertOrganizationData(auditDTO, organization);
    assertLegacyViolationStatusConfigurationData(auditDTO, "disallow", "disable");
  }

  private void assertLegacyViolationStatusData(AuditDTO auditDTO, Integer changedPolicyViolationCount) {
    assertCustomData(auditDTO, "changedPolicyViolationCount", changedPolicyViolationCount);
  }

  private void assertLegacyViolationStatusConfigurationData(
      AuditDTO auditDTO,
      String overrideByChild,
      String localSetting)
  {
    assertCustomData(auditDTO, "overrideByChild", overrideByChild);
    assertCustomData(auditDTO, "localSetting", localSetting);
  }
}
