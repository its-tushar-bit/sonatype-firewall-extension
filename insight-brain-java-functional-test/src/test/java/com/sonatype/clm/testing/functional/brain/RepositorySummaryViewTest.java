/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.ArrayList;
import java.util.List;

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
import com.sonatype.clm.testing.functional.elements.NxDeleteModal;
import com.sonatype.clm.testing.functional.elements.PolicyTile;
import com.sonatype.clm.testing.functional.elements.PolicyTileList;
import com.sonatype.clm.testing.functional.elements.PolicyTileList.PolicyTileListElement;
import com.sonatype.clm.testing.functional.elements.RepositoriesSummaryTile;
import com.sonatype.clm.testing.functional.elements.SummarySection;
import com.sonatype.clm.testing.functional.elements.Tooltip;
import com.sonatype.clm.testing.functional.pages.PolicyEditorPage;
import com.sonatype.clm.testing.functional.pages.RepositoriesSummaryPage;
import com.sonatype.clm.testing.functional.pages.RepositoryReportContainerPage;
import com.sonatype.clm.testing.functional.utils.ScrollUtil;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.repository.ProprietaryComponentNamePattern;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;

public class RepositorySummaryViewTest
    extends AbstractFunctionalTest
{
  private OrganizationDAO organizationDAO;

  @BeforeClass
  public static void startup() {
    refreshOrOpen(RepositoriesSummaryPage.url());
    loginAsAdmin();
  }

  @Before
  public void init() {
    organizationDAO = lookup(OrganizationDAO.class);

    refreshOrOpen(RepositoriesSummaryPage.url());
  }

  @Test
  public void testNavigationPills() {
    RepositoriesSummaryPage.policyPillButton().shouldBe(visible).click();
    PolicyTile policyTile = RepositoriesSummaryPage.policyTile();
    policyTile.shouldBe(visible);
    policyTile.newButton().shouldBe(visible);
    policyTile.policyLists().shouldHave(size(0));
    policyTile.localEmptyDescriptor().shouldBe(visible).shouldHave(text("No local"));
  }

  @Test
  public void testBreadcrumbs() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository proxyRepository = tempEntity.newProxyRepository(repositoryManager, "npm-proxy",
        "npm", true, true);
    Repository hostedRepository = tempEntity.newHostedRepository(repositoryManager, "npm-hosted",
        "npm", true);

    refreshOrOpen(RepositoriesSummaryPage.repositoryUrl(proxyRepository.getId()));
    waitUntilUrl(RepositoriesSummaryPage.repositoryUrl(proxyRepository.getId()));

    NxBreadcrumb breadcrumb = new NxBreadcrumb();
    breadcrumb.listItems().shouldHave(size(4));
    breadcrumb.listItems().get(0).shouldHave(text("Root Organization"));
    breadcrumb.listItems().get(1).shouldHave(text("Repository Managers"));
    breadcrumb.listItems().get(2).shouldHave(text(repositoryManager.getName()));
    breadcrumb.listItems().get(3).shouldHave(text("npm-proxy (npm : proxy)"));

    refreshOrOpen(RepositoriesSummaryPage.repositoryUrl(hostedRepository.getId()));
    waitUntilUrl(RepositoriesSummaryPage.repositoryUrl(hostedRepository.getId()));

    breadcrumb.listItems().shouldHave(size(4));
    breadcrumb.listItems().get(0).shouldHave(text("Root Organization"));
    breadcrumb.listItems().get(1).shouldHave(text("Repository Managers"));
    breadcrumb.listItems().get(2).shouldHave(text(repositoryManager.getName()));
    breadcrumb.listItems().get(3).shouldHave(text("npm-hosted (npm : hosted)"));
  }

  @Test
  public void testRepositorySummaryView_policyTile() {
    RepositoryManager repositoryManager = tempEntity
        .newRepositoryManager("5E7BCC8D-3FAB6390-83FF543B-ECD79639-D031F7AE");
    Repository repository = tempEntity.newProxyRepository(repositoryManager, "npm-proxy",
        "npm", true, true);

    Owner rootOrgOwner = organizationDAO.getByIdNotNull(ROOT_ORGANIZATION_ID);

    List<Policy> localToRepositoryPolicies = new ArrayList<>();
    localToRepositoryPolicies.add(tempEntity.newPolicy(repository.getId(),
        "Repository Local Policy 1", 10, Action.ID_FAIL, Stage.ID_BUILD, null));

    List<Policy> inheritedFromRepositoryManagerPolicies = new ArrayList<>();
    inheritedFromRepositoryManagerPolicies.add(tempEntity.newPolicy(repositoryManager.getId(),
        "Repository Manager Policy 1", 10, Action.ID_FAIL, Stage.ID_BUILD, null));

    List<Policy> inheritedFromRootOrgPolicies = new ArrayList<>();
    inheritedFromRootOrgPolicies.add(tempEntity.newPolicy(rootOrgOwner.getId(), rootOrgOwner.getName() + " Policy 1",
        10, Action.ID_FAIL, Stage.ID_BUILD, null));
    inheritedFromRootOrgPolicies.add(tempEntity.newPolicy(rootOrgOwner.getId(), rootOrgOwner.getName() + " Policy 2",
        5, Action.ID_WARN, Stage.ID_BUILD, null));

    refreshOrOpen(RepositoriesSummaryPage.repositoryUrl(repository.getId()));
    waitUntilUrl(RepositoriesSummaryPage.repositoryUrl(repository.getId()));

    RepositoriesSummaryTile summaryTile = RepositoriesSummaryPage.summaryTile();
    
    String repostioryFormatTypeString = " (" + repository.getFormat() + " : " + repository.getRepositoryType() + ")";
    
    summaryTile.name().shouldBe(visible).shouldHave(text(repository.getName()));
    summaryTile.repositoryFormatType().shouldBe(visible).shouldHave(text(repostioryFormatTypeString));

    PolicyTile policyTile = RepositoriesSummaryPage.policyTile();
    policyTile.shouldBe(visible);
    policyTile.policyLists().shouldHave(size(4));

    policyTile.addPolicyButton().shouldBe(visible);
    policyTile.addPolicyButton().shouldHave(text("Add a Policy"));

    PolicyTileList policyTileLocalList = policyTile.policyList(0);
    policyTileLocalList.ownerName().shouldHave(text("Local to " + repository.getName()));
    policyTileLocalList.rows().shouldHave(size(localToRepositoryPolicies.size() + 1));

    PolicyTileList policyTileRepoManagerList = policyTile.policyList(1);
    policyTileRepoManagerList.ownerName().shouldHave(text("Inherited from " + repositoryManager.getName()));
    policyTileRepoManagerList.rows()
        .shouldHave(size(inheritedFromRepositoryManagerPolicies.size() + 1));

    String repositoryContainerName = RepositoryContainer.SINGLETON.getName();
    PolicyTileList policyTileRepositoryContainerList = policyTile.policyList(2);
    policyTileRepositoryContainerList.ownerName().shouldHave(text("Inherited from " + repositoryContainerName));
    policyTileRepositoryContainerList.rows().shouldHave(size(2));
    policyTileRepositoryContainerList.emptyDescriptor()
        .shouldHave(text("No " + repositoryContainerName + " policies defined"));

    PolicyTileList policyTileInheritedList = policyTile.policyList(3);
    policyTileInheritedList.ownerName().shouldHave(text("Inherited from " + rootOrgOwner.getName()));
    policyTileInheritedList.rows().shouldHave(size(inheritedFromRootOrgPolicies.size() + 1));

    eyesWatcher.eyesCheck("Repository policies tile");

    policyTileInheritedList.row(1).click();
    waitUntilUrl(PolicyEditorPage.firewallUrlToEdit(repository.getType(), repository.getId(),
        inheritedFromRootOrgPolicies.get(0).getId()));

    SummarySection summarySection = PolicyEditorPage.summarySection();
    assertThat(summarySection.policyName().input().getValue()).isEqualTo(inheritedFromRootOrgPolicies.get(0).getName());

    eyesWatcher.eyesCheck("repository policy view page");
  }
  
  @Test
  public void testRepositorySummaryView_veryLongRepositoryPublicIdTooltip() {
    RepositoryManager repositoryManager = tempEntity
        .newRepositoryManager("5E7BCC8D-3FAB6390-83FF543B-ECD79639-D031F7AE");
    String veryLongRepositoryPublicId =
        "veryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryLongPublicId";
    Repository repository = tempEntity.newProxyRepository(repositoryManager, veryLongRepositoryPublicId,
        "npm", true, true);
    refreshOrOpen(RepositoriesSummaryPage.repositoryUrl(repository.getId()));
    waitUntilUrl(RepositoriesSummaryPage.repositoryUrl(repository.getId()));
    RepositoriesSummaryTile summaryTile = RepositoriesSummaryPage.summaryTile();
    summaryTile.name().hover();
    Tooltip.get().shouldBe(visible).shouldHave(text(veryLongRepositoryPublicId));
  }

  @Test
  public void testReturnsToCreatePolicyAfterRemovePolicy() {
    PolicyDAO policyDAO = lookup(PolicyDAO.class);
    RepositoryManager repositoryManager = tempEntity
        .newRepositoryManager("5E7BCC8D-3FAB6390-83FF543B-ECD79639-D031F7AE");
    Repository repository = tempEntity.newProxyRepository(repositoryManager, "npm-proxy",
        "npm", true, true);
    Policy policy = tempEntity.newPolicy(repository.getId(),
        "Repository Local Policy 1", 10, Action.ID_FAIL, Stage.ID_PROXY, null);
    policyDAO.update(policy);

    refreshOrOpen(RepositoriesSummaryPage.repositoryUrl(repository.getId()));
    waitUntilUrl(RepositoriesSummaryPage.repositoryUrl(repository.getId()));

    // Check that the breadcrumb has the right path
    NxBreadcrumb breadcrumb = new NxBreadcrumb();
    breadcrumb.listItems().shouldHave(size(4));
    breadcrumb.listItems().get(0).shouldHave(text("Root Organization"));
    breadcrumb.listItems().get(1).shouldHave(text("Repository Managers"));
    breadcrumb.listItems().get(2).shouldHave(text("5E7BCC8D-3FAB6390-83FF543B-ECD79639-D031F7AE"));
    breadcrumb.listItems().get(3).shouldHave(text("npm-proxy"));

    RepositoriesSummaryPage.policyTile().policyLists().shouldHave(size(4));
    PolicyTileList policyList = RepositoriesSummaryPage.policyTile().policyList(0);
    policyList.ownerName().shouldBe(visible).shouldHave(text("Local"));
    PolicyTileListElement policyElement = policyList.row(1);
    policyElement.name().shouldBe(visible).shouldHave(text("Repository Local Policy 1"));
    policyElement.proxy().shouldBe(visible).shouldHave(text("fail"));
    policyElement.click();

    waitUntilUrl(PolicyEditorPage.firewallUrlToEdit(repository.getType(), repository.getId(), policy.getId()));
    breadcrumb.listItems().shouldHave(size(4));
    breadcrumb.listItems().get(0).shouldHave(text("Root Organization"));
    assertThat(breadcrumb.listItems().get(1).lastChild().attr("class"))
        .isEqualTo("nx-dropdown nx-icon-dropdown");
    breadcrumb.listItems().get(2).shouldHave(text("npm-proxy"));
    breadcrumb.listItems().get(3).shouldHave(text("Repository Policy"));
    PolicyEditorPage.title().shouldHave(text("Edit Policy"));

    SummarySection summary = PolicyEditorPage.summarySection();
    ScrollUtil.awaitEndOfScrolling(PolicyEditorPage.deleteButton());
    PolicyEditorPage.deleteButton().shouldBe(visible, enabled).click();

    NxDeleteModal deleteModal = new NxDeleteModal("#policy-delete-modal");
    deleteModal.shouldBe(visible);
    deleteModal.header().shouldHave(text("Policy"));
    deleteModal.alertContent().shouldHave(text(policy.getName()));
    PolicyEditorPage.deleteConfirmationInput().shouldBe(visible).val("WRONG");
    PolicyEditorPage.deleteConfirmationError().shouldHave(text("Must type DELETE to confirm"));

    deleteModal.submitButton().click();
    PolicyEditorPage.deleteConfirmationFormError().shouldBe(visible).shouldHave(text("Required fields are missing"));

    PolicyEditorPage.deleteConfirmationInput().clear();
    PolicyEditorPage.deleteConfirmationInput().val("DELETE");
    PolicyEditorPage.deleteConfirmationError().shouldBe(hidden);

    deleteModal.submitButton().click();
    FormMask.seeAndWaitForDismissal();
    deleteModal.shouldBe(hidden);

    // Validates return to create policy page
    waitUntilUrl(PolicyEditorPage.firewallUrlToCreate(repository.getType(), repository.getId()));

    breadcrumb.listItems().shouldHave(size(4));
    breadcrumb.listItems().get(0).shouldHave(text("Root Organization"));
    assertThat(breadcrumb.listItems().get(1).lastChild().attr("class"))
            .isEqualTo("nx-dropdown nx-icon-dropdown");
    breadcrumb.listItems().get(2).shouldHave(text("npm-proxy"));
    breadcrumb.listItems().get(3).shouldHave(text("Repository Policy"));
    PolicyEditorPage.title().shouldHave(text("New Policy"));
    summary.policyName().input().shouldBe(empty);
    assertThat(policyDAO.getById(policy.getId())).isNull();
  }

  @Test
  public void testRepositorySummaryView_accessTile() {
    RepositoryManager repositoryManager = tempEntity
            .newRepositoryManager("5E7BCC8D-3FAB6390-83FF543B-ECD79639-D031F7AE");
    Repository repository = tempEntity.newProxyRepository(repositoryManager, "npm-proxy", "npm", true, true);

    testRepositorySummaryViewAccessTile(repositoryManager, repository);
  }

  @Test
  public void testRepositorySummaryView_accessTileForHostedRepositories() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(
        "5E7BCC8D-3FAB6390-83FF543B-ECD79639-D031F7AE"
    );
    Repository repository = tempEntity.newHostedRepository(repositoryManager, "npm-proxy", "npm", true);

    testRepositorySummaryViewAccessTile(repositoryManager, repository);
  }

  public void testRepositorySummaryViewAccessTile(RepositoryManager repositoryManager, Repository repository) {
    User testUser = tempEntity.newUser("testUser", "Test", "User", "testuser@sonatype.com");

    Role readRole = tempEntity.newRole("Local Read Only", false, Permission.READ);
    Role writeRole = tempEntity.newRole("Local Write Only", false, Permission.WRITE);
    Role repoManagerReadRole = tempEntity.newRole("RM Read Only", false, Permission.READ);
    Role repoManagerWriteRole = tempEntity.newRole("RM Write Only", false, Permission.WRITE);
    Role repoContainerReadRole = tempEntity.newRole("RC Read Only", false, Permission.READ);
    Role repoContainerWriteRole = tempEntity.newRole("RC Write Only", false, Permission.WRITE);
    Role rootOrgWriteRole = tempEntity.newRole("RO Write Only", false, Permission.WRITE);
    Role rootOrgReadRole = tempEntity.newRole("RO Read Only", false, Permission.READ);

    tempEntity.newMembershipMapping(repository.getId(), writeRole.getId(), testUser.getUsername());
    tempEntity.newMembershipMapping(repository.getId(), readRole.getId(), "Group", MemberType.GROUP);
    tempEntity.newMembershipMapping(repositoryManager.getId(), repoManagerWriteRole.getId(), testUser.getUsername());
    tempEntity.newMembershipMapping(repositoryManager.getId(), repoManagerReadRole.getId(), "Group", MemberType.GROUP);
    tempEntity
        .newMembershipMapping(Organization.ROOT_ORGANIZATION_ID, rootOrgWriteRole.getId(), testUser.getUsername());
    tempEntity
        .newMembershipMapping(Organization.ROOT_ORGANIZATION_ID, rootOrgReadRole.getId(), "Group", MemberType.GROUP);
    tempEntity.newMembershipMapping(
        RepositoryContainer.REPOSITORY_CONTAINER_ID, repoContainerWriteRole.getId(), testUser.getUsername()
    );
    tempEntity.newMembershipMapping(
        RepositoryContainer.REPOSITORY_CONTAINER_ID, repoContainerReadRole.getId(), "Group", MemberType.GROUP
    );

    refreshOrOpen(RepositoriesSummaryPage.repositoryUrl(repository.getId()));
    waitUntilUrl(RepositoriesSummaryPage.repositoryUrl(repository.getId()));
    RepositoriesSummaryPage.accessPillButton().shouldBe(visible).click();

    AccessTile accessTile = RepositoriesSummaryPage.accessTile();
    accessTile.accessLists().shouldHave(size(4));

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

    InheritedAccessList repoManagerAccessList = accessTile.inheritedAccessList(repositoryManager.getId());

    accessTile.accessListSubheader(0).shouldBe(visible)
        .shouldHave(AccessTile.inheritedText(repositoryManager.getName()));
    repoManagerAccessList.elements().shouldHave(size(2));

    InheritedAccess repoManagerAccessListReadRole = repoManagerAccessList.element(0);
    repoManagerAccessListReadRole.label().shouldBe(visible).shouldHave(text("RM Read Only"));

    AccessTileListElement repoManagerAccessListReadRoleDescription = repoManagerAccessListReadRole.description();
    repoManagerAccessListReadRoleDescription.chevron().shouldBe(hidden);
    repoManagerAccessListReadRoleDescription.groupIcon().shouldBe(visible);
    repoManagerAccessListReadRoleDescription.members().shouldBe(visible).shouldHave(text("Group"));

    InheritedAccess repoManagerAccessListWriteRole = repoManagerAccessList.element(1);
    repoManagerAccessListWriteRole.label().shouldBe(visible).shouldHave(text("RM Write Only"));

    AccessTileListElement repoManagerAccessListWriteRoleDescription = repoManagerAccessListWriteRole.description();
    repoManagerAccessListWriteRoleDescription.chevron().shouldBe(hidden);
    repoManagerAccessListWriteRoleDescription.userIcon().shouldBe(visible);
    repoManagerAccessListWriteRoleDescription.members()
        .shouldBe(visible)
        .shouldHave(text(testUser.calculateDisplayName()));

    InheritedAccessList repoContainerAccessList =
        accessTile.inheritedAccessList(RepositoryContainer.REPOSITORY_CONTAINER_ID);

    accessTile.accessListSubheader(1).shouldBe(visible).shouldHave(AccessTile.inheritedText("Repository Managers"));
    repoContainerAccessList.elements().shouldHave(size(2));

    InheritedAccess repoContainerAccessListReadRole = repoContainerAccessList.element(0);
    repoContainerAccessListReadRole.label().shouldBe(visible).shouldHave(text("RC Read Only"));

    AccessTileListElement repoContainerAccessListReadRoleDescription = repoContainerAccessListReadRole.description();
    repoContainerAccessListReadRoleDescription.chevron().shouldBe(hidden);
    repoContainerAccessListReadRoleDescription.groupIcon().shouldBe(visible);
    repoContainerAccessListReadRoleDescription.members().shouldBe(visible).shouldHave(text("Group"));

    InheritedAccess repoContainerAccessListWriteRole = repoContainerAccessList.element(1);
    repoContainerAccessListWriteRole.label().shouldBe(visible).shouldHave(text("RC Write Only"));

    AccessTileListElement repoContainerAccessListWriteRoleDescription = repoContainerAccessListWriteRole.description();
    repoContainerAccessListWriteRoleDescription.chevron().shouldBe(hidden);
    repoContainerAccessListWriteRoleDescription.userIcon().shouldBe(visible);
    repoContainerAccessListWriteRoleDescription.members()
        .shouldBe(visible)
        .shouldHave(text(testUser.calculateDisplayName()));

    InheritedAccessList rootOrgAccessList = accessTile.inheritedAccessList("ROOT_ORGANIZATION_ID");

    accessTile.accessListSubheader(2).shouldBe(visible).shouldHave(AccessTile.inheritedText("Root Organization"));
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
  public void testRepositorySummaryView_namespaceConfusionProtectionTileForHostedRepository() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager("instanceId");
    Repository repo = tempEntity.newRepository(repoManager, "maven-hosted", RepositoryType.hosted,
        ComponentIdentifier.FORMAT_MAVEN);
    ProprietaryComponentNamePattern namePattern =
        tempEntity.newProprietaryComponentNamePattern(repo, "test", null, true);

    refreshOrOpen(RepositoriesSummaryPage.repositoryUrl(repo.getId()));
    waitUntilUrl(RepositoriesSummaryPage.repositoryUrl(repo.getId()));

    NamespaceConfusionProtectionTile namespaceConfusionProtectionTile =
        RepositoriesSummaryPage.namespaceConfusionProtectionTile();
    ScrollUtil.scrollIntoView(namespaceConfusionProtectionTile.getElement(), false);

    namespaceConfusionProtectionTile.tableBodyRows().shouldHave(size(1));

    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(0)
        .shouldHave(text(namePattern.getNamespacePattern()));
    namespaceConfusionProtectionTile.repositoryManagerIdColumnCells().isEmpty();
    namespaceConfusionProtectionTile.hostedRepositoryNameColumnCells().isEmpty();
    namespaceConfusionProtectionTile.enabledToggleIndicators().get(0).shouldBe(enabled, selected);

    eyesWatcher.eyesCheck("hosted repository manager namespace confusion protection tile");
  }

  @Test
  public void testRepositorySummaryView_namespaceConfusionProtectionTile_independentSortAndFilter() {
    NxBreadcrumb breadcrumb = new NxBreadcrumb();
    RepositoryManager repoManager = tempEntity.newRepositoryManager("instanceId");

    Repository repo1 = tempEntity.newRepository(repoManager, "maven-hosted", RepositoryType.hosted,
        ComponentIdentifier.FORMAT_MAVEN);

    List<ProprietaryComponentNamePattern> namePatterns = new ArrayList<>();

    ProprietaryComponentNamePattern namePattern1 =
        tempEntity.newProprietaryComponentNamePattern(repo1, "test-a", null, true);
    ProprietaryComponentNamePattern namePattern2 =
        tempEntity.newProprietaryComponentNamePattern(repo1, "test-ab", null, false);
    ProprietaryComponentNamePattern namePattern3 =
        tempEntity.newProprietaryComponentNamePattern(repo1, "test-b", null, true);
    ProprietaryComponentNamePattern namePattern4 =
        tempEntity.newProprietaryComponentNamePattern(repo1, "test-bc", null, false);

    namePatterns.add(namePattern1);
    namePatterns.add(namePattern2);
    namePatterns.add(namePattern3);
    namePatterns.add(namePattern4);

    refreshOrOpen(RepositoriesSummaryPage.repositoryUrl(repo1.getId()));
    waitUntilUrl(RepositoriesSummaryPage.repositoryUrl(repo1.getId()));

    NamespaceConfusionProtectionTile namespaceConfusionProtectionTile =
        RepositoriesSummaryPage.namespaceConfusionProtectionTile();
    ScrollUtil.scrollIntoView(namespaceConfusionProtectionTile.getElement(), false);

    checkDefaultSortForNamespaceConfusionTile(namespaceConfusionProtectionTile, namePatterns);

    namespaceConfusionProtectionTile.enabledHeaderSortBtn().click();

    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(0)
        .shouldHave(text(namePattern2.getNamespacePattern()));
    namespaceConfusionProtectionTile.enabledToggleIndicators().get(0).shouldBe(enabled).shouldNotBe(selected);
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(1)
        .shouldHave(text(namePattern4.getNamespacePattern()));
    namespaceConfusionProtectionTile.enabledToggleIndicators().get(1).shouldBe(enabled).shouldNotBe(selected);
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(2)
        .shouldHave(text(namePattern1.getNamespacePattern()));
    namespaceConfusionProtectionTile.enabledToggleIndicators().get(2).shouldBe(enabled, selected);
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(3)
        .shouldHave(text(namePattern3.getNamespacePattern()));
    namespaceConfusionProtectionTile.enabledToggleIndicators().get(3).shouldBe(enabled, selected);

    eyesWatcher.eyesCheck("hosted repository namespace confusion protection tile sorted by enable column");

    ScrollUtil.scrollIntoView(breadcrumb.getElement(), false);
    breadcrumb.listItems().get(2).click();

    waitUntilUrl(RepositoriesSummaryPage.repositoryManagerUrl(repoManager.getId()));
    ScrollUtil.scrollIntoView(namespaceConfusionProtectionTile.getElement(), false);

    checkDefaultSortForNamespaceConfusionTile(namespaceConfusionProtectionTile, namePatterns);

    eyesWatcher.eyesCheck("hosted repository manager namespace confusion protection tile");

    namespaceConfusionProtectionTile.namespaceFilterInput().setValue("a");
    namespaceConfusionProtectionTile.enabledHeaderSortBtn().click();

    namespaceConfusionProtectionTile.tableBodyRows().shouldHave(size(2));

    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(0)
            .shouldHave(text(namePattern2.getNamespacePattern()));
    namespaceConfusionProtectionTile.enabledToggleIndicators().get(0).shouldBe(enabled).shouldNotBe(selected);
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(1)
            .shouldHave(text(namePattern1.getNamespacePattern()));
    namespaceConfusionProtectionTile.enabledToggleIndicators().get(1).shouldBe(enabled, selected);

    eyesWatcher.eyesCheck("hosted repository manager namespace confusion protection tile sorted by enable column");

    ScrollUtil.scrollIntoView(breadcrumb.getElement(), false);
    breadcrumb.listItems().get(1).click();
    waitUntilUrl(RepositoriesSummaryPage.url());
    ScrollUtil.scrollIntoView(namespaceConfusionProtectionTile.getElement(), false);

    checkDefaultSortForNamespaceConfusionTile(namespaceConfusionProtectionTile, namePatterns);
  }

  @Test
  public void testRepositorySummaryView_namespaceConfusionProtectionTileForProxyRepository() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager("instanceId");
    Repository repo = tempEntity.newRepository(repoManager, "maven-central", RepositoryType.proxy,
        ComponentIdentifier.FORMAT_MAVEN);

    refreshOrOpen(RepositoriesSummaryPage.repositoryUrl(repo.getId()));
    waitUntilUrl(RepositoriesSummaryPage.repositoryUrl(repo.getId()));

    RepositoriesSummaryPage.namespaceConfusionProtectionPillButton().shouldNotBe(visible);
    RepositoriesSummaryPage.namespaceConfusionProtectionTile().shouldNotBe(visible);

    eyesWatcher.eyesCheck("proxy repository namespace confusion protection tile");
  }

  @Test
  public void testRepositorySummaryView_checkTilesForProxyAndHostedRepositories() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager("instanceId");
    Repository proxyRepo = tempEntity.newRepository(repoManager, "maven-central", RepositoryType.proxy,
            ComponentIdentifier.FORMAT_MAVEN);
    Repository hostedRepo = tempEntity.newRepository(repoManager, "maven-hosted", RepositoryType.hosted,
            ComponentIdentifier.FORMAT_MAVEN);

    refreshOrOpen(RepositoriesSummaryPage.repositoryUrl(proxyRepo.getId()));
    waitUntilUrl(RepositoriesSummaryPage.repositoryUrl(proxyRepo.getId()));

    RepositoriesSummaryPage.namespaceConfusionProtectionTile().shouldNotBe(visible);
    RepositoriesSummaryPage.policyTile().shouldBe(visible);
    RepositoriesSummaryPage.accessTile().shouldBe(visible);

    eyesWatcher.eyesCheck("check tiles for proxy repository");

    refreshOrOpen(RepositoriesSummaryPage.repositoryUrl(hostedRepo.getId()));
    waitUntilUrl(RepositoriesSummaryPage.repositoryUrl(hostedRepo.getId()));

    RepositoriesSummaryPage.namespaceConfusionProtectionTile().shouldBe(visible);
    RepositoriesSummaryPage.policyTile().shouldNotBe(visible);
    RepositoriesSummaryPage.accessTile().shouldBe(visible);

    eyesWatcher.eyesCheck("check tiles for hosted repository");
  }

  @Test
  public void testRepositorySummaryView_actionMenuForProxyRepository() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager("instanceId");
    Repository repo = tempEntity.newRepository(repoManager, "maven-central", RepositoryType.proxy,
        ComponentIdentifier.FORMAT_MAVEN);

    refreshOrOpen(RepositoriesSummaryPage.repositoryUrl(repo.getId()));
    waitUntilUrl(RepositoriesSummaryPage.repositoryUrl(repo.getId()));

    ActionDropDown.menu().shouldBe(hidden);
    ActionDropDown.actionButton().click();

    // eyescheck with the action menu opened
    eyesWatcher.eyesCheck("action menu for proxy repository");

    ActionDropDown.viewRepositoryResults().shouldHave(text("View repository Results")).shouldBe(visible);
    String repositoryResultsButtonUrl = ActionDropDown.viewRepositoryResults().attr("href");
    String repositoryReportContainerPageUrl = RepositoryReportContainerPage.url(repo.getId()) + "?hideBackButton=true";
    assertThat(repositoryResultsButtonUrl).isEqualTo(repositoryReportContainerPageUrl);
  }

  @Test
  public void testRepositorySummaryView_actionMenuForHostedRepository() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager("instanceId");
    Repository repo = tempEntity.newRepository(repoManager, "npm-hosted", RepositoryType.hosted,
            "npm");

    refreshOrOpen(RepositoriesSummaryPage.repositoryUrl(repo.getId()));
    waitUntilUrl(RepositoriesSummaryPage.repositoryUrl(repo.getId()));

    ActionDropDown.actionButton().shouldNotBe(visible);

    // eyescheck with the action menu opened
    eyesWatcher.eyesCheck("action menu for hosted repository");
  }

  public void checkDefaultSortForNamespaceConfusionTile(
      NamespaceConfusionProtectionTile namespaceConfusionProtectionTile,
      List<ProprietaryComponentNamePattern> namePatterns
  )
  {
    namespaceConfusionProtectionTile.tableBodyRows().shouldHave(size(4));

    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(0)
        .shouldHave(text(namePatterns.get(0).getNamespacePattern()));
    namespaceConfusionProtectionTile.enabledToggleIndicators().get(0).shouldBe(enabled, selected);
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(1)
        .shouldHave(text(namePatterns.get(1).getNamespacePattern()));
    namespaceConfusionProtectionTile.enabledToggleIndicators().get(1).shouldBe(enabled).shouldNotBe(selected);
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(2)
        .shouldHave(text(namePatterns.get(2).getNamespacePattern()));
    namespaceConfusionProtectionTile.enabledToggleIndicators().get(2).shouldBe(enabled, selected);
    namespaceConfusionProtectionTile.componentNamespaceColumnCells().get(3)
        .shouldHave(text(namePatterns.get(3).getNamespacePattern()));
    namespaceConfusionProtectionTile.enabledToggleIndicators().get(3).shouldBe(enabled).shouldNotBe(selected);
  }
}
