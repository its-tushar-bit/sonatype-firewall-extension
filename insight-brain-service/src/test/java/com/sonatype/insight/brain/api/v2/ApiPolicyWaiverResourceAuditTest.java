/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Collections;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.policy.ConstraintFactDTO;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.api.v2.ApiPolicyWaiverResource.BY_POLICY_WAIVER_ID_PATH;
import static com.sonatype.insight.brain.model.repository.RepositoryContainer.REPOSITORY_CONTAINER_ID;

public class ApiPolicyWaiverResourceAuditTest
    extends AbstractAuditTest
{
  private Organization org;

  private Application app;

  private Policy policy;

  @Before
  public void setUpPolicyViolation() {
    org = tempEntity.newOrganization();
    app = tempEntity.newApplication(org.getId());
    policy = tempEntity.newPolicy();
  }

  @Test
  public void testDeletePolicyWaiver_Application() throws Exception {
    ConditionFact conditionFact = new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID, 0, "foo", "bar");
    conditionFact.setTriggerJson("{\"conditionIndex\":1,\"trigger\":{\"refId\":\"1234\",\"severity\":5.7}}");
    ConstraintFact constraintFact = new ConstraintFact("id", "name", LogicalOperator.AND.toString());
    constraintFact.addConditionFact(conditionFact);

    PolicyWaiver policyWaiver =
        tempEntity.newWaiver("0b", policy.getId(), app.getId(), Collections.singletonList(constraintFact));

    restRequest().path(BY_POLICY_WAIVER_ID_PATH).parameter(OwnerType.APPLICATION, app.getId(), policyWaiver.getId())
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_WAIVER, null);
    assertPolicyWaiverData(auditDTO, policyWaiver);
    assertApplicationData(auditDTO, app);
  }

  @Test
  public void testDeletePolicyWaiver_Application_NullHashCode_NullConstraintFacts() throws Exception {
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policy.getId(), app.getId());

    restRequest().path(BY_POLICY_WAIVER_ID_PATH).parameter(OwnerType.APPLICATION, app.getId(), policyWaiver.getId())
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_WAIVER, null);
    assertPolicyWaiverData(auditDTO, policyWaiver);
    assertApplicationData(auditDTO, app);
  }

  @Test
  public void testDeletePolicyWaiver_Application_Unauthorized() throws Exception {
    restRequest().path(BY_POLICY_WAIVER_ID_PATH).parameter(OwnerType.APPLICATION, app.getId(), "policy-waiver-id")
        .with(unauthorizedUser()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_WAIVER, "unauthorized");
    assertApplicationData(auditDTO, app);
  }

  @Test
  public void testDeletePolicyWaiver_Organization() throws Exception {
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policy.getId(), org.getId());

    restRequest().path(BY_POLICY_WAIVER_ID_PATH).parameter(OwnerType.ORGANIZATION, org.getId(), policyWaiver.getId())
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_WAIVER, null);
    assertPolicyWaiverData(auditDTO, policyWaiver);
    assertOrganizationData(auditDTO, org);
  }

  @Test
  public void testDeletePolicyWaiver_Organization_Unauthorized() throws Exception {
    restRequest().path(BY_POLICY_WAIVER_ID_PATH).parameter(OwnerType.ORGANIZATION, org.getId(), "policy-waiver-id")
        .with(unauthorizedUser()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_WAIVER, "unauthorized");
    assertOrganizationData(auditDTO, org);
  }

  @Test
  public void testDeletePolicyWaiver_Repository() throws Exception {
    Repository repository = tempEntity.newRepository();
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policy.getId(), repository.getId());

    restRequest().path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.REPOSITORY, repository.getId(), policyWaiver.getId())
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_WAIVER, null);
    assertPolicyWaiverData(auditDTO, policyWaiver);
    assertRepositoryData(auditDTO, repository);
  }

  @Test
  public void testDeletePolicyWaiver_Repository_Unauthorized() throws Exception {
    Repository repository = tempEntity.newRepository();

    restRequest().path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.REPOSITORY, repository.getId(), "policy-waiver-id")
        .with(unauthorizedUser())
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_WAIVER, "unauthorized");
    assertRepositoryData(auditDTO, repository);
  }

  @Test
  public void testDeletePolicyWaiver_RepositoryContainer() throws Exception {
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policy.getId(), REPOSITORY_CONTAINER_ID);

    restRequest().path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.REPOSITORY_CONTAINER, REPOSITORY_CONTAINER_ID, policyWaiver.getId())
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_WAIVER, null);
    assertPolicyWaiverData(auditDTO, policyWaiver);
    assertRepositoryContainerData(auditDTO);
  }

  @Test
  public void testDeletePolicyWaiver_RepositoryContainer_Unauthorized() throws Exception {
    restRequest().path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.REPOSITORY_CONTAINER, REPOSITORY_CONTAINER_ID, "policy-waiver-id")
        .with(unauthorizedUser())
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_WAIVER, "unauthorized");
    assertRepositoryContainerData(auditDTO);
  }

  private void assertPolicyWaiverData(AuditDTO auditDTO, PolicyWaiver policyWaiver) {
    assertCustomData(auditDTO, "policyId", policyWaiver.getPolicyId());
    assertCustomData(auditDTO, "policyName", policy.getName());
    assertCustomData(auditDTO, "policyWaiverId", policyWaiver.getId());
    assertCustomData(auditDTO, "comment", policyWaiver.getComment());
    assertCustomData(auditDTO, "componentHash", policyWaiver.getHash());
    if (policyWaiver.getConstraintFacts() == null) {
      assertCustomData(auditDTO, "policyConstraints", null);
    }
    else {
      assertCustomObject(auditDTO, "policyConstraints",
          policyWaiver.getConstraintFacts().stream().map(ConstraintFactDTO::new).collect(Collectors.toList()));
    }
  }

  @Override
  public HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.POLICY_WAIVER_PATH);
  }
}
