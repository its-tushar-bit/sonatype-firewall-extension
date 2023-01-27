/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.AccessTile;
import com.sonatype.clm.testing.functional.elements.AccessTileList;
import com.sonatype.clm.testing.functional.elements.AccessTileList.AccessTileListElement;
import com.sonatype.clm.testing.functional.elements.NxDeleteModal;
import com.sonatype.clm.testing.functional.elements.NxSubmitMask;
import com.sonatype.clm.testing.functional.elements.PolicyTile;
import com.sonatype.clm.testing.functional.elements.PolicyTileList;
import com.sonatype.clm.testing.functional.elements.PolicyTileList.PolicyTileListElement;
import com.sonatype.clm.testing.functional.elements.RepositoriesSummaryTile;
import com.sonatype.clm.testing.functional.elements.RepositoryConfigurationTile;
import com.sonatype.clm.testing.functional.elements.RepositoryConfigurationTile.ConfigurationTable;
import com.sonatype.clm.testing.functional.elements.RepositoryConfigurationTile.ConfigurationTable.ConfigurationTableRow;
import com.sonatype.clm.testing.functional.pages.RepositoriesSummaryPage;
import com.sonatype.clm.testing.functional.pages.RepositoryReportContainerPage;
import com.sonatype.clm.testing.functional.pages.RepositoryResultsSummaryPage;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.elements.RepositoryConfigurationTile.EMPTY_LIST_TEXT;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;

