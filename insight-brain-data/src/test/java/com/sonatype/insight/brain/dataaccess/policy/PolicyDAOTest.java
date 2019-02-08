/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.tag.PolicyTagDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.InvalidPolicyException;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.tag.PolicyTag;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PolicyDAOTest
    extends AbstractDbDAOTest
{
  private PolicyDAO policyDAO;

  @Before
  public void setUp() throws Exception {
    policyDAO = new PolicyDAO();
  }

  @Test
  public void testUpdatePolicyDoesNotExist() throws Exception {
    // Create a policy, but don't insert it
    String policyName = "PolicyDAOTest new policy";
    Policy policy = newPolicy(applicationId, policyName);
    policy.setId("yeti");

    // Update the policy
    assertThatThrownBy(() -> {
      policyDAO.update(policy);
    }).isInstanceOf(NotFoundException.class).hasMessage("Cannot find a policy with ID yeti.");
  }

  @Test
  public void testInsertNameNotUnique() throws Exception {
    // Add a policy
    String policyName = "PolicyDAOTest new policy";
    {
      Policy policy = new Policy();
      policy.setName(policyName);
      policy.setOwnerId(applicationId);
      Constraint constraint = new Constraint(null, "PolicyDAOTest new constraint", LogicalOperator.AND);
      constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
      policy.addConstraint(constraint);
      policyDAO.insert(policy);
    }

    // Add another policy with the same name
    Policy policy = new Policy();
    policy.setName(policyName);
    policy.setOwnerId(applicationId);
    Constraint constraint = new Constraint(null, "PolicyDAOTest new constraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    policy.addConstraint(constraint);
    assertThatThrownBy(() -> {
      policyDAO.insert(policy);
    }
    ).isInstanceOf(InvalidPolicyException.class).hasMessage("A policy with name 'PolicyDAOTest new policy' already exists");

    // Add another policy with a case-/whitespace-equivalent name
    policy.setName(policyName.replace("\\s", "").toLowerCase(Locale.ENGLISH));
    assertThatThrownBy(() -> {
      policyDAO.insert(policy);
    }).isInstanceOf(InvalidPolicyException.class)
        .hasMessage("A policy with name 'PolicyDAOTest new policy' already exists");
  }

  @Test
  public void testInsertNameClashWithChildOwnerPolicy() throws Exception {
    // Add a policy at app level
    String policyName = "PolicyDAOTest new policy app";
    tempEntity.newPolicy(application.getId(), policyName);

    Owner parentOwner = new OwnerDAO().getParentOwner(organization);

    // Add another policy with a case-/whitespace-equivalent name at parent owner level
    assertInsertPolicyWithDuplicateName(parentOwner.getId(), policyName, application);

    // Add a policy at org level
    policyName = "PolicyDAOTest new policy org";
    tempEntity.newPolicy(organization.getId(), policyName);

    // Add another policy with a case-/whitespace-equivalent name at parent owner level
    assertInsertPolicyWithDuplicateName(parentOwner.getId(), policyName, organization);
  }

  @Test
  public void testInsertNameClashWithParentOrgPolicy() throws Exception {
    // Add a policy at parent org level
    String policyName = "PolicyDAOTest new policy";
    Policy policy = newPolicy(organization.getParentOrganizationId(), policyName);
    tempEntity.newPolicy(policy);

    Owner expectedOwner = new OwnerDAO().getParentOwner(organization);

    // Add another policy with a case-/whitespace-equivalent name at app level
    assertInsertPolicyWithDuplicateName(application.getId(), policyName, expectedOwner);

    // Add another policy with a case-/whitespace-equivalent name at child org level
    assertInsertPolicyWithDuplicateName(organization.getId(), policyName, expectedOwner);
  }

  private void assertInsertPolicyWithDuplicateName(String ownerId, String policyName, Owner expectedOwner) {
    // Add a policy with a case-/whitespace-equivalent name
    Policy policy = newPolicy(ownerId, policyName.replaceAll("\\s", "").toLowerCase(Locale.ENGLISH));
    assertThatThrownBy(() -> {
      policyDAO.insert(policy);
    }).isInstanceOf(InvalidPolicyException.class).hasMessage("A policy with the same name already exists for "
        + expectedOwner.getType() + " '" + expectedOwner.getName() + "'");
  }

  private void assertUpdatePolicyWithDuplicateName(Owner owner, String policyName, Owner expectedOwner) {
    Policy policy = tempEntity.newPolicy(owner);
    // Update the policy with a case-/whitespace-equivalent name
    policy.setName(policyName.replaceAll("\\s", "").toLowerCase(Locale.ENGLISH));
    assertThatThrownBy(() -> {
      policyDAO.update(policy);
    }).isInstanceOf(InvalidPolicyException.class).hasMessage("A policy with the same name already exists for "
        + expectedOwner.getType() + " '" + expectedOwner.getName() + "'");
  }

  @Test
  public void testUpdateNameNotUnique() throws Exception {
    // Add two policies
    String policyName1 = "PolicyDAOTest new policy 1";
    Policy policy1 = newPolicy(applicationId, policyName1);
    policyDAO.insert(policy1);
    String policyName2 = "PolicyDAOTest new policy 2";
    Policy policy2 = newPolicy(applicationId, policyName2);
    policyDAO.insert(policy2);

    // Update a policy with the same name
    policyDAO.update(policy1);

    // Update a policy with a duplicate name
    policy1.setName(policyName2);
    assertThatThrownBy(() -> {
      policyDAO.update(policy1);
    }).isInstanceOf(InvalidPolicyException.class)
        .hasMessage("A policy with name 'PolicyDAOTest new policy 2' already exists");

    // Update a policy with a case-/whitespace-equivalent name
    policy1.setName(policyName2.replace("\\s", "").toLowerCase(Locale.ENGLISH));
    assertThatThrownBy(() -> {
      policyDAO.update(policy1);
    }).isInstanceOf(InvalidPolicyException.class)
        .hasMessage("A policy with name 'PolicyDAOTest new policy 2' already exists");
  }

  @Test
  public void testUpdateNameClashWithParentOrgPolicy() throws Exception {
    // Add a policy at parent org level
    String policyName = "PolicyDAOTest new policy";
    Policy policy = newPolicy(organization.getParentOrganizationId(), policyName);
    tempEntity.newPolicy(policy);

    Owner expectedOwner = new OwnerDAO().getParentOwner(organization);

    // Update another policy with a case-/whitespace-equivalent name at app level
    assertUpdatePolicyWithDuplicateName(application, policyName, expectedOwner);

    // Update another policy with a case-/whitespace-equivalent name at child org level
    assertUpdatePolicyWithDuplicateName(organization, policyName, expectedOwner);
  }

  @Test
  public void testUpdateNameClashWithChildOwnerPolicy() throws Exception {
    // Add a policy at app level
    String policyName = "PolicyDAOTest new policy app";
    tempEntity.newPolicy(application.getId(), policyName);

    Owner parentOwner = new OwnerDAO().getParentOwner(organization);

    // Add another policy with a case-/whitespace-equivalent name at parent owner level
    assertUpdatePolicyWithDuplicateName(parentOwner, policyName, application);

    // Add a policy at org level
    policyName = "PolicyDAOTest new policy org";
    tempEntity.newPolicy(organization.getId(), policyName);

    // Add another policy with a case-/whitespace-equivalent name at parent owner level
    assertUpdatePolicyWithDuplicateName(parentOwner, policyName, organization);
  }

  @Test
  public void testAllocateIdsOnInsertAndUpdate() throws Exception {
    // Add a policy
    Policy policy = new Policy();
    policy.setName("PolicyDAOTest new policy");
    policy.setOwnerId(applicationId);
    final Constraint constraint1 = new Constraint(null, "PolicyDAOTest new constraint 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    policy.addConstraint(constraint1);
    assertThat(policy.getId()).isNull();
    assertThat(constraint1.getId()).isNull();

    policyDAO.insert(policy);
    assertThat(policy.getId()).isNotNull();
    assertThat(constraint1.getId()).isNotNull();
    String constraintId1 = constraint1.getId();

    List<Policy> policies = policyDAO.getByOwnerId(applicationId);
    assertThat(policies).hasSize(1);
    assertPolicy(policy, policies.get(0));
    policy = policies.get(0);
    assertThat(policy.getId()).isNotNull();
    assertThat(constraint1.getId()).isNotNull();

    // Update the policy - new constraint without id
    policy.setName("PolicyDAOTest updated policy");
    Constraint constraint2 = new Constraint(null, "PolicyDAOTest new constraint 2", LogicalOperator.AND);
    constraint2.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    policy.addConstraint(constraint2);
    assertThat(policy.getId()).isNotNull();
    assertThat(constraint1.getId()).isNotNull();
    assertThat(constraint2.getId()).isNull();

    policyDAO.update(policy);
    assertThat(policy.getId()).isNotNull();
    assertThat(constraint1.getId()).isEqualTo(constraintId1);
    assertThat(constraint2.getId()).isNotNull();
    String constraintId2 = constraint2.getId();

    policies = policyDAO.getByOwnerId(applicationId);
    assertThat(policies).hasSize(1);
    assertPolicy(policy, policies.get(0));
    policy = policies.get(0);
    assertThat(policy.getId()).isNotNull();
    assertThat(constraint1.getId()).isEqualTo(constraintId1);
    assertThat(constraint2.getId()).isEqualTo(constraintId2);

    // Update the policy - new constraint with id - the id should be reallocated during the update
    policy.setName("PolicyDAOTest updated again policy");
    String constraintId3 = "Constraint Id 3";
    Constraint constraint3 = new Constraint(constraintId3, "PolicyDAOTest new constraint 3", LogicalOperator.AND);
    constraint3.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    policy.addConstraint(constraint3);
    assertThat(policy.getId()).isNotNull();
    assertThat(constraint1.getId()).isNotNull();
    assertThat(constraint2.getId()).isNotNull();
    assertThat(constraint3.getId()).isNotNull();

    policyDAO.update(policy);
    assertThat(policy.getId()).isNotNull();
    assertThat(constraint1.getId()).isEqualTo(constraintId1);
    assertThat(constraint2.getId()).isEqualTo(constraintId2);
    assertThat(constraint3.getId()).isNotNull();
    assertThat(constraint3.getId()).isNotEqualTo(constraintId3);
    constraintId3 = constraint3.getId();

    policies = policyDAO.getByOwnerId(applicationId);
    assertThat(policies).hasSize(1);
    assertPolicy(policy, policies.get(0));
    policy = policies.get(0);
    assertThat(policy.getId()).isNotNull();
    assertThat(constraint1.getId()).isEqualTo(constraintId1);
    assertThat(constraint2.getId()).isEqualTo(constraintId2);
    assertThat(constraint3.getId()).isEqualTo(constraintId3);
  }

  @Test
  public void testCRUD() throws Exception {
    // Add
    final Policy policy = new Policy();
    policy.setName("PolicyDAOTest new policy");
    policy.setOwnerId(applicationId);
    final Constraint constraint = new Constraint(null, "PolicyDAOTest new constraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    policy.addConstraint(constraint);
    policyDAO.insert(policy);

    // Get
    List<Policy> policies = policyDAO.getByOwnerId(applicationId);
    assertThat(policies).hasSize(1);
    policy.setOwnerId(applicationId);
    assertPolicy(policy, policies.get(0));

    // Update
    policy.setName("PolicyDAOTest updated policy");
    policyDAO.update(policy);

    policies = policyDAO.getByOwnerId(applicationId);
    assertThat(policies).hasSize(1);
    assertPolicy(policy, policies.get(0));

    // Get
    policies = policyDAO.getByOwnerId(applicationId);
    assertThat(policies).hasSize(1);
    assertPolicy(policy, policies.get(0));

    // Delete
    policyDAO.delete(policy);

    // Get
    policies = policyDAO.getByOwnerId(applicationId);
    assertThat(policies).isEmpty();
  }

  @Test
  public void testValidateOnInsert() throws Exception {
    // Policy without name
    Policy policy = newPolicy(applicationId, null /* name */);
    policy.setOwnerId(applicationId);
    Constraint constraint1 = new Constraint("Constraint Id", "Constraint Name", LogicalOperator.AND);
    constraint1.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    policy.addConstraint(constraint1);
    assertThatThrownBy(() -> {
      policyDAO.insert(policy);
    }).isInstanceOf(InvalidPolicyException.class).hasMessage("The policy name is required.");
  }

  @Test
  public void testValidateOnUpdate() throws Exception {
    // Add a policy
    Policy policy = newPolicy(applicationId, "PolicyDAOTest Policy Name");
    policyDAO.insert(policy);

    // Update the policy
    policy.setName(null);
    assertThatThrownBy(() -> {
      policyDAO.update(policy);
    }).isInstanceOf(InvalidPolicyException.class);
  }

  @Test
  public void testDeleteByOwnerId() throws Exception {
    final Policy policy1 = new Policy();
    policy1.setName("PolicyDAOTest new policy 1");
    policy1.setOwnerId(applicationId);
    final Constraint constraint1 = new Constraint(null, "PolicyDAOTest new constraint 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    policy1.addConstraint(constraint1);
    policyDAO.insert(policy1);
    assertThat(policyDAO.getByOwnerId(applicationId)).hasSize(1);

    try (TransactionContext tx = new PolicyInternalDAO().createTransactionContext()) {
      tx.begin();
      policyDAO.deleteByOwnerId(tx, applicationId);
      tx.commit();
    }
    assertThat(policyDAO.getByOwnerId(applicationId)).isEmpty();
  }

  private static void assertPolicy(final Policy expected, final Policy actual) {
    assertThat(actual.getId()).isEqualTo(expected.getId());
    assertThat(actual.getName()).isEqualTo(expected.getName());
    assertThat(actual.getOwnerId()).isEqualTo(expected.getOwnerId());
    assertThat(actual.getThreatLevel()).isEqualTo(expected.getThreatLevel());
    assertThat(actual.getDroolsCode()).contains("// Begin policy: " + expected.getName());

    List<Constraint> expectedConstraints = expected.getConstraints();
    List<Constraint> actualConstraints = actual.getConstraints();
    assertThat(actualConstraints).hasSameSizeAs(expectedConstraints);

    for (int i = 0; i < expectedConstraints.size(); i++) {
      assertConstraint(expectedConstraints.get(i), actualConstraints.get(i));
    }
  }

  private static void assertConstraint(Constraint expected, Constraint actual) {
    assertThat(actual.getId()).isEqualTo(expected.getId());
    assertThat(actual.getName()).isEqualTo(expected.getName());
    assertThat(actual.getOperator()).isEqualTo(expected.getOperator());

    List<Condition> expectedConditions = expected.getConditions();
    List<Condition> actualConditions = actual.getConditions();
    assertThat(actualConditions).hasSameSizeAs(expectedConditions);

    for (int i = 0; i < expectedConditions.size(); i++) {
      assertCondition(expectedConditions.get(i), actualConditions.get(i));
    }
  }

  private static void assertCondition(Condition expected, Condition actual) {
    assertThat(actual.getConditionTypeId()).isEqualTo(expected.getConditionTypeId());
    assertThat(actual.getOperator()).isEqualTo(expected.getOperator());
    assertThat(actual.getValue()).isEqualTo(expected.getValue());
  }

  private Policy newPolicy(String ownerId, String name) {
    Policy policy = new Policy();
    policy.setName(name);
    policy.setOwnerId(ownerId);
    Constraint constraint = new Constraint(null, "Contraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    policy.addConstraint(constraint);
    return policy;
  }

  @Test
  public void testGetApplicable() {
    String policyNameRootOrg = "testGetApplicableRootOrganization";
    tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, policyNameRootOrg);
    String policyNameOrg = "testGetApplicableOrganization";
    tempEntity.newPolicy(organization.getId(), policyNameOrg);
    String policyNameApp = "testGetApplicableApplication";
    tempEntity.newPolicy(application.getId(), policyNameApp);

    // Check app level
    List<Policy> policies = policyDAO.getApplicableByOwnerId(application.getId());
    assertThat(policies).extracting(Policy::getName).containsExactly(policyNameApp, policyNameOrg, policyNameRootOrg);

    // Check repo level
    policies = policyDAO.getApplicableByOwnerId(repository.getId());
    assertThat(policies).extracting(Policy::getName).containsExactly(policyNameRootOrg);

    // Check org level
    policies = policyDAO.getApplicableByOwnerId(organization.getId());
    assertThat(policies).extracting(Policy::getName).containsExactly(policyNameOrg, policyNameRootOrg);

    // Check root org level
    policies = policyDAO.getApplicableByOwnerId(Organization.ROOT_ORGANIZATION_ID);
    assertThat(policies).extracting(Policy::getName).containsExactly(policyNameRootOrg);
  }

  @Test
  public void testGetApplicable_WithTags() {
    Policy policyOrg1 = tempEntity.newPolicy(organization.getId(), "policyOrg1");
    Policy policyOrg2 = tempEntity.newPolicy(organization.getId(), "policyOrg2");
    Policy policyRootOrg1 = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "policyRootOrg1");
    Policy policyRootOrg2 = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "policyRootOrg2");

    Tag tag1 = tempEntity.newTag(organization.getId());
    tempEntity.newPolicyTag(policyOrg1.getId(), tag1.getId());
    tempEntity.newPolicyTag(policyRootOrg1.getId(), tag1.getId());
    Tag tag2 = tempEntity.newTag(organization.getId());
    tempEntity.newPolicyTag(policyOrg2.getId(), tag2.getId());
    tempEntity.newPolicyTag(policyRootOrg2.getId(), tag2.getId());
    tempEntity.newApplicationTag(application.getId(), tag2.getId());

    // For apps, must retrieve only the org policies that match the tags associated with the app
    List<Policy> policies = policyDAO.getApplicableByOwnerId(application.getId());
    assertThat(policies).extracting(Policy::getName).containsExactly("policyOrg2", "policyRootOrg2");

    // For repositories, must retrieve only the org policies that don't have any tags
    policies = policyDAO.getApplicableByOwnerId(repository.getId());
    assertThat(policies).isEmpty();

    // For orgs, must retrieve all org policies, regardless of the tags associated with them
    policies = policyDAO.getApplicableByOwnerId(organization.getId());
    assertThat(policies).extracting(Policy::getName).containsExactly("policyOrg1", "policyOrg2", "policyRootOrg1",
        "policyRootOrg2");
  }

  @Test
  public void testCascadeDeleteToPolicyWaivers() throws Exception {
    Policy policy = new Policy();
    policy.setName("PolicyDAOTest new policy 1");
    policy.setOwnerId(applicationId);
    final Constraint constraint1 = new Constraint(null, "PolicyDAOTest new constraint 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    policy.addConstraint(constraint1);
    policyDAO.insert(policy);

    PolicyWaiver policyWaiver = new PolicyWaiver("12345678901234567890", policy.getId(), "MyOwnerId", "My comment");
    PolicyWaiverDAO policyWaiverDAO = new PolicyWaiverDAO();
    policyWaiverDAO.insert(policyWaiver);
    List<PolicyWaiver> policyWaivers = policyWaiverDAO.getByPolicyId(policy.getId());
    assertThat(policyWaivers).hasSize(1);

    policyDAO.delete(policy);
    policyWaivers = policyWaiverDAO.getByPolicyId(policy.getId());
    assertThat(policyWaivers).isEmpty();
  }

  @Test
  public void testCascadeDeleteToPolicyTags() throws Exception {
    Policy policy = newPolicy(applicationId, "PolicyDAOTest new policy");
    policyDAO.insert(policy);

    Tag tag = tempEntity.newTag(organization.getId());

    PolicyTag policyTag = new PolicyTag(policy.getId(), tag.getId());
    PolicyTagDAO policyTagDAO = new PolicyTagDAO();
    policyTagDAO.insert(policyTag);
    List<PolicyTag> policyTags = policyTagDAO.getByPolicyId(policy.getId());
    assertThat(policyTags).hasSize(1);

    policyDAO.delete(policy);
    policyTags = policyTagDAO.getByPolicyId(policy.getId());
    assertThat(policyTags).isEmpty();
  }

  @Test
  public void testCascadeDoesNotDeletePolicyViolations() {
    Policy policy = tempEntity.newPolicy(application);
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(applicationId, ReleaseStageType.ID,
        "PolicyEvaluationDAOTest");
    tempEntity.newPolicyViolation(policyEvaluation, policy);
    PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();
    assertThat(policyViolationDAO.getByApplicationId(policyEvaluation.getApplicationId())).hasSize(1);

    policyDAO.delete(policy);
    assertThat(policyViolationDAO.getByApplicationId(policyEvaluation.getApplicationId())).hasSize(1);
  }

  @Test
  public void testGetByOwnerIds() {
    Policy appPolicy = tempEntity.newPolicy(application);
    tempEntity.newPolicy(organization);
    List<Policy> policies;

    policies = policyDAO.getByOwnerIds(null);
    assertThat(policies).isEmpty();

    policies = policyDAO.getByOwnerIds(Collections.emptySet());
    assertThat(policies).isEmpty();

    policies = policyDAO.getByOwnerIds(Arrays.asList(application.getId(), "non-existent"));
    assertThat(policies).extracting(Policy::getId).containsExactly(appPolicy.getId());
  }

  @Test
  public void testUpdateMovePolicyUpInHierarchy() throws Exception {
    Policy policy = tempEntity.newPolicy(application);

    // Should not complain about name clashes
    policy.setOwnerId(application.getOrganizationId());
    policyDAO.update(policy);
  }

  @Test
  public void testGetByOwnerIdAndName() throws Exception {
    Policy policy1 = tempEntity.newPolicy(application.getId(), "Policy 1");
    Policy policy2 = tempEntity.newPolicy(organization.getId(), "Policy 2");
    tempEntity.newPolicy(organization.getId(), "Policy 3");

    try (TransactionContext tx = new PolicyInternalDAO().createTransactionContext()) {
      Policy policy = policyDAO.getByOwnerIdAndName(tx, application.getId(), "policy1");
      assertThat(policy).isNotNull();
      assertThat(policy.getId()).isEqualTo(policy1.getId());

      policy = policyDAO.getByOwnerIdAndName(tx, organization.getId(), "policy2");
      assertThat(policy).isNotNull();
      assertThat(policy.getId()).isEqualTo(policy2.getId());
    }
  }
}
