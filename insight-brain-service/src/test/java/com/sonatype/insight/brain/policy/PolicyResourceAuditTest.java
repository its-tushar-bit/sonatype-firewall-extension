/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class PolicyResourceAuditTest
    extends AbstractPolicyImportAuditTest
{
  private Organization organization;

  @Before
  public void before() {
    organization = tempEntity.newOrganization();
  }

  @Test
  public void testImportPolicies() throws Exception {
    PolicyExportResult policyExportResult = new PolicyExportResult();
    policyExportResult.policies = Arrays.asList(policy(), policy());
    policyExportResult.labels = Arrays.asList(label(), label(), label());
    policyExportResult.licenseThreatGroups = Collections.singletonList(licenseThreatGroup());
    policyExportResult.tags = Arrays.asList(tag(), tag(), tag(), tag());

    restRequest(OwnerType.ORGANIZATION, organization.getId()).path("import").part("file", "file", policyExportResult)
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.IMPORT, null);
    assertOrganizationData(auditDTO, organization);
    assertPolicyImportData(auditDTO, 2, 3, 1, 4);
  }

  @Test
  public void testImportPolicies_Unauthorized() throws Exception {
    PolicyExportResult policyExportResult = new PolicyExportResult();
    policyExportResult.policies = Collections.singletonList(policy());

    restRequest(OwnerType.ORGANIZATION, organization.getId()).with(unauthorizedUser()).path("import")
        .part("file", "file", policyExportResult).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.IMPORT, "unauthorized");
    assertOrganizationData(auditDTO, organization);
    assertPolicyImportData(auditDTO, null, null, null, null);
  }

  @Test
  public void testImportPolicies_DeletesExistingPolicyWaivers() throws Exception {
    Policy policy = tempEntity.newPolicy("policy");
    Application application = tempEntity.newApplication(organization.getId());
    PolicyWaiver rootOrganizationPolicyWaiver = savePolicyWaiver(policy.getId(), Organization.ROOT_ORGANIZATION_ID);
    PolicyWaiver organizationPolicyWaiver = savePolicyWaiver(policy.getId(), organization.getId());
    PolicyWaiver applicationPolicyWaiver = savePolicyWaiver(policy.getId(), application.getId());
    PolicyExportResult policyExportResult = new PolicyExportResult();
    policyExportResult.policies = Collections.singletonList(policy());

    restRequest(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID).path("import")
        .part("file", "file", policyExportResult).post();

    List<AuditDTO> auditDTOs = assertAuditLogs(AuditEvent.DELETE_WAIVER, null);
    assertApplicationData(auditDTOs.get(0), application);
    assertDeletePolicyWaiverData(auditDTOs.get(0), policy, applicationPolicyWaiver);
    assertOrganizationData(auditDTOs.get(1), organization);
    assertDeletePolicyWaiverData(auditDTOs.get(1), policy, organizationPolicyWaiver);
    assertOrganizationData(auditDTOs.get(2), Organization.ROOT_ORGANIZATION_ID, "Root Organization");
    assertDeletePolicyWaiverData(auditDTOs.get(2), policy, rootOrganizationPolicyWaiver);
  }

  @Test
  public void testImportPolicies_DoesNotDeleteExistingPolicyWaivers_BadRequest() throws Exception {
    Policy policy = tempEntity.newPolicy("policy");
    PolicyWaiver policyWaiver = savePolicyWaiver(policy.getId(), organization.getId());
    PolicyExportResult policyExportResult = new PolicyExportResult();
    policyExportResult.policies = Collections.singletonList(policy());
    policyExportResult.labels = Collections.singletonList(
        new Label(organization.getId(), "thisNameIsTooLong________________________________51", "description",
            Color.yellow));

    restRequest(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID).path("import")
        .part("file", "file", policyExportResult).post();
    
    AuditDTO auditDTO = assertAuditLog(AuditEvent.IMPORT, "bad-request");
    assertOrganizationData(auditDTO, Organization.ROOT_ORGANIZATION_ID, "Root Organization");
    assertPolicyImportData(auditDTO, 1, 1, 0, 0);
    assertThat(new PolicyWaiverDAO().getById(policyWaiver.getId()), is(notNullValue()));
    assertThat(awaitLogEntries(AuditEvent.DELETE_WAIVER, 0), empty());
  }

  private HttpRequest restRequest(OwnerType ownerType, String ownerId) {
    return restRequest().path(PolicyResource.RESOURCE_PATH).parameter(ownerType, ownerId);
  }

  private AuditDTO assertAuditLog(AuditEvent auditEvent, String error) {
    AuditDTO auditDTO = awaitLogEntries(auditEvent, 1).get(0);
    assertStandardData(auditDTO, auditEvent, error);
    return auditDTO;
  }

  private PolicyWaiver savePolicyWaiver(String policyId, String ownerId) {
    return tempEntity.newWaiver("hash", policyId, ownerId, constraintFacts(), "comment");
  }

  private List<ConstraintFact> constraintFacts() {
    return Arrays.asList(
        constraintFact("constraintName1", conditionFact("summary1", "reason1"), conditionFact("summary2", "reason1"),
            conditionFact("summary3", "reason2")),
        constraintFact("constraintName2", conditionFact("summary1", "reason1"), conditionFact("summary2", "reason2")));
  }

  private ConstraintFact constraintFact(String constraintName, ConditionFact... conditionFacts) {
    return new ConstraintFact("constraintId", constraintName, "operatorName").with(conditionFacts);
  }

  private ConditionFact conditionFact(String summary, String reason) {
    return new ConditionFact("conditionTypeId", 0, summary, reason);
  }

  private List<AuditDTO> assertAuditLogs(AuditEvent auditEvent, String error) {
    List<AuditDTO> auditDTOs = awaitLogEntries(auditEvent, 3);
    auditDTOs.forEach(auditDTO -> assertStandardData(auditDTO, auditEvent, error));
    return auditDTOs;
  }

  private void assertDeletePolicyWaiverData(AuditDTO auditDTO, Policy policy, PolicyWaiver policyWaiver) {
    assertCustomData(auditDTO, "policyId", policy.getId());
    assertCustomData(auditDTO, "policyName", policy.getName());
    assertCustomData(auditDTO, "policyWaiverId", policyWaiver.getId());
    assertCustomData(auditDTO, "comment", null);
    assertCustomData(auditDTO, "componentHash", policyWaiver.getHash());
    if (policyWaiver.getConstraintFacts() == null) {
      assertCustomData(auditDTO, "policyConstraints", null);
    }
    else {
      assertCustomObject(auditDTO, "policyConstraints",
          policyWaiver.getConstraintFacts().stream().map(ConstraintFactDTO::new).collect(Collectors.toList()));
    }
  }
}
