/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import javax.imageio.ImageIO;

import com.codeborne.selenide.CheckResult;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Driver;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import com.codeborne.selenide.WebElementCondition;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.AccessTile;
import com.sonatype.clm.testing.functional.elements.AccessTile.InheritedAccess;
import com.sonatype.clm.testing.functional.elements.AccessTile.InheritedAccessList;
import com.sonatype.clm.testing.functional.elements.AccessTileList;
import com.sonatype.clm.testing.functional.elements.AccessTileList.AccessTileListElement;
import com.sonatype.clm.testing.functional.elements.ActionDropDown;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.NamespaceConfusionProtectionTile;
import com.sonatype.clm.testing.functional.elements.NxBreadcrumb;
import com.sonatype.clm.testing.functional.elements.NxCollapsible;
import com.sonatype.clm.testing.functional.elements.NxDeleteModal;
import com.sonatype.clm.testing.functional.elements.NxModal;
import com.sonatype.clm.testing.functional.elements.NxSubmitMask;
import com.sonatype.clm.testing.functional.elements.OrgsAndPoliciesSidebar;
import com.sonatype.clm.testing.functional.elements.OwnerEditorDialog;
import com.sonatype.clm.testing.functional.elements.PolicyTile;
import com.sonatype.clm.testing.functional.elements.PolicyTileList;
import com.sonatype.clm.testing.functional.elements.RepositoriesSummaryTile;
import com.sonatype.clm.testing.functional.elements.RepositoryConfigurationTile;
import com.sonatype.clm.testing.functional.elements.RepositoryConfigurationTile.ConfigurationTable;
import com.sonatype.clm.testing.functional.elements.RepositoryConfigurationTile.ConfigurationTable.ConfigurationTableRow;
import com.sonatype.clm.testing.functional.elements.SummarySection;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.PolicyEditorPage;
import com.sonatype.clm.testing.functional.pages.RepositoriesSummaryPage;
import com.sonatype.clm.testing.functional.pages.RepositoryReportContainerPage;
import com.sonatype.clm.testing.functional.pages.RepositoryResultsSummaryPage;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.clm.testing.functional.utils.ScrollUtil;
import com.sonatype.insight.brain.dataaccess.IconDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.repository.ProprietaryComponentNamePatternDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.repository.ProprietaryComponentNamePattern;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.InsightWork;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.*;
import static com.sonatype.clm.testing.functional.elements.RepositoryConfigurationTile.EMPTY_LIST_TEXT;
import static com.sonatype.clm.testing.functional.pages.OwnerSummaryPage.sidebar;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;

