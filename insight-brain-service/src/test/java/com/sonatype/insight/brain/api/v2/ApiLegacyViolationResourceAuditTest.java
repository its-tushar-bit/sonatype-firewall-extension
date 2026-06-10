/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Audit tests for {@link ApiLegacyViolationResource}.
 * Verifies that grant/revoke and list operations write the expected audit events.
 */
public class ApiLegacyViolationResourceAuditTest
    extends AbstractAuditTest
{
  private ApplicationDAO applicationDAO;

  private PolicyDAO policyDAO;

  private Application application;

  private PolicyEvaluation policyEvaluation;

  @Before
  public void before() {
    applicationDAO = lookup(ApplicationDAO.class);
    policyDAO = lookup(PolicyDAO.class);

    application = tempEntity.newApplicationWithParent();
    policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_BUILD, "scanId");
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.LEGACY_VIOLATIONS_PATH_V2);
  }

  @Test
  public void testGrant_AuditsApplyLegacyViolationStatus() throws Exception {
    application.setLegacyViolationEnabled(true);
    applicationDAO.update(application);

    Policy legacyAllowed = tempEntity.newPolicy();
    legacyAllowed.setLegacyViolationAllowed(true);
    policyDAO.update(legacyAllowed);
    tempEntity.newPolicyViolation(policyEvaluation, legacyAllowed);

    restRequest().path(ApiLegacyViolationResource.GRANT_PATH)
        .parameter(application.getPublicId())
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.APPLY_LEGACY_VIOLATION_STATUS, null);
    assertApplicationData(auditDTO, application);
    assertThat(auditDTO.data).containsEntry("changedPolicyViolationCount", 1);
  }

  @Test
  public void testRevoke_AuditsRevokeLegacyViolationStatus() throws Exception {
    tempEntity.newLegacyPolicyViolation(policyEvaluation, tempEntity.newPolicy());

    restRequest().path(ApiLegacyViolationResource.REVOKE_PATH)
        .parameter(application.getPublicId())
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.REVOKE_LEGACY_VIOLATION_STATUS, null);
    assertApplicationData(auditDTO, application);
    assertThat(auditDTO.data).containsEntry("changedPolicyViolationCount", 1);
  }

  @Test
  public void testList_AuditsExportPolicyViolations() throws Exception {
    tempEntity.newLegacyPolicyViolation(policyEvaluation, tempEntity.newPolicy());

    restRequest().path(ApiLegacyViolationResource.APPLICATION_PATH)
        .parameter(application.getPublicId())
        .get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.EXPORT_POLICY_VIOLATIONS, null);
    assertApplicationData(auditDTO, application);
  }
}
