/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.io.File;
import java.util.List;
import java.util.Locale;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.InvalidPolicyException;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityConditionType;
import com.sonatype.insight.brain.model.tag.Tag;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;

public class PolicyDAOTest
    extends AbstractDbDAOTest
{
  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  private PolicyDAO policyDAO;

  @Before
  public void setUp() throws Exception {
    createDefaultApplication();
    policyDAO = new PolicyDAO(tempDir.newFolder());
  }

  @Test
  public void testUpdatePolicyDoesNotExist() throws Exception {
    final File dataStoreDir = tempDir.newFolder("PolicyDAOTest_testInsertNameNotUnique");
    final PolicyDAO policyDAO = new PolicyDAO(dataStoreDir);
    final String applicationId = "PolicyDAOTest_AppId";

    // Add a policy
    String policyName = "PolicyDAOTest new policy";
    Policy policy = new Policy();
    policy.setName(policyName);
    Constraint constraint = new Constraint(null, "PolicyDAOTest new constraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    policy.addConstraint(constraint);
    policyDAO.insert(applicationId, policy);

    // Delete the policy
    policyDAO.delete(applicationId, policy.getId());

    // Update the policy
    try {
      policyDAO.update(applicationId, policy);
      Assert.fail("Expected InvalidPolicyException");
    }
    catch (InvalidPolicyException expected) {
      if (!"The policy does not exist".equals(expected.getMessage())) {
        throw expected;
      }
    }
  }

  @Test
  public void testInsertNameNotUnique() throws Exception {
    final File dataStoreDir = tempDir.newFolder("PolicyDAOTest_testInsertNameNotUnique");
    final PolicyDAO policyDAO = new PolicyDAO(dataStoreDir);
    final String applicationId = "PolicyDAOTest_AppId";

    // Add a policy
    String policyName = "PolicyDAOTest new policy";
    Policy policy = new Policy();
    policy.setName(policyName);
    Constraint constraint = new Constraint(null, "PolicyDAOTest new constraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    policy.addConstraint(constraint);
    policyDAO.insert(applicationId, policy);

    // Add another policy with the same name
    policy = new Policy();
    policy.setName(policyName);
    constraint = new Constraint(null, "PolicyDAOTest new constraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    policy.addConstraint(constraint);
    try {
      policyDAO.insert(applicationId, policy);
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
      policyDAO.insert(applicationId, policy);
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
    Policy policy = newPolicy(policyName);
    policyDAO.insert(application.getId(), policy);

    // Add another policy with the same name at org level
    policy = newPolicy(policyName);
    try {
      policyDAO.insert(organization.getId(), policy);
      Assert.fail("Expected InvalidPolicyException");
    }
    catch (InvalidPolicyException expected) {
      Assert.assertEquals("A policy with the same name already exists for application '" + application.getName() + "'",
          expected.getMessage());
    }

    // Add another policy with a case-/whitespace-equivalent name at org level
    policy = newPolicy(policyName.replaceAll("\\s", "").toLowerCase(Locale.ENGLISH));
    try {
      policyDAO.insert(organization.getId(), policy);
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
    Policy policy = newPolicy(policyName);
    policyDAO.insert(organization.getId(), policy);

    // Add another policy with the same name at app level
    policy = newPolicy(policyName);
    try {
      policyDAO.insert(application.getId(), policy);
      Assert.fail("Expected InvalidPolicyException");
    }
    catch (InvalidPolicyException expected) {
      Assert.assertEquals("A policy with the same name already exists for the parent organization",
          expected.getMessage());
    }

    // Add another policy with a case-/whitespace-equivalent name at app level
    policy = newPolicy(policyName.replaceAll("\\s", "").toLowerCase(Locale.ENGLISH));
    try {
      policyDAO.insert(application.getId(), policy);
      Assert.fail("Expected InvalidPolicyException");
    }
    catch (InvalidPolicyException expected) {
      Assert.assertEquals("A policy with the same name already exists for the parent organization",
          expected.getMessage());
    }
  }

  @Test
  public void testUpdateNameNotUnique() throws Exception {
    final File dataStoreDir = tempDir.newFolder("PolicyDAOTest_testInsertNameNotUnique");
    final PolicyDAO policyDAO = new PolicyDAO(dataStoreDir);
    final String applicationId = "PolicyDAOTest_AppId";

    // Add two policies
    String policyName1 = "PolicyDAOTest new policy 1";
    Policy policy1 = new Policy();
    policy1.setName(policyName1);
    Constraint constraint = new Constraint(null, "PolicyDAOTest new constraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    policy1.addConstraint(constraint);
    policyDAO.insert(applicationId, policy1);
    String policyName2 = "PolicyDAOTest new policy 2";
    Policy policy2 = new Policy();
    policy2.setName(policyName2);
    constraint = new Constraint(null, "PolicyDAOTest new constraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    policy2.addConstraint(constraint);
    policyDAO.insert(applicationId, policy2);

    // Update a policy with the same name
    policyDAO.update(applicationId, policy1);

    // Update a policy with a duplicate name
    policy1.setName(policyName2);
    try {
      policyDAO.update(applicationId, policy1);
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
      policyDAO.update(applicationId, policy1);
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
    Policy policy = newPolicy(policyName);
    policyDAO.insert(organization.getId(), policy);

    // Add a policy at app level
    policy = newPolicy("unique-name");
    policyDAO.insert(application.getId(), policy);

    // Rename policy at app level
    policy.setName(policyName);
    try {
      policyDAO.update(application.getId(), policy);
      Assert.fail("Expected InvalidPolicyException");
    }
    catch (InvalidPolicyException expected) {
      Assert.assertEquals("A policy with the same name already exists for the parent organization",
          expected.getMessage());
    }

    // Rename policy at app level with a case-/whitespace-equivalent name
    policy.setName(policyName.replaceAll("\\s", "").toLowerCase(Locale.ENGLISH));
    try {
      policyDAO.update(application.getId(), policy);
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
    Policy policy = newPolicy(policyName);
    policyDAO.insert(application.getId(), policy);

    // Add a policy at org level
    policy = newPolicy("unique-name");
    policyDAO.insert(organization.getId(), policy);

    // Rename policy at org level
    policy.setName(policyName);
    try {
      policyDAO.update(organization.getId(), policy);
      Assert.fail("Expected InvalidPolicyException");
    }
    catch (InvalidPolicyException expected) {
      Assert.assertEquals("A policy with the same name already exists for application '" + application.getName() + "'",
          expected.getMessage());
    }

    // Rename policy at org level with a case-/whitespace-equivalent name
    policy.setName(policyName.replaceAll("\\s", "").toLowerCase(Locale.ENGLISH));
    try {
      policyDAO.update(organization.getId(), policy);
      Assert.fail("Expected InvalidPolicyException");
    }
    catch (InvalidPolicyException expected) {
      Assert.assertEquals("A policy with the same name already exists for application '" + application.getName() + "'",
          expected.getMessage());
    }
  }

  @Test
  public void testAllocateIdsOnInsertAndUpdate() throws Exception {
    final File dataStoreDir = tempDir.newFolder("PolicyDAOTest");
    final PolicyDAO policyDAO = new PolicyDAO(dataStoreDir);
    final String applicationId = "PolicyDAOTest_AppId";

    // Add a policy
    Policy policy = new Policy();
    policy.setName("PolicyDAOTest new policy");
    final Constraint constraint1 = new Constraint(null, "PolicyDAOTest new constraint 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    policy.addConstraint(constraint1);
    Assert.assertNull(policy.getId());
    Assert.assertNull(constraint1.getId());

    policyDAO.insert(applicationId, policy);
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

    policyDAO.update(applicationId, policy);
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

    policyDAO.update(applicationId, policy);
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
    final File dataStoreDir = tempDir.newFolder("PolicyDAOTest");
    final PolicyDAO policyDAO = new PolicyDAO(dataStoreDir);
    final String applicationId = "PolicyDAOTest_AppId";

    // Add a policy
    final Policy policy1 = new Policy();
    policy1.setName("PolicyDAOTest new policy 1");
    final Constraint constraint1 = new Constraint(null, "PolicyDAOTest new constraint 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    policy1.addConstraint(constraint1);
    policyDAO.insert(applicationId, policy1);

    List<Policy> policies = policyDAO.getByOwnerId(applicationId);
    Assert.assertNotNull(policies);
    Assert.assertEquals(1, policies.size());
    policy1.setOwnerId(applicationId);
    assertPolicy(policy1, policies.get(0));

    // Add another policy
    final Policy policy2 = new Policy();
    policy2.setName("PolicyDAOTest new policy 2");
    final Constraint constraint2 = new Constraint(null, "PolicyDAOTest new constraint 2", LogicalOperator.AND);
    constraint2.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    policy2.addConstraint(constraint2);
    policyDAO.insert(applicationId, policy2);

    policies = policyDAO.getByOwnerId(applicationId);
    Assert.assertNotNull(policies);
    Assert.assertEquals(2, policies.size());
    assertPolicy(policy1, policies.get(0));
    policy2.setOwnerId(applicationId);
    assertPolicy(policy2, policies.get(1));

    // Update a policy
    policy1.setName("PolicyDAOTest updated policy 1");
    policyDAO.update(applicationId, policy1);

    policies = policyDAO.getByOwnerId(applicationId);
    Assert.assertNotNull(policies);
    Assert.assertEquals(2, policies.size());
    assertPolicy(policy1, policies.get(0));
    assertPolicy(policy2, policies.get(1));

    // Update another policy
    policy2.setName("PolicyDAOTest updated policy 2");
    policyDAO.update(applicationId, policy2);

    policies = policyDAO.getByOwnerId(applicationId);
    Assert.assertNotNull(policies);
    Assert.assertEquals(2, policies.size());
    assertPolicy(policy1, policies.get(0));
    assertPolicy(policy2, policies.get(1));

    // Delete a policy
    policyDAO.delete(applicationId, policy1.getId());

    policies = policyDAO.getByOwnerId(applicationId);
    Assert.assertNotNull(policies);
    Assert.assertEquals(1, policies.size());
    assertPolicy(policy2, policies.get(0));

    // Delete another policy
    policyDAO.delete(applicationId, policy2.getId());

    policies = policyDAO.getByOwnerId(applicationId);
    Assert.assertNotNull(policies);
    Assert.assertEquals(0, policies.size());
  }

  @Test
  public void testValidateOnInsert() throws Exception {
    File dataStoreDir = tempDir.newFolder("PolicyDAOTest");
    PolicyDAO policyDAO = new PolicyDAO(dataStoreDir);
    String applicationId = "PolicyDAOTest_AppId";

    // Policy without name
    Policy policy = new Policy();
    Constraint constraint1 = new Constraint("Constraint Id", "Constraint Name", LogicalOperator.AND);
    constraint1.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    policy.addConstraint(constraint1);
    try {
      policyDAO.insert(applicationId, policy);
      Assert.fail("Expected InvalidPolicyException");
    }
    catch (InvalidPolicyException expected) {
    }
  }

  @Test
  public void testValidateOnUpdate() throws Exception {
    File dataStoreDir = tempDir.newFolder("PolicyDAOTest");
    PolicyDAO policyDAO = new PolicyDAO(dataStoreDir);
    String applicationId = "PolicyDAOTest_AppId";

    // Add a policy
    Policy policy = new Policy();
    policy.setName("PolicyDAOTest Policy Name");
    Constraint constraint1 = new Constraint("Constraint Id", "Constraint Name", LogicalOperator.AND);
    constraint1.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    policy.addConstraint(constraint1);
    policyDAO.insert(applicationId, policy);

    // Update the policy
    policy.setName(null);
    try {
      policyDAO.update(applicationId, policy);
      Assert.fail("Expected InvalidPolicyException");
    }
    catch (InvalidPolicyException expected) {
    }
  }

  @Test
  public void testDeleteAllApplicationPolicies() throws Exception {
    final File dataStoreDir = tempDir.newFolder();
    final PolicyDAO policyDAO = new PolicyDAO(dataStoreDir);
    final String applicationId = "PolicyDAOTest_BulkDelete";
    final File policyDir = policyDAO.getPolicyDir(applicationId);

    final Policy policy1 = new Policy();
    policy1.setName("PolicyDAOTest new policy 1");
    final Constraint constraint1 = new Constraint(null, "PolicyDAOTest new constraint 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    policy1.addConstraint(constraint1);
    policyDAO.insert(applicationId, policy1);
    Assert.assertEquals(1, policyDAO.getByOwnerId(applicationId).size());
    Assert.assertEquals(true, policyDir.isDirectory());

    policyDAO.deleteByOwnerId(applicationId);
    Assert.assertEquals(0, policyDAO.getByOwnerId(applicationId).size());
    Assert.assertEquals(false, policyDir.exists());
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

  private Policy newPolicy(String name) {
    Policy policy = new Policy();
    policy.setName(name);
    Constraint constraint = new Constraint(null, "Contraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    policy.addConstraint(constraint);
    return policy;
  }

  @Test
  public void testGetApplicable_Organization() {
    String policyNameOrg = "testGetApplicableOrganization";
    Policy policyOrg = newPolicy(policyNameOrg);
    policyDAO.insert(organization.getId(), policyOrg);
    String policyNameApp = "testGetApplicableApplication";
    Policy policyApp = newPolicy(policyNameApp);
    policyDAO.insert(application.getId(), policyApp);

    List<Policy> policies = policyDAO.getApplicableByOwnerId(organization.getId());
    Assert.assertEquals(1, policies.size());
    Assert.assertEquals(policyNameOrg, policies.get(0).getName());
  }

  @Test
  public void testGetApplicable_Organization_WithTags() {
    // Must retrieve all org policies, regardless of the tags associated with them
    Policy policyOrg1 = newPolicy("policy1");
    policyDAO.insert(organization.getId(), policyOrg1);
    Policy policyOrg2 = newPolicy("policy2");
    policyDAO.insert(organization.getId(), policyOrg2);
    
    // One policy has a tag associated, the other doesn't
    Tag tag = createTag("tag name", "tag description", organization.getId());
    createPolicyTag(policyOrg1.getId(), tag.getId());

    List<Policy> policies = policyDAO.getApplicableByOwnerId(organization.getId());
    Assert.assertEquals(2, policies.size());
  }

  @Test
  public void testGetApplicable_Application() {
    String policyNameOrg = "testGetApplicableOrganization";
    Policy policyOrg = newPolicy(policyNameOrg);
    policyDAO.insert(organization.getId(), policyOrg);
    String policyNameApp = "testGetApplicableApplication";
    Policy policyApp = newPolicy(policyNameApp);
    policyDAO.insert(application.getId(), policyApp);

    List<Policy> policies = policyDAO.getApplicableByOwnerId(application.getId());
    Assert.assertEquals(2, policies.size());
    Assert.assertEquals(policyNameApp, policies.get(0).getName());
    Assert.assertEquals(policyNameOrg, policies.get(1).getName());
  }

  @Test
  public void testGetApplicable_Application_WithTags() {
    // Must retrieve only the org policies that match the tags associated with the app
    Policy policyOrg1 = newPolicy("policy1");
    policyDAO.insert(organization.getId(), policyOrg1);
    Policy policyOrg2 = newPolicy("policy2");
    policyDAO.insert(organization.getId(), policyOrg2);

    Tag tag1 = createTag("tag name 1", "tag description 1", organization.getId());
    createPolicyTag(policyOrg1.getId(), tag1.getId());
    Tag tag2 = createTag("tag name 2", "tag description 2", organization.getId());
    createPolicyTag(policyOrg1.getId(), tag2.getId());

    List<Policy> policies = policyDAO.getApplicableByOwnerId(application.getId());
    Assert.assertEquals(1, policies.size());
    Assert.assertEquals("policy2", policies.get(0).getName());
  }

  @Test
  public void testCascadeDeleteToPolicyWaivers() throws Exception {
    File dataStoreDir = tempDir.newFolder("PolicyDAOTest");
    PolicyDAO policyDAO = new PolicyDAO(dataStoreDir);
    String applicationId = "PolicyDAOTest_AppId";

    Policy policy = new Policy();
    policy.setName("PolicyDAOTest new policy 1");
    final Constraint constraint1 = new Constraint(null, "PolicyDAOTest new constraint 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    policy.addConstraint(constraint1);
    policyDAO.insert(applicationId, policy);

    PolicyWaiver policyWaiver = new PolicyWaiver("12345678901234567890", policy.getId(), "MyOwnerId", "My comment");
    PolicyWaiverDAO policyWaiverDAO = new PolicyWaiverDAO();
    policyWaiverDAO.insert(policyWaiver);
    List<PolicyWaiver> policyWaivers = policyWaiverDAO.getByPolicyId(policy.getId());
    assertEquals(1, policyWaivers.size());

    policyDAO.delete(applicationId, policy.getId());
    policyWaivers = policyWaiverDAO.getByOwnerId(policy.getId());
    assertEquals(0, policyWaivers.size());
  }
}
