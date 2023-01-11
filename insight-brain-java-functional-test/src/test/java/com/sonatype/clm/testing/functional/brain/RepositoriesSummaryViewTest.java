/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.AccessTile;
import com.sonatype.clm.testing.functional.elements.AccessTileList;
import com.sonatype.clm.testing.functional.elements.AccessTileList.AccessTileListElement;
import com.sonatype.clm.testing.functional.elements.NxDeleteModal;
import com.sonatype.clm.testing.functional.elements.NxSubmitMask;
import com.sonatype.clm.testing.functional.elements.RepositoriesSummaryTile;
import com.sonatype.clm.testing.functional.elements.RepositoryConfigurationTile;
import com.sonatype.clm.testing.functional.elements.RepositoryConfigurationTile.ConfigurationTable;
import com.sonatype.clm.testing.functional.elements.RepositoryConfigurationTile.ConfigurationTable.ConfigurationTableRow;
import com.sonatype.clm.testing.functional.pages.RepositoriesSummaryPage;
import com.sonatype.clm.testing.functional.pages.RepositoryReportContainerPage;
import com.sonatype.clm.testing.functional.pages.RepositoryResultsSummaryPage;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.model.Organization;
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

import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Condition.attribute;
import static com.sonatype.clm.testing.functional.elements.RepositoryConfigurationTile.EMPTY_LIST_TEXT;
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
  public void repositorySummaryViewTest() {
    RepositoriesSummaryTile summaryTile = RepositoriesSummaryPage.summaryTile();
    summaryTile.name().shouldBe(visible).shouldHave(text("Repositories"));
    repositorySummaryViewTest_configurationTile();
  }

  private void repositorySummaryViewTest_configurationTile() {
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

    repositorySummaryViewTest_configurationTile_deleteRepository(configurationTable.row(2), repositories.get(1));
    repositorySummaryViewTest_configurationTile_deleteRepository(configurationTable.row(1), repositories.get(0));
  }

  private void repositorySummaryViewTest_configurationTile_deleteRepository(ConfigurationTableRow repositoryRow,
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
    accessTile.newButton().shouldBe(visible, enabled);
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

  @Test
  public void testRepositoryConfigurationTable_Sorting() {
    RepositoryConfigurationTile configurationTile = RepositoriesSummaryPage.configTile();
    List<Repository> repositories = new ArrayList<>();
    RepositoryManager repositoryA = tempEntity.newRepositoryManager("5E7BCC8D-3FAB6390-83FF543B-ECD79639-D031F7AE");
    repositories.add(tempEntity.newRepository(repositoryA, "maven-central", false));
    RepositoryManager repositoryB = tempEntity.newRepositoryManager("P39Q1VFX-3AHOOK7Y-0L0XMIQA-WLMW6J4J-9KIPBV6Y");
    repositories.add(tempEntity.newRepository(repositoryB, "Repository-central", true));
    RepositoryManager repositoryC = tempEntity.newRepositoryManager("3AHOOK7Y-P39Q1VFX-WLMW6J4J-0L0XMIQA-9KIPBV6Y");
    repositories.add(tempEntity.newRepository(repositoryC, "sonatype-private", true));

    refreshOrOpen(RepositoryResultsSummaryPage.url());

    RepositoryResultsSummaryPage.configurationTile().shouldBe(visible).shouldHave(text("Configuration"));
    RepositoryResultsSummaryPage.componentsTableRepositoryNameHeaderSortBtn().shouldBe(visible);

    configurationTile.componentsTableConfigurationCountCols().get(0).shouldHave(Condition.text("maven-central"));
    configurationTile.componentsTableConfigurationCountCols().get(1).shouldHave(Condition.text("Repository-central"));
    configurationTile.componentsTableConfigurationCountCols().get(2).shouldHave(Condition.text("sonatype-private"));
    RepositoryResultsSummaryPage.componentsTableRepositoryNameHeaderSortBtn().shouldHave(
        attribute("aria-label", "Repository ascending"));
    RepositoryResultsSummaryPage.componentsTableRepositoryNameHeaderSortBtn().click();

    configurationTile.componentsTableConfigurationCountCols().get(0).shouldHave(Condition.text("sonatype-private"));
    configurationTile.componentsTableConfigurationCountCols().get(1).shouldHave(Condition.text("Repository-central"));
    configurationTile.componentsTableConfigurationCountCols().get(2).shouldHave(Condition.text("maven-central"));
    RepositoryResultsSummaryPage.componentsTableRepositoryNameHeaderSortBtn().shouldHave(
        attribute("aria-label", "Repository descending"));
    RepositoryResultsSummaryPage.componentsTableRepositoryNameHeaderSortBtn().click();

    configurationTile.componentsTableConfigurationCountCols().get(0).shouldHave(Condition.text("maven-central"));
    configurationTile.componentsTableConfigurationCountCols().get(1).shouldHave(Condition.text("Repository-central"));
    configurationTile.componentsTableConfigurationCountCols().get(2).shouldHave(Condition.text("sonatype-private"));
    RepositoryResultsSummaryPage.componentsTableRepositoryNameHeaderSortBtn().shouldHave(
        attribute("aria-label", "Repository unsorted"));
    RepositoryResultsSummaryPage.componentsTableRepositoryNameHeaderSortBtn().click();

    configurationTile.componentsTableConfigurationCountCols().get(0).shouldHave(Condition.text("maven-central"));
    configurationTile.componentsTableConfigurationCountCols().get(1).shouldHave(Condition.text("Repository-central"));
    configurationTile.componentsTableConfigurationCountCols().get(2).shouldHave(Condition.text("sonatype-private"));
    RepositoryResultsSummaryPage.componentsTableRepositoryNameHeaderSortBtn().shouldHave(
        attribute("aria-label", "Repository ascending"));
  }
}
