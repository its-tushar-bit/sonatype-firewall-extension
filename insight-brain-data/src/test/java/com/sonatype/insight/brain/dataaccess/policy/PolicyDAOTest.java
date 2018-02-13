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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

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
    try {
      policyDAO.update(policy);
      Assert.fail("Expected NotFoundException");
    }
    catch (NotFoundException expected) {
      if (!"Cannot find a policy with ID yeti.".equals(expected.getMessage())) {
        throw expected;
      }
    }
  }

  @Test
  public void testInsertNameNotUnique() throws Exception {
    // Add a policy
    String policyName = "PolicyDAOTest new policy";
    Policy policy = new Policy();
    policy.setName(policyName);
    policy.setOwnerId(applicationId);
    Constraint constraint = new Constraint(null, "PolicyDAOTest new constraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    policy.addConstraint(constraint);
    policyDAO.insert(policy);

    // Add another policy with the same name
    policy = new Policy();
    policy.setName(policyName);
    policy.setOwnerId(applicationId);
    constraint = new Constraint(null, "PolicyDAOTest new constraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    policy.addConstraint(constraint);
    try {
      policyDAO.insert(policy);
      Assert.fail("Expected InvalidPolicyException");
    }
    catch (InvalidPolicyException expected) {
      if (!"A policy with name 'PolicyDAOTest new policy' already exists".equals(expected.getMessage())) {
        throw expected;
      }
    }

    // Add another policy with a case-/whitespace-equivalent name
    policy.setName(policyName.replace("\\s", "").toLowerCase(Locale.ENGLISH));
    try {
      policyDAO.insert(policy);
      Assert.fail("Expected InvalidPolicyException");
    }
    catch (InvalidPolicyException expected) {
      Assert.assertEquals("A policy with name 'PolicyDAOTest new policy' already exists", expected.getMessage());
    }
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
    try {
      policyDAO.insert(policy);
      fail("Expected InvalidPolicyException");
    }
    catch (InvalidPolicyException expected) {
      assertEquals(
          "A policy with the same name already exists for " + expectedOwner.getType() + " '" + expectedOwner.getName()
              + "'", expected.getMessage());
    }
  }

  private void assertUpdatePolicyWithDuplicateName(String ownerId, String policyName, Owner expectedOwner) {
    Policy policy = tempEntity.newPolicy(ownerId, tempEntity.uuid());
    // Update the policy with a case-/whitespace-equivalent name
    policy.setName(policyName.replaceAll("\\s", "").toLowerCase(Locale.ENGLISH));
    try {
      policyDAO.update(policy);
      fail("Expected InvalidPolicyException");
    }
    catch (InvalidPolicyException expected) {
      assertEquals(
          "A policy with the same name already exists for " + expectedOwner.getType() + " '" + expectedOwner.getName()
              + "'", expected.getMessage());
    }
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
    try {
      policyDAO.update(policy1);
      Assert.fail("Expected InvalidPolicyException");
    }
    catch (InvalidPolicyException expected) {
      if (!"A policy with name 'PolicyDAOTest new policy 2' already exists".equals(expected.getMessage())) {
        throw expected;
      }
    }

    // Update a policy with a case-/whitespace-equivalent name
    policy1.setName(policyName2.replace("\\s", "").toLowerCase(Locale.ENGLISH));
    try {
      policyDAO.update(policy1);
      Assert.fail("Expected InvalidPolicyException");
    }
    catch (InvalidPolicyException expected) {
      Assert.assertEquals("A policy with name 'PolicyDAOTest new policy 2' already exists", expected.getMessage());
    }
  }

  @Test
  public void testUpdateNameClashWithParentOrgPolicy() throws Exception {
    // Add a policy at parent org level
    String policyName = "PolicyDAOTest new policy";
    Policy policy = newPolicy(organization.getParentOrganizationId(), policyName);
    tempEntity.newPolicy(policy);

    Owner expectedOwner = new OwnerDAO().getParentOwner(organization);

    // Update another policy with a case-/whitespace-equivalent name at app level
    assertUpdatePolicyWithDuplicateName(application.getId(), policyName, expectedOwner);

    // Update another policy with a case-/whitespace-equivalent name at child org level
    assertUpdatePolicyWithDuplicateName(organization.getId(), policyName, expectedOwner);
  }

  @Test
  public void testUpdateNameClashWithChildOwnerPolicy() throws Exception {
    // Add a policy at app level
    String policyName = "PolicyDAOTest new policy app";
    tempEntity.newPolicy(application.getId(), policyName);

    Owner parentOwner = new OwnerDAO().getParentOwner(organization);

    // Add another policy with a case-/whitespace-equivalent name at parent owner level
    assertUpdatePolicyWithDuplicateName(parentOwner.getId(), policyName, application);

    // Add a policy at org level
    policyName = "PolicyDAOTest new policy org";
    tempEntity.newPolicy(organization.getId(), policyName);

    // Add another policy with a case-/whitespace-equivalent name at parent owner level
    assertUpdatePolicyWithDuplicateName(parentOwner.getId(), policyName, organization);
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
    Assert.assertNull(policy.getId());
    Assert.assertNull(constraint1.getId());

    policyDAO.insert(policy);
    Assert.assertNotNull(policy.getId());
    Assert.assertNotNull(constraint1.getId());
    String constraintId1 = constraint1.getId();

    List<Policy> policies = policyDAO.getByOwnerId(applicationId);
    Assert.assertNotNull(policies);
    Assert.assertEquals(1, policies.size());
    assertPolicy(policy, policies.get(0));
    policy = policies.get(0);
    Assert.assertNotNull(policy.getId());
    Assert.assertNotNull(constraint1.getId());

    // Update the policy - new constraint without id
    policy.setName("PolicyDAOTest updated policy");
    Constraint constraint2 = new Constraint(null, "PolicyDAOTest new constraint 2", LogicalOperator.AND);
    constraint2.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    policy.addConstraint(constraint2);
    Assert.assertNotNull(policy.getId());
    Assert.assertNotNull(constraint1.getId());
    Assert.assertNull(constraint2.getId());

    policyDAO.update(policy);
    Assert.assertNotNull(policy.getId());
    Assert.assertNotNull(constraint1.getId());
    Assert.assertEquals(constraintId1, constraint1.getId());
    Assert.assertNotNull(constraint2.getId());
    String constraintId2 = constraint2.getId();

    policies = policyDAO.getByOwnerId(applicationId);
    Assert.assertNotNull(policies);
    Assert.assertEquals(1, policies.size());
    assertPolicy(policy, policies.get(0));
    policy = policies.get(0);
    Assert.assertNotNull(policy.getId());
    Assert.assertNotNull(constraint1.getId());
    Assert.assertEquals(constraintId1, constraint1.getId());
    Assert.assertNotNull(constraint2.getId());
    Assert.assertEquals(constraintId2, constraint2.getId());

    // Update the policy - new constraint with id - the id should be reallocated during the update
    policy.setName("PolicyDAOTest updated again policy");
    String constraintId3 = "Constraint Id 3";
    Constraint constraint3 = new Constraint(constraintId3, "PolicyDAOTest new constraint 3", LogicalOperator.AND);
    constraint3.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    policy.addConstraint(constraint3);
    Assert.assertNotNull(policy.getId());
    Assert.assertNotNull(constraint1.getId());
    Assert.assertNotNull(constraint2.getId());
    Assert.assertNotNull(constraint3.getId());

    policyDAO.update(policy);
    Assert.assertNotNull(policy.getId());
    Assert.assertNotNull(constraint1.getId());
    Assert.assertEquals(constraintId1, constraint1.getId());
    Assert.assertNotNull(constraint2.getId());
    Assert.assertEquals(constraintId2, constraint2.getId());
    Assert.assertNotNull(constraint3.getId());
    Assert.assertNotEquals(constraintId3, constraint3.getId());
    constraintId3 = constraint3.getId();

    policies = policyDAO.getByOwnerId(applicationId);
    Assert.assertNotNull(policies);
    Assert.assertEquals(1, policies.size());
    assertPolicy(policy, policies.get(0));
    policy = policies.get(0);
    Assert.assertNotNull(policy.getId());
    Assert.assertNotNull(constraint1.getId());
    Assert.assertEquals(constraintId1, constraint1.getId());
    Assert.assertNotNull(constraint2.getId());
    Assert.assertEquals(constraintId2, constraint2.getId());
    Assert.assertNotNull(constraint3.getId());
    Assert.assertEquals(constraintId3, constraint3.getId());
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
    Assert.assertNotNull(policies);
    Assert.assertEquals(1, policies.size());
    policy.setOwnerId(applicationId);
    assertPolicy(policy, policies.get(0));

    // Update
    policy.setName("PolicyDAOTest updated policy");
    policyDAO.update(policy);

    policies = policyDAO.getByOwnerId(applicationId);
    Assert.assertNotNull(policies);
    Assert.assertEquals(1, policies.size());
    assertPolicy(policy, policies.get(0));

    // Get
    policies = policyDAO.getByOwnerId(applicationId);
    Assert.assertNotNull(policies);
    Assert.assertEquals(1, policies.size());
    assertPolicy(policy, policies.get(0));

    // Delete
    policyDAO.delete(policy);

    // Get
    policies = policyDAO.getByOwnerId(applicationId);
    Assert.assertNotNull(policies);
    Assert.assertEquals(0, policies.size());
  }

  @Test
  public void testValidateOnInsert() throws Exception {
    // Policy without name
    Policy policy = newPolicy(applicationId, null /* name */);
    policy.setOwnerId(applicationId);
    Constraint constraint1 = new Constraint("Constraint Id", "Constraint Name", LogicalOperator.AND);
    constraint1.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    policy.addConstraint(constraint1);
    try {
      policyDAO.insert(policy);
      Assert.fail("Expected InvalidPolicyException");
    }
    catch (InvalidPolicyException expected) {
      if (!"The policy name is required.".equals(expected.getMessage())) {
        throw expected;
      }
    }
  }

  @Test
  public void testValidateOnUpdate() throws Exception {
    // Add a policy
    Policy policy = newPolicy(applicationId, "PolicyDAOTest Policy Name");
    policyDAO.insert(policy);

    // Update the policy
    policy.setName(null);
    try {
      policyDAO.update(policy);
      Assert.fail("Expected InvalidPolicyException");
    }
    catch (InvalidPolicyException expected) {
    }
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
    Assert.assertEquals(1, policyDAO.getByOwnerId(applicationId).size());

    try (TransactionContext tx = new PolicyInternalDAO().createTransactionContext()) {
      tx.begin();
      policyDAO.deleteByOwnerId(tx, applicationId);
      tx.commit();
    }
    Assert.assertEquals(0, policyDAO.getByOwnerId(applicationId).size());
  }

  private static void assertPolicy(final Policy expected, final Policy actual) {
    Assert.assertEquals(expected.getId(), actual.getId());
    Assert.assertEquals(expected.getName(), actual.getName());
    Assert.assertEquals(expected.getOwnerId(), actual.getOwnerId());
    Assert.assertEquals(expected.isEnabled(), actual.isEnabled());
    Assert.assertEquals(expected.getThreatLevel(), actual.getThreatLevel());
    assertThat(actual.getDroolsCode(), is(notNullValue()));
    assertThat(actual.getDroolsCode(), containsString("// Begin policy: " + expected.getName()));

    List<Constraint> expectedConstraints = expected.getConstraints();
    List<Constraint> actualConstraints = actual.getConstraints();
    Assert.assertEquals(expectedConstraints.size(), actualConstraints.size());

    for (int i = 0; i < expectedConstraints.size(); i++) {
      assertConstraint(expectedConstraints.get(i), actualConstraints.get(i));
    }
  }

  private static void assertConstraint(Constraint expected, Constraint actual) {
    Assert.assertEquals(expected.getId(), actual.getId());
    Assert.assertEquals(expected.getName(), actual.getName());
    Assert.assertEquals(expected.isEnabled(), actual.isEnabled());
    Assert.assertEquals(expected.getOperator(), actual.getOperator());

    List<Condition> expectedConditions = expected.getConditions();
    List<Condition> actualConditions = actual.getConditions();
    Assert.assertEquals(expectedConditions.size(), actualConditions.size());

    for (int i = 0; i < expectedConditions.size(); i++) {
      assertCondition(expectedConditions.get(i), actualConditions.get(i));
    }
  }

  private static void assertCondition(Condition expected, Condition actual) {
    Assert.assertEquals(expected.getConditionTypeId(), actual.getConditionTypeId());
    Assert.assertEquals(expected.getOperator(), actual.getOperator());
    Assert.assertEquals(expected.getValue(), actual.getValue());
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
    assertThat(policies, hasSize(3));
    assertEquals(policyNameApp, policies.get(0).getName());
    assertEquals(policyNameOrg, policies.get(1).getName());
    assertEquals(policyNameRootOrg, policies.get(2).getName());

    // Check repo level
    policies = policyDAO.getApplicableByOwnerId(repository.getId());
    assertThat(policies, hasSize(1));
    assertEquals(policyNameRootOrg, policies.get(0).getName());

    // Check org level
    policies = policyDAO.getApplicableByOwnerId(organization.getId());
    assertThat(policies, hasSize(2));
    assertEquals(policyNameOrg, policies.get(0).getName());
    assertEquals(policyNameRootOrg, policies.get(1).getName());

    // Check root org level
    policies = policyDAO.getApplicableByOwnerId(Organization.ROOT_ORGANIZATION_ID);
    assertThat(policies, hasSize(1));
    assertEquals(policyNameRootOrg, policies.get(0).getName());
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
    assertThat(policies, hasSize(2));
    assertEquals("policyOrg2", policies.get(0).getName());
    assertEquals("policyRootOrg2", policies.get(1).getName());

    // For repositories, must retrieve only the org policies that don't have any tags
    policies = policyDAO.getApplicableByOwnerId(repository.getId());
    assertThat(policies, hasSize(0));

    // For orgs, must retrieve all org policies, regardless of the tags associated with them
    policies = policyDAO.getApplicableByOwnerId(organization.getId());
    assertThat(policies, hasSize(4));
    assertEquals("policyOrg1", policies.get(0).getName());
    assertEquals("policyOrg2", policies.get(1).getName());
    assertEquals("policyRootOrg1", policies.get(2).getName());
    assertEquals("policyRootOrg2", policies.get(3).getName());
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
    assertEquals(1, policyWaivers.size());

    policyDAO.delete(policy);
    policyWaivers = policyWaiverDAO.getByPolicyId(policy.getId());
    assertEquals(0, policyWaivers.size());
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
    assertEquals(1, policyTags.size());

    policyDAO.delete(policy);
    policyTags = policyTagDAO.getByPolicyId(policy.getId());
    assertEquals(0, policyTags.size());
  }

  @Test
  public void testCascadeDoesNotDeletePolicyViolations() {
    Policy policy = tempEntity.newPolicy(applicationId, "testCascadeDoesNotDeleteToPolicyViolations");
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(applicationId, ReleaseStageType.ID,
        "PolicyEvaluationDAOTest");
    tempEntity.newPolicyViolation(policyEvaluation, policy);
    PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();
    assertThat(policyViolationDAO.getByApplicationId(policyEvaluation.getApplicationId()), hasSize(1));

    policyDAO.delete(policy);
    assertThat(policyViolationDAO.getByApplicationId(policyEvaluation.getApplicationId()), hasSize(1));
  }

  @Test
  public void testGetByOwnerIds() {
    Policy appPolicy = tempEntity.newPolicy(application.getId(), "app-policy");
    tempEntity.newPolicy(organization.getId(), "org-policy");
    List<Policy> policies;

    policies = policyDAO.getByOwnerIds(null);
    assertThat(policies, hasSize(0));

    policies = policyDAO.getByOwnerIds(Collections.<String> emptySet());
    assertThat(policies, hasSize(0));

    policies = policyDAO.getByOwnerIds(Arrays.asList(application.getId(), "non-existent"));
    assertThat(policies, hasSize(1));
    assertThat(policies.get(0).getId(), is(appPolicy.getId()));
  }

  @Test
  public void testUpdateMovePolicyUpInHierarchy() throws Exception {
    Policy policy = tempEntity.newPolicy(application.getId(), "My policy");

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
      assertThat(policy, is(notNullValue()));
      assertThat(policy.getId(), is(policy1.getId()));

      policy = policyDAO.getByOwnerIdAndName(tx, organization.getId(), "policy2");
      assertThat(policy, is(notNullValue()));
      assertThat(policy.getId(), is(policy2.getId()));
    }
  }
}