public class RepositoriesSummaryViewTest
    extends AbstractFunctionalTest
{
  private static final int HIERARCHY_SIZE = 2;

  private static Dimension originalSize;

  private RoleDAO roleDAO;

  private OrganizationDAO organizationDAO;

  private RepositoryDAO repositoryDAO;

  private RepositoryManagerDAO repositoryManagerDAO;

  private ProprietaryComponentNamePatternDAO proprietaryComponentNamePatternDAO;

  private IconDAO iconDAO;

  private static final int IMAGE_RESIZE_WIDTH = 52;

  private static final int IMAGE_RESIZE_HEIGHT = 52;

  @BeforeClass
  public static void startup() {
    refreshOrOpen(RepositoriesSummaryPage.url());
    loginAsAdmin();
    originalSize = WebDriverRunner.getWebDriver().manage().window().getSize();
  }

  @Before
  public void init() {
    roleDAO = lookup(RoleDAO.class);
    organizationDAO = lookup(OrganizationDAO.class);
    repositoryDAO = lookup(RepositoryDAO.class);
    iconDAO = lookup(IconDAO.class);
    repositoryManagerDAO = lookup(RepositoryManagerDAO.class);
    proprietaryComponentNamePatternDAO = lookup(ProprietaryComponentNamePatternDAO.class);

    refreshOrOpen(RepositoriesSummaryPage.url());
  }

  @After
  public void cleanup() {
    WebDriverRunner.getWebDriver().manage().window().setSize(originalSize);
  }

  @Test
  public void testRepositorySummaryView() {
    RepositoriesSummaryTile summaryTile = RepositoriesSummaryPage.summaryTile();
    summaryTile.name().shouldBe(visible).shouldHave(text("Repository Managers"));

    WebDriverRunner.getWebDriver().manage().window().setSize(new Dimension(1800, 1000));

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

    eyesWatcher.eyesCheck("Repository Managers visible");

    OrgsAndPoliciesSidebar orgsAndPoliciesSidebar = new OrgsAndPoliciesSidebar();
    orgsAndPoliciesSidebar.repositories().shouldBe(hidden);

    configurationTile = RepositoriesSummaryPage.configTile();
    configurationTable = configurationTile.configurationTable();
    // 1 header row, 1 row for header filters, 2 repository managers, 2 repository rows
    configurationTable.rows().shouldHave(size(6));
    configurationTile.emptyDescriptor().shouldBe(hidden);

    for (int i = 0; i < repositories.size(); i++) {
      Repository repository = repositories.get(i);

      configurationTable.row(i + 1, 1)
          .managerId()
          .shouldHave(text(repositoryManagerDAO.getById(repository.getRepositoryManagerId()).getInstanceId()));
      configurationTable.row(i + 1, 2).publicId().shouldHave(text(repository.getPublicId()));
      if (repository.isAuditEnabled()) {
        configurationTable.row(i + 1, 2).enablement().shouldHave(text("Audit"));
      }
    }

    Repository firstRepo = repositories.get(0);
    configurationTable.row(1, 2).publicId().click();

    waitUntilUrl(RepositoryReportContainerPage.url(firstRepo.getId()));
    RepositoryReportContainerPage.title().shouldHave(text(firstRepo.getName() + " Repository Results"));
    RepositoryReportContainerPage.backButton().click();

    waitUntilUrl(RepositoriesSummaryPage.url());
    testRepositorySummaryView_configurationTile_deleteRepository(configurationTable.row(1, 2), repositories.get(0));
    testRepositorySummaryView_configurationTile_deleteRepository(configurationTable.row(2, 2), repositories.get(1));
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
  public void testRepositorySummaryView_configurationTile_editRepositoryManagerName() {
    RepositoryManager repositoryManager1 = tempEntity.newRepositoryManager("instanceId1");
    tempEntity.newRepository(repositoryManager1, "r1");
    tempEntity.newRepository(repositoryManager1, "r2");
    RepositoryManager repositoryManager2 = tempEntity.newRepositoryManager("instanceId2");
    tempEntity.newRepository(repositoryManager2, "r3");

    WebDriverRunner.getWebDriver().manage().window().setSize(new Dimension(1800, 1000));
    refresh();

    RepositoryConfigurationTile configurationTile = RepositoriesSummaryPage.configTile();
    ConfigurationTable configurationTable = configurationTile.configurationTable();

    configurationTable.row(1, 1).managerId().shouldHave(text(repositoryManager1.getInstanceId()));
    configurationTable.row(2, 1).managerId().shouldHave(text(repositoryManager2.getInstanceId()));

    configurationTable.row(1, 1).editRepositoryManagerNameButton().click();

    NxModal editRepositoryManagerNameModal = new NxModal("#edit-repository-manager-name-modal");
    editRepositoryManagerNameModal.getElement().$(".nx-text-input__input").shouldHave(value(""));
    editRepositoryManagerNameModal.getElement().$(".nx-text-input__input").setValue("customName");
    editRepositoryManagerNameModal.submitButton().click();

    configurationTable.row(1, 1).managerId().shouldHave(text("customName"));
    configurationTable.row(2, 1).managerId().shouldHave(text(repositoryManager2.getInstanceId()));
    sidebar().getRepoManagerList().click();
    sidebar().getRepoManagerList().children().get(0).shouldHave(text("customName"));

    configurationTable.row(1, 1).editRepositoryManagerNameButton().click();
    editRepositoryManagerNameModal.getElement().$(".nx-text-input__input").shouldHave(value("customName"));
    eyesWatcher.eyesCheck("Edit repository manager name modal");
    editRepositoryManagerNameModal.closeButton().click();

    configurationTable.row(2, 1).editRepositoryManagerNameButton().click();
    editRepositoryManagerNameModal = new NxModal("#edit-repository-manager-name-modal");
    editRepositoryManagerNameModal.getElement().$(".nx-text-input__input").shouldHave(value(""));
    editRepositoryManagerNameModal.getElement().$(".nx-text-input__input").setValue("customName");
    editRepositoryManagerNameModal.submitButton().click();
    editRepositoryManagerNameModal.error()
        .shouldHave(text("An error occurred saving data. customName is already used as a name."));
    editRepositoryManagerNameModal.getElement().$(".nx-text-input__input").setValue("customName2");
    editRepositoryManagerNameModal.getElement().$(".nx-load-error__retry").click();

    sidebar().getRepoManagerList().click();
    configurationTable.row(1, 1).managerId().shouldHave(text("customName"));
    sidebar().getRepoManagerList().children().get(0).shouldHave(text("customName"));
    configurationTable.row(2, 1).managerId().shouldHave(text("customName2"));
    sidebar().getRepoManagerList().children().get(1).shouldHave(text("customName2"));
  }

  @Test
  public void testRepositorySummaryView_configurationTile_filterByRepositoryName() {
    RepositoryManager repositoryManager1 = tempEntity.newRepositoryManager("instanceId1");
    tempEntity.newRepository(repositoryManager1, "r11");
    tempEntity.newRepository(repositoryManager1, "r21");
    RepositoryManager repositoryManager2 = tempEntity.newRepositoryManager("instanceId2");
    tempEntity.newRepository(repositoryManager2, "r12");
    tempEntity.newRepository(repositoryManager2, "r22");

    refresh();

    RepositoryConfigurationTile configurationTile = RepositoriesSummaryPage.configTile();
    ConfigurationTable configurationTable = configurationTile.configurationTable();
    configurationTable.rows().shouldHave(size(8));

    configurationTable.repositoryPublicIdFilter().setValue("r1");

    configurationTable.rows().shouldHave(size(6));
    configurationTable.row(1, 2).publicId().shouldHave(text("r11"));
    configurationTable.row(2, 2).publicId().shouldHave(text("r12"));
  }

  @Test
  public void testRepositorySummaryView_configurationTile_filterByRepositoryFormat() {
    RepositoryManager repositoryManager1 = tempEntity.newRepositoryManager("instanceId1");
    tempEntity.newRepository(repositoryManager1, "r11", "maven");
    tempEntity.newRepository(repositoryManager1, "r21", "npm");
    tempEntity.newRepository(repositoryManager1, "r31", null);
    RepositoryManager repositoryManager2 = tempEntity.newRepositoryManager("instanceId2");
    tempEntity.newRepository(repositoryManager2, "r12", "npm");
    tempEntity.newRepository(repositoryManager2, "r22", "maven");
    tempEntity.newRepository(repositoryManager2, "r32", null);

    refresh();

    RepositoryConfigurationTile configurationTile = RepositoriesSummaryPage.configTile();
    ConfigurationTable configurationTable = configurationTile.configurationTable();
    configurationTable.rows().shouldHave(size(10));

    configurationTable.repositoryFormatFilter().click();
    configurationTable.repositoryFormatFilter().$(".nx-radio-checkbox").click();

    configurationTable.rows().shouldHave(size(6));
    configurationTable.row(1, 2).publicId().shouldHave(text("r11"));
    configurationTable.row(2, 2).publicId().shouldHave(text("r22"));
  }

  @Test
  public void testRepositoryTile_default() {
    AccessTile accessTile = RepositoriesSummaryPage.accessTile();
    accessTile.nxSubHeader().shouldBe(visible).shouldHave(AccessTile.subHeaderText("Repository Managers"));
    accessTile.addRoleButton().scrollIntoView(true).shouldBe(enabled, visible);
    accessTile.accessLists().shouldHave(size(1));

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
    List<Role> roleList = new ArrayList<>(roleDAO.getApplicationRoles());
    tempEntity
        .newMembershipMapping(RepositoryContainer.REPOSITORY_CONTAINER_ID, writeRole.getId(), testUser.getUsername());
    tempEntity
        .newMembershipMapping(RepositoryContainer.REPOSITORY_CONTAINER_ID, readRole.getId(), "Group", MemberType.GROUP);
    roleList.add(readRole);
    roleList.add(writeRole);

    refresh();

    AccessTile accessTile = RepositoriesSummaryPage.accessTile();
    accessTile.accessLists().shouldHave(size(1));

    AccessTileList localList = accessTile.accessList(0);
    localList.emptyDescriptor().shouldBe(hidden);

    localList.elements().shouldHave(size(2));
    localList.ownerName().shouldBe(visible).shouldHave(text("Local"));

    AccessTileListElement readOnly = localList.element(0);
    readOnly.roleNoPermission().shouldBe(visible).shouldHave(text("Read Only"));

    AccessTileListElement descriptionRead = readOnly.description();
    descriptionRead.chevron().shouldBe(visible);
    descriptionRead.groupIcon().shouldBe(visible);
    descriptionRead.members().shouldBe(visible).shouldHave(text("Group"));

    AccessTileListElement writeOnly = localList.element(1);
    writeOnly.roleNoPermission().shouldBe(visible).shouldHave(text("Write Only"));

    AccessTileListElement descriptionWrite = writeOnly.description();
    descriptionWrite.chevron().shouldBe(visible);
    descriptionWrite.userIcon().shouldBe(visible);
    descriptionWrite.members().shouldBe(visible).shouldHave(text(testUser.calculateDisplayName()));

    AccessTileList inheritedList = accessTile.accessList(1);

    inheritedList.ownerName().shouldBe(hidden);
    inheritedList.elements().shouldHave(size(0));
  }

  @Test
  public void testNamespaceConfusionProtection_EmptyTable() {
    NamespaceConfusionProtectionTile namespaceConfusionProtectionTile =
        RepositoriesSummaryPage.namespaceConfusionProtectionTile();

    ScrollUtil.scrollIntoView(namespaceConfusionProtectionTile.getElement(), false);

    namespaceConfusionProtectionTile.shouldBe(visible).shouldHave(text("Namespace Confusion Protection"));
    namespaceConfusionProtectionTile.emptyDescriptor().shouldBe(visible).shouldHave(text("No results"));
    namespaceConfusionProtectionTile.previousPageBtn().shouldNotBe(visible);
    namespaceConfusionProtectionTile.nextPageBtn().shouldNotBe(visible);
  }

  @Test
  public void testNamespaceConfusionProtection_FilterRows() {
    String repositoryManagerInstanceId = "1E111629-6B9EDCBA-B5989887-132718F9-8C354DFB";
    RepositoryManager repoManager = tempEntity.newRepositoryManager(repositoryManagerInstanceId);
    Repository repo1 = tempEntity.newRepository(repoManager, "functional-theory-release", RepositoryType.hosted,
        ComponentIdentifier.FORMAT_MAVEN);
    Repository repo2 = tempEntity.newRepository(repoManager, "jsPlugin-release", RepositoryType.hosted,
        ComponentIdentifier.FORMAT_MAVEN);
    Repository repo3 = tempEntity.newRepository(repoManager, "bigdestero-release", RepositoryType.hosted,
        ComponentIdentifier.FORMAT_MAVEN);
    Repository repo4 = tempEntity.newRepository(repoManager, "lifecycle-release", RepositoryType.hosted,
        ComponentIdentifier.FORMAT_MAVEN);
    tempEntity.newProprietaryComponentNamePattern(repo1, "shiedlytics", null);
    tempEntity.newProprietaryComponentNamePattern(repo2, "acceronix", null);
    tempEntity.newProprietaryComponentNamePattern(repo3, "maven-center", null);
    tempEntity.newProprietaryComponentNamePattern(repo4, null, "blue-space");

    refresh();

    NamespaceConfusionProtectionTile namespaceConfusionProtectionTile =
        RepositoriesSummaryPage.namespaceConfusionProtectionTile();
    ScrollUtil.scrollIntoView(namespaceConfusionProtectionTile.getElement(), true);

    namespaceConfusionProtectionTile.tableBodyRows().shouldHave(size(4));

    namespaceConfusionProtectionTile.namespaceFilterInput().setValue("claimed");
    namespaceConfusionProtectionTile.resultRows().shouldHave(size(1));
    namespaceConfusionProtectionTile.resultRow(1).shouldHave(text("No Results"));

    namespaceConfusionProtectionTile.namespaceFilterInput().setValue("maven-center");
    namespaceConfusionProtectionTile.resultRow(1).shouldHave(text("maven-center"));
    namespaceConfusionProtectionTile.previousPageBtn().shouldNotBe(visible);
    namespaceConfusionProtectionTile.nextPageBtn().shouldNotBe(visible);
  }

  @Test
  public void testNamespaceConfusionProtection_sortTableByComponentNamespaces() {
    String[] componentNameSpaces = {"z", "a", "m"};
    String repositoryManagerInstanceId = "1E111629-6B9EDCBA-B5989887-132718F9-8C354DFB";
    RepositoryManager repoManager = tempEntity.newRepositoryManager(repositoryManagerInstanceId);
    Repository repo1 =
        tempEntity.newRepository(repoManager, "hosted-npm", RepositoryType.hosted, ComponentIdentifier.FORMAT_NPM);
    Repository repo2 = tempEntity.newRepository(repoManager, "custom-hosted-maven", RepositoryType.hosted,
        ComponentIdentifier.FORMAT_MAVEN);
    Repository repo3 = tempEntity.newRepository(repoManager, "\"my-hosted-maven", RepositoryType.hosted,
        ComponentIdentifier.FORMAT_MAVEN);
    tempEntity.newProprietaryComponentNamePattern(repo1, null, componentNameSpaces[0]);
    tempEntity.newProprietaryComponentNamePattern(repo2, componentNameSpaces[1], null);
    tempEntity.newProprietaryComponentNamePattern(repo3, componentNameSpaces[2], null);

    refresh();

    NamespaceConfusionProtectionTile namespaceConfusionProtectionTile =
        RepositoriesSummaryPage.namespaceConfusionProtectionTile();
    ScrollUtil.scrollIntoView(namespaceConfusionProtectionTile.getElement(), true);

    namespaceConfusionProtectionTile.tableBodyRows().shouldHave(size(3));

    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(0).shouldHave(text(componentNameSpaces[1]));
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(1).shouldHave(text(componentNameSpaces[2]));
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(2).shouldHave(text(componentNameSpaces[0]));

    namespaceConfusionProtectionTile.componentNamespaceHeaderSortBtn()
        .shouldHave(attribute("aria-label", "Component Namespace ascending"));

    namespaceConfusionProtectionTile.componentNamespaceHeaderSortBtn().click();
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(0).shouldHave(text(componentNameSpaces[0]));
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(1).shouldHave(text(componentNameSpaces[2]));
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(2).shouldHave(text(componentNameSpaces[1]));
    namespaceConfusionProtectionTile.componentNamespaceHeaderSortBtn()
        .shouldHave(attribute("aria-label", "Component Namespace descending"));
    namespaceConfusionProtectionTile.previousPageBtn().shouldNotBe(visible);
    namespaceConfusionProtectionTile.nextPageBtn().shouldNotBe(visible);
  }

  @Test
  public void testNamespaceConfusionProtection_sortTableByRepository() {
    String[] repositoryManagerInstanceIds = {
      "1E111629-6B9EDCBA-B5989887-132718F9-8C354DFB",
      "2E111629-6B9EDCBA-B5989887-132718F9-8C354DFB", "3E111629-6B9EDCBA-B5989887-132718F9-8C354DFB"
    };
    String[] repositoryPublicIds = {"my-hosted-maven", "hosted-npm", "custom-hosted-maven"};

    RepositoryManager repoManager1 = tempEntity.newRepositoryManager(repositoryManagerInstanceIds[0]);
    Repository repo1 = tempEntity.newRepository(repoManager1, repositoryPublicIds[0], RepositoryType.hosted,
        ComponentIdentifier.FORMAT_MAVEN);
    RepositoryManager repoManager2 = tempEntity.newRepositoryManager(repositoryManagerInstanceIds[1]);
    Repository repo2 = tempEntity.newRepository(repoManager2, repositoryPublicIds[1], RepositoryType.hosted,
        ComponentIdentifier.FORMAT_MAVEN);
    RepositoryManager repoManager3 = tempEntity.newRepositoryManager(repositoryManagerInstanceIds[2]);
    Repository repo3 = tempEntity.newRepository(repoManager3, repositoryPublicIds[2], RepositoryType.hosted,
        ComponentIdentifier.FORMAT_NPM);

    tempEntity.newProprietaryComponentNamePattern(repo1, "ant", null);
    tempEntity.newProprietaryComponentNamePattern(repo2, "b-social", null);
    tempEntity.newProprietaryComponentNamePattern(repo3, null, "moment");

    refreshOrOpen(RepositoryResultsSummaryPage.url());
    NamespaceConfusionProtectionTile namespaceConfusionProtectionTile =
        RepositoriesSummaryPage.namespaceConfusionProtectionTile();
    ScrollUtil.scrollIntoView(namespaceConfusionProtectionTile.getElement(), true);

    WebDriverRunner.getWebDriver().manage().window().setSize(new Dimension(1800, 1000));

    namespaceConfusionProtectionTile.tableBodyRows().shouldHave(size(3));

    namespaceConfusionProtectionTile.hostedRepositoryNameColumnCells().get(0).shouldHave(text(repositoryPublicIds[0]));
    namespaceConfusionProtectionTile.hostedRepositoryNameColumnCells().get(1).shouldHave(text(repositoryPublicIds[1]));
    namespaceConfusionProtectionTile.hostedRepositoryNameColumnCells().get(2).shouldHave(text(repositoryPublicIds[2]));

    namespaceConfusionProtectionTile.hostedRepositoryNameHeaderSortBtn()
        .shouldHave(attribute("aria-label", "Repository unsorted"));

    namespaceConfusionProtectionTile.hostedRepositoryNameHeaderSortBtn().click();
    namespaceConfusionProtectionTile.hostedRepositoryNameHeaderSortBtn()
        .shouldHave(attribute("aria-label", "Repository ascending"));

    namespaceConfusionProtectionTile.hostedRepositoryNameColumnCells().get(0).shouldHave(text(repositoryPublicIds[2]));
    namespaceConfusionProtectionTile.hostedRepositoryNameColumnCells().get(1).shouldHave(text(repositoryPublicIds[1]));
    namespaceConfusionProtectionTile.hostedRepositoryNameColumnCells().get(2).shouldHave(text(repositoryPublicIds[0]));

    namespaceConfusionProtectionTile.hostedRepositoryNameHeaderSortBtn().click();
    namespaceConfusionProtectionTile.hostedRepositoryNameHeaderSortBtn()
        .shouldHave(attribute("aria-label", "Repository descending"));

    namespaceConfusionProtectionTile.hostedRepositoryNameColumnCells().get(0).shouldHave(text(repositoryPublicIds[0]));
    namespaceConfusionProtectionTile.hostedRepositoryNameColumnCells().get(1).shouldHave(text(repositoryPublicIds[1]));
    namespaceConfusionProtectionTile.hostedRepositoryNameColumnCells().get(2).shouldHave(text(repositoryPublicIds[2]));
  }

  @Test
  public void testNamespaceConfusionProtection_sortTableByRepoManagerInstanceId() {
    String[] componentNamespaces = {"ant", "b-social", "moment"};
    String[] repositoryManagerInstanceIds = {
      "2E111629-6B9EDCBA-B5989887-132718F9-8C354DFB",
      "1E111629-6B9EDCBA-B5989887-132718F9-8C354DFB", "3E111629-6B9EDCBA-B5989887-132718F9-8C354DFB"
    };
    String[] repositoryPublicIds = {"custom-maven-hosted", "my-maven-hosted", "custom-npm-hosted"};

    RepositoryManager repoManager1 = tempEntity.newRepositoryManager(repositoryManagerInstanceIds[0]);
    Repository repo1 = tempEntity.newRepository(repoManager1, repositoryPublicIds[0], RepositoryType.hosted,
        ComponentIdentifier.FORMAT_MAVEN);
    RepositoryManager repoManager2 = tempEntity.newRepositoryManager(repositoryManagerInstanceIds[1]);
    Repository repo2 = tempEntity.newRepository(repoManager2, repositoryPublicIds[1], RepositoryType.hosted,
        ComponentIdentifier.FORMAT_MAVEN);
    RepositoryManager repoManager3 = tempEntity.newRepositoryManager(repositoryManagerInstanceIds[2]);
    Repository repo3 = tempEntity.newRepository(repoManager3, repositoryPublicIds[2], RepositoryType.hosted,
        ComponentIdentifier.FORMAT_NPM);

    tempEntity.newProprietaryComponentNamePattern(repo1, componentNamespaces[0], null);
    tempEntity.newProprietaryComponentNamePattern(repo2, componentNamespaces[1], null);
    tempEntity.newProprietaryComponentNamePattern(repo3, null, componentNamespaces[2]);

    refresh();
    NamespaceConfusionProtectionTile namespaceConfusionProtectionTile =
        RepositoriesSummaryPage.namespaceConfusionProtectionTile();
    ScrollUtil.scrollIntoView(namespaceConfusionProtectionTile.getElement(), true);

    namespaceConfusionProtectionTile.tableBodyRows().shouldHave(size(3));

    namespaceConfusionProtectionTile.repositoryManagerHeaderSortBtn()
        .shouldHave(attribute("aria-label", "Repository Manager unsorted"));

    namespaceConfusionProtectionTile.repositoryManagerHeaderSortBtn().click();

    namespaceConfusionProtectionTile.repositoryManagerHeaderSortBtn()
        .shouldHave(attribute("aria-label", "Repository Manager ascending"));

    namespaceConfusionProtectionTile.repositoryManagerIdColumnCells()
        .get(0)
        .shouldHave(text(repositoryManagerInstanceIds[1]));
    namespaceConfusionProtectionTile.repositoryManagerIdColumnCells()
        .get(1)
        .shouldHave(text(repositoryManagerInstanceIds[0]));
    namespaceConfusionProtectionTile.repositoryManagerIdColumnCells()
        .get(2)
        .shouldHave(text(repositoryManagerInstanceIds[2]));

    namespaceConfusionProtectionTile.repositoryManagerHeaderSortBtn().click();

    namespaceConfusionProtectionTile.repositoryManagerHeaderSortBtn()
        .shouldHave(attribute("aria-label", "Repository Manager descending"));

    namespaceConfusionProtectionTile.repositoryManagerIdColumnCells()
        .get(0)
        .shouldHave(text(repositoryManagerInstanceIds[2]));
    namespaceConfusionProtectionTile.repositoryManagerIdColumnCells()
        .get(1)
        .shouldHave(text(repositoryManagerInstanceIds[0]));
    namespaceConfusionProtectionTile.repositoryManagerIdColumnCells()
        .get(2)
        .shouldHave(text(repositoryManagerInstanceIds[1]));
  }

  @Test
  public void testNamespaceConfusionProtection_sortTableByEnabled() {
    String repositoryManagerInstanceId = "1E111629-6B9EDCBA-B5989887-132718F9-8C354DFB";
    RepositoryManager repoManager = tempEntity.newRepositoryManager(repositoryManagerInstanceId);
    Repository repo1 =
        tempEntity.newRepository(repoManager, "hosted-npm", RepositoryType.hosted, ComponentIdentifier.FORMAT_NPM);
    tempEntity.newProprietaryComponentNamePattern(repo1, null, "a", true);
    Repository repo2 = tempEntity.newRepository(repoManager, "custom-hosted-maven", RepositoryType.hosted,
        ComponentIdentifier.FORMAT_MAVEN);
    tempEntity.newProprietaryComponentNamePattern(repo2, "b", null, false);
    Repository repo3 = tempEntity.newRepository(repoManager, "my-hosted-maven", RepositoryType.hosted,
        ComponentIdentifier.FORMAT_MAVEN);
    tempEntity.newProprietaryComponentNamePattern(repo3, "c", null, true);

    WebDriverRunner.getWebDriver().manage().window().setSize(new Dimension(1800, 1000));

    refresh();

    NamespaceConfusionProtectionTile namespaceConfusionProtectionTile =
        RepositoriesSummaryPage.namespaceConfusionProtectionTile();

    namespaceConfusionProtectionTile.tableBodyRows().shouldHave(size(3));

    namespaceConfusionProtectionTile.enabledHeaderSortBtn()
        .shouldHave(attribute("aria-label", "Enabled unsorted"));

    namespaceConfusionProtectionTile.enabledToggleIndicators().get(0).shouldBe(enabled, selected);
    namespaceConfusionProtectionTile.enabledToggleIndicators().get(1).shouldBe(enabled).shouldNotBe(selected);
    namespaceConfusionProtectionTile.enabledToggleIndicators().get(2).shouldBe(enabled, selected);

    ScrollUtil.scrollIntoView(namespaceConfusionProtectionTile.getElement(), false);
    namespaceConfusionProtectionTile.enabledHeaderSortBtn().click();
    namespaceConfusionProtectionTile.enabledHeaderSortBtn()
        .shouldHave(attribute("aria-label", "Enabled ascending"));

    namespaceConfusionProtectionTile.enabledToggleIndicators().get(0).shouldBe(enabled).shouldNotBe(selected);
    namespaceConfusionProtectionTile.enabledToggleIndicators().get(1).shouldBe(enabled, selected);
    namespaceConfusionProtectionTile.enabledToggleIndicators().get(2).shouldBe(enabled, selected);

    namespaceConfusionProtectionTile.enabledHeaderSortBtn().click();
    namespaceConfusionProtectionTile.enabledHeaderSortBtn()
        .shouldHave(attribute("aria-label", "Enabled descending"));

    namespaceConfusionProtectionTile.enabledToggleIndicators().get(0).shouldBe(enabled, selected);
    namespaceConfusionProtectionTile.enabledToggleIndicators().get(1).shouldBe(enabled, selected);
    namespaceConfusionProtectionTile.enabledToggleIndicators().get(2).shouldBe(enabled).shouldNotBe(selected);

    namespaceConfusionProtectionTile.previousPageBtn().shouldNotBe(visible);
    namespaceConfusionProtectionTile.nextPageBtn().shouldNotBe(visible);
  }

  @Test
  public void testNamespaceConfusionProtection_ToggleEnabled() {
    String repositoryManagerInstanceId = "1E111629-6B9EDCBA-B5989887-132718F9-8C354DFB";
    RepositoryManager repoManager = tempEntity.newRepositoryManager(repositoryManagerInstanceId);
    Repository repo =
        tempEntity.newRepository(repoManager, "hosted-npm", RepositoryType.hosted, ComponentIdentifier.FORMAT_NPM);
    tempEntity.newProprietaryComponentNamePattern(repo, null, "a", true);

    WebDriverRunner.getWebDriver().manage().window().setSize(new Dimension(1500, 1000));

    refresh();

    NamespaceConfusionProtectionTile namespaceConfusionProtectionTile =
        RepositoriesSummaryPage.namespaceConfusionProtectionTile();

    SelenideElement toggle = namespaceConfusionProtectionTile.enabledToggleIndicators().get(0);
    ScrollUtil.scrollIntoView(toggle, false);

    namespaceConfusionProtectionTile.enabledToggleIndicators().get(0).shouldBe(selected);
    assertThat(proprietaryComponentNamePatternDAO.getByFormat("npm").get(0).isEnabled()).isTrue();

    new Actions(WebDriverRunner.getWebDriver()).moveToElement(toggle).click().perform();
    namespaceConfusionProtectionTile.enabledToggleIndicators().get(0).shouldNotBe(selected);
    assertThat(proprietaryComponentNamePatternDAO.getByFormat("npm").get(0).isEnabled()).isFalse();
  }

  @Test
  public void testNamespaceConfusionProtection_Pagination() {
    String[] componentNamespaces = {
      "@testing-library/react", "ant", "b-social", "express", "high-c", "itext", "jproc",
      "lodash", "moment", "net.ju-n.compile-command-annotations", "underscore", "v-core", "z-com"
    };
    String mvnRepositoryManagerId = "1E111629-6B9EDCBA-B5989887-132718F9-8C354DFB";
    String npmRepositoryManagerId = "9E111629-6B9EDCBA-B5989887-132718F9-8C354DFB";
    String mvnRepositoryPublicName = "hosted-mvn";
    String npmRepositoryPublicName = "hosted-npm";
    RepositoryManager mvnRepoManager = tempEntity.newRepositoryManager(mvnRepositoryManagerId);
    Repository mvnRepo = tempEntity.newRepository(mvnRepoManager, mvnRepositoryPublicName, RepositoryType.hosted,
        ComponentIdentifier.FORMAT_MAVEN);
    RepositoryManager npmRepoManager = tempEntity.newRepositoryManager(npmRepositoryManagerId);
    Repository npmRepo = tempEntity.newRepository(npmRepoManager, npmRepositoryPublicName, RepositoryType.hosted,
        ComponentIdentifier.FORMAT_NPM);

    tempEntity.newProprietaryComponentNamePattern(npmRepo, null, componentNamespaces[0]);
    tempEntity.newProprietaryComponentNamePattern(mvnRepo, componentNamespaces[1], null);
    tempEntity.newProprietaryComponentNamePattern(mvnRepo, componentNamespaces[2], null);
    tempEntity.newProprietaryComponentNamePattern(npmRepo, null, componentNamespaces[3]);
    tempEntity.newProprietaryComponentNamePattern(mvnRepo, componentNamespaces[4], null);
    tempEntity.newProprietaryComponentNamePattern(mvnRepo, componentNamespaces[5], null);
    tempEntity.newProprietaryComponentNamePattern(mvnRepo, componentNamespaces[6], null);
    tempEntity.newProprietaryComponentNamePattern(npmRepo, null, componentNamespaces[7]);
    tempEntity.newProprietaryComponentNamePattern(npmRepo, null, componentNamespaces[8]);
    tempEntity.newProprietaryComponentNamePattern(mvnRepo, componentNamespaces[9], null);
    tempEntity.newProprietaryComponentNamePattern(npmRepo, null, componentNamespaces[10]);
    tempEntity.newProprietaryComponentNamePattern(mvnRepo, componentNamespaces[11], null);
    tempEntity.newProprietaryComponentNamePattern(mvnRepo, componentNamespaces[12], null);

    refresh();

    NamespaceConfusionProtectionTile namespaceConfusionProtectionTile =
        RepositoriesSummaryPage.namespaceConfusionProtectionTile();

    ScrollUtil.scrollIntoView(namespaceConfusionProtectionTile.getElement(), false);

    namespaceConfusionProtectionTile.tableBodyRows().shouldHave(size(6));

    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(0).shouldHave(text(componentNamespaces[0]));
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(1).shouldHave(text(componentNamespaces[1]));
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(2).shouldHave(text(componentNamespaces[2]));
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(3).shouldHave(text(componentNamespaces[3]));
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(4).shouldHave(text(componentNamespaces[4]));
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(5).shouldHave(text(componentNamespaces[5]));

    namespaceConfusionProtectionTile.shouldBe(visible);
    namespaceConfusionProtectionTile.previousPageBtn().shouldNotBe(visible);
    namespaceConfusionProtectionTile.nextPageBtn().shouldBe(visible).shouldHave(attribute("aria-label", "next page"));

    namespaceConfusionProtectionTile.nextPageBtn().click();

    namespaceConfusionProtectionTile.previousPageBtn()
        .shouldBe(visible)
        .shouldHave(attribute("aria-label", "previous page"));
    namespaceConfusionProtectionTile.nextPageBtn().shouldBe(visible).shouldHave(attribute("aria-label", "next page"));

    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(0).shouldHave(text(componentNamespaces[6]));
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(1).shouldHave(text(componentNamespaces[7]));
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(2).shouldHave(text(componentNamespaces[8]));
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(3).shouldHave(text(componentNamespaces[9]));
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(4).shouldHave(text(componentNamespaces[10]));
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(5).shouldHave(text(componentNamespaces[11]));

    namespaceConfusionProtectionTile.nextPageBtn().click();

    namespaceConfusionProtectionTile.tableBodyRows().shouldHave(size(1));
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(0).shouldHave(text(componentNamespaces[12]));
    namespaceConfusionProtectionTile.previousPageBtn()
        .shouldBe(visible)
        .shouldHave(attribute("aria-label", "previous page"));
    namespaceConfusionProtectionTile.nextPageBtn().shouldNotBe(visible);

    namespaceConfusionProtectionTile.previousPageBtn().click();

    namespaceConfusionProtectionTile.tableBodyRows().shouldHave(size(6));
    namespaceConfusionProtectionTile.previousPageBtn()
        .shouldBe(visible)
        .shouldHave(attribute("aria-label", "previous page"));
    namespaceConfusionProtectionTile.nextPageBtn().shouldBe(visible).shouldHave(attribute("aria-label", "next page"));

    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(0).shouldHave(text(componentNamespaces[6]));
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(1).shouldHave(text(componentNamespaces[7]));
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(2).shouldHave(text(componentNamespaces[8]));
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(3).shouldHave(text(componentNamespaces[9]));
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(4).shouldHave(text(componentNamespaces[10]));
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(5).shouldHave(text(componentNamespaces[11]));

    namespaceConfusionProtectionTile.previousPageBtn().click();

    namespaceConfusionProtectionTile.tableBodyRows().shouldHave(size(6));
    namespaceConfusionProtectionTile.previousPageBtn().shouldNotBe(visible);
    namespaceConfusionProtectionTile.nextPageBtn().shouldBe(visible).shouldHave(attribute("aria-label", "next page"));

    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(0).shouldHave(text(componentNamespaces[0]));
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(1).shouldHave(text(componentNamespaces[1]));
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(2).shouldHave(text(componentNamespaces[2]));
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(3).shouldHave(text(componentNamespaces[3]));
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(4).shouldHave(text(componentNamespaces[4]));
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(5).shouldHave(text(componentNamespaces[5]));
  }

  @Test
  public void testNamespaceConfusionProtection_MultiSorting() {
    String[] componentNamespaces = {"b-social", "underscore", "lodash", "moment", "express", "ant"};
    String[] repositoryManagerInstanceIds =
        {"9E111629-6B9EDCBA-B5989887-132718F9-8C354DFB", "1E111629-6B9EDCBA-B5989887-132718F9-8C354DFB"};
    String[] repositoryPublicIds = {"my-hosted-npm", "custom-hosted-maven", "custom-hosted-npm"};
    RepositoryManager repoManager1 = tempEntity.newRepositoryManager(repositoryManagerInstanceIds[0]);
    Repository repo1 = tempEntity.newRepository(repoManager1, repositoryPublicIds[1], RepositoryType.hosted,
        ComponentIdentifier.FORMAT_MAVEN);
    RepositoryManager repoManager2 = tempEntity.newRepositoryManager(repositoryManagerInstanceIds[1]);
    Repository repo2 = tempEntity.newRepository(repoManager2, repositoryPublicIds[0], RepositoryType.hosted,
        ComponentIdentifier.FORMAT_NPM);
    Repository repo3 = tempEntity.newRepository(repoManager2, repositoryPublicIds[2], RepositoryType.hosted,
        ComponentIdentifier.FORMAT_NPM);

    tempEntity.newProprietaryComponentNamePattern(repo1, componentNamespaces[0], null);
    tempEntity.newProprietaryComponentNamePattern(repo2, null, componentNamespaces[1]);
    tempEntity.newProprietaryComponentNamePattern(repo2, null, componentNamespaces[2]);
    tempEntity.newProprietaryComponentNamePattern(repo3, null, componentNamespaces[3]);
    tempEntity.newProprietaryComponentNamePattern(repo3, null, componentNamespaces[4]);
    tempEntity.newProprietaryComponentNamePattern(repo1, componentNamespaces[5], null);

    refresh();

    NamespaceConfusionProtectionTile namespaceConfusionProtectionTile =
        RepositoriesSummaryPage.namespaceConfusionProtectionTile();
    ScrollUtil.scrollIntoView(namespaceConfusionProtectionTile.getElement(), true);

    namespaceConfusionProtectionTile.tableBodyRows().shouldHave(size(6));

    namespaceConfusionProtectionTile.componentNamespaceHeaderSortBtn()
        .shouldHave(attribute("aria-label", "Component Namespace ascending"));

    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(0).shouldHave(text(componentNamespaces[5]));
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(1).shouldHave(text(componentNamespaces[0]));
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(2).shouldHave(text(componentNamespaces[4]));
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(3).shouldHave(text(componentNamespaces[2]));
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(4).shouldHave(text(componentNamespaces[3]));
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(5).shouldHave(text(componentNamespaces[1]));

    namespaceConfusionProtectionTile.componentNamespaceHeaderSortBtn().click();
    namespaceConfusionProtectionTile.componentNamespaceHeaderSortBtn()
        .shouldHave(attribute("aria-label", "Component Namespace descending"));

    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(0).shouldHave(text(componentNamespaces[1]));
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(1).shouldHave(text(componentNamespaces[3]));
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(2).shouldHave(text(componentNamespaces[2]));
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(3).shouldHave(text(componentNamespaces[4]));
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(4).shouldHave(text(componentNamespaces[0]));
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(5).shouldHave(text(componentNamespaces[5]));

    namespaceConfusionProtectionTile.repositoryManagerHeaderSortBtn()
        .shouldHave(attribute("aria-label", "Repository Manager unsorted"));

    namespaceConfusionProtectionTile.repositoryManagerHeaderSortBtn().click();
    namespaceConfusionProtectionTile.repositoryManagerHeaderSortBtn()
        .shouldHave(attribute("aria-label", "Repository Manager ascending"));

    namespaceConfusionProtectionTile.repositoryManagerIdColumnCells()
        .get(0)
        .shouldHave(text(repositoryManagerInstanceIds[1]));
    namespaceConfusionProtectionTile.repositoryManagerIdColumnCells()
        .get(1)
        .shouldHave(text(repositoryManagerInstanceIds[1]));
    namespaceConfusionProtectionTile.repositoryManagerIdColumnCells()
        .get(2)
        .shouldHave(text(repositoryManagerInstanceIds[1]));
    namespaceConfusionProtectionTile.repositoryManagerIdColumnCells()
        .get(3)
        .shouldHave(text(repositoryManagerInstanceIds[1]));
    namespaceConfusionProtectionTile.repositoryManagerIdColumnCells()
        .get(4)
        .shouldHave(text(repositoryManagerInstanceIds[0]));
    namespaceConfusionProtectionTile.repositoryManagerIdColumnCells()
        .get(5)
        .shouldHave(text(repositoryManagerInstanceIds[0]));

    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(0).shouldHave(text(componentNamespaces[1]));
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(1).shouldHave(text(componentNamespaces[3]));
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(2).shouldHave(text(componentNamespaces[2]));
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(3).shouldHave(text(componentNamespaces[4]));
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(4).shouldHave(text(componentNamespaces[0]));
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(5).shouldHave(text(componentNamespaces[5]));

    namespaceConfusionProtectionTile.repositoryManagerHeaderSortBtn().click();
    namespaceConfusionProtectionTile.repositoryManagerHeaderSortBtn()
        .shouldHave(attribute("aria-label", "Repository Manager descending"));

    namespaceConfusionProtectionTile.repositoryManagerIdColumnCells()
        .get(0)
        .shouldHave(text(repositoryManagerInstanceIds[0]));
    namespaceConfusionProtectionTile.repositoryManagerIdColumnCells()
        .get(1)
        .shouldHave(text(repositoryManagerInstanceIds[0]));
    namespaceConfusionProtectionTile.repositoryManagerIdColumnCells()
        .get(2)
        .shouldHave(text(repositoryManagerInstanceIds[1]));
    namespaceConfusionProtectionTile.repositoryManagerIdColumnCells()
        .get(3)
        .shouldHave(text(repositoryManagerInstanceIds[1]));
    namespaceConfusionProtectionTile.repositoryManagerIdColumnCells()
        .get(4)
        .shouldHave(text(repositoryManagerInstanceIds[1]));
    namespaceConfusionProtectionTile.repositoryManagerIdColumnCells()
        .get(5)
        .shouldHave(text(repositoryManagerInstanceIds[1]));

    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(0).shouldHave(text(componentNamespaces[0]));
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(1).shouldHave(text(componentNamespaces[5]));
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(2).shouldHave(text(componentNamespaces[1]));
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(3).shouldHave(text(componentNamespaces[3]));
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(4).shouldHave(text(componentNamespaces[2]));
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(5).shouldHave(text(componentNamespaces[4]));

    namespaceConfusionProtectionTile.hostedRepositoryNameHeaderSortBtn()
        .shouldHave(attribute("aria-label", "Repository unsorted"));

    namespaceConfusionProtectionTile.hostedRepositoryNameHeaderSortBtn().click();
    namespaceConfusionProtectionTile.hostedRepositoryNameHeaderSortBtn()
        .shouldHave(attribute("aria-label", "Repository ascending"));

    namespaceConfusionProtectionTile.hostedRepositoryNameColumnCells().get(0).shouldHave(text(repositoryPublicIds[1]));
    namespaceConfusionProtectionTile.hostedRepositoryNameColumnCells().get(1).shouldHave(text(repositoryPublicIds[1]));
    namespaceConfusionProtectionTile.hostedRepositoryNameColumnCells().get(2).shouldHave(text(repositoryPublicIds[2]));
    namespaceConfusionProtectionTile.hostedRepositoryNameColumnCells().get(3).shouldHave(text(repositoryPublicIds[2]));
    namespaceConfusionProtectionTile.hostedRepositoryNameColumnCells().get(4).shouldHave(text(repositoryPublicIds[0]));
    namespaceConfusionProtectionTile.hostedRepositoryNameColumnCells().get(5).shouldHave(text(repositoryPublicIds[0]));

    namespaceConfusionProtectionTile.repositoryManagerIdColumnCells()
        .get(0)
        .shouldHave(text(repositoryManagerInstanceIds[0]));
    namespaceConfusionProtectionTile.repositoryManagerIdColumnCells()
        .get(1)
        .shouldHave(text(repositoryManagerInstanceIds[0]));
    namespaceConfusionProtectionTile.repositoryManagerIdColumnCells()
        .get(2)
        .shouldHave(text(repositoryManagerInstanceIds[1]));
    namespaceConfusionProtectionTile.repositoryManagerIdColumnCells()
        .get(3)
        .shouldHave(text(repositoryManagerInstanceIds[1]));
    namespaceConfusionProtectionTile.repositoryManagerIdColumnCells()
        .get(4)
        .shouldHave(text(repositoryManagerInstanceIds[1]));
    namespaceConfusionProtectionTile.repositoryManagerIdColumnCells()
        .get(5)
        .shouldHave(text(repositoryManagerInstanceIds[1]));

    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(0).shouldHave(text(componentNamespaces[0]));
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(1).shouldHave(text(componentNamespaces[5]));
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(2).shouldHave(text(componentNamespaces[3]));
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(3).shouldHave(text(componentNamespaces[4]));
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(4).shouldHave(text(componentNamespaces[1]));
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(5).shouldHave(text(componentNamespaces[2]));

    namespaceConfusionProtectionTile.hostedRepositoryNameHeaderSortBtn().click();
    namespaceConfusionProtectionTile.hostedRepositoryNameHeaderSortBtn()
        .shouldHave(attribute("aria-label", "Repository descending"));

    namespaceConfusionProtectionTile.hostedRepositoryNameColumnCells().get(0).shouldHave(text(repositoryPublicIds[0]));
    namespaceConfusionProtectionTile.hostedRepositoryNameColumnCells().get(1).shouldHave(text(repositoryPublicIds[0]));
    namespaceConfusionProtectionTile.hostedRepositoryNameColumnCells().get(2).shouldHave(text(repositoryPublicIds[2]));
    namespaceConfusionProtectionTile.hostedRepositoryNameColumnCells().get(3).shouldHave(text(repositoryPublicIds[2]));
    namespaceConfusionProtectionTile.hostedRepositoryNameColumnCells().get(4).shouldHave(text(repositoryPublicIds[1]));
    namespaceConfusionProtectionTile.hostedRepositoryNameColumnCells().get(5).shouldHave(text(repositoryPublicIds[1]));

    namespaceConfusionProtectionTile.repositoryManagerIdColumnCells()
        .get(0)
        .shouldHave(text(repositoryManagerInstanceIds[1]));
    namespaceConfusionProtectionTile.repositoryManagerIdColumnCells()
        .get(1)
        .shouldHave(text(repositoryManagerInstanceIds[1]));
    namespaceConfusionProtectionTile.repositoryManagerIdColumnCells()
        .get(2)
        .shouldHave(text(repositoryManagerInstanceIds[1]));
    namespaceConfusionProtectionTile.repositoryManagerIdColumnCells()
        .get(3)
        .shouldHave(text(repositoryManagerInstanceIds[1]));
    namespaceConfusionProtectionTile.repositoryManagerIdColumnCells()
        .get(4)
        .shouldHave(text(repositoryManagerInstanceIds[0]));
    namespaceConfusionProtectionTile.repositoryManagerIdColumnCells()
        .get(5)
        .shouldHave(text(repositoryManagerInstanceIds[0]));

    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(0).shouldHave(text(componentNamespaces[1]));
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(1).shouldHave(text(componentNamespaces[2]));
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(2).shouldHave(text(componentNamespaces[3]));
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(3).shouldHave(text(componentNamespaces[4]));
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(4).shouldHave(text(componentNamespaces[0]));
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(5).shouldHave(text(componentNamespaces[5]));
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
    accessTile.accessLists().shouldHave(size(HIERARCHY_SIZE));

    AccessTileList localList = accessTile.accessList(0);

    localList.ownerName().shouldBe(visible).shouldHave(text("Local"));
    localList.emptyDescriptor().should(exist);

    InheritedAccessList inheritedAccessList = accessTile.inheritedAccessList("ROOT_ORGANIZATION_ID");

    accessTile.accessListSubheader(0).shouldBe(visible).shouldHave(AccessTile.inheritedText("Root Organization"));
    inheritedAccessList.elements().shouldHave(size(2));

    InheritedAccess readOnly = inheritedAccessList.element(0);
    readOnly.label().shouldBe(visible).shouldHave(text("Read Only"));

    AccessTileListElement descriptionRead = readOnly.description();
    descriptionRead.chevron().shouldBe(hidden);
    descriptionRead.groupIcon().shouldBe(visible);
    descriptionRead.members().shouldBe(visible).shouldHave(text("Group"));

    InheritedAccess writeOnly = inheritedAccessList.element(1);
    writeOnly.label().shouldBe(visible).shouldHave(text("Write Only"));

    AccessTileListElement descriptionWrite = writeOnly.description();
    descriptionWrite.chevron().shouldBe(hidden);
    descriptionWrite.userIcon().shouldBe(visible);
    descriptionWrite.members().shouldBe(visible).shouldHave(text(testUser.calculateDisplayName()));
  }

  private void setupDataForSorting() {
    List<Repository> repositories = new ArrayList<>();
    RepositoryManager rm2 = tempEntity.newRepositoryManager("rm2");
    RepositoryManager rm1 = tempEntity.newRepositoryManager("rm1");

    repositories.add(tempEntity.newProxyRepository(rm1, "i", "maven", true, false));
    repositories.add(tempEntity.newProxyRepository(rm1, "b", "maven", true, true));
    repositories.add(tempEntity.newProxyRepository(rm1, "g", "npm", false, false));
    repositories.add(tempEntity.newHostedRepository(rm1, "d", "npm", true));
    repositories.add(tempEntity.newHostedRepository(rm1, "e", "maven", true));

    repositories.add(tempEntity.newProxyRepository(rm2, "c", "maven", true, false));
    repositories.add(tempEntity.newProxyRepository(rm2, "f", "maven", true, true));
    repositories.add(tempEntity.newProxyRepository(rm2, "h", "npm", false, false));
    repositories.add(tempEntity.newHostedRepository(rm2, "a", "npm", true));
    repositories.add(tempEntity.newHostedRepository(rm2, "j", "maven", true));
  }

  @Test
  public void testRepositoryConfigurationTableSorting_CaseInsensitivity() {
    List<Repository> repositories = new ArrayList<>();
    RepositoryManager rm2 = tempEntity.newRepositoryManager("df");
    RepositoryManager rm1 = tempEntity.newRepositoryManager("De");
    RepositoryManager rm3 = tempEntity.newRepositoryManager("ee");
    repositories.add(tempEntity.newRepository(rm2, "ac", true));
    repositories.add(tempEntity.newRepository(rm3, "Ab", false));
    repositories.add(tempEntity.newRepository(rm1, "bb", true));

    refreshOrOpen(RepositoryResultsSummaryPage.url());

    RepositoryResultsSummaryPage.repositoriesTableRepositoryNameHeaderSortBtn()
        .shouldHave(
            attribute("aria-label", "Repository ascending"));
    assertRowOrder("De", "bb", "df", "ac", "ee", "Ab");
  }

  @Test
  public void testRepositoryConfigurationTableSorting_Default() {
    setupDataForSorting();
    refreshOrOpen(RepositoryResultsSummaryPage.url());

    RepositoryResultsSummaryPage.configurationTile().shouldBe(visible).shouldHave(text("Configuration"));
    RepositoryResultsSummaryPage.repositoriesTableRepositoryNameHeaderSortBtn().shouldBe(visible);
    RepositoryResultsSummaryPage.repositoriesTableRepositoryFormatHeaderSortBtn().shouldBe(visible);
    RepositoryResultsSummaryPage.repositoriesTableRepositoryTypeHeaderSortBtn().shouldBe(visible);
    RepositoryResultsSummaryPage.repositoriesTableRepositoryNameHeaderSortBtn()
        .shouldHave(
            attribute("aria-label", "Repository ascending"));
    RepositoryResultsSummaryPage.repositoriesTableRepositoryFormatHeaderSortBtn()
        .shouldHave(
            attribute("aria-label", "Format unsorted"));
    RepositoryResultsSummaryPage.repositoriesTableRepositoryTypeHeaderSortBtn()
        .shouldHave(
            attribute("aria-label", "Type unsorted"));

    assertRowOrder("rm1", "b", "d", "e", "g", "i", "rm2", "a", "c", "f", "h", "j");
  }

  @Test
  public void testRepositoryConfigurationTableSorting_DefaultWithEmptyRepositoryManager() {
    RepositoryManager rm1 = tempEntity.newRepositoryManager("rm1");
    tempEntity.newRepositoryManager("rm2");
    tempEntity.newProxyRepository(rm1, "i", "maven", true, false);
    tempEntity.newProxyRepository(rm1, "b", "maven", true, true);
    tempEntity.newProxyRepository(rm1, "g", "npm", false, false);

    refreshOrOpen(RepositoryResultsSummaryPage.url());
    RepositoryResultsSummaryPage.configurationTile().shouldBe(visible).shouldHave(text("Configuration"));

    assertRowOrder("rm1", "b", "g", "i", "rm2", "There are no repositories registered with the server.");
  }

  private void assertRowOrder(String... ids) {
    RepositoryConfigurationTile configurationTile = RepositoriesSummaryPage.configTile();
    ConfigurationTable configurationTable = configurationTile.configurationTable();
    configurationTable.shouldBe(visible);

    for (int i = 0; i < ids.length; i++) {
      ScrollUtil.scrollIntoView(configurationTable.rows().get(i + 2));
      configurationTable.rows().get(i + 2).shouldHave(matchText(ids[i] + ".*"));
    }
  }

  @Test
  public void testRepositoryConfigurationTableSorting_RepositoryNameDescending() {
    setupDataForSorting();
    refreshOrOpen(RepositoryResultsSummaryPage.url());

    RepositoryResultsSummaryPage.repositoriesTableRepositoryNameHeaderSortBtn().click();
    RepositoryResultsSummaryPage.repositoriesTableRepositoryNameHeaderSortBtn()
        .shouldHave(
            attribute("aria-label", "Repository descending"));

    assertRowOrder("rm1", "i", "g", "e", "d", "b", "rm2", "j", "h", "f", "c", "a");
  }

  @Test
  public void testRepositoryConfigurationTableSorting_withEmptyAndNotEmptyRepoManagers() {
    WebDriverRunner.getWebDriver().manage().window().setSize(new Dimension(1366, 1800));

    RepositoryManager rm1 = tempEntity.newRepositoryManager("rmA");
    tempEntity.newProxyRepository(rm1, "a", "maven", true, false);
    tempEntity.newProxyRepository(rm1, "b", "maven", true, true);
    tempEntity.newProxyRepository(rm1, "c", "npm", false, false);
    RepositoryManager rm2 = tempEntity.newRepositoryManager("rmB");
    tempEntity.newProxyRepository(rm2, "d", "maven", true, false);
    tempEntity.newProxyRepository(rm2, "e", "maven", true, true);
    tempEntity.newProxyRepository(rm2, "f", "npm", false, false);
    tempEntity.newRepositoryManager("instanceId3", "rmAE", "repoProductName3",
        "repoProductVersion3");
    RepositoryManager rm3 = tempEntity.newRepositoryManager("instanceId4", "rmBC", "repoProductName4",
        "repoProductVersion4");
    tempEntity.newProxyRepository(rm3, "ad", "maven", true, false);
    tempEntity.newProxyRepository(rm3, "ae", "maven", true, true);
    tempEntity.newProxyRepository(rm3, "af", "npm", false, false);
    tempEntity.newRepositoryManager("instanceId5", "emptyrmC", "repoProductName5",
        "repoProductVersion5");

    refreshOrOpen(RepositoryResultsSummaryPage.url());

    assertRowOrder(
        "emptyrmC", "There are no repositories registered with the server.",
        "rmA", "a", "b", "c", "rmAE", "There are no repositories registered with the server.",
        "rmB", "d", "e", "f", "rmBC", "ad", "ae", "af");

    RepositoryResultsSummaryPage.repositoriesTableRepositoryNameHeaderSortBtn().click();
    RepositoryResultsSummaryPage.repositoriesTableRepositoryNameHeaderSortBtn()
        .shouldHave(
            attribute("aria-label", "Repository descending"));

    RepositoryConfigurationTile configurationTile = RepositoriesSummaryPage.configTile();
    ConfigurationTable configurationTable = configurationTile.configurationTable();
    configurationTable.shouldBe(visible);

    assertRowOrder(
        "emptyrmC", "There are no repositories registered with the server.",
        "rmA", "c", "b", "a", "rmAE", "There are no repositories registered with the server.",
        "rmB", "f", "e", "d", "rmBC", "af", "ae", "ad");
  }

  @Test
  public void testRepositoryConfigurationTableSorting_RepositoryNameAndRepositoryFormat() {
    setupDataForSorting();
    refreshOrOpen(RepositoryResultsSummaryPage.url());

    RepositoryResultsSummaryPage.repositoriesTableRepositoryNameHeaderSortBtn().click();
    RepositoryResultsSummaryPage.repositoriesTableRepositoryNameHeaderSortBtn()
        .shouldHave(
            attribute("aria-label", "Repository descending"));
    RepositoryResultsSummaryPage.repositoriesTableRepositoryFormatHeaderSortBtn().click();
    RepositoryResultsSummaryPage.repositoriesTableRepositoryFormatHeaderSortBtn()
        .shouldHave(
            attribute("aria-label", "Format ascending"));

    assertRowOrder("rm1", "i", "e", "b", "g", "d", "rm2", "j", "f", "c", "h", "a");

    ScrollUtil.scrollIntoView(
        RepositoryResultsSummaryPage.repositoriesTableRepositoryFormatHeaderSortBtn().getElement());
    RepositoryResultsSummaryPage.repositoriesTableRepositoryFormatHeaderSortBtn().click();
    RepositoryResultsSummaryPage.repositoriesTableRepositoryFormatHeaderSortBtn()
        .shouldHave(
            attribute("aria-label", "Format descending"));

    assertRowOrder("rm1", "g", "d", "i", "e", "b", "rm2", "h", "a", "j", "f", "c");
  }

  @Test
  public void testRepositoryConfigurationTableSorting_RepositoryNameAndRepositoryType() {
    setupDataForSorting();
    refreshOrOpen(RepositoryResultsSummaryPage.url());

    RepositoryResultsSummaryPage.repositoriesTableRepositoryNameHeaderSortBtn().click();
    RepositoryResultsSummaryPage.repositoriesTableRepositoryNameHeaderSortBtn()
        .shouldHave(
            attribute("aria-label", "Repository descending"));
    RepositoryResultsSummaryPage.repositoriesTableRepositoryTypeHeaderSortBtn().click();
    RepositoryResultsSummaryPage.repositoriesTableRepositoryTypeHeaderSortBtn()
        .shouldHave(
            attribute("aria-label", "Type ascending"));

    assertRowOrder("rm1", "e", "d", "i", "g", "b", "rm2", "j", "a", "h", "f", "c");

    ScrollUtil.scrollIntoView(RepositoryResultsSummaryPage.repositoriesTableRepositoryTypeHeaderSortBtn().getElement());
    RepositoryResultsSummaryPage.repositoriesTableRepositoryTypeHeaderSortBtn().click();
    RepositoryResultsSummaryPage.repositoriesTableRepositoryTypeHeaderSortBtn()
        .shouldHave(
            attribute("aria-label", "Type descending"));

    assertRowOrder("rm1", "i", "g", "b", "e", "d", "rm2", "h", "f", "c", "j", "a");
  }

  @Test
  public void testPolicyTile_NoPolicies() {
    // Sanity Check
    RepositoriesSummaryTile summaryTile = RepositoriesSummaryPage.summaryTile();
    summaryTile.name().shouldBe(visible).shouldHave(text("Repository Managers"));

    PolicyTile policyTile = RepositoriesSummaryPage.policyTile();
    policyTile.shouldBe(visible);
    policyTile.newButton().shouldBe(visible);
    policyTile.policyLists().shouldHave(size(0));
    policyTile.localEmptyDescriptor().shouldBe(visible).shouldHave(text("No local"));
  }

  @Test
  public void testPolicyTile_InheritedPolicies() {
    Owner parentOwner = organizationDAO.getByIdNotNull(ROOT_ORGANIZATION_ID);

    List<Policy> inheritedPolicies = new ArrayList<>();

    inheritedPolicies.add(tempEntity.newPolicy(parentOwner.getId(), "Policy 1 " + parentOwner.getName(), 10,
        Action.ID_FAIL, Stage.ID_BUILD, null));
    inheritedPolicies.add(tempEntity.newPolicy(parentOwner.getId(), "Policy 2 " + parentOwner.getName(), 5,
        Action.ID_WARN, Stage.ID_BUILD, null));

    WebDriverRunner.getWebDriver().manage().window().setSize(new Dimension(1800, 1200));

    refreshOrOpen(RepositoriesSummaryPage.url());

    PolicyTile policyTile = RepositoriesSummaryPage.policyTile();
    PolicyTileList policyTileList = policyTile.policyList(1);
    policyTileList.ownerName().shouldHave(text("Inherited from"));

    // The plus one is added because the rows method selects the table header
    policyTileList.rows().shouldHave(size(inheritedPolicies.size() + 1));

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
    ScrollUtil.scrollIntoView(policyTileList.row(2).getElement());
    policyTileList.row(2).threatLegend().shouldHave(text("5"));
    policyTileList.row(2).name().shouldHave(text("Policy 2 Root Organization"));
    policyTileList.row(2).proxy().shouldHave(PolicyTile.noActionText());
    policyTileList.row(2).develop().shouldHave(PolicyTile.noActionText());
    policyTileList.row(2).source().shouldHave(PolicyTile.noActionText());
    policyTileList.row(2).build().shouldHave(text("Warn"));
    policyTileList.row(2).stageRelease().shouldHave(PolicyTile.noActionText());
    policyTileList.row(2).release().shouldHave(PolicyTile.noActionText());
    policyTileList.row(2).operate().shouldHave(PolicyTile.noActionText());

    policyTileList.ownerName().click();
    policyTileList.rows().shouldHave(size(1)); // no rows only collapsible row header is visible

    policyTileList.ownerName().click();
    policyTileList.rows().shouldHave(size(3)); // rows plus header row

    policyTileList.row(1).chevron().shouldBe(visible);
    policyTileList.row(1).click();
    waitUntilUrl(PolicyEditorPage.firewallUrlToEdit(OwnerType.REPOSITORY_CONTAINER,
        RepositoryContainer.REPOSITORY_CONTAINER_ID, inheritedPolicies.get(0).getId()));
    PolicyEditorPage.title().shouldHave(Condition.text("View Policy"));

    NxBreadcrumb breadcrumb = new NxBreadcrumb();
    breadcrumb.links().shouldHave(size(3));
    breadcrumb.links().get(1).shouldHave(Condition.text("Repository Managers"));
    breadcrumb.links().get(1).click();

    waitUntilUrl(RepositoriesSummaryPage.url());
  }

  @Test
  public void testNavigationPills() {
    WebDriverRunner.getWebDriver().manage().window().setSize(new Dimension(1800, 1200));

    RepositoriesSummaryTile repositoriesSummaryTile = RepositoriesSummaryPage.summaryTile();
    repositoriesSummaryTile.name().shouldBe(visible).shouldHave(text("Repository Managers"));
    RepositoriesSummaryPage.repositoriesPillConfigurationButton().shouldBe(visible).click();
    RepositoryConfigurationTile configurationTile = RepositoriesSummaryPage.configTile();
    configurationTile.emptyDescriptor().shouldBe(visible).shouldHave(EMPTY_LIST_TEXT);

    RepositoriesSummaryPage.policyPillButton().shouldBe(visible).click();
    PolicyTile policyTile = RepositoriesSummaryPage.policyTile();
    policyTile.shouldHave(visible);
    policyTile.newButton().shouldBe(visible);
    policyTile.policyLists().shouldHave(size(0));
    policyTile.localEmptyDescriptor().shouldBe(visible).shouldHave(text("No local"));

    RepositoriesSummaryPage.namespaceConfusionProtectionPillButton().shouldBe(visible).click();
    NamespaceConfusionProtectionTile namespaceConfusionProtectionTile =
        RepositoriesSummaryPage.namespaceConfusionProtectionTile();
    namespaceConfusionProtectionTile.shouldBe(visible);
    namespaceConfusionProtectionTile.shouldBe(visible).shouldHave(text("Namespace Confusion Protection"));
    namespaceConfusionProtectionTile.emptyDescriptor().shouldBe(visible).shouldHave(text("No results"));

    RepositoriesSummaryPage.accessPillButton().shouldBe(visible).click();
    AccessTile accessTile = RepositoriesSummaryPage.accessTile();
    accessTile.shouldBe(visible);
    AccessTileList list = accessTile.accessList(0);
    list.emptyDescriptor().should(exist);
  }

  @Test
  public void testRepositoryManagerSummaryView_policyTile() {
    RepositoryManager repositoryManager = tempEntity
        .newRepositoryManager("5E7BCC8D-3FAB6390-83FF543B-ECD79639-D031F7AE");

    Owner rootOrgOwner = organizationDAO.getByIdNotNull(ROOT_ORGANIZATION_ID);

    List<Policy> localToRepositoryManagerPolicies = new ArrayList<>();
    localToRepositoryManagerPolicies.add(tempEntity.newPolicy(repositoryManager.getId(),
        "Policy local 1 " + repositoryManager.getName(), 10, Action.ID_FAIL, Stage.ID_BUILD, null));

    List<Policy> inheritedFromRootOrgPolicies = new ArrayList<>();
    inheritedFromRootOrgPolicies.add(tempEntity.newPolicy(rootOrgOwner.getId(), "Policy 1 " + rootOrgOwner.getName(),
        10, Action.ID_FAIL, Stage.ID_BUILD, null));
    inheritedFromRootOrgPolicies.add(tempEntity.newPolicy(rootOrgOwner.getId(), "Policy 2 " + rootOrgOwner.getName(),
        5, Action.ID_WARN, Stage.ID_BUILD, null));

    refreshOrOpen(RepositoriesSummaryPage.repositoryManagerUrl(repositoryManager.getId()));
    waitUntilUrl(RepositoriesSummaryPage.repositoryManagerUrl(repositoryManager.getId()));

    RepositoriesSummaryTile summaryTile = RepositoriesSummaryPage.summaryTile();
    summaryTile.name().shouldBe(visible).shouldHave(text(repositoryManager.getName()));

    PolicyTile policyTile = RepositoriesSummaryPage.policyTile();
    policyTile.shouldBe(visible);
    policyTile.policyLists().shouldHave(size(3));

    PolicyTileList policyTileLocalList = policyTile.policyList(0);
    policyTileLocalList.ownerName().shouldHave(text("Local to " + repositoryManager.getName()));
    policyTileLocalList.rows().shouldHave(size(localToRepositoryManagerPolicies.size() + 1));

    String repositoryContainerName = RepositoryContainer.SINGLETON.getName();
    PolicyTileList policyTileRepositoryContainerList = policyTile.policyList(1);
    policyTileRepositoryContainerList.ownerName().shouldHave(text("Inherited from " + repositoryContainerName));
    policyTileRepositoryContainerList.rows().shouldHave(size(2));
    policyTileRepositoryContainerList.emptyDescriptor()
        .shouldHave(text("No " + repositoryContainerName + " policies defined"));

    PolicyTileList policyTileInheritedList = policyTile.policyList(2);
    policyTileInheritedList.ownerName().shouldHave(text("Inherited from " + rootOrgOwner.getName()));
    policyTileInheritedList.rows().shouldHave(size(inheritedFromRootOrgPolicies.size() + 1));

    eyesWatcher.eyesCheck("repository manager policies tile");

    policyTileInheritedList.row(1).click();
    waitUntilUrl(PolicyEditorPage.firewallUrlToEdit(repositoryManager.getType(), repositoryManager.getId(),
        inheritedFromRootOrgPolicies.get(0).getId()));

    SummarySection summarySection = PolicyEditorPage.summarySection();
    assertThat(summarySection.policyName().input().getValue()).isEqualTo(inheritedFromRootOrgPolicies.get(0).getName());

    eyesWatcher.eyesCheck("repository manager policy view page");
  }

  @Test
  public void testRepositoryManagerSummaryView_actionMenu() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();

    refreshOrOpen(RepositoriesSummaryPage.repositoryManagerUrl(repositoryManager.getId()));
    waitUntilUrl(RepositoriesSummaryPage.repositoryManagerUrl(repositoryManager.getId()));

    ActionDropDown.menu().shouldBe(hidden);
    ActionDropDown.actionButton().click();
    ActionDropDown.deleteOwnerButton().shouldHave(text(repositoryManager.getInstanceId())).shouldBe(visible);
    ActionDropDown.copyOrgIdButton().shouldBe(visible);
    ActionDropDown.editOwner().shouldHave(text("Repository Manager")).shouldBe(visible);
    ActionDropDown.actions().shouldHave(size(3));
  }

  @Test
  public void testRepositoryManagerSummaryView_delete() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();

    refreshOrOpen(RepositoriesSummaryPage.repositoryManagerUrl(repositoryManager.getId()));

    ActionDropDown.actionButton().click();
    ActionDropDown.deleteOwnerButton().click();

    NxDeleteModal deleteModal = new NxDeleteModal("#owner-delete-modal");
    deleteModal.header().shouldHave(text("Delete Repository Manager"));
    deleteModal.alertContent()
        .shouldHave(text("You are about to permanently remove " +
            repositoryManager.getInstanceId() + " and 0 descendants. This action cannot be undone."));

    deleteModal.submitButton().click();
    deleteModal.shouldNotBe(visible);

    waitUntilUrl(RepositoriesSummaryPage.url());
    OrgsAndPoliciesSidebar orgsAndPoliciesSidebar = OwnerSummaryPage.sidebar();
    NxCollapsible repoManagerList = orgsAndPoliciesSidebar.getRepoManagerList();
    repoManagerList.children().shouldHave(size(0));

    assertThat(repositoryManagerDAO.getById(repositoryManager.getId())).isNull();
  }

  @Test
  public void testRepositoryManagerSummaryView_cancelDelete() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();

    refreshOrOpen(RepositoriesSummaryPage.repositoryManagerUrl(repositoryManager.getId()));
    ActionDropDown.actionButton().click();
    ActionDropDown.deleteOwnerButton().click();

    NxDeleteModal deleteModal = new NxDeleteModal("#owner-delete-modal");
    deleteModal.header().shouldHave(text("Delete Repository Manager"));
    deleteModal.alertContent()
        .shouldHave(text("You are about to permanently remove " +
            repositoryManager.getInstanceId() + " and 0 descendants. This action cannot be undone."));

    deleteModal.closeButton().click();
    deleteModal.shouldNotBe(visible);

    waitUntilUrl(RepositoriesSummaryPage.repositoryManagerUrl(repositoryManager.getId()));

    refreshOrOpen(RepositoriesSummaryPage.url());
    OrgsAndPoliciesSidebar orgsAndPoliciesSidebar = OwnerSummaryPage.sidebar();
    NxCollapsible repoManagerList = orgsAndPoliciesSidebar.getRepoManagerList();
    repoManagerList.click();
    repoManagerList.children().shouldHave(size(1));
  }

  @Test
  public void testRepositoryManagerSummaryView_namespaceConfusionProtectionTile() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager("instanceId");
    Repository repo = tempEntity.newRepository(repoManager, "maven-hosted", RepositoryType.hosted,
        ComponentIdentifier.FORMAT_MAVEN);
    ProprietaryComponentNamePattern namePattern =
        tempEntity.newProprietaryComponentNamePattern(repo, "test", null, true);

    refreshOrOpen(RepositoriesSummaryPage.repositoryManagerUrl(repoManager.getId()));
    waitUntilUrl(RepositoriesSummaryPage.repositoryManagerUrl(repoManager.getId()));

    NamespaceConfusionProtectionTile namespaceConfusionProtectionTile =
        RepositoriesSummaryPage.namespaceConfusionProtectionTile();
    ScrollUtil.scrollIntoView(namespaceConfusionProtectionTile.getElement(), false);

    namespaceConfusionProtectionTile.tableBodyRows().shouldHave(size(1));

    namespaceConfusionProtectionTile.componentNamespaceColumnCells()
        .get(0)
        .shouldHave(text(namePattern.getNamespacePattern()));
    namespaceConfusionProtectionTile.repositoryManagerIdColumnCells().isEmpty();
    namespaceConfusionProtectionTile.hostedRepositoryNameColumnCells().get(0).shouldHave(text(repo.getPublicId()));
    namespaceConfusionProtectionTile.enabledToggleIndicators().get(0).shouldBe(enabled, selected);

    eyesWatcher.eyesCheck("repository manager namespace confusion protection tile");
  }

  @Test
  public void testRepositoryManagerSummaryView_rename() {
    // create a repository manager
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    refreshOrOpen(RepositoriesSummaryPage.repositoryManagerUrl(repositoryManager.getId()));

    // when the user clicks the edit button, the edit dialog should appear
    ActionDropDown.actionButton().click();
    ActionDropDown.editOwner().click();

    // and the new name is saved
    String newRepositoryManagerName = "New Name";
    OwnerEditorDialog.root().shouldBe(visible);
    OwnerEditorDialog.title().shouldHave(text("Edit Repository Manager"));
    OwnerEditorDialog.name().val(newRepositoryManagerName);
    OwnerEditorDialog.saveButton().click();
    FormMask.seeAndWaitForDismissal();

    // then the name change should be reflected in the summary view
    RepositoriesSummaryPage.summaryTile().name().shouldHave(text(newRepositoryManagerName));

    // and the name change should be reflected in the sidebar
    refreshOrOpen(RepositoriesSummaryPage.url());
    OrgsAndPoliciesSidebar orgsAndPoliciesSidebar = OwnerSummaryPage.sidebar();
    NxCollapsible repoManagerList = orgsAndPoliciesSidebar.getRepoManagerList();
    repoManagerList.click();
    repoManagerList.children().shouldHave(size(1));
    repoManagerList.children().get(0).shouldHave(text(newRepositoryManagerName));

    // and the name change should be reflected in the database
    assertThat(repositoryManagerDAO.getById(repositoryManager.getId()).getName()).isEqualTo(newRepositoryManagerName);
  }

  @Test
  public void testRepositoryManagerSummaryView_cancelRename() {
    // create a repository manager
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    refreshOrOpen(RepositoriesSummaryPage.repositoryManagerUrl(repositoryManager.getId()));

    // when the user clicks the edit button, the edit dialog should appear
    ActionDropDown.actionButton().click();
    ActionDropDown.editOwner().click();

    // and the dialog is dismissed without saving
    OwnerEditorDialog.root().shouldBe(visible);
    OwnerEditorDialog.title().shouldHave(text("Edit Repository Manager"));
    OwnerEditorDialog.cancelButton().click();
    FormMask.seeAndWaitForDismissal();

    // then the name change should be unchanged in the summary view
    RepositoriesSummaryPage.summaryTile().name().shouldHave(text(repositoryManager.getInstanceId()));

    // and the name should be unchanged in the sidebar
    refreshOrOpen(RepositoriesSummaryPage.url());
    OrgsAndPoliciesSidebar orgsAndPoliciesSidebar = OwnerSummaryPage.sidebar();
    NxCollapsible repoManagerList = orgsAndPoliciesSidebar.getRepoManagerList();
    repoManagerList.click();
    repoManagerList.children().shouldHave(size(1));
    repoManagerList.children().get(0).shouldHave(text(repositoryManager.getInstanceId()));

    // and the name should be unchanged in the database
    assertThat(repositoryManagerDAO.getById(repositoryManager.getId()).getName()).isEqualTo(
        repositoryManager.getInstanceId());
  }

  @Test
  public void testRepositoryManagerSummaryView_configTile() {
    RepositoryManager repositoryManager = tempEntity
        .newRepositoryManager("5E7BCC8D-3FAB6390-83FF543B-ECD79639-D031F7AE");
    Repository proxyRepo = tempEntity.newProxyRepository(repositoryManager, "proxy-repo", "maven", true, false);
    Repository hostedRepo = tempEntity.newHostedRepository(repositoryManager, "hosted-repo", "npm", true);

    refreshOrOpen(RepositoriesSummaryPage.repositoryManagerUrl(repositoryManager.getId()));
    waitUntilUrl(RepositoriesSummaryPage.repositoryManagerUrl(repositoryManager.getId()));

    RepositoryConfigurationTile configTile = RepositoriesSummaryPage.configTile();
    ConfigurationTable configTable = configTile.configurationTable();
    configTile.shouldBe(visible);
    // 1 header row, 1 row for header filters, 2 repository rows
    configTable.rows().shouldHave(size(4));

    configTable.repoManagerConfigTableRow(1)
        .repoManagerConfigTablePublicId()
        .shouldHave(text(hostedRepo.getPublicId()));
    configTable.repoManagerConfigTableRow(1).format().shouldHave(text(hostedRepo.getFormat()));
    configTable.repoManagerConfigTableRow(1).repositoryType().shouldHave(text(hostedRepo.getRepositoryType().name()));
    configTable.repoManagerConfigTableRow(1).enablement().shouldHave(text("Namespace Scanning"));

    configTable.repoManagerConfigTableRow(2).repoManagerConfigTablePublicId().shouldHave(text(proxyRepo.getPublicId()));
    configTable.repoManagerConfigTableRow(2).format().shouldHave(text(proxyRepo.getFormat()));
    configTable.repoManagerConfigTableRow(2).repositoryType().shouldHave(text(proxyRepo.getRepositoryType().name()));
    configTable.repoManagerConfigTableRow(2).enablement().shouldHave(text("Audit"));

    eyesWatcher.eyesCheck("repository manager configuration tile");

    configTable.repoManagerConfigTableRow(2).repoManagerConfigTableLink().click();

    waitUntilUrl(RepositoryReportContainerPage.url(proxyRepo.getId()));
    RepositoryReportContainerPage.title().shouldHave(text(proxyRepo.getName() + " Repository Results"));
    RepositoryReportContainerPage.backButton().click();

    waitUntilUrl(RepositoriesSummaryPage.repositoryManagerUrl(repositoryManager.getId()));
    testRepositorySummaryView_configurationTile_deleteRepository(configTable.repoManagerConfigTableRow(1), hostedRepo);
  }

  @Test
  public void testRepositoryManagerSummaryView_accessTile() {
    RepositoryManager repositoryManager = tempEntity
        .newRepositoryManager("5E7BCC8D-3FAB6390-83FF543B-ECD79639-D031F7AE");

    User testUser = tempEntity.newUser("testUser", "Test", "User", "testuser@sonatype.com");

    Role readRole = tempEntity.newRole("Local Read Only", false, Permission.READ);
    Role writeRole = tempEntity.newRole("Local Write Only", false, Permission.WRITE);
    Role repoContainerReadRole = tempEntity.newRole("RC Read Only", false, Permission.READ);
    Role repoContainerWriteRole = tempEntity.newRole("RC Write Only", false, Permission.WRITE);
    Role rootOrgWriteRole = tempEntity.newRole("RO Write Only", false, Permission.WRITE);
    Role rootOrgReadRole = tempEntity.newRole("RO Read Only", false, Permission.READ);

    tempEntity.newMembershipMapping(repositoryManager.getId(), writeRole.getId(), testUser.getUsername());
    tempEntity.newMembershipMapping(repositoryManager.getId(), readRole.getId(), "Group", MemberType.GROUP);
    tempEntity
        .newMembershipMapping(Organization.ROOT_ORGANIZATION_ID, rootOrgWriteRole.getId(), testUser.getUsername());
    tempEntity
        .newMembershipMapping(Organization.ROOT_ORGANIZATION_ID, rootOrgReadRole.getId(), "Group", MemberType.GROUP);
    tempEntity.newMembershipMapping(
        RepositoryContainer.REPOSITORY_CONTAINER_ID, repoContainerWriteRole.getId(), testUser.getUsername());
    tempEntity.newMembershipMapping(
        RepositoryContainer.REPOSITORY_CONTAINER_ID, repoContainerReadRole.getId(), "Group", MemberType.GROUP);

    refreshOrOpen(RepositoriesSummaryPage.repositoryManagerUrl(repositoryManager.getId()));
    waitUntilUrl(RepositoriesSummaryPage.repositoryManagerUrl(repositoryManager.getId()));
    RepositoriesSummaryPage.accessPillButton().shouldBe(visible).click();

    AccessTile accessTile = RepositoriesSummaryPage.accessTile();
    accessTile.accessLists().shouldHave(size(3));

    AccessTileList localList = accessTile.accessList(0);
    localList.emptyDescriptor().shouldBe(hidden);

    localList.elements().shouldHave(size(2));
    localList.ownerName().shouldBe(visible).shouldHave(text("Local"));

    AccessTileListElement readOnly = localList.element(0);
    readOnly.roleNoPermission().shouldBe(visible).shouldHave(text("Read Only"));

    AccessTileListElement descriptionRead = readOnly.description();
    descriptionRead.chevron().shouldBe(visible);
    descriptionRead.groupIcon().shouldBe(visible);
    descriptionRead.members().shouldBe(visible).shouldHave(text("Group"));

    AccessTileListElement writeOnly = localList.element(1);
    writeOnly.roleNoPermission().shouldBe(visible).shouldHave(text("Write Only"));

    AccessTileListElement descriptionWrite = writeOnly.description();
    descriptionWrite.chevron().shouldBe(visible);
    descriptionWrite.userIcon().shouldBe(visible);
    descriptionWrite.members().shouldBe(visible).shouldHave(text(testUser.calculateDisplayName()));

    InheritedAccessList inheritedAccessList =
        accessTile.inheritedAccessList(RepositoryContainer.REPOSITORY_CONTAINER_ID);

    accessTile.accessListSubheader(0).shouldBe(visible).shouldHave(AccessTile.inheritedText("Repository Managers"));
    inheritedAccessList.elements().shouldHave(size(2));

    InheritedAccess repoContainerAccessListReadRole = inheritedAccessList.element(0);
    repoContainerAccessListReadRole.label().shouldBe(visible).shouldHave(text("RC Read Only"));

    AccessTileListElement repoContainerAccessListReadRoleDescription = repoContainerAccessListReadRole.description();
    repoContainerAccessListReadRoleDescription.chevron().shouldBe(hidden);
    repoContainerAccessListReadRoleDescription.groupIcon().shouldBe(visible);
    repoContainerAccessListReadRoleDescription.members().shouldBe(visible).shouldHave(text("Group"));

    InheritedAccess repoContainerAccessListWriteRole = inheritedAccessList.element(1);
    repoContainerAccessListWriteRole.label().shouldBe(visible).shouldHave(text("RC Write Only"));

    AccessTileListElement repoContainerAccessListWriteRoleDescription = repoContainerAccessListWriteRole.description();
    repoContainerAccessListWriteRoleDescription.chevron().shouldBe(hidden);
    repoContainerAccessListWriteRoleDescription.userIcon().shouldBe(visible);
    repoContainerAccessListWriteRoleDescription.members()
        .shouldBe(visible)
        .shouldHave(text(testUser.calculateDisplayName()));

    InheritedAccessList rootOrgAccessList = accessTile.inheritedAccessList("ROOT_ORGANIZATION_ID");

    accessTile.accessListSubheader(1).shouldBe(visible).shouldHave(AccessTile.inheritedText("Root Organization"));
    rootOrgAccessList.elements().shouldHave(size(2));

    InheritedAccess rootOrgAccessListReadRole = rootOrgAccessList.element(0);
    rootOrgAccessListReadRole.label().shouldBe(visible).shouldHave(text("RO Read Only"));

    AccessTileListElement rootOrgAccessListReadRoleDescription = rootOrgAccessListReadRole.description();
    rootOrgAccessListReadRoleDescription.chevron().shouldBe(hidden);
    rootOrgAccessListReadRoleDescription.groupIcon().shouldBe(visible);
    rootOrgAccessListReadRoleDescription.members().shouldBe(visible).shouldHave(text("Group"));

    InheritedAccess rootOrgAccessListWriteRole = rootOrgAccessList.element(1);
    rootOrgAccessListWriteRole.label().shouldBe(visible).shouldHave(text("RO Write Only"));

    AccessTileListElement rootOrgAccessListWriteRoleDescription = rootOrgAccessListWriteRole.description();
    rootOrgAccessListWriteRoleDescription.chevron().shouldBe(hidden);
    rootOrgAccessListWriteRoleDescription.userIcon().shouldBe(visible);
    rootOrgAccessListWriteRoleDescription.members().shouldBe(visible).shouldHave(text(testUser.calculateDisplayName()));

    eyesWatcher.eyesCheck("repository manager access tile");
  }

  @Test
  public void testRepositoryManagerSummaryView_editRobotIcon() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager("repomanagerId", "repo manager instance");

    refreshOrOpen(RepositoriesSummaryPage.repositoryManagerUrl(repositoryManager.getId()));
    waitUntilUrl(RepositoriesSummaryPage.repositoryManagerUrl(repositoryManager.getId()));

    RepositoriesSummaryPage.summaryTile().name().should(appear).shouldHave(text("repomanagerId"));
    assertImage(RepositoriesSummaryPage.summaryTile().headerIcon());
    String summaryTileHeaderIconSrc = RepositoriesSummaryPage.summaryTile().headerIcon().attr("src");
    BufferedImage originalDefaultImage = fetchImage(summaryTileHeaderIconSrc);

    ActionDropDown.menu().shouldBe(hidden);
    ActionDropDown.actionButton().click();
    ActionDropDown.actions().shouldHave(size(3));
    ActionDropDown.deleteOwnerButton().shouldHave(text(repositoryManager.getInstanceId())).shouldBe(visible);
    ActionDropDown.copyOrgIdButton().shouldBe(visible);
    ActionDropDown.editOwner().shouldHave(text("Repository Manager name / icon")).shouldBe(visible);
    ActionDropDown.editOwner().click();

    // select a robot image
    OwnerEditorDialog.robotIcon().click();
    OwnerEditorDialog.RobotIconSelector.button().click();

    // validate image is displayed
    assertImage(OwnerEditorDialog.RobotIconSelector.icon());
    String userSelectedImageSrc = OwnerEditorDialog.RobotIconSelector.icon().attr("src");
    BufferedImage userSelectedImage = fetchImage(userSelectedImageSrc);

    OwnerEditorDialog.saveButton().click();
    OwnerEditorDialog.root().should(disappear);

    waitUntilUrl(RepositoriesSummaryPage.repositoryManagerUrl(repositoryManager.getId()));

    // validate the selected image is displayed
    RepositoriesSummaryPage.summaryTile().name().should(appear).shouldHave(text("repomanagerId"));
    assertImage(RepositoriesSummaryPage.summaryTile().headerIcon());
    summaryTileHeaderIconSrc = RepositoriesSummaryPage.summaryTile().headerIcon().attr("src");
    BufferedImage displayedImage = fetchImage(summaryTileHeaderIconSrc);

    // validate image saved is the same as image that was selected and displayed
    BufferedImage persistedImage = readImage(repositoryManager.getId());
    assertImageEquals(userSelectedImage, persistedImage);
    assertImageEquals(displayedImage, persistedImage);

    // resetting icon back to default
    ActionDropDown.actionButton().click();
    ActionDropDown.editOwner().shouldHave(text("Repository Manager name / icon")).click();
    OwnerEditorDialog.saveButton().click();
    OwnerEditorDialog.root().should(disappear);

    waitUntilUrl(RepositoriesSummaryPage.repositoryManagerUrl(repositoryManager.getId()));

    // validate the selected image is displayed
    RepositoriesSummaryPage.summaryTile().name().should(appear).shouldHave(text("repomanagerId"));
    assertImage(RepositoriesSummaryPage.summaryTile().headerIcon());
    summaryTileHeaderIconSrc = RepositoriesSummaryPage.summaryTile().headerIcon().attr("src");
    BufferedImage imageAfterResettingToDefault = fetchImage(summaryTileHeaderIconSrc);

    // validate image saved is the default image
    assertImageEquals(originalDefaultImage, imageAfterResettingToDefault);
  }

  @Test
  public void testRepositoryManagerSummaryView_editRobotIconWithCustomIcon() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager("repomanagerId", "repo manager instance");

    refreshOrOpen(RepositoriesSummaryPage.repositoryManagerUrl(repositoryManager.getId()));
    waitUntilUrl(RepositoriesSummaryPage.repositoryManagerUrl(repositoryManager.getId()));

    RepositoriesSummaryPage.summaryTile().name().should(appear).shouldHave(text("repomanagerId"));
    assertImage(RepositoriesSummaryPage.summaryTile().headerIcon());
    String summaryTileHeaderIconSrc = RepositoriesSummaryPage.summaryTile().headerIcon().attr("src");
    BufferedImage originalDefaultImage = fetchImage(summaryTileHeaderIconSrc);

    ActionDropDown.menu().shouldBe(hidden);
    ActionDropDown.actionButton().click();
    ActionDropDown.actions().shouldHave(size(3));
    ActionDropDown.deleteOwnerButton().shouldHave(text(repositoryManager.getInstanceId())).shouldBe(visible);
    ActionDropDown.copyOrgIdButton().shouldBe(visible);
    ActionDropDown.editOwner().shouldHave(text("Repository Manager name / icon")).shouldBe(visible);
    ActionDropDown.editOwner().click();

    // select a custom image
    File customIcon = new File(
        Objects.requireNonNull(getClass().getClassLoader().getResource("RepoManagerHeaderIcons/customIcon.png"))
            .getFile());

    OwnerEditorDialog.customIcon().click();
    OwnerEditorDialog.customIconInput().shouldBe(visible).sendKeys(customIcon.getAbsolutePath());
    OwnerEditorDialog.saveButton().click();
    OwnerEditorDialog.root().should(disappear);

    waitUntilUrl(RepositoriesSummaryPage.repositoryManagerUrl(repositoryManager.getId()));

    // validate the selected image is displayed
    RepositoriesSummaryPage.summaryTile().name().should(appear).shouldHave(text("repomanagerId"));
    assertImage(RepositoriesSummaryPage.summaryTile().headerIcon());
    summaryTileHeaderIconSrc = RepositoriesSummaryPage.summaryTile().headerIcon().attr("src");
    BufferedImage displayedImage = fetchImage(summaryTileHeaderIconSrc);

    // validate image saved is the same as image that was uploaded and displayed
    BufferedImage persistedImage = readImage(repositoryManager.getId());
    assertImageEquals(displayedImage, persistedImage);

    // validate uploaded image saved is different to the default image
    assertImageIsNotEquals(originalDefaultImage, displayedImage);
  }

  @Test
  public void testRepositoryManagersSummaryView_configurationTile_editRepositoryButton() {
    RepositoryManager repositoryManager =
        tempEntity.newRepositoryManager("5E7BCC8D-3FAB6390-83FF543B-EUD79639-D031F7AE");
    Repository repository = tempEntity.newProxyRepository(repositoryManager, "a1", "maven", true, true);

    refreshOrOpen(RepositoriesSummaryPage.url());

    RepositoriesSummaryTile summaryTile = RepositoriesSummaryPage.summaryTile();
    summaryTile.name().shouldBe(visible).shouldHave(text("Repository Managers"));

    NxBreadcrumb breadcrumb = new NxBreadcrumb();
    breadcrumb.listItems().shouldHave(size(2));
    breadcrumb.listItems().get(0).shouldHave(text("Root Organization"));
    breadcrumb.listItems().get(1).shouldHave(text("Repository Managers"));

    RepositoryConfigurationTile configurationTile = RepositoriesSummaryPage.configTile();
    configurationTile.configurationTable().row(1, 2).editRepositoryButton().shouldBe(visible, enabled).click();

    waitUntilUrl(RepositoriesSummaryPage.repositoryUrl(repository.getId()));

    summaryTile.name().shouldBe(visible).shouldHave(text(repository.getPublicId()));

    breadcrumb.listItems().shouldHave(size(4));
    breadcrumb.listItems().get(0).shouldHave(text("Root Organization"));
    breadcrumb.listItems().get(1).shouldHave(text("Repository Managers"));
    breadcrumb.listItems().get(2).shouldHave(text(repositoryManager.getName()));
    breadcrumb.listItems().get(3).shouldHave(text(repository.getPublicId()));
  }

  @Test
  public void testRepositoryManagerSummaryView_configurationTile_editRepositoryButton() {
    RepositoryManager repositoryManager =
        tempEntity.newRepositoryManager("5E7BCC8D-3FAB6390-83FF543B-ECD79639-D03UF7AE");
    Repository repository = tempEntity.newHostedRepository(repositoryManager, "a1", "maven", true);

    refreshOrOpen(RepositoriesSummaryPage.repositoryManagerUrl(repositoryManager.getId()));
    waitUntilUrl(RepositoriesSummaryPage.repositoryManagerUrl(repositoryManager.getId()));

    RepositoriesSummaryTile summaryTile = RepositoriesSummaryPage.summaryTile();
    summaryTile.name().shouldBe(visible).shouldHave(text(repositoryManager.getName()));

    NxBreadcrumb breadcrumb = new NxBreadcrumb();
    breadcrumb.listItems().shouldHave(size(3));
    breadcrumb.listItems().get(0).shouldHave(text("Root Organization"));
    breadcrumb.listItems().get(1).shouldHave(text("Repository Managers"));
    breadcrumb.listItems().get(2).shouldHave(text(repositoryManager.getName()));

    RepositoryConfigurationTile configurationTile = RepositoriesSummaryPage.configTile();
    configurationTile.configurationTable()
        .repoManagerConfigTableRow(1)
        .editRepositoryButton()
        .shouldBe(visible, enabled)
        .click();

    waitUntilUrl(RepositoriesSummaryPage.repositoryUrl(repository.getId()));

    summaryTile.name().shouldBe(visible).shouldHave(text(repository.getPublicId()));

    breadcrumb.listItems().shouldHave(size(4));
    breadcrumb.listItems().get(0).shouldHave(text("Root Organization"));
    breadcrumb.listItems().get(1).shouldHave(text("Repository Managers"));
    breadcrumb.listItems().get(2).shouldHave(text(repositoryManager.getName()));
    breadcrumb.listItems().get(3).shouldHave(text(repository.getPublicId()));
  }

  private void assertImageEquals(BufferedImage image1, BufferedImage image2) throws IOException {
    BufferedImage resizedImage1 = resizeImage(image1, image1.getType());
    byte[] resizedImage1Bytes = bufferedImageToBytesArray(resizedImage1);
    BufferedImage resizedImage2 = resizeImage(image2, image2.getType());
    byte[] resizedImage2Bytes = bufferedImageToBytesArray(resizedImage2);
    assertThat(resizedImage1Bytes).isEqualTo(resizedImage2Bytes);
  }

  private void assertImageIsNotEquals(BufferedImage image1, BufferedImage image2) throws IOException {
    BufferedImage resizedImage1 = resizeImage(image1, image1.getType());
    byte[] resizedImage1Bytes = bufferedImageToBytesArray(resizedImage1);
    BufferedImage resizedImage2 = resizeImage(image2, image2.getType());
    byte[] resizedImage2Bytes = bufferedImageToBytesArray(resizedImage2);
    assertThat(resizedImage1Bytes).isNotEqualTo(resizedImage2Bytes);
  }

  private BufferedImage resizeImage(BufferedImage originalImage, int type) {
    BufferedImage resizedImage = new BufferedImage(IMAGE_RESIZE_WIDTH, IMAGE_RESIZE_HEIGHT, type);
    Graphics2D g = resizedImage.createGraphics();
    g.drawImage(originalImage, 0, 0, IMAGE_RESIZE_WIDTH, IMAGE_RESIZE_HEIGHT, null);
    g.dispose();
    return resizedImage;
  }

  private byte[] bufferedImageToBytesArray(BufferedImage image) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write(image, "png", baos);
    return baos.toByteArray();
  }

  private BufferedImage fetchImage(String urlString) throws IOException {
    HttpClient client = HttpClientBuilder.create().build();
    HttpGet get = new HttpGet(BaseUrl.convertContainerUrlToHostUrl(urlString));
    get.setHeader("Authorization",
        "Basic " + Base64.getEncoder().encodeToString("admin:admin123".getBytes(StandardCharsets.UTF_8)));
    HttpResponse response = client.execute(get);
    return ImageIO.read(response.getEntity().getContent());
  }

  private BufferedImage readImage(String ownerId) throws Exception {
    InsightWork insightWork = testCLMServer.getCLMServer().getInstance(InsightWork.class);
    return ImageIO.read(new ByteArrayInputStream(iconDAO.getIcon(ownerId, insightWork.getRepositoryManagerIconDir())));
  }

  private void assertImage(SelenideElement element) {
    element.shouldBe(new WebElementCondition("image")
    {
      @Override
      public CheckResult check(Driver driver, WebElement ignored) {
        boolean isImage = element.isImage();
        return new CheckResult(isImage, element);
      }
    });
  }
}
