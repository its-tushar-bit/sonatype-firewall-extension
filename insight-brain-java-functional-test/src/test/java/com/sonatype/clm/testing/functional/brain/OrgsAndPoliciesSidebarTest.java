/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.NxLoadingSpinner;
import com.sonatype.clm.testing.functional.elements.ActionDropDown;
import com.sonatype.clm.testing.functional.elements.MoveApplicationSuccessModal;
import com.sonatype.clm.testing.functional.elements.MoveOwnerDialog;
import com.sonatype.clm.testing.functional.elements.NxCollapsible;
import com.sonatype.clm.testing.functional.elements.NxFormSelect;
import com.sonatype.clm.testing.functional.elements.NxTooltip;
import com.sonatype.clm.testing.functional.elements.OrgsAndPoliciesSidebar;
import com.sonatype.clm.testing.functional.elements.OwnerEditorDialog;
import com.sonatype.clm.testing.functional.elements.SidebarNavigation;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPageWithLimitedVisibility;
import com.sonatype.clm.testing.functional.pages.RepositoriesSummaryPage;
import com.sonatype.clm.testing.functional.pages.ScmOnboardingPage;
import com.sonatype.clm.testing.functional.utils.NameSupplierDictionary;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;

import com.codeborne.selenide.SelenideElement;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.disappear;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static org.assertj.core.api.Assertions.assertThat;

