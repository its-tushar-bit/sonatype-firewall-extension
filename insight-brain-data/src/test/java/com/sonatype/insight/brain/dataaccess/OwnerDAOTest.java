/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.dataaccess.configuration.CallFlowAnalysisConfigDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityGroupDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityGroupVulnerabilityDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.configuration.CallFlowAnalysisConfig;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityGroup;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityGroupVulnerability;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.model.HasStringId;

import com.google.common.collect.Sets;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class OwnerDAOTest
    extends AbstractDbDAOTest
{
  private RepositoryManagerDAO repositoryManagerDAO;

  private ApplicationDAO applicationDAO;

  private PolicyWaiverDAO policyWaiverDAO;

  private CallFlowAnalysisConfigDAO callFlowAnalysisConfigDAO;

  private VulnerabilityGroupDAO vulnerabilityGroupDAO;

  private VulnerabilityGroupVulnerabilityDAO vulnerabilityGroupVulnerabilityDAO;

  private OrganizationDAO organizationDAO;

  private OwnerDAO ownerDAO;

  @Before
  @Override
  public void setup() {
    super.setup();
    ownerDAO = daoFactory.createOwnerDAO();
    applicationDAO = daoFactory.createApplicationDAO();
    policyWaiverDAO = daoFactory.createPolicyWaiverDAO();
    vulnerabilityGroupDAO = daoFactory.createVulnerabilityGroupDAO();
    vulnerabilityGroupVulnerabilityDAO = daoFactory.createVulnerabilityGroupVulnerabilityDAO();
    organizationDAO = daoFactory.createOrganizationDAO();
    repositoryManagerDAO = daoFactory.createRepositoryManagerDAO();
    callFlowAnalysisConfigDAO = daoFactory.createCallFlowAnalysisConfigDAO();
  }

  @Test
  public void testWalkHierarchy_Application() {
    List<String> ownersIds = new ArrayList<>();
    List<Organization> testList = tempEntity.newRelatedOrganizationsAsList(1, 3, 0);
    Application app = tempEntity.newApplicationWithParent(testList.get(0));
    for (Owner owner : ownerDAO.walkHierarchy(app)) {
      ownersIds.add(owner.getId());
    }
    assertThat(ownersIds).containsExactly(app.getId(), testList.get(0).getId(), testList.get(1).getId(),
        testList.get(2).getId(),
        organization.getParentOrganizationId());
  }

  @Test
  public void testWalkHierarchy_ApplicationId() {
    List<String> ownersIds = new ArrayList<>();
    List<Organization> testList = tempEntity.newRelatedOrganizationsAsList(1, 3, 0);
    Application app = tempEntity.newApplicationWithParent(testList.get(0));
    for (Owner owner : ownerDAO.walkHierarchy(app.getId())) {
      ownersIds.add(owner.getId());
    }
    assertThat(ownersIds).containsExactly(app.getId(), testList.get(0).getId(), testList.get(1).getId(),
        testList.get(2).getId(),
        organization.getParentOrganizationId());
  }

  @Test
  public void testWalkHierarchy_Organization() {
    List<String> ownersIds = new ArrayList<>();
    for (Owner owner : ownerDAO.walkHierarchy(organization)) {
      ownersIds.add(owner.getId());
    }
    assertThat(ownersIds).containsExactly(organization.getId(), organization.getParentOrganizationId());
  }

  @Test
  public void testWalkHierarchy_OrganizationId() {
    List<String> ownersIds = new ArrayList<>();
    for (Owner owner : ownerDAO.walkHierarchy(organization.getId())) {
      ownersIds.add(owner.getId());
    }
    assertThat(ownersIds).containsExactly(organization.getId(), organization.getParentOrganizationId());
  }

  @Test
  public void testWalkHierarchy_RepositoryContainer() {
    List<String> ownersIds = new ArrayList<>();
    for (Owner owner : ownerDAO.walkHierarchy(RepositoryContainer.SINGLETON)) {
      ownersIds.add(owner.getId());
    }
    assertThat(ownersIds).containsExactly(RepositoryContainer.REPOSITORY_CONTAINER_ID,
        Organization.ROOT_ORGANIZATION_ID);
  }

  @Test
  public void testWalkHierarchy_RepositoryContainerId() {
    List<String> ownersIds = new ArrayList<>();
    for (Owner owner : ownerDAO.walkHierarchy(RepositoryContainer.REPOSITORY_CONTAINER_ID)) {
      ownersIds.add(owner.getId());
    }
    assertThat(ownersIds).containsExactly(RepositoryContainer.REPOSITORY_CONTAINER_ID,
        Organization.ROOT_ORGANIZATION_ID);
  }

  @Test
  public void testWalkHierarchy_Repository() {
    List<String> ownersIds = new ArrayList<>();
    for (Owner owner : ownerDAO.walkHierarchy(repository)) {
      ownersIds.add(owner.getId());
    }
    assertThat(ownersIds).containsExactly(repository.getId(), repository.getRepositoryManagerId(),
        RepositoryContainer.REPOSITORY_CONTAINER_ID, Organization.ROOT_ORGANIZATION_ID);
  }

  @Test
  public void testWalkHierarchy_RepositoryId() {
    List<String> ownersIds = new ArrayList<>();
    for (Owner owner : ownerDAO.walkHierarchy(repository.getId())) {
      ownersIds.add(owner.getId());
    }
    assertThat(ownersIds).containsExactly(repository.getId(), repository.getRepositoryManagerId(),
        RepositoryContainer.REPOSITORY_CONTAINER_ID, Organization.ROOT_ORGANIZATION_ID);
  }

  @Test
  public void testWalkHierarchy_RepositoryManager() {
    RepositoryManager repoManager = repositoryManagerDAO.getById(repository.getRepositoryManagerId());
    List<String> ownersIds = new ArrayList<>();
    for (Owner owner : ownerDAO.walkHierarchy(repoManager)) {
      ownersIds.add(owner.getId());
    }
    assertThat(ownersIds).containsExactly(repoManager.getId(), RepositoryContainer.REPOSITORY_CONTAINER_ID,
        Organization.ROOT_ORGANIZATION_ID);
  }

  @Test
  public void testWalkHierarchy_RepositoryManagerId() {
    List<String> ownersIds = new ArrayList<>();
    for (Owner owner : ownerDAO.walkHierarchy(repository.getRepositoryManagerId())) {
      ownersIds.add(owner.getId());
    }
    assertThat(ownersIds).containsExactly(repository.getRepositoryManagerId(),
        RepositoryContainer.REPOSITORY_CONTAINER_ID, Organization.ROOT_ORGANIZATION_ID);
  }

  @Test
  public void testWalkChildren_SingleOrgBranch() {
    List<String> ownersIds = new ArrayList<>();
    List<Organization> testList = tempEntity.newRelatedOrganizationsAsList(1, 3, 0);
    Application app = tempEntity.newApplicationWithParent(testList.get(0));

    Organization lastBranchOrg = testList.get(testList.size() - 1);

    for (Owner owner : ownerDAO.walkChildren(lastBranchOrg)) {
      ownersIds.add(owner.getId());
    }
    // Cannot guarantee any order
    assertThat(ownersIds).containsOnly(testList.get(0).getId(), testList.get(1).getId(), app.getId());
  }

  @Test
  public void testWalkChildren_MultiOrgBranches() {
    List<String> ownersIds = new ArrayList<>();
    List<Organization> mainBranch = tempEntity.newRelatedOrganizationsAsList(1, 5, 0);
    List<Organization> secondBranch = tempEntity.newRelatedOrganizationsAsList(mainBranch.get(2), 1, 2, 0);
    List<Organization> thirdBranch = tempEntity.newRelatedOrganizationsAsList(secondBranch.get(1), 1, 4, 0);
    Application app1 = tempEntity.newApplicationWithParent(mainBranch.get(2));
    Application app2 = tempEntity.newApplicationWithParent(secondBranch.get(0));
    Application app3 = tempEntity.newApplicationWithParent(thirdBranch.get(1));

    Organization lastBranchOrg = mainBranch.get(mainBranch.size() - 1);

    for (Owner owner : ownerDAO.walkChildren(lastBranchOrg)) {
      ownersIds.add(owner.getId());
    }
    // Cannot guarantee any order
    assertThat(ownersIds).containsOnly(mainBranch.get(0).getId(), mainBranch.get(1).getId(),
        mainBranch.get(2).getId(), mainBranch.get(3).getId(), secondBranch.get(0).getId(),
        secondBranch.get(1).getId(), thirdBranch.get(0).getId(), thirdBranch.get(1).getId(),
        thirdBranch.get(2).getId(), thirdBranch.get(3).getId(), app1.getId(), app2.getId(), app3.getId());
  }

  @Test
  public void testWalkChildren_ApplicationAsRoot() {
    List<String> ownersIds = new ArrayList<>();
    Application app = tempEntity.newApplication(Organization.ROOT_ORGANIZATION_ID);
    for (Owner owner : ownerDAO.walkChildren(app)) {
      ownersIds.add(owner.getId());
    }
    assertThat(ownersIds).isEmpty();
  }

  @Test
  public void testWalkChildren_RepositoryAsRoot() {
    List<String> ownersIds = new ArrayList<>();
    Repository repo = tempEntity.newRepository();
    for (Owner owner : ownerDAO.walkChildren(repo)) {
      ownersIds.add(owner.getId());
    }
    assertThat(ownersIds).isEmpty();
  }

  @Test
  public void testWalkChildren_RepoManagerAsRoot() {
    List<String> ownersIds = new ArrayList<>();
    Repository repo = tempEntity.newRepository();
    RepositoryManager repoManager = repositoryManagerDAO.getById(repo.getRepositoryManagerId());
    for (Owner owner : ownerDAO.walkChildren(repoManager)) {
      ownersIds.add(owner.getId());
    }
    // Cannot guarantee any order
    assertThat(ownersIds).containsOnly(repo.getId());
  }

  @Test
  public void testWalkChildren_RepoContainerAsRoot() {
    List<String> ownersIds = new ArrayList<>();
    Owner repositoryContainer = ownerDAO.getById(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    Repository repo = tempEntity.newRepository();
    for (Owner owner : ownerDAO.walkChildren(repositoryContainer)) {
      ownersIds.add(owner.getId());
    }
    // Cannot guarantee any order
    assertThat(ownersIds).containsOnly(repo.getRepositoryManagerId(), repo.getId(), repository.getRepositoryManagerId(),
        repository.getId());
  }

  @Test
  public void testGetChildOwners_Application() {
    assertThat(ownerDAO.getChildOwners(application)).isEmpty();
  }

  @Test
  public void testGetChildOwners_Organization() {
    List<Organization> testList = tempEntity.newRelatedOrganizationsAsList(1, 3, 0);

    List<Owner> childOwners = ownerDAO.getChildOwners(testList.get(2));
    List<String> ids = childOwners.stream().map(HasStringId::getId).collect(Collectors.toList());
    assertThat(ids).containsExactly(testList.get(1).getId());
  }

  @Test
  public void testGetChildOwners_RootOrganization() {
    List<Owner> childOwners = ownerDAO.getChildOwners(ownerDAO.getById(Organization.ROOT_ORGANIZATION_ID));
    assertThat(childOwners).extracting(Owner::getId)
        .containsExactly(organization.getId(),
            RepositoryContainer.REPOSITORY_CONTAINER_ID);
  }

  @Test
  public void testGetChildOwners_RepositoryContainer() {
    List<Owner> childOwners = ownerDAO.getChildOwners(RepositoryContainer.SINGLETON);
    assertThat(childOwners).extracting(Owner::getId).containsExactly(repository.getRepositoryManagerId());
  }

  @Test
  public void testGetChildOwners_RepositoryManager() {
    RepositoryManager repoManager = repositoryManagerDAO.getById(repository.getRepositoryManagerId());
    List<Owner> childOwners = ownerDAO.getChildOwners(repoManager);
    assertThat(childOwners).extracting(Owner::getId).containsExactly(repository.getId());
  }

  @Test
  public void testGetChildOwners_Repository() {
    assertThat(ownerDAO.getChildOwners(repository)).isEmpty();
  }

  @Test
  public void testCascadeDelete_PolicyWaivers() {
    try (TransactionContext tx = applicationDAO.createTransactionContext()) {
      DateTime now = DateTime.now();
      Owner owner = ownerDAO.getById(organization.getId());
      Application app = tempEntity.newApplication(organization.getId());
      Policy appPolicy = tempEntity.newPolicy(app);
      tempEntity.newWaiver("noexpiry", appPolicy.getId(), owner.getId(), null, "comment", now.toDate(), null);
      tempEntity.newWaiver("expiring", appPolicy.getId(), owner.getId(), null, "comment", now.toDate(),
          now.plusHours(1).toDate());
      tempEntity.newWaiver("expired", appPolicy.getId(), owner.getId(), null, "comment", now.toDate(),
          now.minusHours(1).toDate());
      List<PolicyWaiver> policyWaivers = policyWaiverDAO.getByOwnerId(tx, owner.getId());
      assertThat(policyWaivers).hasSize(3);
      tx.begin();
      ownerDAO.cascadeDelete(tx, owner);
      tx.commit();
      policyWaivers = policyWaiverDAO.getByOwnerId(tx, owner.getId());
      assertThat(policyWaivers).isEmpty();
    }
  }

  @Test
  public void testCascadeDelete_VulnerabilityGroups() {
    try (TransactionContext tx = applicationDAO.createTransactionContext()) {
      Owner owner = ownerDAO.getById(organization.getId());
      VulnerabilityGroup vulnGroup = tempEntity.newVulnerabilityGroup("TestGroup", owner.getId());
      tempEntity.newVulnerabilityGroupVulnerability(vulnGroup.getId(), "CVE-1234");
      VulnerabilityGroup vulnGroup2 = tempEntity.newVulnerabilityGroup("TestGroup2", owner.getId());
      tempEntity.newVulnerabilityGroupVulnerability(vulnGroup2.getId(), "CVE-456");
      List<VulnerabilityGroup> vulnerabilityGroupList = vulnerabilityGroupDAO.getByOwnerId(owner.getId());
      assertThat(vulnerabilityGroupList).hasSize(2);
      tx.begin();
      ownerDAO.cascadeDelete(tx, owner);
      tx.commit();
      vulnerabilityGroupList = vulnerabilityGroupDAO.getByOwnerId(owner.getId());
      assertThat(vulnerabilityGroupList).isEmpty();
      List<VulnerabilityGroupVulnerability> vulnerabilityList =
          vulnerabilityGroupVulnerabilityDAO.getByGroupId(vulnGroup.getId());
      assertThat(vulnerabilityList).isEmpty();
      vulnerabilityList = vulnerabilityGroupVulnerabilityDAO.getByGroupId(vulnGroup2.getId());
      assertThat(vulnerabilityList).isEmpty();
    }
  }

  @Test
  public void testCascadeDelete_CallFlowAnalysisConfig() {
    try (TransactionContext tx = applicationDAO.createTransactionContext()) {
      Owner owner = ownerDAO.getById(organization.getId());
      tempEntity.newCallFlowAnalysisConfig(owner.getId(), 2);
      CallFlowAnalysisConfig callFlowAnalysisConfigs = callFlowAnalysisConfigDAO.getByOwnerId(owner.getId());
      assertThat(callFlowAnalysisConfigs).isNotNull();
      tx.begin();
      ownerDAO.cascadeDelete(tx, owner);
      tx.commit();
      callFlowAnalysisConfigs = callFlowAnalysisConfigDAO.getByOwnerId(owner.getId());
      assertThat(callFlowAnalysisConfigs).isNull();
    }
  }

  @Test
  public void testGetDescendantOrSelfApplicationIds_Application() {
    Application application = tempEntity.newApplicationWithParent();

    assertThat(ownerDAO.getDescendantOrSelfApplicationIds(application)).containsExactly(application.getId());
  }

  @Test
  public void testGetDescendantOrSelfApplicationIds_Organization_NoDescendants() {
    Organization organization = tempEntity.newOrganization();

    assertThat(ownerDAO.getDescendantOrSelfApplicationIds(organization)).isEmpty();
  }

  @Test
  public void testGetDescendantOrSelfApplicationIds_RootOrganization_NoDescendants() {
    List<Organization> testList = tempEntity.newRelatedOrganizationsAsList(1, 2, 0);
    Application app = tempEntity.newApplicationWithParent(testList.get(0));

    applicationDAO.delete(app);
    applicationDAO.delete(application);
    organizationDAO.delete(organization);
    organizationDAO.delete(testList.get(0));
    organizationDAO.delete(testList.get(1));
    Organization rootOrganization = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);

    assertThat(ownerDAO.getDescendantOrSelfApplicationIds(rootOrganization)).isEmpty();
  }

  @Test
  public void testGetDescendantOrSelfApplicationIds_RootOrganization_OnlyOrganizationDescendants() {
    applicationDAO.delete(application);
    Organization rootOrganization = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    tempEntity.newOrganization();

    assertThat(ownerDAO.getDescendantOrSelfApplicationIds(rootOrganization)).isEmpty();
  }

  @Test
  public void testGetDescendantOrSelfApplicationIds_Organization() {
    Organization organization = tempEntity.newOrganization();
    Application application1 = tempEntity.newApplication(organization.getId());
    Application application2 = tempEntity.newApplication(organization.getId());
    tempEntity.newApplicationWithParent();

    assertThat(ownerDAO.getDescendantOrSelfApplicationIds(organization))
        .containsExactlyInAnyOrder(application1.getId(), application2.getId());
  }

  @Test
  public void testGetDescendantOrSelfApplicationIds_RootOrganization() {
    Organization rootOrganization = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    Organization organization1 = tempEntity.newOrganization();
    Application application1 = tempEntity.newApplication(organization1.getId());
    Application application2 = tempEntity.newApplication(organization1.getId());
    Organization organization2 = tempEntity.newOrganization();
    Application application3 = tempEntity.newApplication(organization2.getId());
    Application application4 = tempEntity.newApplication(organization2.getId());
    tempEntity.newOrganization();

    assertThat(ownerDAO.getDescendantOrSelfApplicationIds(rootOrganization)).containsExactlyInAnyOrder(
        application1.getId(), application2.getId(), application3.getId(), application4.getId(), application.getId());
  }

  @Test
  public void testGetOwnersByAppTagsAndOrgs_shouldReturnEmptyListGivenAllIdSetsEmpty() {
    // create some apps and orgs, that won't be returned
    final var rootOrganization = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    final var organization1 = tempEntity.newOrganization("org-1", rootOrganization);
    final var organization2 = tempEntity.newOrganization("org-2", organization1);
    tempEntity.newApplication(organization1.getId());
    tempEntity.newApplication(organization2.getId());
    tempEntity.newApplication(rootOrganization.getId());

    var results = ownerDAO.getOwnersByAppTagsAndOrgs(null, null, null);
    assertThat(results).isEmpty();

    results = ownerDAO.getOwnersByAppTagsAndOrgs(Sets.newHashSet(), Sets.newHashSet(), Sets.newHashSet());
    assertThat(results).isEmpty();
  }

  @Test
  public void testGetOwnersByAppTagsAndOrgs_shouldReturnOnlyAppsAndTheirParentsGivenOnlyAppIdsSupplied() {
    final var rootOrganization = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    final var tree1Organization1 = tempEntity.newOrganization("tree-1-org-1", rootOrganization);
    final var tree1Organization2 = tempEntity.newOrganization("tree-1-org-2", tree1Organization1);

    // two apps along a tree, we should get these apps and their parents
    final var app1 = tempEntity.newApplication(tree1Organization2.getId());
    final var app2 = tempEntity.newApplication(rootOrganization.getId());

    // some apps in tree 1, that are specified by our query and should not be returned
    tempEntity.newApplication(tree1Organization1.getId());
    tempEntity.newApplication(rootOrganization.getId());

    final var results = ownerDAO.getOwnersByAppTagsAndOrgs(
        Sets.newHashSet(app1.getId(), app2.getId()),
        null,
        Sets.newHashSet());

    assertOwnersEqualInAnyOrder(
        results,
        app1,
        app2);
  }

  @Test
  public void testGetOwnersByAppTagsAndOrgs_shouldReturnOnlyAppsWithSpecifiedTagsAndTheirParentsWhenProvided() {
    final var rootOrganization = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    final var tree1Organization1 = tempEntity.newOrganization("tree1-org1", rootOrganization);
    final var tree1Organization2 = tempEntity.newOrganization("tree1-org2", tree1Organization1);

    final var app1 = tempEntity.newApplication(tree1Organization1.getId());
    final var app2 = tempEntity.newApplication(tree1Organization2.getId());
    final var app3 = tempEntity.newApplication(rootOrganization.getId());

    // an app with a tag not in our list, which will not be in our results
    final var app4 = tempEntity.newApplication(rootOrganization.getId());

    // an app with no tag will not be in the results
    tempEntity.newApplication(rootOrganization.getId());

    final var tag1 = tempEntity.newTag(rootOrganization.getId());
    final var tag2 = tempEntity.newTag(rootOrganization.getId());
    final var tag3 = tempEntity.newTag(rootOrganization.getId());

    tempEntity.newApplicationTag(app1.getId(), tag1.getId());
    tempEntity.newApplicationTag(app2.getId(), tag2.getId());
    tempEntity.newApplicationTag(app3.getId(), tag2.getId());

    // we will not ask for apps matching tag3 in our query
    tempEntity.newApplicationTag(app4.getId(), tag3.getId());

    final var results = ownerDAO.getOwnersByAppTagsAndOrgs(
        null,
        Sets.newHashSet(tag1.getId(), tag2.getId()),
        Sets.newHashSet());

    assertOwnersEqualInAnyOrder(
        results,
        app1,
        app2,
        app3);
  }

  @Test
  public void testGetOwnersByAppTagsAndOrgs_shouldReturnOnlyAppsWithoutTagsWhenNullIsInTagList() {
    final var rootOrganization = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    final var tree1Organization1 = tempEntity.newOrganization("tree1-org1", rootOrganization);
    final var tree1Organization2 = tempEntity.newOrganization("tree1-org2", tree1Organization1);

    final var app1 = tempEntity.newApplication(tree1Organization1.getId());
    final var app2 = tempEntity.newApplication(tree1Organization2.getId());
    final var app3 = tempEntity.newApplication(rootOrganization.getId());
    final var app4 = tempEntity.newApplication(rootOrganization.getId());

    final var app5 = tempEntity.newApplication(rootOrganization.getId()); // should be in result set because no tag

    final var tag1 = tempEntity.newTag(rootOrganization.getId());
    final var tag2 = tempEntity.newTag(rootOrganization.getId());

    tempEntity.newApplicationTag(app1.getId(), tag1.getId()); // should be in result set because matching tag

    tempEntity.newApplicationTag(app2.getId(), tag2.getId());
    tempEntity.newApplicationTag(app3.getId(), tag2.getId());
    tempEntity.newApplicationTag(app4.getId(), tag2.getId());

    final var results = ownerDAO.getOwnersByAppTagsAndOrgs(
        null,
        Sets.newHashSet(tag1.getId(), null),
        Sets.newHashSet());

    // includes app1 because it has tag1 in addition to any apps with no tags
    // tree1Organization2 won't be included because our matching apps are linked lower in the tree than this
    assertOwnersEqualInAnyOrder(
        results,
        app1,
        app5,
        application // this is created by AbstractDbDAOTest and included because not tagged
    );
  }

  @Test
  public void testGetOwnersByAppTagsAndOrgs_shouldReturnOnlyIntersectionOfAppIdsAndTagsAndTheirParentsWhenBothGiven() {
    final var rootOrganization = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    final var tree1Organization1 = tempEntity.newOrganization("tree1-org1", rootOrganization);
    final var tree1Organization2 = tempEntity.newOrganization("tree1-org2", tree1Organization1);

    final var app1 = tempEntity.newApplication(tree1Organization1.getId());
    final var app2 = tempEntity.newApplication(tree1Organization2.getId());
    final var app3 = tempEntity.newApplication(rootOrganization.getId());
    final var app4 = tempEntity.newApplication(rootOrganization.getId());
    final var app5 = tempEntity.newApplication(rootOrganization.getId());
    final var app6 = tempEntity.newApplication(rootOrganization.getId());

    final var tag1 = tempEntity.newTag(rootOrganization.getId());
    final var tag2 = tempEntity.newTag(rootOrganization.getId());
    final var tag3 = tempEntity.newTag(rootOrganization.getId());
    tempEntity.newApplicationTag(app1.getId(), tag1.getId());
    tempEntity.newApplicationTag(app2.getId(), tag2.getId());
    tempEntity.newApplicationTag(app3.getId(), tag2.getId());
    tempEntity.newApplicationTag(app4.getId(), tag2.getId());
    tempEntity.newApplicationTag(app6.getId(), tag3.getId());

    // a second branch off root that will not be in our results as no apps on it will match
    final var tree2Organization1 = tempEntity.newOrganization("tree2-org1", rootOrganization);
    final var tree2Organization2 = tempEntity.newOrganization("tree2-org2", tree2Organization1);
    // we'll ask for this appId, but it's not tagged so will not intersect with our tag list
    final var app7 = tempEntity.newApplication(tree2Organization2.getId());

    var results = ownerDAO.getOwnersByAppTagsAndOrgs(
        Sets.newHashSet(app2.getId(), app3.getId(), app4.getId(), app5.getId(), app6.getId(), app7.getId()),
        Sets.newHashSet(tag2.getId(), tag3.getId()),
        Sets.newHashSet());

    assertThat(results.stream().map(Owner::getId))
        .containsExactlyInAnyOrder(
            app2.getId(),
            app3.getId(),
            app4.getId(),
            app6.getId());

    // also includes app 5 and app7 with its tree if we add a null to tags, so that it includes non tagged
    results = ownerDAO.getOwnersByAppTagsAndOrgs(
        Sets.newHashSet(app2.getId(), app3.getId(), app4.getId(), app5.getId(), app6.getId(), app7.getId()),
        Sets.newHashSet(tag2.getId(), tag3.getId(), null),
        Sets.newHashSet());

    assertOwnersEqualInAnyOrder(
        results,
        app2,
        app3,
        app4,
        app5,
        app6,
        app7);
  }

  @Test
  public void testGetOwnersByAppTagsAndOrgs_shouldReturnBothMatchingOrgsAndApps() {
    final var rootOrganization = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    final var tree1Organization1 = tempEntity.newOrganization("tree1-org1", rootOrganization);
    final var tree1Organization2 = tempEntity.newOrganization("tree1-org2", tree1Organization1);

    final var app1 = tempEntity.newApplication(rootOrganization.getId());
    final var app2 = tempEntity.newApplication(tree1Organization2.getId());
    tempEntity.newApplication(tree1Organization2.getId());

    var results = ownerDAO.getOwnersByAppTagsAndOrgs(
        Sets.newHashSet(app1.getId(), app2.getId()),
        Sets.newHashSet(),
        Sets.newHashSet(tree1Organization1.getId(), rootOrganization.getId()));

    assertOwnersEqualInAnyOrder(
        results,
        app1,
        app2,
        tree1Organization1,
        rootOrganization);
  }

  @Test
  public void testGetOwnersByAppTagsAndOrgs_shouldReturnMatchingOrgsWhenOnlyOrgsArePassed() {
    final var rootOrganization = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    final var tree1Organization1 = tempEntity.newOrganization("tree1-org1", rootOrganization);
    final var tree1Organization2 = tempEntity.newOrganization("tree1-org2", tree1Organization1);

    tempEntity.newApplication(rootOrganization.getId());
    tempEntity.newApplication(tree1Organization2.getId());
    tempEntity.newApplication(tree1Organization2.getId());

    var results = ownerDAO.getOwnersByAppTagsAndOrgs(
        Sets.newHashSet(),
        Sets.newHashSet(),
        Sets.newHashSet(tree1Organization1.getId(), rootOrganization.getId()));

    assertOwnersEqualInAnyOrder(
        results,
        tree1Organization1,
        rootOrganization);
  }

  @Test
  public void testGetOwnersByAppTagsAndOrgs_shouldReturnMatchingAppsMatchingTagsAndIdPlusOrgsWhenAllProvided() {
    final var rootOrganization = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    final var tree1Organization1 = tempEntity.newOrganization("tree1-org1", rootOrganization);
    final var tree1Organization2 = tempEntity.newOrganization("tree1-org2", tree1Organization1);

    final var tag1 = tempEntity.newTag(rootOrganization.getId(), "tag1");
    final var tag2 = tempEntity.newTag(rootOrganization.getId(), "tag2");

    // specified and tag matches will be returned
    final var app1 = tempEntity.newApplication(tree1Organization2.getId());
    tempEntity.newApplicationTag(app1.getId(), tag1.getId());

    // specified in ids but tag does not match, won't be returned
    final var app2 = tempEntity.newApplication(tree1Organization2.getId());
    tempEntity.newApplicationTag(app2.getId(), tag2.getId());

    // specified and un-tagged, will be returned
    final var app3 = tempEntity.newApplication(tree1Organization2.getId());

    // matching tag but id not specified, won't be returned
    final var app4 = tempEntity.newApplication(tree1Organization2.getId());
    tempEntity.newApplicationTag(app4.getId(), tag1.getId());

    // un-tagged, but not specified in app ids, will not be returned
    tempEntity.newApplication(tree1Organization2.getId());

    var results = ownerDAO.getOwnersByAppTagsAndOrgs(
        Sets.newHashSet(app1.getId(), app2.getId(), app3.getId()),
        Sets.newHashSet(tag1.getId(), null),
        Sets.newHashSet(tree1Organization1.getId(), rootOrganization.getId()));

    assertOwnersEqualInAnyOrder(
        results,
        app1,
        app3,
        tree1Organization1,
        rootOrganization);
  }

  @Test
  public void testGetAll_shouldReturnAllAppsAndOrgsAppsAndOrganizationsAsOwners() {
    final var rootOrganization = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    final var tree1Organization1 = tempEntity.newOrganization("tree1-org1", rootOrganization);
    final var tree1Organization2 = tempEntity.newOrganization("tree1-org2", tree1Organization1);

    final var tree2Organization1 = tempEntity.newOrganization("tree2-org1", rootOrganization);
    final var tree2Organization2 = tempEntity.newOrganization("tree2-org2", tree1Organization1);

    final var tree3Organization1 = tempEntity.newOrganization("tree3-org1", rootOrganization);

    final var app1 = tempEntity.newApplication(rootOrganization.getId());
    final var app2 = tempEntity.newApplication(tree2Organization2.getId());
    final var app3 = tempEntity.newApplication(tree1Organization1.getId());

    final var results = ownerDAO.getAllAppsAndOrgs();

    assertOwnersEqualInAnyOrder(results,
        rootOrganization,
        tree1Organization1,
        tree1Organization2,
        tree2Organization1,
        tree2Organization2,
        tree3Organization1,
        app1,
        app2,
        app3,
        application,
        organization);
  }

  private void assertOwnersEqualInAnyOrder(final List<Owner> actual, final Owner... expected) {
    assertThat(actual)
        .usingElementComparator((a, b) -> {
          if (Objects.equals(a.getId(), b.getId()) &&
              Objects.equals(a.getName(), b.getName()) &&
              Objects.equals(a.getParentOwnerId(), b.getParentOwnerId()) &&
              Objects.equals(a.getType(), b.getType()) &&
              Objects.equals(a.getPublicId(), b.getPublicId()) &&
              a.canHaveChildren() == b.canHaveChildren())
        {
            return 0;
          }

          return -1;
        })
        .containsExactlyInAnyOrder(expected);
  }
}
