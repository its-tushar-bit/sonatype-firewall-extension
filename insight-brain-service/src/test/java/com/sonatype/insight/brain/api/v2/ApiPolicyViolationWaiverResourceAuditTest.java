/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.stream.Collectors;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.policy.ConstraintFactDTO;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Before;
import org.junit.Test;

public class ApiPolicyViolationWaiverResourceAuditTest
    extends AbstractAuditTest
{
  private PolicyWaiverDAO policyWaiverDAO;

  private Organization org;

  private Application app;

  private Policy policy;

  private PolicyViolation policyViolation;

  @Before
  public void setUpPolicyViolation() {
    policyWaiverDAO = lookup(PolicyWaiverDAO.class);

    org = tempEntity.newOrganization();
    app = tempEntity.newApplication(org.getId());
    policy = tempEntity.newPolicy();

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scan1");
    policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
  }

  @Test
  public void testAddPolicyWaiver_Application() throws Exception {
    restRequest(policyViolation.getId(), OwnerType.APPLICATION).body("waiver comment", MediaType.TEXT_PLAIN).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_WAIVER, null);
    assertPolicyWaiverData(auditDTO);
    assertApplicationData(auditDTO, app);
  }

  @Test
  public void testAddPolicyWaiver_Organization() throws Exception {
    restRequest(policyViolation.getId(), OwnerType.ORGANIZATION).body("waiver comment", MediaType.TEXT_PLAIN).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_WAIVER, null);
    assertPolicyWaiverData(auditDTO);
    assertOrganizationData(auditDTO, org);
  }

  @Test
  public void testAddPolicyWaiver_Application_Unauthorized() throws Exception {
    restRequest(policyViolation.getId(), OwnerType.APPLICATION).with(unauthorizedUser())
        .body("waiver comment", MediaType.TEXT_PLAIN).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_WAIVER, "unauthorized");
    assertApplicationData(auditDTO, app);
  }

  @Test
  public void testAddPolicyWaiver_Organization_Unauthorized() throws Exception {
    restRequest(policyViolation.getId(), OwnerType.ORGANIZATION).with(unauthorizedUser())
        .body("waiver comment", MediaType.TEXT_PLAIN).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_WAIVER, "unauthorized");
    assertOrganizationData(auditDTO, org);
  }

  private void assertPolicyWaiverData(AuditDTO auditDTO) {
    PolicyWaiver policyWaiver = policyWaiverDAO.getByIdNotNull((String) auditDTO.data.get("policyWaiverId"));
    assertCustomData(auditDTO, "policyId", policyWaiver.getPolicyId());
    assertCustomData(auditDTO, "policyName", policy.getName());
    assertCustomData(auditDTO, "policyWaiverId", policyWaiver.getId());
    assertCustomData(auditDTO, "comment", policyWaiver.getComment());
    assertCustomData(auditDTO, "componentHash", policyWaiver.getHash());
    assertCustomObject(auditDTO, "policyConstraints",
        policyWaiver.getConstraintFacts().stream().map(ConstraintFactDTO::new).collect(Collectors.toList()));
  }

  private HttpRequest restRequest(String policyViolationId, OwnerType ownerType) {
    return restRequest().path(PublicApiPaths.POLICY_VIOLATION_WAIVER_PATH)
        .parameter(policyViolationId, ownerType.toString());
  }
}