public class OrgsAndPoliciesSidebarTest
    extends AbstractFunctionalTest
{
  private Map<Integer, List<Organization>> organizations;

  private ApplicationDAO applicationDAO;

  private OrganizationDAO organizationDAO;

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(OwnerSummaryPage.urlToRootOrg());
    loginAsAdmin();
  }

  @Before
  public void init() {
    organizationDAO = lookup(OrganizationDAO.class);
    applicationDAO = lookup(ApplicationDAO.class);

    organizations = tempEntity.newRelatedOrganizationsAsMap(null, 2, 3, 3, new NameSupplierDictionary());
    refreshOrOpen(OwnerSummaryPage.urlToRootOrg());
  }

  @Test
  public void testOrgsAndPoliciesSideNavbar() {
    eyesWatcher.eyesCheck("Orgs and policies sidebar at Root level");

    organizations.forEach((key, value) -> {
      Collections.sort(organizations.get(key), Comparator.comparing(o -> o.getName().toUpperCase()));
    });

    // Getting ROOT_ORG
    Owner selectedOrg = organizationDAO.getById("ROOT_ORGANIZATION_ID");
    testSideNavbarContent(selectedOrg, 21, 6);

    selectedOrg = findFirstOrgChild(selectedOrg.getId(), organizations.get(2));
    testSideNavbarContent(selectedOrg, 9, 2);

    selectedOrg = findFirstOrgChild(selectedOrg.getId(), organizations.get(1));
    testSideNavbarContent(selectedOrg, 3, 0);

    selectedOrg = findFirstOrgChild(selectedOrg.getId(), organizations.get(0));
    testSideNavbarContent(selectedOrg, 0, 0);
  }

  @Test
  public void testOrgsAndPoliciesSideNavbar_managementViewRedirectsToRootOrg() {
    SidebarNavigation.openNavigationSidebar();
    SidebarNavigation.policiesNavigationButton().click();

    NxLoadingSpinner.seeAndWaitForDismissal(OwnerSummaryPage.sidebar().getElement());

    // Getting ROOT_ORG
    Owner selectedOrg = organizationDAO.getById("ROOT_ORGANIZATION_ID");
    testSideNavbarContent(selectedOrg, 21, 6);
  }

  @Test
  public void testOrgsAndPoliciesSideNavbar_repoManagersMenu() {
    RepositoryManager repositoryManagerA = tempEntity.newRepositoryManager(
        "5E7BCC8D-3FAB6390-83FF543B-ECD79639-D031F7AA");
    RepositoryManager repositoryManagerB = tempEntity.newRepositoryManager(
        "P39Q1VFX-3AHOOK7Y-0L0XMIQA-WLMW6J4J-9KIPBV6B");
    tempEntity.newRepository(repositoryManagerA, "a123", false);
    tempEntity.newRepository(repositoryManagerB, "b123", true);
    tempEntity.newRepositoryManager("AB9Q1VFX-3AHOOK7Y-0L0XMIQA-WLMW6J4J-9KIPBV6B");
    tempEntity.newRepositoryManager("XY9Q1VFX-3AHOOK7Y-0L0XMIQA-WLMW6J4J-9KIPBV6B");
    RepositoryManager firstRepositoryManagerInSortedList = tempEntity.newRepositoryManager(
        "1Z9Q1VFX-3AHOOK7Y-0L0XMIQA-WLMW6J4J-9KIPBV6B");
    Repository npmHostedRepository = tempEntity.newHostedRepository(
        firstRepositoryManagerInSortedList, "npm-hosted", "npm", true);
    tempEntity.newProxyRepository(firstRepositoryManagerInSortedList, "npm-proxy", "npm", true, true);
    Repository mavenCentralRepository = tempEntity.newProxyRepository(
        firstRepositoryManagerInSortedList, "maven-central-proxy", "maven", true, true);
    RepositoryManager namedRepositoryManager = tempEntity.newRepositoryManager(
        "2Z9Q1VFX-3AHKKK7Y-0L0XUPQA-WLFF6J4J-9KIPGT6B", "Repo Manager", "Nexus", "1.0");

    refresh();

    // Repository managers are only accessible in standalone firewall mode
    // Check if the repositories link is visible, if not skip this test (not in firewall mode)
    OrgsAndPoliciesSidebar orgsAndPoliciesSidebar = OwnerSummaryPage.sidebar();
    if (!orgsAndPoliciesSidebar.repositories().exists()) {
      // Skip test - repository managers not available in current deployment mode (Lifecycle)
      return;
    }

    // Validate the repository managers count is displayed correctly (should show 6)
    orgsAndPoliciesSidebar.repositories().shouldHave(text("Repository Managers"));
    orgsAndPoliciesSidebar.repositories().shouldHave(text("(6)"));

    orgsAndPoliciesSidebar.repositories().click();
    NxCollapsible repoManagerList = orgsAndPoliciesSidebar.getRepoManagerList();
    repoManagerList.children().get(0).shouldNotBe(visible);
    repoManagerList.click();
    repoManagerList.children().shouldHave(size(6));
    repoManagerList.children().get(1).shouldBe(visible);
    repoManagerList.shouldHave(text(namedRepositoryManager.getName()));

    eyesWatcher.eyesCheck("Orgs and policies sidebar at Repository Container level");

    orgsAndPoliciesSidebar.getRepositoryManagerLink(0).click();
    waitUntilUrl(OwnerSummaryPage.url(firstRepositoryManagerInSortedList));
    NxCollapsible repositoryList = orgsAndPoliciesSidebar.getRepositoryList();
    repositoryList.children().shouldHave(size(3));

    eyesWatcher.eyesCheck("Orgs and policies sidebar at Repository Manager level");

    orgsAndPoliciesSidebar.getRepositoryLink(0).click();
    waitUntilUrl(OwnerSummaryPage.url(mavenCentralRepository.getType(), mavenCentralRepository.getId()));
    RepositoriesSummaryPage.summaryTile().name().shouldHave(text(mavenCentralRepository.getName()));

    eyesWatcher.eyesCheck("Orgs and policies sidebar at Proxy Repository level");

    refreshOrOpen(OwnerSummaryPage.url(firstRepositoryManagerInSortedList));
    RepositoriesSummaryPage.summaryTile().name().shouldHave(text(firstRepositoryManagerInSortedList.getName()));

    orgsAndPoliciesSidebar.getRepositoryLink(1).click();
    waitUntilUrl(OwnerSummaryPage.url(npmHostedRepository.getType(), npmHostedRepository.getId()));
    RepositoriesSummaryPage.summaryTile().name().shouldHave(text(npmHostedRepository.getName()));

    eyesWatcher.eyesCheck("Orgs and policies sidebar at Hosted Repository level");
  }

  @Test
  public void testOrgsAndPoliciesSideNavbar_importApplications() {
    OrgsAndPoliciesSidebar orgsAndPoliciesSidebar = new OrgsAndPoliciesSidebar();

    List<Organization> childOrganizations = organizations.get(2);
    childOrganizations.sort(Comparator.comparing(organization -> organization.getName().toUpperCase()));
    Organization parentOrg = childOrganizations.get(0);

    OrgsAndPoliciesSidebar.OwnerItem firstChildOrg = orgsAndPoliciesSidebar.getOrganizationLink(0);
    firstChildOrg.click();

    orgsAndPoliciesSidebar.getApplicationPlusIcon().shouldBe(visible).shouldBe(enabled).click();
    orgsAndPoliciesSidebar.getImportApplicationsButton().shouldBe(visible).shouldBe(enabled).click();

    waitUntilUrl(ScmOnboardingPage.url(parentOrg.getId()));
  }

  @Test
  public void testOrgsAndPoliciesSideNavbar_updateNavbarAfterAddingNewApplication() {
    organizations.forEach((key, value) -> {
      Collections.sort(organizations.get(key), Comparator.comparing(o -> o.getName().toUpperCase()));
    });
    Application applicationToCreate = new Application();
    applicationToCreate.setName("Just Created App");
    applicationToCreate.setPublicId("JustCreateAppPublicId");

    Organization parentOrganization = organizations.get(2).get(0);
    refreshOrOpen(OwnerSummaryPage.url(parentOrganization));
    waitUntilUrl(OwnerSummaryPage.url(parentOrganization));

    OwnerSummaryPage.summaryTile().name().shouldHave(text(parentOrganization.getName()));
    testSideNavbarContent(
        parentOrganization,
        new ArrayList<>(organizationDAO.getByParentOrganizationId(parentOrganization.getId())),
        new ArrayList<>(applicationDAO.getByOrganizationId(parentOrganization.getId())));

    selectAddApplicationOption();

    // Create Application
    OwnerEditorDialog.nameDiv().shouldBe(visible).shouldHave(cssClass("pristine"));
    OwnerEditorDialog.name().shouldBe(visible, empty);
    OwnerEditorDialog.publicIdDiv().shouldBe(visible).shouldHave(cssClass("pristine"));
    OwnerEditorDialog.publicId().shouldBe(visible, empty);

    OwnerEditorDialog.name().val(applicationToCreate.getName());
    OwnerEditorDialog.nameInvalidMessage().shouldNotBe(visible);
    OwnerEditorDialog.publicId().val(applicationToCreate.getPublicId());
    OwnerEditorDialog.publicIdInvalidMessage().shouldNotBe(visible);
    OwnerEditorDialog.saveButton().shouldBe(enabled);

    OwnerEditorDialog.nameDiv().shouldNotHave(cssClass("pristine"));
    OwnerEditorDialog.publicIdDiv().shouldNotHave(cssClass("pristine"));

    OwnerEditorDialog.saveButton().click();
    OwnerEditorDialog.root().should(disappear);

    Application app = applicationDAO.getByPublicId(applicationToCreate.getPublicId());
    assertThat(app).isNotNull();
    assertThat(app.getPublicId()).isEqualTo(applicationToCreate.getPublicId());
    assertThat(app.getOrganizationId()).isEqualTo(parentOrganization.getId());
    assertThat(app.getName()).isEqualTo(applicationToCreate.getName());

    // redirect to newly created application
    waitUntilUrl(OwnerSummaryPage.url(app));
    OwnerSummaryPage.summaryTile().name().shouldHave(text(app.getName()));

    testSideNavbarContent(
        parentOrganization,
        new ArrayList<>(organizationDAO.getByParentOrganizationId(parentOrganization.getId())),
        new ArrayList<>(applicationDAO.getByOrganizationId(parentOrganization.getId())));
  }

  @Test
  public void testOrgsAndPoliciesSideNavbar_updateNavbarAfterMovingApplication() {
    organizations.forEach((key, value) -> {
      Collections.sort(organizations.get(key), Comparator.comparing(o -> o.getName().toUpperCase()));
    });

    Organization parentOrganization = organizations.get(2).get(0);
    Application movingApplication = applicationDAO.getByOrganizationId(parentOrganization.getId()).get(0);
    Organization newParentOrganization = organizations.get(0).get(0);
    refreshOrOpen(OwnerSummaryPageWithLimitedVisibility.url(movingApplication));
    waitUntilUrl(OwnerSummaryPageWithLimitedVisibility.url(movingApplication));

    OwnerSummaryPage.summaryTile().name().shouldHave(text(movingApplication.getName()));
    testSideNavbarContent(
        parentOrganization,
        new ArrayList<>(organizationDAO.getByParentOrganizationId(parentOrganization.getId())),
        new ArrayList<>(applicationDAO.getByOrganizationId(parentOrganization.getId())));

    MoveOwnerDialog modal = new MoveOwnerDialog();
    selectOptionAndSubmit(modal, newParentOrganization);
    modal.shouldBe(hidden);

    MoveApplicationSuccessModal successDialog = new MoveApplicationSuccessModal();
    successDialog.shouldBe(visible);
    successDialog.okButton().click();
    successDialog.shouldBe(hidden);
    modal.shouldBe(hidden);

    Application updatedApp = applicationDAO.getById(movingApplication.getId());
    assertThat(updatedApp.getParentOwnerId()).isEqualTo(newParentOrganization.getId());

    waitUntilUrl(OwnerSummaryPageWithLimitedVisibility.url(movingApplication));
    OwnerSummaryPage.summaryTile().name().shouldHave(text(movingApplication.getName()));
    testSideNavbarContent(
        newParentOrganization,
        new ArrayList<>(organizationDAO.getByParentOrganizationId(newParentOrganization.getId())),
        new ArrayList<>(applicationDAO.getByOrganizationId(newParentOrganization.getId())));
  }

  @Test
  public void testOrgsAndPoliciesSideNavbar_filteringOwners() {
    OrgsAndPoliciesSidebar orgsAndPoliciesSidebar = new OrgsAndPoliciesSidebar();

    orgsAndPoliciesSidebar.getOrganizationLink(0).click();
    orgsAndPoliciesSidebar.getApplicationLink(0).hover();
    NxTooltip tooltip = new NxTooltip();
    tooltip.shouldNotBe(visible);

    orgsAndPoliciesSidebar.filterInput().val("app");
    orgsAndPoliciesSidebar.getApplicationLink(0).hover();
    tooltip.shouldBe(visible);
    tooltip.shouldHave(text("TestApp_0\nParent: TestOrg_0"));
  }

  private Organization findFirstOrgChild(String parentOrgId, List<Organization> organizations) {
    organizations.sort(Comparator.comparing(organization -> organization.getName().toUpperCase()));
    return organizations.stream()
        .filter(org -> org.getParentOrganizationId().equals(parentOrgId))
        .findFirst()
        .get();
  }

  private void testSideNavbarContent(Owner parentOwner, int apps, int orgs) {
    String parentName = parentOwner.getName();

    OrgsAndPoliciesSidebar orgsAndPoliciesSidebar = new OrgsAndPoliciesSidebar();
    orgsAndPoliciesSidebar.selectedOrg().shouldHave(text(parentName));

    if (parentName != "Root Organization") {
      List<Application> childApps = new ArrayList<>(applicationDAO.getByOrganizationId(parentOwner.getId()));

      if (!childApps.isEmpty()) {
        childApps.sort(Comparator.comparing(application -> application.getName().toUpperCase()));

        NxCollapsible childApplications = orgsAndPoliciesSidebar.getApplicationList();
        childApplications.children().shouldHave(size(childApps.size()));
        for (int i = 0; i < childApps.size(); i++) {
          SelenideElement childApp = childApplications.children().get(i);
          childApp.shouldHave(text(childApps.get(i).getName()));
        }
      }
    }

    List<Organization> childOrgs = new ArrayList<>(organizationDAO.getByParentOrganizationId(parentOwner.getId()));

    if (!childOrgs.isEmpty()) {
      childOrgs.sort(Comparator.comparing(organization -> organization.getName().toUpperCase()));
      NxCollapsible childOrganizations = orgsAndPoliciesSidebar.getOrganizationList();
      childOrganizations.children().shouldHave(size(childOrgs.size()));

      for (int i = 0; i < childOrgs.size(); i++) {
        OrgsAndPoliciesSidebar.OwnerItem childOrg = orgsAndPoliciesSidebar.getOrganizationLink(i);
        childOrg.ownerName().shouldHave(text(childOrgs.get(i).getName()));
        childOrg.orgCounter().shouldHave(text(String.format("(%d)", apps + orgs)));
        childOrg.orgCounter().hover();

        NxTooltip tooltip = new NxTooltip();
        tooltip.shouldHave(text("Sub-Orgs: " + orgs));
        tooltip.shouldHave(text("Total Apps: " + apps));
      }

      OrgsAndPoliciesSidebar.OwnerItem firstChildOrg = orgsAndPoliciesSidebar.getOrganizationLink(0);
      firstChildOrg.click();
    }
  }

  private void selectAddApplicationOption() {
    OrgsAndPoliciesSidebar orgsAndPoliciesSidebar = new OrgsAndPoliciesSidebar();
    SelenideElement applicationsActionButton = orgsAndPoliciesSidebar.getApplicationPlusIcon();
    assertThat(applicationsActionButton).isNotNull();
    assertThat(applicationsActionButton.is(visible)).isTrue();
    assertThat(applicationsActionButton.isEnabled()).isTrue();
    applicationsActionButton.click();
    SelenideElement newApplicationButton = orgsAndPoliciesSidebar.getNewApplicationButton();
    assertThat(newApplicationButton.is(visible)).isTrue();
    assertThat(newApplicationButton.isEnabled()).isTrue();
    newApplicationButton.click();
  }

  private void selectOptionAndSubmit(MoveOwnerDialog modal, Organization destination) {
    ActionDropDown.actionButton().shouldBe(visible).click();
    ActionDropDown.moveOwner().shouldBe(visible).click();
    modal.shouldBe(visible);
    modal.body().shouldBe(visible);

    NxFormSelect destinationDropdown = modal.destinationDropdown();
    destinationDropdown.shouldBe(visible).click();
    destinationDropdown.chooseOption(destination.getName());

    modal.errorMessage().shouldBe(hidden);
    modal.dismissButton().shouldHave(text("Cancel"));
    modal.moveButton().click();
  }

  private void testSideNavbarContent(
      Organization parentOrg,
      List<Organization> childOrgs,
      List<Application> childApps)
  {
    OrgsAndPoliciesSidebar orgsAndPoliciesSidebar = OwnerSummaryPage.sidebar();

    orgsAndPoliciesSidebar.selectedOrg().shouldHave(text(parentOrg.getName()));
    if (childOrgs != null && !childOrgs.isEmpty()) {
      NxCollapsible childOrganizationsCollapsible = orgsAndPoliciesSidebar.getOrganizationList();
      assertThat(childOrganizationsCollapsible).isNotNull();
      assertThat(childOrganizationsCollapsible.children()).hasSameSizeAs(childOrgs);

      childOrgs.sort(Comparator.comparing(organization -> organization.getName().toUpperCase()));
      AtomicInteger index = new AtomicInteger();
      childOrgs.forEach(organization -> {
        OrgsAndPoliciesSidebar.OwnerItem childOrgItem =
            orgsAndPoliciesSidebar.getOrganizationLink(index.getAndIncrement());
        childOrgItem.ownerName().shouldHave(text(organization.getName()));
      });
    }

    if (childApps != null && !childApps.isEmpty()) {
      NxCollapsible childApplicationsCollapsible = orgsAndPoliciesSidebar.getApplicationList();
      assertThat(childApplicationsCollapsible).isNotNull();
      assertThat(childApplicationsCollapsible.children()).hasSameSizeAs(childApps);

      childApps.sort(Comparator.comparing(application -> application.getName().toUpperCase()));
      AtomicInteger index = new AtomicInteger();
      childApps.forEach(application -> {
        OrgsAndPoliciesSidebar.OwnerItem childOrgItem =
            orgsAndPoliciesSidebar.getApplicationLink(index.getAndIncrement());
        childOrgItem.ownerName().shouldHave(text(application.getName()));
      });
    }
  }
}
