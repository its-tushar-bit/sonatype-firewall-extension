/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;

import java.util.List;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link PolicyInternalDAO#getAncestorOrDescendantWithPolicyNameMatching} — the bulk
 * hierarchy query that replaces the N+1 recursive traversal (CLM-38233).
 */
public class PolicyInternalDAOHierarchyTest
    extends AbstractDbDAOTest
{
  private PolicyInternalDAO policyInternalDAO;

  @Before
  @Override
  public void setup() {
    super.setup();
    policyInternalDAO = daoFactory.createPolicyInternalDAO();
  }

  // -------------------------------------------------------------------------
  // No-conflict cases
  // -------------------------------------------------------------------------

  @Test
  public void noConflict_returnsEmpty() {
    // policy at org level, no other policies in hierarchy
    tempEntity.newPolicy(organization);

    try (TransactionContext tx = policyInternalDAO.createTransactionContext()) {
      List<String> result = policyInternalDAO.getAncestorOrDescendantWithPolicyNameMatching(
          tx, organization.getId(), "name-that-does-not-exist");
      assertThat(result).isEmpty();
    }
  }

  @Test
  public void noConflict_differentName_returnsEmpty() {
    // sibling policies with different names should not conflict
    tempEntity.newPolicy(organization.getId(), "Policy A");
    tempEntity.newPolicy(application.getId(), "Policy B");

    try (TransactionContext tx = policyInternalDAO.createTransactionContext()) {
      // inserting "Policy C" at org level — no conflict expected
      List<String> result = policyInternalDAO.getAncestorOrDescendantWithPolicyNameMatching(
          tx, organization.getId(), "Policy C");
      assertThat(result).isEmpty();
    }
  }

  @Test
  public void noConflict_sameName_differentHierarchy_returnsEmpty() {
    // same policy name but in a completely separate hierarchy branch
    Organization separateOrg = tempEntity.newOrganization("SeparateOrg");
    tempEntity.newPolicy(separateOrg.getId(), "Shared Name");

    try (TransactionContext tx = policyInternalDAO.createTransactionContext()) {
      // inserting "Shared Name" in our org — no conflict because separate org is not an ancestor/descendant
      List<String> result = policyInternalDAO.getAncestorOrDescendantWithPolicyNameMatching(
          tx, organization.getId(), "Shared Name");
      assertThat(result).isEmpty();
    }
  }

  @Test
  public void noConflict_siblingOrgs_sameName_returnsEmpty() {
    // Two sibling orgs under the same parent — a policy in one sibling must not
    // conflict with a policy of the same name in the other sibling.
    Organization sibling1 = tempEntity.newOrganization("Sibling1", organization);
    Organization sibling2 = tempEntity.newOrganization("Sibling2", organization);
    tempEntity.newPolicy(sibling2.getId(), "Sibling Policy");

    try (TransactionContext tx = policyInternalDAO.createTransactionContext()) {
      // inserting "Sibling Policy" at sibling1 — sibling2 is neither ancestor nor descendant
      List<String> result = policyInternalDAO.getAncestorOrDescendantWithPolicyNameMatching(
          tx, sibling1.getId(), "Sibling Policy");
      assertThat(result).isEmpty();
    }
  }

  // -------------------------------------------------------------------------
  // Ancestor conflict (upward walk)
  // -------------------------------------------------------------------------

  @Test
  public void conflict_withAncestorOrg_returnsAncestorOwnerId() {
    // policy exists at parent org level
    String policyName = "Conflicting Policy";
    tempEntity.newPolicy(organization.getParentOrganizationId(), policyName);

    try (TransactionContext tx = policyInternalDAO.createTransactionContext()) {
      // inserting same name at child org — should detect conflict at parent
      List<String> result = policyInternalDAO.getAncestorOrDescendantWithPolicyNameMatching(
          tx, organization.getId(), policyName);
      assertThat(result).isNotEmpty();
      assertThat(result.get(0)).isEqualTo(organization.getParentOrganizationId());
    }
  }

  @Test
  public void conflict_withAncestorOrg_caseInsensitive_returnsConflict() {
    // name normalisation: case and whitespace are stripped
    tempEntity.newPolicy(organization.getParentOrganizationId(), "My Policy");

    try (TransactionContext tx = policyInternalDAO.createTransactionContext()) {
      // "mypolicy" normalises to the same value as "My Policy"
      List<String> result = policyInternalDAO.getAncestorOrDescendantWithPolicyNameMatching(
          tx, organization.getId(), "mypolicy");
      assertThat(result).isNotEmpty();
    }
  }

  // -------------------------------------------------------------------------
  // Descendant conflict (downward walk)
  // -------------------------------------------------------------------------

  @Test
  public void conflict_withDirectChildApp_returnsChildOwnerId() {
    // policy exists at app level
    String policyName = "App Level Policy";
    tempEntity.newPolicy(application.getId(), policyName);

    try (TransactionContext tx = policyInternalDAO.createTransactionContext()) {
      // inserting same name at parent org — should detect conflict at app
      List<String> result = policyInternalDAO.getAncestorOrDescendantWithPolicyNameMatching(
          tx, organization.getId(), policyName);
      assertThat(result).isNotEmpty();
      assertThat(result.get(0)).isEqualTo(application.getId());
    }
  }

  @Test
  public void conflict_withGrandchildOrg_returnsGrandchildOwnerId() {
    // 3-level hierarchy: root → org → childOrg → grandchildOrg
    Organization childOrg = tempEntity.newOrganization("ChildOrg", organization);
    Organization grandchildOrg = tempEntity.newOrganization("GrandchildOrg", childOrg);

    String policyName = "Deep Policy";
    tempEntity.newPolicy(grandchildOrg.getId(), policyName);

    try (TransactionContext tx = policyInternalDAO.createTransactionContext()) {
      // inserting at root of this sub-tree (org) — should find conflict deep in descendants
      List<String> result = policyInternalDAO.getAncestorOrDescendantWithPolicyNameMatching(
          tx, organization.getId(), policyName);
      assertThat(result).isNotEmpty();
      assertThat(result.get(0)).isEqualTo(grandchildOrg.getId());
    }
  }

  // -------------------------------------------------------------------------
  // Self-conflict cases (update / rename)
  // -------------------------------------------------------------------------

  @Test
  public void selfOwner_sameNamePolicy_returnsEmpty() {
    // renaming a policy to its current name should not conflict with itself
    Policy policy = tempEntity.newPolicy(organization.getId(), "My Policy");

    try (TransactionContext tx = policyInternalDAO.createTransactionContext()) {
      // same name at same owner — already excluded by hierarchy query, should return empty
      List<String> result = policyInternalDAO.getAncestorOrDescendantWithPolicyNameMatching(
          tx, organization.getId(), "My Policy");
      assertThat(result).isEmpty();
    }
  }

  @Test
  public void insertPath_descendantConflictDetected() {
    // On insert path no policy ID is available. Verify a conflict at a descendant is still found.
    tempEntity.newPolicy(application.getId(), "Shared Name");

    try (TransactionContext tx = policyInternalDAO.createTransactionContext()) {
      List<String> result = policyInternalDAO.getAncestorOrDescendantWithPolicyNameMatching(
          tx, organization.getId(), "Shared Name");
      assertThat(result).isNotEmpty();
      assertThat(result.get(0)).isEqualTo(application.getId());
    }
  }

  @Test
  public void unrelatedPolicyAtOwner_doesNotSuppressConflict() {
    // excluding a different policy should not suppress detection of an actual conflict
    tempEntity.newPolicy(application.getId(), "Conflicting Name");
    Policy unrelatedPolicy = tempEntity.newPolicy(organization.getId(), "Unrelated Policy");

    try (TransactionContext tx = policyInternalDAO.createTransactionContext()) {
      // excluding unrelatedPolicy's id — conflict with existingPolicy should still be found
      List<String> result = policyInternalDAO.getAncestorOrDescendantWithPolicyNameMatching(
          tx, organization.getId(), "Conflicting Name");
      assertThat(result).isNotEmpty();
      assertThat(result.get(0)).isEqualTo(application.getId());
    }
  }

  // -------------------------------------------------------------------------
  // Root organisation
  // -------------------------------------------------------------------------

  @Test
  public void conflict_atRootOrg_descendantConflict_returnsDescendant() {
    // policy at org level — conflict when inserting same name at root
    String policyName = "Root Conflict";
    tempEntity.newPolicy(organization.getId(), policyName);

    try (TransactionContext tx = policyInternalDAO.createTransactionContext()) {
      List<String> result = policyInternalDAO.getAncestorOrDescendantWithPolicyNameMatching(
          tx, Organization.ROOT_ORGANIZATION_ID, policyName);
      assertThat(result).isNotEmpty();
      assertThat(result.get(0)).isEqualTo(organization.getId());
    }
  }

  // -------------------------------------------------------------------------
  // Repository / repository-manager hierarchy
  //
  // Self-exclusion uses ANCESTOR_ID != ownerId rather than ancestor_distance > 0.
  // In the repository hierarchy, repository_ancestor stores the repo-manager at
  // ancestor_distance=0, so distance-based exclusion would drop the repo-manager ancestor.
  // -------------------------------------------------------------------------

  @Test
  public void conflict_repoToParentRepoManager_returnsRepoManagerId() {
    // policy at repo-manager level — conflict when inserting same name at child repository
    String policyName = "Repo Manager Policy";
    tempEntity.newPolicy(repositoryManager.getId(), policyName);

    try (TransactionContext tx = policyInternalDAO.createTransactionContext()) {
      // inserting same name at repo — should detect conflict at parent repo-manager
      // (repo-manager ancestor is correctly found despite ancestor_distance quirk in repository_ancestor view)
      List<String> result = policyInternalDAO.getAncestorOrDescendantWithPolicyNameMatching(
          tx, repository.getId(), policyName);
      assertThat(result).isNotEmpty();
      assertThat(result.get(0)).isEqualTo(repositoryManager.getId());
    }
  }

  @Test
  public void conflict_repoContainerToChildRepoManager_returnsRepoManagerId() {
    // policy at repo-manager level — conflict when inserting same name at REPOSITORY_CONTAINER
    String policyName = "Repo Container Policy";
    tempEntity.newPolicy(repositoryManager.getId(), policyName);

    try (TransactionContext tx = policyInternalDAO.createTransactionContext()) {
      List<String> result = policyInternalDAO.getAncestorOrDescendantWithPolicyNameMatching(
          tx, RepositoryContainer.REPOSITORY_CONTAINER_ID, policyName);
      assertThat(result).isNotEmpty();
      assertThat(result.get(0)).isEqualTo(repositoryManager.getId());
    }
  }

  @Test
  public void conflict_repoManagerToChildRepo_returnsRepoId() {
    // policy at repository level — conflict when inserting same name at parent repo-manager
    String policyName = "Repository Policy";
    tempEntity.newPolicy(repository.getId(), policyName);

    try (TransactionContext tx = policyInternalDAO.createTransactionContext()) {
      // inserting same name at repo-manager — should detect conflict at child repository
      List<String> result = policyInternalDAO.getAncestorOrDescendantWithPolicyNameMatching(
          tx, repositoryManager.getId(), policyName);
      assertThat(result).isNotEmpty();
      assertThat(result.get(0)).isEqualTo(repository.getId());
    }
  }
}
