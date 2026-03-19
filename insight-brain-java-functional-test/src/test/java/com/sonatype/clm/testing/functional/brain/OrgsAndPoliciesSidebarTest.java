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
import com.sonatype.clm.testing.functional.elements.NxCollapsible;
import com.sonatype.clm.testing.functional.elements.NxTooltip;
import com.sonatype.clm.testing.functional.elements.OrgsAndPoliciesSidebar;
import com.sonatype.clm.testing.functional.elements.OwnerEditorDialog;
import com.sonatype.clm.testing.functional.elements.SidebarNavigation;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.utils.NameSupplierDictionary;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;

import com.codeborne.selenide.SelenideElement;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.disappear;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.enabled;
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
