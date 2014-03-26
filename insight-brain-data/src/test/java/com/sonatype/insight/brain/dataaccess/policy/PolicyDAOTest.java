/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.List;
import java.util.Locale;

import javax.persistence.EntityManager;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.tag.PolicyTagDAO;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.InvalidPolicyException;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityConditionType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.tag.PolicyTag;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class PolicyDAOTest
    extends AbstractDbDAOTest
{
  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

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
      if (!"Cannot find a policy with id yeti".equals(expected.getMessage())) {
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
    constraint.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    policy.addConstraint(constraint);
    policyDAO.insert(policy);

    // Add another policy with the same name
    policy = new Policy();
    policy.setName(policyName);
    policy.setOwnerId(applicationId);
    constraint = new Constraint(null, "PolicyDAOTest new constraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
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
  public void testInsertNameClashWithChildAppPolicy() throws Exception {
    // Add a policy at app level
    String policyName = "PolicyDAOTest new policy";
    Policy policy = newPolicy(application.getId(), policyName);
    policyDAO.insert(policy);

    // Add another policy with the same name at org level
    policy = newPolicy(organization.getId(), policyName);
    try {
      policyDAO.insert(policy);
      Assert.fail("Expected InvalidPolicyException");
    }
    catch (InvalidPolicyException expected) {
      Assert.assertEquals("A policy with the same name already exists for application '" + application.getName() + "'",
          expected.getMessage());
    }

    // Add another policy with a case-/whitespace-equivalent name at org level
    policy = newPolicy(organization.getId(), policyName.replaceAll("\\s", "").toLowerCase(Locale.ENGLISH));
    try {
      policyDAO.insert(policy);
      Assert.fail("Expected InvalidPolicyException");
    }
    catch (InvalidPolicyException expected) {
      Assert.assertEquals("A policy with the same name already exists for application '" + application.getName() + "'",
          expected.getMessage());
    }
  }

  @Test
  public void testInsertNameClashWithParentOrgPolicy() throws Exception {
    // Add a policy at org level
    String policyName = "PolicyDAOTest new policy";
    Policy policy = newPolicy(organization.getId(), policyName);
    policyDAO.insert(policy);

    // Add another policy with the same name at app level
    policy = newPolicy(application.getId(), policyName);
    try {
      policyDAO.insert(policy);
      Assert.fail("Expected InvalidPolicyException");
    }
    catch (InvalidPolicyException expected) {
      Assert.assertEquals("A policy with the same name already exists for the parent organization",
          expected.getMessage());
    }

    // Add another policy with a case-/whitespace-equivalent name at app level
    policy = newPolicy(application.getId(), policyName.replaceAll("\\s", "").toLowerCase(Locale.ENGLISH));
    try {
      policyDAO.insert(policy);
      Assert.fail("Expected InvalidPolicyException");
    }
    catch (InvalidPolicyException expected) {
      Assert.assertEquals("A policy with the same name already exists for the parent organization",
          expected.getMessage());
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
    // Add a policy at org level
    String policyName = "PolicyDAOTest new policy";
    Policy policy = newPolicy(organization.getId(), policyName);
    policyDAO.insert(policy);

    // Add a policy at app level
    policy = newPolicy(application.getId(), "unique-name");
    policyDAO.insert(policy);

    // Rename policy at app level
    policy.setName(policyName);
    try {
      policyDAO.update(policy);
      Assert.fail("Expected InvalidPolicyException");
    }
    catch (InvalidPolicyException expected) {
      Assert.assertEquals("A policy with the same name already exists for the parent organization",
          expected.getMessage());
    }

    // Rename policy at app level with a case-/whitespace-equivalent name
    policy.setName(policyName.replaceAll("\\s", "").toLowerCase(Locale.ENGLISH));
    try {
      policyDAO.update(policy);
      Assert.fail("Expected InvalidPolicyException");
    }
    catch (InvalidPolicyException expected) {
      Assert.assertEquals("A policy with the same name already exists for the parent organization",
          expected.getMessage());
    }
  }

  @Test
  public void testUpdateNameClashWithChildAppPolicy() throws Exception {
    // Add a policy at app level
    String policyName = "PolicyDAOTest new policy";
    Policy policy = newPolicy(application.getId(), policyName);
    policyDAO.insert(policy);

    // Add a policy at org level
    policy = newPolicy(organization.getId(), "unique-name");
    policyDAO.insert(policy);

    // Rename policy at org level
    policy.setName(policyName);
    try {
      policyDAO.update(policy);
      Assert.fail("Expected InvalidPolicyException");
    }
    catch (InvalidPolicyException expected) {
      Assert.assertEquals("A policy with the same name already exists for application '" + application.getName() + "'",
          expected.getMessage());
    }

    // Rename policy at org level with a case-/whitespace-equivalent name
    policy.setName(policyName.replaceAll("\\s", "").toLowerCase(Locale.ENGLISH));
    try {
      policyDAO.update(policy);
      Assert.fail("Expected InvalidPolicyException");
    }
    catch (InvalidPolicyException expected) {
      Assert.assertEquals("A policy with the same name already exists for application '" + application.getName() + "'",
          expected.getMessage());
    }
  }

  @Test
  public void testAllocateIdsOnInsertAndUpdate() throws Exception {
    // Add a policy
    Policy policy = new Policy();
    policy.setName("PolicyDAOTest new policy");
    policy.setOwnerId(applicationId);
    final Constraint constraint1 = new Constraint(null, "PolicyDAOTest new constraint 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
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
    constraint2.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
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
    constraint3.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
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
    constraint.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
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
    constraint1.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
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
    constraint1.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    policy1.addConstraint(constraint1);
    policyDAO.insert(policy1);
    Assert.assertEquals(1, policyDAO.getByOwnerId(applicationId).size());

    EntityManager em = new PolicyInternalDAO().createEntityManager();
    try {
      em.getTransaction().begin();
      policyDAO.deleteByOwnerId(em, applicationId);
      em.getTransaction().commit();
    }
    finally {
      PolicyInternalDAO.close(em);
    }
    Assert.assertEquals(0, policyDAO.getByOwnerId(applicationId).size());
  }

  private static void assertPolicy(final Policy expected, final Policy actual) {
    Assert.assertEquals(expected.getId(), actual.getId());
    Assert.assertEquals(expected.getName(), actual.getName());
    Assert.assertEquals(expected.getOwnerId(), actual.getOwnerId());
    Assert.assertEquals(expected.isEnabled(), actual.isEnabled());
    Assert.assertEquals(expected.getThreatLevel(), actual.getThreatLevel());

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
    constraint.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    policy.addConstraint(constraint);
    return policy;
  }

  @Test
  public void testGetApplicable_Organization() {
    String policyNameOrg = "testGetApplicableOrganization";
    Policy policyOrg = newPolicy(organization.getId(), policyNameOrg);
    policyDAO.insert(policyOrg);
    String policyNameApp = "testGetApplicableApplication";
    Policy policyApp = newPolicy(application.getId(), policyNameApp);
    policyDAO.insert(policyApp);

    List<Policy> policies = policyDAO.getApplicableByOwnerId(organization.getId());
    Assert.assertEquals(1, policies.size());
    Assert.assertEquals(policyNameOrg, policies.get(0).getName());
  }

  @Test
  public void testGetApplicable_Organization_WithTags() {
    // Must retrieve all org policies, regardless of the tags associated with them
    Policy policyOrg1 = newPolicy(organization.getId(), "policy1");
    policyDAO.insert(policyOrg1);
    Policy policyOrg2 = newPolicy(organization.getId(), "policy2");
    policyDAO.insert(policyOrg2);
    
    // One policy has a tag associated, the other doesn't
    Tag tag = tempEntity.newTag(organization.getId());
    tempEntity.newPolicyTag(policyOrg1.getId(), tag.getId());

    List<Policy> policies = policyDAO.getApplicableByOwnerId(organization.getId());
    Assert.assertEquals(2, policies.size());
  }

  @Test
  public void testGetApplicable_Application() {
    String policyNameOrg = "testGetApplicableOrganization";
    Policy policyOrg = newPolicy(organization.getId(), policyNameOrg);
    policyDAO.insert(policyOrg);
    String policyNameApp = "testGetApplicableApplication";
    Policy policyApp = newPolicy(application.getId(), policyNameApp);
    policyDAO.insert(policyApp);

    List<Policy> policies = policyDAO.getApplicableByOwnerId(application.getId());
    Assert.assertEquals(2, policies.size());
    Assert.assertEquals(policyNameApp, policies.get(0).getName());
    Assert.assertEquals(policyNameOrg, policies.get(1).getName());
  }

  @Test
  public void testGetApplicable_Application_WithTags() {
    // Must retrieve only the org policies that match the tags associated with the app
    Policy policyOrg1 = newPolicy(organization.getId(), "policy1");
    policyDAO.insert(policyOrg1);
    Policy policyOrg2 = newPolicy(organization.getId(), "policy2");
    policyDAO.insert(policyOrg2);

    Tag tag1 = tempEntity.newTag(organization.getId());
    tempEntity.newPolicyTag(policyOrg1.getId(), tag1.getId());
    Tag tag2 = tempEntity.newTag(organization.getId());
    tempEntity.newPolicyTag(policyOrg2.getId(), tag2.getId());
    tempEntity.newApplicationTag(application.getId(), tag2.getId());

    List<Policy> policies = policyDAO.getApplicableByOwnerId(application.getId());
    Assert.assertEquals(1, policies.size());
    Assert.assertEquals("policy2", policies.get(0).getName());
  }

  @Test
  public void testCascadeDeleteToPolicyWaivers() throws Exception {
    Policy policy = new Policy();
    policy.setName("PolicyDAOTest new policy 1");
    policy.setOwnerId(applicationId);
    final Constraint constraint1 = new Constraint(null, "PolicyDAOTest new constraint 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
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
  public void testCascadeDeleteToPolicyViolations() {
    Policy policy = tempEntity.newPolicy(applicationId, "testCascadeDeleteToPolicyViolations");
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(applicationId, ReleaseStageType.ID,
        "PolicyEvaluationDAOTest");
    tempEntity.newPolicyViolation(policyEvaluation.getId(), policy.getId());
    PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();
    assertThat(policyViolationDAO.getByEvaluationId(policyEvaluation.getId()), hasSize(1));

    policyDAO.delete(policy);
    assertThat(policyViolationDAO.getByEvaluationId(policyEvaluation.getId()), hasSize(0));
  }
}
