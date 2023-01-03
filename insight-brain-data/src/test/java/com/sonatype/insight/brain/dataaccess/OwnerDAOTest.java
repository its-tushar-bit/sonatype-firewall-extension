/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomDetailDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityGroupDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityGroupVulnerabilityDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityCustomDetail;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityGroup;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityGroupVulnerability;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.joda.time.DateTime;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class OwnerDAOTest
    extends AbstractDbDAOTest
{
  private final OwnerDAO ownerDAO = new OwnerDAO();

  @Test
  public void testWalkHierarchy_Application() {
    List<String> ownersIds = new ArrayList<>();
    for (Owner owner : ownerDAO.walkHierarchy(application)) {
      ownersIds.add(owner.getId());
    }
    assertThat(ownersIds).containsExactly(application.getId(), organization.getId(),
        organization.getParentOrganizationId());
  }

  @Test
  public void testWalkHierarchy_ApplicationId() {
    List<String> ownersIds = new ArrayList<>();
    for (Owner owner : ownerDAO.walkHierarchy(application.getId())) {
      ownersIds.add(owner.getId());
    }
    assertThat(ownersIds).containsExactly(application.getId(), organization.getId(),
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
    assertThat(ownersIds).containsExactly(repository.getId(), RepositoryContainer.REPOSITORY_CONTAINER_ID,
        Organization.ROOT_ORGANIZATION_ID);
  }

  @Test
  public void testWalkHierarchy_RepositoryId() {
    List<String> ownersIds = new ArrayList<>();
    for (Owner owner : ownerDAO.walkHierarchy(repository.getId())) {
      ownersIds.add(owner.getId());
    }
    assertThat(ownersIds).containsExactly(repository.getId(), RepositoryContainer.REPOSITORY_CONTAINER_ID,
        Organization.ROOT_ORGANIZATION_ID);
  }

  @Test
  public void testGetChildOwners_Application() {
    assertThat(ownerDAO.getChildOwners(application)).isEmpty();
  }

  @Test
  public void testGetChildOwners_Organization() {
    List<Owner> childOwners = ownerDAO.getChildOwners(organization);
    assertThat(childOwners).extracting(Owner::getId).containsExactly(application.getId());
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
    assertThat(childOwners).extracting(Owner::getId).containsExactly(repository.getId());
  }

  @Test
  public void testGetChildOwners_Repository() {
    assertThat(ownerDAO.getChildOwners(repository)).isEmpty();
  }

  @Test
  public void testCascadeDelete_PolicyWaivers() {
    try (TransactionContext tx = new ApplicationDAO().createTransactionContext()) {
      DateTime now = DateTime.now();
      Owner owner = ownerDAO.getById(organization.getId());
      Application app = tempEntity.newApplication(organization.getId());
      Policy appPolicy = tempEntity.newPolicy(app);
      tempEntity.newWaiver("noexpiry", appPolicy.getId(), owner.getId(), null, "comment", now.toDate(), null);
      tempEntity.newWaiver("expiring", appPolicy.getId(), owner.getId(), null, "comment", now.toDate(),
          now.plusHours(1).toDate());
      tempEntity.newWaiver("expired", appPolicy.getId(), owner.getId(), null, "comment", now.toDate(),
          now.minusHours(1).toDate());
      List<PolicyWaiver> policyWaivers = new PolicyWaiverDAO().getByOwnerId(tx, owner.getId());
      assertThat(policyWaivers).hasSize(3);
      tx.begin();
      ownerDAO.cascadeDelete(tx, owner);
      tx.commit();
      policyWaivers = new PolicyWaiverDAO().getByOwnerId(tx, owner.getId());
      assertThat(policyWaivers).isEmpty();
    }
  }

  @Test
  public void testCascadeDelete_VulnerabilityGroups() {
    try (TransactionContext tx = new ApplicationDAO().createTransactionContext()) {
      Owner owner = ownerDAO.getById(organization.getId());
      VulnerabilityGroup vulnGroup = tempEntity.newVulnerabilityGroup("TestGroup", owner.getId());
      tempEntity.newVulnerabilityGroupVulnerability(vulnGroup.getId(), "CVE-1234");
      VulnerabilityGroup vulnGroup2 = tempEntity.newVulnerabilityGroup("TestGroup2", owner.getId());
      tempEntity.newVulnerabilityGroupVulnerability(vulnGroup2.getId(), "CVE-456");
      List<VulnerabilityGroup> vulnerabilityGroupList = new VulnerabilityGroupDAO().getByOwnerId(owner.getId());
      assertThat(vulnerabilityGroupList).hasSize(2);
      tx.begin();
      ownerDAO.cascadeDelete(tx, owner);
      tx.commit();
      vulnerabilityGroupList = new VulnerabilityGroupDAO().getByOwnerId(owner.getId());
      assertThat(vulnerabilityGroupList).isEmpty();
      List<VulnerabilityGroupVulnerability> vulnerabilityList =
          new VulnerabilityGroupVulnerabilityDAO().getByGroupId(vulnGroup.getId());
      assertThat(vulnerabilityList).isEmpty();
      vulnerabilityList = new VulnerabilityGroupVulnerabilityDAO().getByGroupId(vulnGroup2.getId());
      assertThat(vulnerabilityList).isEmpty();
    }
  }

  @Test
  public void testCascadeDelete_VulnerabilityCustomDetail() {
    try (TransactionContext tx = new ApplicationDAO().createTransactionContext()) {
      Owner owner = ownerDAO.getById(organization.getId());
      tempEntity.newVulnerabilityCustomDetail(owner.getId(), "CVE-2022-1234",
          ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"));
      tempEntity.newVulnerabilityCustomDetail(owner.getId(), "CVE-2022-4321",
          ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"));
      List<VulnerabilityCustomDetail> vulnerabilityCustomDetailList = new VulnerabilityCustomDetailDAO()
          .getByOwnerId(owner.getId());
      assertThat(vulnerabilityCustomDetailList).extracting("refId")
          .containsExactlyInAnyOrder("CVE-2022-1234", "CVE-2022-4321");
      tx.begin();
      ownerDAO.cascadeDelete(tx, owner);
      tx.commit();
      vulnerabilityCustomDetailList = new VulnerabilityCustomDetailDAO().getByOwnerId(owner.getId());
      assertThat(vulnerabilityCustomDetailList).isEmpty();
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
    new ApplicationDAO().delete(application);
    new OrganizationDAO().delete(organization);
    Organization rootOrganization = new OrganizationDAO().getById(Organization.ROOT_ORGANIZATION_ID);

    assertThat(ownerDAO.getDescendantOrSelfApplicationIds(rootOrganization)).isEmpty();
  }

  @Test
  public void testGetDescendantOrSelfApplicationIds_RootOrganization_OnlyOrganizationDescendants() {
    new ApplicationDAO().delete(application);
    Organization rootOrganization = new OrganizationDAO().getById(Organization.ROOT_ORGANIZATION_ID);
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
    Organization rootOrganization = new OrganizationDAO().getById(Organization.ROOT_ORGANIZATION_ID);
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
