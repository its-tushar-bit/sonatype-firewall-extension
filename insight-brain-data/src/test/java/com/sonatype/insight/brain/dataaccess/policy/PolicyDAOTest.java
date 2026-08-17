/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.SearchIndexChangeDAO;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.dataaccess.tag.PolicyTagDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.brain.model.SearchIndexChange.ChangeType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.InvalidPolicyException;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequest;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.repository.HostedRepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.tag.PolicyTag;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PolicyDAOTest
    extends AbstractDbDAOTest
{
  private OwnerDAO ownerDAO;

  private PolicyInternalDAO policyInternalDAO;

  private PolicyWaiverDAO policyWaiverDAO;

  private PolicyWaiverRequestDAO policyWaiverRequestDAO;

  private PolicyTagDAO policyTagDAO;

  private PolicyViolationDAO policyViolationDAO;

  private SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  private SearchIndexChangeDAO searchIndexChangeDAO;

  private PolicyDAO policyDAO;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    ownerDAO = daoFactory.createOwnerDAO();
    policyInternalDAO = daoFactory.createPolicyInternalDAO();
    policyWaiverDAO = daoFactory.createPolicyWaiverDAO();
    policyWaiverRequestDAO = daoFactory.createPolicyWaiverRequestDAO();
    policyTagDAO = daoFactory.createPolicyTagDAO();
    policyViolationDAO = daoFactory.createPolicyViolationDAO();
    systemConfigurationPropertyDAO = daoFactory.createSystemConfigurationPropertyDAO();
    searchIndexChangeDAO = daoFactory.createSearchIndexChangeDAO();
    policyDAO = daoFactory.createPolicyDAO();
  }

  @Test
  public void testUpdate_PolicyDoesNotExist() {
    // Create a policy, but don't insert it
    Policy policy = new Policy();
    policy.setName("test policy");
    policy.setOwnerId(application.getId());
    policy.addConstraint(newConstraint(null, "Constraint"));
    policy.setId("yeti");

    // Update the policy
    assertThatThrownBy(() -> policyDAO.update(policy)).isInstanceOf(NotFoundException.class)
        .hasMessage("PolicyInternal with ID yeti does not exist.");
  }

  @Test
  public void testInsert_NameNotUnique() {
    // Add a policy
    String policyName = "Test Policy";
    tempEntity.newPolicy(application.getId(), policyName);

    // Add another policy with the same name
    assertThatThrownBy(() -> tempEntity.newPolicy(application.getId(), policyName))
        .isInstanceOf(InvalidPolicyException.class)
        .hasMessage("A policy with name '" + policyName + "' already exists");

    // Add another policy with a case-/whitespace-equivalent name
    assertThatThrownBy(() -> tempEntity.newPolicy(application.getId(), "testpolicy"))
        .isInstanceOf(InvalidPolicyException.class)
        .hasMessage("A policy with name '" + policyName + "' already exists");
  }

  @Test
  public void testInsert_NameClashWithChildOwnerPolicy() {
    // Add a policy at app level
    String policyName = "Test Policy App";
    tempEntity.newPolicy(application.getId(), policyName);

    Owner parentOwner = ownerDAO.getParentOwner(organization);

    // Add another policy with a case-/whitespace-equivalent name at parent owner level
    assertInsertPolicyWithDuplicateName(parentOwner.getId(), policyName, application);

    // Add a policy at org level
    policyName = "Test Policy Org";
    tempEntity.newPolicy(organization.getId(), policyName);

    // Add another policy with a case-/whitespace-equivalent name at parent owner level
    assertInsertPolicyWithDuplicateName(parentOwner.getId(), policyName, organization);
  }

  @Test
  public void testInsert_NameClashWithParentOrgPolicy() {
    // Add a policy at parent org level
    String policyName = "Test Policy";
    tempEntity.newPolicy(organization.getParentOrganizationId(), policyName);

    Owner expectedOwner = ownerDAO.getParentOwner(organization);

    // Add another policy with a case-/whitespace-equivalent name at app level
    assertInsertPolicyWithDuplicateName(application.getId(), policyName, expectedOwner);

    // Add another policy with a case-/whitespace-equivalent name at child org level
    assertInsertPolicyWithDuplicateName(organization.getId(), policyName, expectedOwner);
  }

  @Test
  public void testInsert_NameClashWithParentRepositoryManagerPolicy() {
    // Add a policy at parent repository manager level
    String policyName = "Test Policy";
    tempEntity.newPolicy(repository.getRepositoryManagerId(), policyName);

    Owner expectedOwner = ownerDAO.getParentOwner(repository);

    // Add another policy with a case-/whitespace-equivalent name at repository level
    assertInsertPolicyWithDuplicateName(repository.getId(), policyName, expectedOwner);
  }

  private void assertInsertPolicyWithDuplicateName(String ownerId, String policyName, Owner expectedOwner) {
    // Add a policy with a case-/whitespace-equivalent name
    assertThatThrownBy(
        () -> tempEntity.newPolicy(ownerId, policyName.replaceAll("\\s", "").toLowerCase(Locale.ENGLISH)))
            .isInstanceOf(InvalidPolicyException.class)
            .hasMessage("A policy with the same name already exists for " + expectedOwner.getType() + " '" +
                expectedOwner.getName() + "'");
  }

  private void assertUpdatePolicyWithDuplicateName(Owner owner, String policyName, Owner expectedOwner) {
    Policy policy = tempEntity.newPolicy(owner);
    // Update the policy with a case-/whitespace-equivalent name
    policy.setName(policyName.replaceAll("\\s", "").toLowerCase(Locale.ENGLISH));
    assertThatThrownBy(() -> policyDAO.update(policy)).isInstanceOf(InvalidPolicyException.class)
        .hasMessage("A policy with the same name already exists for " + expectedOwner.getType() + " '" +
            expectedOwner.getName() + "'");
  }

  @Test
  public void testUpdate_NameNotUnique() {
    // Add two policies
    String policyName1 = "Test Policy 1";
    Policy policy1 = tempEntity.newPolicy(application.getId(), policyName1);
    String policyName2 = "Test Policy 2";
    tempEntity.newPolicy(application.getId(), policyName2);

    // Update a policy with the same name
    policyDAO.update(policy1);

    // Update a policy with a duplicate name
    policy1.setName(policyName2);
    assertThatThrownBy(() -> policyDAO.update(policy1)).isInstanceOf(InvalidPolicyException.class)
        .hasMessage("A policy with name '" + policyName2 + "' already exists");

    // Update a policy with a case-/whitespace-equivalent name
    policy1.setName(policyName2.replace("\\s", "").toLowerCase(Locale.ENGLISH));
    assertThatThrownBy(() -> policyDAO.update(policy1)).isInstanceOf(InvalidPolicyException.class)
        .hasMessage("A policy with name '" + policyName2 + "' already exists");
  }

  @Test
  public void testUpdate_NameClashWithParentOrgPolicy() {
    // Add a policy at parent org level
    String policyName = "Test Policy";
    tempEntity.newPolicy(organization.getParentOrganizationId(), policyName);

    Owner expectedOwner = ownerDAO.getParentOwner(organization);

    // Update another policy with a case-/whitespace-equivalent name at app level
    assertUpdatePolicyWithDuplicateName(application, policyName, expectedOwner);

    // Update another policy with a case-/whitespace-equivalent name at child org level
    assertUpdatePolicyWithDuplicateName(organization, policyName, expectedOwner);
  }

  @Test
  public void testUpdate_NameClashWithParentRepositoryManagerPolicy() {
    // Add a policy at parent repository manager level
    String policyName = "Test Policy";
    tempEntity.newPolicy(repository.getParentOwnerId(), policyName);

    Owner expectedOwner = ownerDAO.getParentOwner(repository);

    // Update another policy with a case-/whitespace-equivalent name at repository level
    assertUpdatePolicyWithDuplicateName(repository, policyName, expectedOwner);
  }

  @Test
  public void testUpdate_NameClashWithChildOwnerPolicy() {
    // Add a policy at app level
    String policyName = "Test Policy App";
    tempEntity.newPolicy(application.getId(), policyName);

    Owner parentOwner = ownerDAO.getParentOwner(organization);

    // Add another policy with a case-/whitespace-equivalent name at parent owner level
    assertUpdatePolicyWithDuplicateName(parentOwner, policyName, application);

    // Add a policy at org level
    policyName = "Test Policy Org";
    tempEntity.newPolicy(organization.getId(), policyName);

    // Add another policy with a case-/whitespace-equivalent name at parent owner level
    assertUpdatePolicyWithDuplicateName(parentOwner, policyName, organization);
  }

  @Test
  public void testInsert_GeneratesConstraintIds() {
    Policy policy = new Policy();
    policy.setName("Test Policy");
    policy.setOwnerId(application.getId());

    Constraint constraint1 = newConstraint(null, "Constraint without ID");
    policy.addConstraint(constraint1);
    assertThat(constraint1.getId()).isNull();

    String constraintId = "foo id";
    Constraint constraint2 = newConstraint(constraintId, "Constraint with ID");
    policy.addConstraint(constraint2);
    assertThat(constraint2.getId()).isEqualTo(constraintId);

    policyDAO.insert(policy);
    assertThat(constraint1.getId()).isNotNull();
    assertThat(constraint2.getId()).isNotNull();
    assertThat(constraint2.getId()).isNotEqualTo(constraintId);

    List<Policy> policies = policyDAO.getByOwnerId(application.getId());
    assertThat(policies).hasSize(1);
    assertPolicy(policy, policies.get(0));
  }

  @Test
  public void testUpdate_GeneratesConstraintIds() {
    // Add a policy
    Policy policy = tempEntity.newPolicy(application);

    // Update the policy
    Constraint constraint1 = newConstraint(null, "Constraint without ID");
    policy.addConstraint(constraint1);
    assertThat(constraint1.getId()).isNull();

    String constraintId = "foo id";
    Constraint constraint2 = newConstraint(constraintId, "Constraint with ID");
    policy.addConstraint(constraint2);
    assertThat(constraint2.getId()).isEqualTo(constraintId);

    policyDAO.update(policy);
    assertThat(constraint1.getId()).isNotNull();
    assertThat(constraint2.getId()).isNotNull();
    assertThat(constraint2.getId()).isNotEqualTo(constraintId);

    List<Policy> policies = policyDAO.getByOwnerId(application.getId());
    assertThat(policies).hasSize(1);
    assertPolicy(policy, policies.get(0));
  }

  @Test
  public void testCRUD() {
    // Add
    Policy policy = tempEntity.newPolicy(application.getId());

    // Get
    List<Policy> policies = policyDAO.getByOwnerId(application.getId());
    assertThat(policies).hasSize(1);
    assertPolicy(policy, policies.get(0));

    // Update
    policy.setName("Test Policy updated");
    policyDAO.update(policy);

    policies = policyDAO.getByOwnerId(application.getId());
    assertThat(policies).hasSize(1);
    assertPolicy(policy, policies.get(0));

    // Get
    policies = policyDAO.getByOwnerId(application.getId());
    assertThat(policies).hasSize(1);
    assertPolicy(policy, policies.get(0));

    // Delete
    policyDAO.delete(policy);

    // Get
    policies = policyDAO.getByOwnerId(application.getId());
    assertThat(policies).isEmpty();
  }

  @Test
  public void testInsert_NullName() {
    String name = null;
    assertThatThrownBy(() -> tempEntity.newPolicy(application.getId(), name)).isInstanceOf(InvalidPolicyException.class)
        .hasMessage("The policy name is required.");
  }

  @Test
  public void testUpdate_NullName() {
    // Add a policy
    Policy policy = tempEntity.newPolicy(application.getId());

    // Update the policy
    policy.setName(null);
    assertThatThrownBy(() -> policyDAO.update(policy)).isInstanceOf(InvalidPolicyException.class);
  }

  @Test
  public void testDeleteByOwnerId() {
    tempEntity.newPolicy(application.getId());
    assertThat(policyDAO.getByOwnerId(application.getId())).hasSize(1);

    try (TransactionContext tx = policyInternalDAO.createTransactionContext()) {
      tx.begin();
      policyDAO.deleteByOwnerId(tx, application.getId());
      tx.commit();
    }
    assertThat(policyDAO.getByOwnerId(application.getId())).isEmpty();
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

  private Constraint newConstraint(String constraintId, String constraintName) {
    Constraint constraint = new Constraint(constraintId, constraintName, LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    return constraint;
  }

  @Test
  public void testGetApplicableByOwnerIdWithHierarchy() {
    String policyNameRootOrg = "RootOrganizationPolicy";
    tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, policyNameRootOrg);
    String policyNameOrg = "OrganizationPolicy";
    tempEntity.newPolicy(organization.getId(), policyNameOrg);
    String policyNameApp = "ApplicationPolicy";
    tempEntity.newPolicy(application.getId(), policyNameApp);
    String policyNameRepoContainer = "RepositoryContainerPolicy";
    tempEntity.newPolicy(RepositoryContainer.REPOSITORY_CONTAINER_ID, policyNameRepoContainer);
    String policyNameRepoManager = "RepositoryManagerPolicy";
    tempEntity.newPolicy(repositoryManager.getId(), policyNameRepoManager);
    String policyNameRepo = "RepositoryPolicy";
    tempEntity.newPolicy(repository.getId(), policyNameRepo);

    // Check app level
    List<Policy> policies = policyDAO.getApplicableByOwnerIdWithHierarchy(application.getId());
    assertThat(policies).extracting(Policy::getName).containsExactly(policyNameApp, policyNameOrg, policyNameRootOrg);

    // Check repo level
    policies = policyDAO.getApplicableByOwnerIdWithHierarchy(repository.getId());
    assertThat(policies).extracting(Policy::getName)
        .containsExactlyInAnyOrder(policyNameRepo, policyNameRepoManager, policyNameRepoContainer, policyNameRootOrg);

    // Check repo manager level
    policies = policyDAO.getApplicableByOwnerIdWithHierarchy(repositoryManager.getId());
    assertThat(policies).extracting(Policy::getName)
        .containsExactlyInAnyOrder(policyNameRepoManager, policyNameRepoContainer, policyNameRootOrg);

    // Check org level
    policies = policyDAO.getApplicableByOwnerIdWithHierarchy(organization.getId());
    assertThat(policies).extracting(Policy::getName).containsExactlyInAnyOrder(policyNameOrg, policyNameRootOrg);

    // Check root org level
    policies = policyDAO.getApplicableByOwnerIdWithHierarchy(Organization.ROOT_ORGANIZATION_ID);
    assertThat(policies).extracting(Policy::getName).containsExactlyInAnyOrder(policyNameRootOrg);
  }

  // does not apply to apps with no tags
  // does apply to policy with no tags
  @Test
  public void testGetApplicableByOwnerIdWithHierarchy_ReturnsPoliciesIfAnyAppTagMatchesAnyPolicyTag() {
    final var appWithNoTags = tempEntity.newApplication(
        "appWithNoTags",
        "appWithNoTags-AppPublicId",
        organization.getId());

    final var appWithTag1 = tempEntity.newApplication(
        "appWithTag1",
        "appWithTag1-AppPublicId",
        organization.getId());

    final var appWithTag2 = tempEntity.newApplication(
        "appWithTag2",
        "appWithTag2-AppPublicId",
        organization.getId());

    final var tag1 = tempEntity.newTag(organization.getId());
    final var tag2 = tempEntity.newTag(organization.getId());

    // given the policy has two tags associated
    final var policyWithTwoTags = tempEntity.newPolicy(organization.getId(), "policyWithMultipleTags");
    tempEntity.newPolicyTag(policyWithTwoTags.getId(), tag1.getId());
    tempEntity.newPolicyTag(policyWithTwoTags.getId(), tag2.getId());

    // associate the tags to apps
    tempEntity.newApplicationTag(appWithTag1.getId(), tag1.getId());
    tempEntity.newApplicationTag(appWithTag2.getId(), tag2.getId());

    // should return the policy for an app that has at least one of the tags applied
    var results = policyDAO.getApplicableByOwnerIdWithHierarchy(appWithTag1.getId());
    assertThat(results).extracting(Policy::getName).containsExactly("policyWithMultipleTags");
    results = policyDAO.getApplicableByOwnerIdWithHierarchy(appWithTag2.getId());
    assertThat(results).extracting(Policy::getName).containsExactly("policyWithMultipleTags");

    // should not return the policy if an app has none of the tags
    results = policyDAO.getApplicableByOwnerIdWithHierarchy(appWithNoTags.getId());
    assertThat(results).extracting(Policy::getName).isEmpty();
  }

  @Test
  public void testGetApplicableByOwnerIdWithHierarchy_WithTags() {
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
    List<Policy> policies = policyDAO.getApplicableByOwnerIdWithHierarchy(application.getId());
    assertThat(policies).extracting(Policy::getName).containsExactly("policyOrg2", "policyRootOrg2");

    // For repositories, must retrieve only the org policies that don't have any tags
    policies = policyDAO.getApplicableByOwnerIdWithHierarchy(repository.getId());
    assertThat(policies).isEmpty();

    // For repository managers, must retrieve only the org policies that don't have any tags
    policies = policyDAO.getApplicableByOwnerIdWithHierarchy(repositoryManager.getId());
    assertThat(policies).isEmpty();

    // For repository containers, must retrieve only the org policies that don't have any tags
    policies = policyDAO.getApplicableByOwnerIdWithHierarchy(RepositoryContainer.SINGLETON.getId());
    assertThat(policies).isEmpty();

    // For orgs, must retrieve all org policies, regardless of the tags associated with them
    policies = policyDAO.getApplicableByOwnerIdWithHierarchy(organization.getId());
    assertThat(policies).extracting(Policy::getName)
        .containsExactlyInAnyOrder("policyOrg1", "policyOrg2",
            "policyRootOrg1", "policyRootOrg2");
  }

  // HRC owners follow the repository-family filter: every untagged policy in the ancestor
  // chain applies. Policy tags are application-scoped and HRC has no tag axis, so tagged
  // ancestor policies are excluded (see the sibling _HrcWithTags test).
  @Test
  public void testGetApplicableByOwnerIdWithHierarchy_HrcHierarchy() {
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repository);

    String policyNameRootOrg = "RootOrganizationPolicy";
    tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, policyNameRootOrg);
    String policyNameRepoContainer = "RepositoryContainerPolicy";
    tempEntity.newPolicy(RepositoryContainer.REPOSITORY_CONTAINER_ID, policyNameRepoContainer);
    String policyNameRepoManager = "RepositoryManagerPolicy";
    tempEntity.newPolicy(repositoryManager.getId(), policyNameRepoManager);
    String policyNameRepo = "RepositoryPolicy";
    tempEntity.newPolicy(repository.getId(), policyNameRepo);
    String policyNameHrc = "HrcPolicy";
    tempEntity.newPolicy(hrc.getId(), policyNameHrc);

    List<Policy> policies = policyDAO.getApplicableByOwnerIdWithHierarchy(hrc.getId());
    assertThat(policies).extracting(Policy::getName)
        .containsExactlyInAnyOrder(policyNameHrc, policyNameRepo, policyNameRepoManager,
            policyNameRepoContainer, policyNameRootOrg);
  }

  // HRC owners follow the repository-family filter: only untagged ancestor policies apply.
  // Policy tags are application-scoped; HRC has no tag axis.
  @Test
  public void testGetApplicableByOwnerIdWithHierarchy_HrcWithTags() {
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repository);

    Policy policyRootOrgUntagged = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "policyRootOrgUntagged");
    Policy policyRootOrgTagged = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "policyRootOrgTagged");

    Tag tag = tempEntity.newTag(organization.getId());
    tempEntity.newPolicyTag(policyRootOrgTagged.getId(), tag.getId());

    List<Policy> policies = policyDAO.getApplicableByOwnerIdWithHierarchy(hrc.getId());
    assertThat(policies).extracting(Policy::getName).containsExactly("policyRootOrgUntagged");
  }

  // Policies attached directly to an HRC apply regardless of tags: isDirectlyAttached
  // bypasses the tag/hierarchy filters, matching the behavior every other owner type gets
  // for self-attached policies.
  @Test
  public void testGetApplicableByOwnerIdWithHierarchy_HrcDirectlyAttachedPolicyWithTagsStillIncluded() {
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repository);

    Policy directlyAttached = tempEntity.newPolicy(hrc.getId(), "directlyAttachedHrcPolicy");
    Tag tag = tempEntity.newTag(organization.getId());
    tempEntity.newPolicyTag(directlyAttached.getId(), tag.getId());

    List<Policy> policies = policyDAO.getApplicableByOwnerIdWithHierarchy(hrc.getId());
    assertThat(policies).extracting(Policy::getName).containsExactly("directlyAttachedHrcPolicy");
  }

  @Test
  public void testGetApplicableByOwnerIdWithHierarchy_UnknownOwnerId() {
    assertThat(policyDAO.getApplicableByOwnerIdWithHierarchy("unknown-owner-id")).isEmpty();
  }

  @Test
  public void testDelete_CascadesToPolicyWaivers() {
    Policy policy = tempEntity.newPolicy(application.getId());

    tempEntity.newWaiver(policy.getId(), "ownerId");
    List<PolicyWaiver> policyWaivers = policyWaiverDAO.getByPolicyId(policy.getId());
    assertThat(policyWaivers).hasSize(1);

    policyDAO.delete(policy);
    policyWaivers = policyWaiverDAO.getByPolicyId(policy.getId());
    assertThat(policyWaivers).isEmpty();
  }

  @Test
  public void testDelete_CascadesToPolicyWaiverRequests() {
    Policy policy = tempEntity.newPolicy(application.getId());

    tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setPolicyId(policy.getId())
        .setOwnerId("ownerId")
        .setPolicyViolationId("policyViolationId"));
    List<PolicyWaiverRequest> policyWaiverRequests = policyWaiverRequestDAO.getByPolicyId(policy.getId());
    assertThat(policyWaiverRequests).hasSize(1);

    policyDAO.delete(policy);
    policyWaiverRequests = policyWaiverRequestDAO.getByPolicyId(policy.getId());
    assertThat(policyWaiverRequests).isEmpty();
  }

  @Test
  public void testDelete_CascadesToPolicyTags() {
    Policy policy = tempEntity.newPolicy(application.getId());

    Tag tag = tempEntity.newTag(organization.getId());
    tempEntity.newPolicyTag(policy.getId(), tag.getId());
    List<PolicyTag> policyTags = policyTagDAO.getByPolicyId(policy.getId());
    assertThat(policyTags).hasSize(1);

    policyDAO.delete(policy);
    policyTags = policyTagDAO.getByPolicyId(policy.getId());
    assertThat(policyTags).isEmpty();
  }

  @Test
  public void testDelete_DoesNotCascadeToPolicyViolations() {
    Policy policy = tempEntity.newPolicy(application);
    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID, "scanid");
    tempEntity.newPolicyViolation(policyEvaluation, policy);
    assertThat(policyViolationDAO.getByOwnerId(policyEvaluation.getOwnerId())).hasSize(1);

    policyDAO.delete(policy);
    assertThat(policyViolationDAO.getByOwnerId(policyEvaluation.getOwnerId())).hasSize(1);
  }

  @Test
  public void testGetByIds() {
    Policy policy1 = tempEntity.newPolicy(application);
    Policy policy2 = tempEntity.newPolicy(organization);
    tempEntity.newPolicy(application);

    assertThat(policyDAO.getByIds(Collections.emptySet())).isEmpty();

    assertThat(policyDAO.getByIds(Arrays.asList(policy1.getId(), policy2.getId(), "non-existent")))
        .extracting(Policy::getId)
        .containsExactlyInAnyOrder(policy1.getId(), policy2.getId());
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

    policies = policyDAO.getByOwnerIds(Set.of(application.getId(), "non-existent"));
    assertThat(policies).extracting(Policy::getId).containsExactly(appPolicy.getId());
  }

  @Test
  public void selectCountByOwnerIds_nullCountsGlobalPolicyPopulation() {
    long before = policyDAO.selectCountByOwnerIds(null);
    tempEntity.newPolicy(application);

    assertThat(policyDAO.selectCountByOwnerIds(null)).isEqualTo(before + 1);
  }

  @Test
  public void selectCountByOwnerIds_emptyReturnsZero() {
    assertThat(policyDAO.selectCountByOwnerIds(Set.of())).isZero();
  }

  @Test
  public void selectCountByOwnerIds_countsDirectOrgAndAppOwnershipOnly() {
    Policy organizationPolicy = tempEntity.newPolicy(organization);
    Policy applicationPolicy = tempEntity.newPolicy(application);

    assertThat(policyDAO.selectCountByOwnerIds(Set.of(organization.getId(), application.getId()))).isEqualTo(2);
    assertThat(Set.of(organizationPolicy.getId(), applicationPolicy.getId())).hasSize(2);
  }

  @Test
  public void selectCountByOwnerIds_doesNotCountHierarchyApplicablePolicies() {
    Policy inheritedPolicy = tempEntity.newPolicy(organization);

    assertThat(policyDAO.getApplicableByOwnerIdWithHierarchy(application.getId()))
        .extracting(Policy::getId)
        .contains(inheritedPolicy.getId());
    assertThat(policyDAO.selectCountByOwnerIds(Set.of(application.getId()))).isZero();
  }

  @Test
  public void selectCountByOwnerIds_sumsDisjointChunks() {
    Policy first = tempEntity.newPolicy(application);
    Policy second = tempEntity.newPolicy(organization);
    Set<String> ownerIds = new HashSet<>();
    ownerIds.add(application.getId());
    ownerIds.add(organization.getId());
    for (int i = 0; i < AbstractOperationalSqlDAO.H2_IN_OPERATOR_THRESHOLD; i++) {
      ownerIds.add("missing-owner-" + i);
    }

    assertThat(policyDAO.selectCountByOwnerIds(ownerIds)).isEqualTo(2);
    assertThat(Set.of(first.getId(), second.getId())).hasSize(2);
  }

  private void testGetByOwnerIds_internal() {
    Organization organization = tempEntity.newOrganization("TempOrg");
    Application application = tempEntity.newApplication("TempAppName", "TempAppPublicId",
        organization.getId());
    Policy appPolicy = tempEntity.newPolicy(application);
    tempEntity.newPolicy(organization);
    List<Policy> policies;

    Set<String> largeIdList = new HashSet<>();
    for (int i = 0; i < AbstractOperationalSqlDAO.POSTGRES_IN_OPERATOR_THRESHOLD; i++) {
      largeIdList.add(Integer.toString(i));
    }
    largeIdList.add(application.getId());

    policies = policyDAO.getByOwnerIds(largeIdList);
    assertThat(policies).extracting(Policy::getId).containsExactly(appPolicy.getId());
  }

  @Test
  public void testUpdate_MovePolicyUpInHierarchy() {
    Policy policy = tempEntity.newPolicy(application);

    // Should not complain about name clashes
    policy.setOwnerId(application.getOrganizationId());
    policyDAO.update(policy);
  }

  @Test
  public void testGetByOwnerIdAndName() {
    Policy policy1 = tempEntity.newPolicy(application.getId(), "Policy 1");
    Policy policy2 = tempEntity.newPolicy(organization.getId(), "Policy 2");
    tempEntity.newPolicy(organization.getId());

    try (TransactionContext tx = policyInternalDAO.createTransactionContext()) {
      Policy policy = policyDAO.getByOwnerIdAndName(tx, application.getId(), "policy1");
      assertThat(policy).isNotNull();
      assertThat(policy.getId()).isEqualTo(policy1.getId());

      policy = policyDAO.getByOwnerIdAndName(tx, organization.getId(), "policy2");
      assertThat(policy).isNotNull();
      assertThat(policy.getId()).isEqualTo(policy2.getId());
    }
  }

  @Test
  public void testCRUD_RecordSearchIndexChange() {
    systemConfigurationPropertyDAO
        .update(new SystemConfigurationProperty(SystemConfigurationProperty.ADVANCED_SEARCH_ENABLED, "true"));
    Policy policy = tempEntity.newPolicy(application);

    List<SearchIndexChange> searchIndexChanges = searchIndexChangeDAO.getAll();
    assertThat(searchIndexChanges).hasSize(1);
    assertThat(searchIndexChanges.get(0).getChangeType()).isEqualTo(ChangeType.POLICY);
    assertThat(searchIndexChanges.get(0).getChangeData()).isEqualTo(policy.getId());
    searchIndexChangeDAO.delete(searchIndexChanges.get(0));

    policyDAO.update(policy);
    searchIndexChanges = searchIndexChangeDAO.getAll();
    assertThat(searchIndexChanges).hasSize(1);
    assertThat(searchIndexChanges.get(0).getChangeType()).isEqualTo(ChangeType.POLICY);
    assertThat(searchIndexChanges.get(0).getChangeData()).isEqualTo(policy.getId());
    searchIndexChangeDAO.delete(searchIndexChanges.get(0));

    policyDAO.delete(policy);
    searchIndexChanges = searchIndexChangeDAO.getAll();
    assertThat(searchIndexChanges).hasSize(1);
    assertThat(searchIndexChanges.get(0).getChangeType()).isEqualTo(ChangeType.POLICY);
    assertThat(searchIndexChanges.get(0).getChangeData()).isEqualTo(policy.getId());
  }
}
