/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.ArrayList;
import java.util.List;
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
    List<Organization> secondBranch = tempEntity.newRelatedOrganizationsAsList(mainBranch.get(2) ,1, 2, 0);
    List<Organization> thirdBranch = tempEntity.newRelatedOrganizationsAsList(secondBranch.get(1) ,1, 4, 0);
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
    assertThat(childOwners).extracting(Owner::getId).containsExactly(organization.getId(),
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
}
