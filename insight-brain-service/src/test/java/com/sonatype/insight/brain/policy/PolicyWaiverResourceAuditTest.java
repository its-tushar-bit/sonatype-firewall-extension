/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Before;
import org.junit.Test;

public class PolicyWaiverResourceAuditTest
    extends AbstractAuditTest
{
  private static final String COMPONENT_HASH = "hash";

  private final PolicyWaiverDAO policyWaiverDAO = new PolicyWaiverDAO();

  private Policy policy;

  @Before
  public void before() {
    policy = tempEntity.newPolicy();
  }

  @Test
  public void testAddPolicyWaiver_Application() throws Exception {
    Application application = tempEntity.newApplicationWithParent();

    restRequest(application).body(policyWaiver()).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_WAIVER, null);
    assertApplicationData(auditDTO, application);
    assertPolicyWaiverData(auditDTO);
  }

  @Test
  public void testAddPolicyWaiver_Organization() throws Exception {
    Organization organization = tempEntity.newOrganization();

    restRequest(organization).body(policyWaiver()).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_WAIVER, null);
    assertOrganizationData(auditDTO, organization);
    assertPolicyWaiverData(auditDTO);
  }

  @Test
  public void testAddPolicyWaiver_Repository() throws Exception {
    Repository repository = tempEntity.newRepository();

    restRequest(repository).body(policyWaiver()).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_WAIVER, null);
    assertRepositoryData(auditDTO, repository);
    assertPolicyWaiverData(auditDTO);
  }

  @Test
  public void testAddPolicyWaiver_RepositoryContainer() throws Exception {
    restRequest(RepositoryContainer.SINGLETON).body(policyWaiver()).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_WAIVER, null);
    assertRepositoryContainerData(auditDTO);
    assertPolicyWaiverData(auditDTO);
  }

  @Test
  public void testAddPolicyWaiver_Unauthorized() throws Exception {
    Application application = tempEntity.newApplicationWithParent();

    restRequest(application).with(unauthorizedUser()).body(new PolicyWaiver()).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_WAIVER, "unauthorized");
    assertApplicationData(auditDTO, application);
  }

  @Test
  public void testGetPolicyWaiversByHash_Application() throws Exception {
    final Application application = tempEntity.newApplicationWithParent();
    restRequest(application).path("component", COMPONENT_HASH).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_INFORMATION, null);
    assertApplicationData(auditDTO, application);
    assertCustomData(auditDTO, "componentHash", COMPONENT_HASH);
  }

  @Test
  public void testGetPolicyWaiversByHash_Organization() throws Exception {
    final Organization organization = tempEntity.newOrganization();
    restRequest(organization).path("component", COMPONENT_HASH).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_INFORMATION, null);
    assertOrganizationData(auditDTO, organization);
    assertCustomData(auditDTO, "componentHash", COMPONENT_HASH);
  }

  @Test
  public void testGetPolicyWaiversByHash_Repository() throws Exception {
    final Repository repository = tempEntity.newRepository();
    restRequest(repository).path("component", COMPONENT_HASH).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_INFORMATION, null);
    assertRepositoryData(auditDTO, repository);
    assertCustomData(auditDTO, "componentHash", COMPONENT_HASH);
  }

  @Test
  public void testGetPolicyWaiversByHash_RepositoryContainer() throws Exception {
    restRequest(RepositoryContainer.SINGLETON).path("component", COMPONENT_HASH).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_INFORMATION, null);
    assertRepositoryContainerData(auditDTO);
    assertCustomData(auditDTO, "componentHash", COMPONENT_HASH);
  }

  @Test
  public void testGetPolicyWaiversByHash_Unauthorized() throws Exception {
    Application application = tempEntity.newApplicationWithParent();

    restRequest(application).path("component", COMPONENT_HASH).with(unauthorizedUser()).get();
    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_INFORMATION, "unauthorized");
    assertApplicationData(auditDTO, application);
  }

  private PolicyWaiver policyWaiver() {
    return policyWaiver(COMPONENT_HASH, constraintFacts());
  }

  private PolicyWaiver policyWaiver(String hash, List<ConstraintFact> constraintFacts) {
    return new PolicyWaiver(hash, policy.getId(), null, constraintFacts, "comment");
  }

  private List<ConstraintFact> constraintFacts() {
    return Arrays.asList(
        constraintFact("constraintName1", conditionFact("summary1", "reason1"), conditionFact("summary2", "reason1"),
            conditionFact("summary3", "reason2")),
        constraintFact("constraintName2", conditionFact("summary1", "reason1"), conditionFact("summary2", "reason2")));
  }

  private ConstraintFact constraintFact(String constraintName, ConditionFact... conditionFacts) {
    return new ConstraintFact("constraintId", constraintName, "operatorName", conditionFacts);
  }

  private ConditionFact conditionFact(String summary, String reason) {
    return new ConditionFact("conditionTypeId", 0, summary, reason);
  }

  private HttpRequest restRequest(Owner owner) {
    return restRequest().path(PolicyWaiverResource.RESOURCE_PATH).parameter(owner.getType(),
        owner.getType().equals(OwnerType.APPLICATION) ? owner.getPublicId() : owner.getId());
  }

  private void assertPolicyWaiverData(AuditDTO auditDTO) {
    assertPolicyWaiverData(auditDTO, policyWaiverDAO.getById((String) auditDTO.data.get("policyWaiverId")));
  }

  private void assertPolicyWaiverData(AuditDTO auditDTO, PolicyWaiver policyWaiver) {
    assertCustomData(auditDTO, "policyId", policy.getId());
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
}