public class RepositoriesSummaryViewTest
    extends AbstractFunctionalTest
{
  private static final int HIERARCHY_SIZE = 2;

  private final RepositoryDAO repositoryDAO = new RepositoryDAO();

  private final RepositoryManagerDAO repositoryManagerDAO = new RepositoryManagerDAO();

  @BeforeClass
  public static void startup() {
    refreshOrOpen(RepositoriesSummaryPage.url());
    loginAsAdmin();
  }

  @Before
  public void init() {
    refreshOrOpen(RepositoriesSummaryPage.url());
  }

  @Test
  public void testRepositorySummaryView() {
    RepositoriesSummaryTile summaryTile = RepositoriesSummaryPage.summaryTile();
    summaryTile.name().shouldBe(visible).shouldHave(text("Repositories"));
    testRepositorySummaryView_configurationTile();
  }

  private void testRepositorySummaryView_configurationTile() {
    RepositoryConfigurationTile configurationTile = RepositoriesSummaryPage.configTile();
    ConfigurationTable configurationTable = configurationTile.configurationTable();

    configurationTile.emptyDescriptor().shouldBe(visible).shouldHave(EMPTY_LIST_TEXT);

    eyesWatcher.eyesCheck("Empty state (no repositories added)");

    List<Repository> repositories = new ArrayList<>();
    RepositoryManager repositoryA = tempEntity.newRepositoryManager("5E7BCC8D-3FAB6390-83FF543B-ECD79639-D031F7AE");
    repositories.add(tempEntity.newRepository(repositoryA, "a123", false));
    RepositoryManager repositoryB = tempEntity.newRepositoryManager("P39Q1VFX-3AHOOK7Y-0L0XMIQA-WLMW6J4J-9KIPBV6Y");
    repositories.add(tempEntity.newRepository(repositoryB, "b123", true));

    refresh();

    eyesWatcher.eyesCheck("Repositories visible");

    configurationTile = RepositoriesSummaryPage.configTile();
    configurationTable = configurationTile.configurationTable();
    configurationTable.rows().shouldHaveSize(3); // 2 repository rows and header
    configurationTile.emptyDescriptor().shouldBe(hidden);

    for (int i = 0; i < repositories.size(); i++) {
      ConfigurationTableRow configurationRow = configurationTable.row(i + 1);
      Repository repository = repositories.get(i);

      configurationRow.publicId().shouldHave(text(repository.getPublicId()));
      configurationRow.managerId()
          .shouldHave(text(repositoryManagerDAO.getById(repository.getRepositoryManagerId()).getInstanceId()));
      configurationRow.status().shouldHave(text(repository.isEnabled() ? "Enabled" : "Disabled"));
    }

    Repository firstRepo = repositories.get(0);
    configurationTable.row(1).publicId().click();

    try {
      Selenide.switchTo().window(1);
      waitUntilUrl(RepositoryReportContainerPage.url(firstRepo.getId()));
      RepositoryReportContainerPage.title().shouldHave(text("Repository results for " + firstRepo.getName()));
    }
    finally {
      Selenide.switchTo().window(0);
    }

    testRepositorySummaryView_configurationTile_deleteRepository(configurationTable.row(2), repositories.get(1));
    testRepositorySummaryView_configurationTile_deleteRepository(configurationTable.row(1), repositories.get(0));
  }

  private void testRepositorySummaryView_configurationTile_deleteRepository(
      ConfigurationTableRow repositoryRow,
      Repository repositoryToDelete)
  {
    repositoryRow.deleteButton().shouldBe(visible, enabled).click();

    NxDeleteModal deleteModal = new NxDeleteModal("#repositories-delete-modal");

    deleteModal.shouldBe(visible);
    deleteModal.header().shouldHave(text("Remove Repository"));
    deleteModal.alertContent().shouldHave(ConfigurationTableRow.deleteRepositoryText(repositoryToDelete.getPublicId()));
    deleteModal.submitButton().shouldBe(visible);
    deleteModal.closeButton().shouldBe(visible).click();
    deleteModal.shouldBe(hidden);

    assertThat(repositoryDAO.getById(repositoryToDelete.getId())).isNotNull();

    repositoryRow.deleteButton().shouldBe(visible, enabled).click();
    deleteModal.shouldBe(visible);
    deleteModal.closeButton().shouldBe(visible);
    deleteModal.submitButton().shouldBe(visible).click();
    NxSubmitMask.seeAndWaitForDismissal();
    deleteModal.shouldBe(hidden);

    assertThat(repositoryDAO.getById(repositoryToDelete.getId())).isNull();
  }

  @Test
  public void testRepositoryTile_default() {
    AccessTile accessTile = RepositoriesSummaryPage.accessTile();
    accessTile.nxSubHeader().shouldBe(visible).shouldHave(AccessTile.subHeaderText("All Repositories"));
    accessTile.addRoleButton().shouldBe(enabled, visible);
    accessTile.accessLists().shouldHaveSize(1);

    AccessTileList localList = accessTile.accessList(0);

    localList.ownerName().shouldBe(visible).shouldHave(text("Local"));
    localList.emptyDescriptor().should(exist);

    AccessTileList inheritedList = accessTile.accessList(1);

    inheritedList.ownerName().shouldBe(hidden);
    inheritedList.emptyDescriptor().shouldBe(hidden);
  }

  @Test
  public void testRepositoryTile_Local() {
    Role readRole = tempEntity.newRole("Read Only", false, Permission.READ);
    Role writeRole = tempEntity.newRole("Write Only", false, Permission.WRITE);
    User testUser = tempEntity.newUser("testUser", "Test", "User", "testuser@sonatype.com");
    RoleDAO roleDAO = new RoleDAO();
    List<Role> roleList = new ArrayList<>(roleDAO.getApplicationRoles());
    tempEntity
        .newMembershipMapping(RepositoryContainer.REPOSITORY_CONTAINER_ID, writeRole.getId(), testUser.getUsername());
    tempEntity
        .newMembershipMapping(RepositoryContainer.REPOSITORY_CONTAINER_ID, readRole.getId(), "Group", MemberType.GROUP);
    roleList.add(readRole);
    roleList.add(writeRole);

    refresh();

    AccessTile accessTile = RepositoriesSummaryPage.accessTile();
    accessTile.accessLists().shouldHaveSize(1);

    AccessTileList localList = accessTile.accessList(0);
    localList.emptyDescriptor().shouldBe(hidden);

    localList.elements().shouldHaveSize(2);
    localList.ownerName().shouldBe(visible).shouldHave(text("Local"));

    AccessTileListElement readOnly = localList.element(0);
    readOnly.chevron().shouldBe(visible);
    readOnly.role().shouldBe(visible).shouldHave(text("Read Only"));
    readOnly.groupIcon().shouldBe(visible);
    readOnly.members().shouldBe(visible).shouldHave(text("Group"));

    AccessTileListElement writeOnly = localList.element(1);
    writeOnly.chevron().shouldBe(visible);
    writeOnly.role().shouldBe(visible).shouldHave(text("Write Only"));
    writeOnly.userIcon().shouldBe(visible);
    writeOnly.members().shouldBe(visible).shouldHave(text(testUser.calculateDisplayName()));

    AccessTileList inheritedList = accessTile.accessList(1);

    inheritedList.ownerName().shouldBe(hidden);
    inheritedList.elements().shouldHaveSize(0);
  }

  @Test
  public void testTiles_Inherited() {
    User testUser = tempEntity.newUser("testUser", "Inherited Test", "User", "testuser@sonatype.com");
    Role readRole = tempEntity.newRole("Read Only", false, Permission.READ);
    Role writeRole = tempEntity.newRole("Write Only", false, Permission.WRITE);

    tempEntity.newMembershipMapping(Organization.ROOT_ORGANIZATION_ID, writeRole.getId(), testUser.getUsername());
    tempEntity.newMembershipMapping(Organization.ROOT_ORGANIZATION_ID, readRole.getId(), "Group", MemberType.GROUP);

    refresh();

    AccessTile accessTile = RepositoriesSummaryPage.accessTile();
    accessTile.accessLists().shouldHaveSize(HIERARCHY_SIZE);

    AccessTileList localList = accessTile.accessList(0);

    localList.ownerName().shouldBe(visible).shouldHave(text("Local"));
    localList.emptyDescriptor().should(exist);

    AccessTileList inheritedList = accessTile.accessList(1);

    inheritedList.emptyDescriptor().shouldBe(hidden);
    inheritedList.ownerName().shouldBe(visible).shouldHave(AccessTile.inheritedText("Root Organization"));
    inheritedList.elements().shouldHaveSize(2);

    AccessTileListElement readOnly = inheritedList.element(0);
    readOnly.chevron().shouldBe(hidden);
    readOnly.role().shouldBe(visible).shouldHave(text("Read Only"));
    readOnly.groupIcon().shouldBe(visible);
    readOnly.members().shouldBe(visible).shouldHave(text("Group"));

    AccessTileListElement writeOnly = inheritedList.element(1);
    writeOnly.chevron().shouldBe(hidden);
    writeOnly.role().shouldBe(visible).shouldHave(text("Write Only"));
    writeOnly.userIcon().shouldBe(visible);
    writeOnly.members().shouldBe(visible).shouldHave(text(testUser.calculateDisplayName()));
  }

  private void setupDataForSorting() {
    List<Repository> repositories = new ArrayList<>();
    RepositoryManager rm2 = tempEntity.newRepositoryManager("rm2");
    RepositoryManager rm1 = tempEntity.newRepositoryManager("rm1");
    repositories.add(tempEntity.newRepository(rm1, "d", true));
    repositories.add(tempEntity.newRepository(rm2, "c", true));
    repositories.add(tempEntity.newRepository(rm1, "b", false));
    repositories.add(tempEntity.newRepository(rm2, "d", false));
    repositories.add(tempEntity.newRepository(rm1, "a", true));
  }

  @Test
  public void testRepositoryConfigurationTableSorting_Default() {
    RepositoryConfigurationTile configurationTile = RepositoriesSummaryPage.configTile();
    ConfigurationTable configurationTable = configurationTile.configurationTable();
    setupDataForSorting();
    refreshOrOpen(RepositoryResultsSummaryPage.url());

    RepositoryResultsSummaryPage.configurationTile().shouldBe(visible).shouldHave(text("Configuration"));
    RepositoryResultsSummaryPage.repositoriesTableRepositoryNameHeaderSortBtn().shouldBe(visible);
    RepositoryResultsSummaryPage.repositoriesTableRepositoryManagerHeaderSortBtn().shouldBe(visible);
    RepositoryResultsSummaryPage.repositoriesTableStatusHeaderSortBtn().shouldBe(visible);
    RepositoryResultsSummaryPage.repositoriesTableRepositoryNameHeaderSortBtn().shouldHave(
        attribute("aria-label", "Repository ascending"));
    RepositoryResultsSummaryPage.repositoriesTableRepositoryManagerHeaderSortBtn().shouldHave(
        attribute("aria-label", "Repository Manager unsorted"));
    RepositoryResultsSummaryPage.repositoriesTableStatusHeaderSortBtn().shouldHave(
        attribute("aria-label", "Status unsorted"));

    configurationTable.row(1).publicId().shouldHave(Condition.text("a"));
    configurationTable.row(1).managerId().shouldHave(Condition.text("rm1"));
    configurationTable.row(1).status().shouldHave(Condition.text("Enabled"));
    configurationTable.row(2).publicId().shouldHave(Condition.text("b"));
    configurationTable.row(2).managerId().shouldHave(Condition.text("rm1"));
    configurationTable.row(2).status().shouldHave(Condition.text("Disabled"));
    configurationTable.row(3).publicId().shouldHave(Condition.text("c"));
    configurationTable.row(3).managerId().shouldHave(Condition.text("rm2"));
    configurationTable.row(3).status().shouldHave(Condition.text("Enabled"));
    configurationTable.row(4).publicId().shouldHave(Condition.text("d"));
    configurationTable.row(4).managerId().shouldHave(Condition.text("rm1"));
    configurationTable.row(4).status().shouldHave(Condition.text("Enabled"));
    configurationTable.row(5).publicId().shouldHave(Condition.text("d"));
    configurationTable.row(5).managerId().shouldHave(Condition.text("rm2"));
    configurationTable.row(5).status().shouldHave(Condition.text("Disabled"));
  }

  @Test
  public void testRepositoryConfigurationTableSorting_RepositoryNameDescending() {
    RepositoryConfigurationTile configurationTile = RepositoriesSummaryPage.configTile();
    ConfigurationTable configurationTable = configurationTile.configurationTable();
    setupDataForSorting();
    refreshOrOpen(RepositoryResultsSummaryPage.url());

    RepositoryResultsSummaryPage.repositoriesTableRepositoryNameHeaderSortBtn().click();
    RepositoryResultsSummaryPage.repositoriesTableRepositoryNameHeaderSortBtn().shouldHave(
        attribute("aria-label", "Repository descending"));

    configurationTable.row(1).publicId().shouldHave(Condition.text("d"));
    configurationTable.row(1).managerId().shouldHave(Condition.text("rm1"));
    configurationTable.row(1).status().shouldHave(Condition.text("Enabled"));
    configurationTable.row(2).publicId().shouldHave(Condition.text("d"));
    configurationTable.row(2).managerId().shouldHave(Condition.text("rm2"));
    configurationTable.row(2).status().shouldHave(Condition.text("Disabled"));
    configurationTable.row(3).publicId().shouldHave(Condition.text("c"));
    configurationTable.row(3).managerId().shouldHave(Condition.text("rm2"));
    configurationTable.row(3).status().shouldHave(Condition.text("Enabled"));
    configurationTable.row(4).publicId().shouldHave(Condition.text("b"));
    configurationTable.row(4).managerId().shouldHave(Condition.text("rm1"));
    configurationTable.row(4).status().shouldHave(Condition.text("Disabled"));
    configurationTable.row(5).publicId().shouldHave(Condition.text("a"));
    configurationTable.row(5).managerId().shouldHave(Condition.text("rm1"));
    configurationTable.row(5).status().shouldHave(Condition.text("Enabled"));
  }

  @Test
  public void testRepositoryConfigurationTableSorting_RepositoryNameAndRepositoryManager() {
    RepositoryConfigurationTile configurationTile = RepositoriesSummaryPage.configTile();
    ConfigurationTable configurationTable = configurationTile.configurationTable();
    setupDataForSorting();
    refreshOrOpen(RepositoryResultsSummaryPage.url());

    RepositoryResultsSummaryPage.repositoriesTableRepositoryNameHeaderSortBtn().click();
    RepositoryResultsSummaryPage.repositoriesTableRepositoryNameHeaderSortBtn().shouldHave(
        attribute("aria-label", "Repository descending"));
    RepositoryResultsSummaryPage.repositoriesTableRepositoryManagerHeaderSortBtn().click();
    RepositoryResultsSummaryPage.repositoriesTableRepositoryManagerHeaderSortBtn().shouldHave(
        attribute("aria-label", "Repository Manager ascending"));

    configurationTable.row(1).publicId().shouldHave(Condition.text("d"));
    configurationTable.row(1).managerId().shouldHave(Condition.text("rm1"));
    configurationTable.row(1).status().shouldHave(Condition.text("Enabled"));
    configurationTable.row(2).publicId().shouldHave(Condition.text("b"));
    configurationTable.row(2).managerId().shouldHave(Condition.text("rm1"));
    configurationTable.row(2).status().shouldHave(Condition.text("Disabled"));
    configurationTable.row(3).publicId().shouldHave(Condition.text("a"));
    configurationTable.row(3).managerId().shouldHave(Condition.text("rm1"));
    configurationTable.row(3).status().shouldHave(Condition.text("Enabled"));
    configurationTable.row(4).publicId().shouldHave(Condition.text("d"));
    configurationTable.row(4).managerId().shouldHave(Condition.text("rm2"));
    configurationTable.row(4).status().shouldHave(Condition.text("Disabled"));
    configurationTable.row(5).publicId().shouldHave(Condition.text("c"));
    configurationTable.row(5).managerId().shouldHave(Condition.text("rm2"));
    configurationTable.row(5).status().shouldHave(Condition.text("Enabled"));
  }

  @Test
  public void testRepositoryConfigurationTableSorting_StatusAndRepositoryManager() {
    RepositoryConfigurationTile configurationTile = RepositoriesSummaryPage.configTile();
    ConfigurationTable configurationTable = configurationTile.configurationTable();
    setupDataForSorting();
    refreshOrOpen(RepositoryResultsSummaryPage.url());

    RepositoryResultsSummaryPage.repositoriesTableStatusHeaderSortBtn().click();
    RepositoryResultsSummaryPage.repositoriesTableStatusHeaderSortBtn().shouldHave(
        attribute("aria-label", "Status ascending"));
    RepositoryResultsSummaryPage.repositoriesTableRepositoryManagerHeaderSortBtn().click();
    RepositoryResultsSummaryPage.repositoriesTableRepositoryManagerHeaderSortBtn().shouldHave(
        attribute("aria-label", "Repository Manager ascending"));

    configurationTable.row(1).publicId().shouldHave(Condition.text("b"));
    configurationTable.row(1).managerId().shouldHave(Condition.text("rm1"));
    configurationTable.row(1).status().shouldHave(Condition.text("Disabled"));
    configurationTable.row(2).publicId().shouldHave(Condition.text("a"));
    configurationTable.row(2).managerId().shouldHave(Condition.text("rm1"));
    configurationTable.row(2).status().shouldHave(Condition.text("Enabled"));
    configurationTable.row(3).publicId().shouldHave(Condition.text("d"));
    configurationTable.row(3).managerId().shouldHave(Condition.text("rm1"));
    configurationTable.row(3).status().shouldHave(Condition.text("Enabled"));
    configurationTable.row(4).publicId().shouldHave(Condition.text("d"));
    configurationTable.row(4).managerId().shouldHave(Condition.text("rm2"));
    configurationTable.row(4).status().shouldHave(Condition.text("Disabled"));
    configurationTable.row(5).publicId().shouldHave(Condition.text("c"));
    configurationTable.row(5).managerId().shouldHave(Condition.text("rm2"));
    configurationTable.row(5).status().shouldHave(Condition.text("Enabled"));
  }

  @Test
  public void testRepositoryConfigurationTableSorting_multiSortBtnClicks() {
    RepositoryConfigurationTile configurationTile = RepositoriesSummaryPage.configTile();
    ConfigurationTable configurationTable = configurationTile.configurationTable();
    setupDataForSorting();
    refreshOrOpen(RepositoryResultsSummaryPage.url());

    RepositoryResultsSummaryPage.repositoriesTableRepositoryManagerHeaderSortBtn().click();
    RepositoryResultsSummaryPage.repositoriesTableRepositoryNameHeaderSortBtn().click();
    RepositoryResultsSummaryPage.repositoriesTableRepositoryNameHeaderSortBtn().click();
    RepositoryResultsSummaryPage.repositoriesTableRepositoryNameHeaderSortBtn().shouldHave(
        attribute("aria-label", "Repository descending"));

    configurationTable.row(1).publicId().shouldHave(Condition.text("d"));
    configurationTable.row(1).managerId().shouldHave(Condition.text("rm1"));
    configurationTable.row(1).status().shouldHave(Condition.text("Enabled"));
    configurationTable.row(2).publicId().shouldHave(Condition.text("d"));
    configurationTable.row(2).managerId().shouldHave(Condition.text("rm2"));
    configurationTable.row(2).status().shouldHave(Condition.text("Disabled"));
    configurationTable.row(3).publicId().shouldHave(Condition.text("c"));
    configurationTable.row(3).managerId().shouldHave(Condition.text("rm2"));
    configurationTable.row(3).status().shouldHave(Condition.text("Enabled"));
    configurationTable.row(4).publicId().shouldHave(Condition.text("b"));
    configurationTable.row(4).managerId().shouldHave(Condition.text("rm1"));
    configurationTable.row(4).status().shouldHave(Condition.text("Disabled"));
    configurationTable.row(5).publicId().shouldHave(Condition.text("a"));
    configurationTable.row(5).managerId().shouldHave(Condition.text("rm1"));
    configurationTable.row(5).status().shouldHave(Condition.text("Enabled"));
  }

  @Test
  public void testPolicyTile_NoPolicies() {
    // Sanity Check
    RepositoriesSummaryTile summaryTile = RepositoriesSummaryPage.summaryTile();
    summaryTile.name().shouldBe(visible).shouldHave(text("Repositories"));

    PolicyTile policyTile = RepositoriesSummaryPage.policyTile();
    policyTile.shouldBe(visible);
    policyTile.subHeader().shouldBe(visible).shouldHave(PolicyTile.subHeaderText("All Repositories"));
    policyTile.newButton().shouldNotBe(visible);

    PolicyTileList policyList = policyTile.policyList(0);
    policyList.ownerName().shouldBe(visible).shouldHave(text("Local"));
    policyList.localEmptyDescriptor().shouldBe(visible);
  }

  @Test
  public void testPolicyTile_InheritedPolicies() {
    Owner parentOwner = new OrganizationDAO().getByIdNotNull(ROOT_ORGANIZATION_ID);

    List<Policy> inheritedPolicies = new ArrayList<>();

    inheritedPolicies.add(tempEntity.newPolicy(parentOwner.getId(), "Policy 1 " + parentOwner.getName(), 10,
        Action.ID_FAIL, Stage.ID_BUILD, null));
    inheritedPolicies.add(tempEntity.newPolicy(parentOwner.getId(), "Policy 2 " + parentOwner.getName(), 5,
        Action.ID_WARN, Stage.ID_BUILD, null));

    refreshOrOpen(RepositoriesSummaryPage.url());

    PolicyTile policyTile = RepositoriesSummaryPage.policyTile();
    PolicyTileList policyTileList = policyTile.policyList(1);

    // The plus one is added because the rows method selects the table header
    policyTileList.rows().shouldHaveSize(inheritedPolicies.size() + 1);

    // Verifying Policy 1
    policyTileList.row(1).threatLegend().shouldHave(text("10"));
    policyTileList.row(1).name().shouldHave(text("Policy 1 Root Organization"));
    policyTileList.row(1).proxy().shouldHave(PolicyTile.noActionText());
    policyTileList.row(1).develop().shouldHave(PolicyTile.noActionText());
    policyTileList.row(1).source().shouldHave(PolicyTile.noActionText());
    policyTileList.row(1).build().shouldHave(text("Fail"));
    policyTileList.row(1).stageRelease().shouldHave(PolicyTile.noActionText());
    policyTileList.row(1).release().shouldHave(PolicyTile.noActionText());
    policyTileList.row(1).operate().shouldHave(PolicyTile.noActionText());

    // Verifying Policy 2
    policyTileList.row(2).threatLegend().shouldHave(text("5"));
    policyTileList.row(2).name().shouldHave(text("Policy 2 Root Organization"));
    policyTileList.row(2).proxy().shouldHave(PolicyTile.noActionText());
    policyTileList.row(2).develop().shouldHave(PolicyTile.noActionText());
    policyTileList.row(2).source().shouldHave(PolicyTile.noActionText());
    policyTileList.row(2).build().shouldHave(text("Warn"));
    policyTileList.row(2).stageRelease().shouldHave(PolicyTile.noActionText());
    policyTileList.row(2).release().shouldHave(PolicyTile.noActionText());
    policyTileList.row(2).operate().shouldHave(PolicyTile.noActionText());
  }

  @Test
  public void testPolicyTileIsReadOnly() {
    Owner parentOwner = new OrganizationDAO().getByIdNotNull(ROOT_ORGANIZATION_ID);

    tempEntity.newPolicy(parentOwner.getId(), "Policy 1 " + parentOwner.getName(), 10,
        Action.ID_FAIL, Stage.ID_BUILD, null);
    tempEntity.newPolicy(parentOwner.getId(), "Policy 2 " + parentOwner.getName(), 5,
        Action.ID_WARN, Stage.ID_BUILD, null);

    refreshOrOpen(RepositoriesSummaryPage.url());

    PolicyTile policyTile = RepositoriesSummaryPage.policyTile();
    PolicyTileList policyTileList = policyTile.policyList(1);

    for (int i = 0; i < policyTileList.rows().size(); i++) {
      verifyTableRowIsReadOnly(policyTileList.row(i));
    }
  }

  public void verifyTableRowIsReadOnly(PolicyTileListElement row) {
    row.chevron().shouldBe(hidden);
  }
}
