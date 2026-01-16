/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.NxSubmitMask;
import com.sonatype.clm.testing.functional.elements.SidebarNavigation;
import com.sonatype.clm.testing.functional.elements.UnsavedModal;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.InnerSourceRepositoryBaseConfigurationsPage;
import com.sonatype.clm.testing.functional.pages.InnerSourceRepositoryBaseConfigurationsPage.DeleteModal;
import com.sonatype.clm.testing.functional.pages.InnerSourceRepositoryBaseConfigurationsPage.RepositoryConnectionRow;
import com.sonatype.clm.testing.functional.pages.InnerSourceRepositoryConfigurationModal;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.RepositoryConnection;
import com.sonatype.insight.brain.model.repository.RepositoryFormat;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import static com.codeborne.selenide.Condition.appear;
import static com.codeborne.selenide.Condition.checked;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class InnerSourceRepositoryBaseConfigurationsPageTest
    extends AbstractFunctionalTest
{
  @Rule
  public WireMockRule nxrm3MockServer = new WireMockRule(wireMockConfig().dynamicPort());

  private OrganizationDAO organizationDAO;

  private ApplicationDAO applicationDAO;

  private Organization rootOrg;

  private Organization org;

  private Application app;

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(DashboardPage.url());
    loginAsAdmin();
  }

  @Before
  public void before() {
    organizationDAO = lookup(OrganizationDAO.class);
    applicationDAO = lookup(ApplicationDAO.class);

    rootOrg = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);

    org = tempEntity.newOrganization();
    app = tempEntity.newApplication(org.getId());
  }

  @After
  public void after() {
    rootOrg.setRepositoryConnectionEnabled(null);
    rootOrg.setAllowRepositoryConnectionOverride(true);
    organizationDAO.update(rootOrg);
  }

  @Test
  public void testLoad_RepositoryConfiguration_RootOrg() {
    testLoad_RepositoryConfiguration(rootOrg);
  }

  @Test
  public void testLoad_RepositoryConfiguration_Org() {
    testLoad_RepositoryConfiguration(org);
  }

  @Test
  public void testLoad_RepositoryConfiguration_App() {
    testLoad_RepositoryConfiguration(app);
  }

  private void testLoad_RepositoryConfiguration(Owner owner) {
    testLoad_RepositoryConfiguration(owner, null, true);
    testLoad_RepositoryConfiguration(owner, false, true);
    testLoad_RepositoryConfiguration(owner, true, true);
    if (OwnerType.ORGANIZATION.equals(owner.getType())) {
      testLoad_RepositoryConfiguration(owner, null, false);
      testLoad_RepositoryConfiguration(owner, false, false);
      testLoad_RepositoryConfiguration(owner, true, false);
    }
  }

  private void testLoad_RepositoryConfiguration(
      Owner owner,
      Boolean enabled,
      boolean allowOverride)
  {
    setRepositoryConfiguration(owner, enabled, allowOverride);
    InnerSourceRepositoryBaseConfigurationsPage page = visitPage(owner.getType().toString(), owner.getId());
    if (OwnerType.APPLICATION.equals(owner.getType())) {
      page.allowOverride().shouldBe(hidden);
    }
    else {
      if (allowOverride) {
        page.allowOverride().shouldBe(visible).input().shouldBe(checked);
      }
      else {
        page.allowOverride().shouldBe(visible).input().shouldNotBe(checked);
      }
    }
    if (owner.getId().equals(Organization.ROOT_ORGANIZATION_ID)) {
      page.inherit().shouldBe(hidden);
    }
    if (enabled == null) {
      if (owner.getId().equals(Organization.ROOT_ORGANIZATION_ID)) {
        page.disable().shouldBe(visible).input().shouldBe(selected);
      }
      else {
        page.inherit().shouldBe(visible).input().shouldBe(selected);
      }
    }
    else {
      if (enabled) {
        page.enable().shouldBe(visible).input().shouldBe(selected);
      }
      else {
        page.disable().shouldBe(visible).input().shouldBe(selected);
      }
    }
  }

  private void setRepositoryConfiguration(
      Owner owner,
      Boolean enabled,
      boolean allowOverride)
  {
    switch (owner.getType()) {
      case APPLICATION: {
        Application application = (Application) owner;
        application.setRepositoryConnectionEnabled(enabled);
        applicationDAO.update(application);
        break;
      }
      case ORGANIZATION: {
        Organization organization = (Organization) owner;
        organization.setRepositoryConnectionEnabled(enabled);
        organization.setAllowRepositoryConnectionOverride(allowOverride);
        organizationDAO.update(organization);
        break;
      }
      default: {
        fail("Unknown owner type.");
      }
    }
  }

  @Test
  public void testLoad_RepositoryConnections_RootOrg() {
    testLoad_RepositoryConnections(rootOrg);

    eyesWatcher.eyesCheck();
  }

  @Test
  public void testLoad_RepositoryConnections_Org() {
    testLoad_RepositoryConnections(org);
  }

  @Test
  public void testLoad_RepositoryConnections_App() {
    testLoad_RepositoryConnections(app);
  }

  private void testLoad_RepositoryConnections(Owner owner) {
    RepositoryConnection repositoryConnection1 = tempEntity.newRepositoryConnection(owner.getId(),
        "baseUrl1", RepositoryFormat.GENERIC, null, null);
    RepositoryConnection repositoryConnection2 = tempEntity.newRepositoryConnection(owner.getId(),
        "baseUrl2", RepositoryFormat.MAVEN, null, null);
    InnerSourceRepositoryBaseConfigurationsPage page =
        visitPage(owner.getType().toString(), owner.getId());

    if (!owner.getId().equals(Organization.ROOT_ORGANIZATION_ID)) {
      page.inherit().click();
      page.row(repositoryConnection1.getId()).shouldNotBe(visible);
      page.row(repositoryConnection2.getId()).shouldNotBe(visible);
    }
    page.disable().click();
    page.row(repositoryConnection1.getId()).shouldNotBe(visible);
    page.row(repositoryConnection2.getId()).shouldNotBe(visible);
    page.enable().click();
    RepositoryConnectionRow repositoryConnectionRow1 = page.row(repositoryConnection1.getId()).shouldBe(visible);
    repositoryConnectionRow1.text().shouldHave(text(repositoryConnection1.getBaseUrl()),
        text(repositoryConnection1.getFormat().toString()));
    RepositoryConnectionRow repositoryConnectionRow2 = page.row(repositoryConnection2.getId()).shouldBe(visible);
    repositoryConnectionRow2.text().shouldHave(text(repositoryConnection2.getBaseUrl()),
        text(repositoryConnection2.getFormat().toString()));
  }

  @Test
  public void testSave_RepositoryConfiguration_RootOrg() {
    testSave_RepositoryConfiguration(rootOrg);
  }

  @Test
  public void testSave_RepositoryConfiguration_Org() {
    testSave_RepositoryConfiguration(org);
  }

  @Test
  public void testSave_RepositoryConfiguration_App() {
    testSave_RepositoryConfiguration(app);
  }

  private void testSave_RepositoryConfiguration(Owner owner) {
    setRepositoryConfiguration(owner, true, false);
    testSave_RepositoryConfiguration(owner, null, true);

    setRepositoryConfiguration(owner, true, false);
    testSave_RepositoryConfiguration(owner, false, true);

    setRepositoryConfiguration(owner, false, false);
    testSave_RepositoryConfiguration(owner, true, true);

    if (OwnerType.ORGANIZATION.equals(owner.getType())) {
      setRepositoryConfiguration(owner, true, true);
      testSave_RepositoryConfiguration(owner, null, false);

      setRepositoryConfiguration(owner, true, true);
      testSave_RepositoryConfiguration(owner, false, false);

      setRepositoryConfiguration(owner, false, true);
      testSave_RepositoryConfiguration(owner, true, false);
    }
  }

  private void testSave_RepositoryConfiguration(Owner owner, Boolean enabled, boolean allowOverride) {
    InnerSourceRepositoryBaseConfigurationsPage page = visitPage(owner.getType().toString(), owner.getId());
    if (OwnerType.ORGANIZATION.equals(owner.getType()) && page.allowOverride().input().is(checked) != allowOverride) {
      page.allowOverride().click();
    }
    if (enabled == null) {
      if (!owner.getId().equals(Organization.ROOT_ORGANIZATION_ID)) {
        page.inherit().click();
      }
      else {
        enabled = ((Organization) owner).isRepositoryConnectionEnabled();
      }
    }
    else {
      if (enabled) {
        page.enable().click();
      }
      else {
        page.disable().click();
      }
    }
    page.save().click();
    NxSubmitMask.seeAndWaitForDismissal();
    checkRepositoryBaseConfiguration(owner, enabled, allowOverride);
  }

  private void checkRepositoryBaseConfiguration(
      Owner owner,
      Boolean enabled,
      boolean allowOverride)
  {
    switch (owner.getType()) {
      case APPLICATION: {
        Application application = applicationDAO.getById(owner.getId());
        assertThat(application.isRepositoryConnectionEnabled()).isEqualTo(enabled);
        break;
      }
      case ORGANIZATION: {
        Organization organization = organizationDAO.getById(owner.getId());
        assertThat(organization.isRepositoryConnectionEnabled()).isEqualTo(enabled);
        assertThat(organization.isAllowRepositoryConnectionOverride()).isEqualTo(allowOverride);
        break;
      }
      default: {
        fail("Unknown owner type.");
      }
    }
  }

  @Test
  public void testLinks_RootOrg() {
    testModalButtonsAndLinks(rootOrg);
  }

  @Test
  public void testLinks_Org() {
    testModalButtonsAndLinks(org);
  }

  @Test
  public void testLinks_App() {
    testModalButtonsAndLinks(app);
  }

  private void testModalButtonsAndLinks(Owner owner) {
    RepositoryConnection repositoryConnection = tempEntity.newRepositoryConnection(owner.getId());
    setRepositoryConfiguration(owner, true, true);
    InnerSourceRepositoryBaseConfigurationsPage page = visitPage(owner.getType().toString(), owner.getId());

    page.add().click();
    InnerSourceRepositoryConfigurationModal modal = new InnerSourceRepositoryConfigurationModal();
    modal.should(appear);
    modal.cancel().shouldBe(visible, enabled).click();

    page.row(repositoryConnection.getId()).edit().click();
    modal.cancel().click();
    modal.shouldNotBe(visible);

    page.back().click();
    waitUntilUrl(OwnerSummaryPage.url(owner));
    refresh();
    SidebarNavigation.closeNavigationSidebar();
    OwnerSummaryPage.summaryTile().innerSourceRepositoryButton().shouldBe(visible).click();
    OwnerSummaryPage.innerSourceRepositoryTile().editButton().click();
    waitUntilUrl(InnerSourceRepositoryBaseConfigurationsPage.url(owner.getType().toString(), owner.getId()));
  }

  @Test
  public void testOverrideNotAllowed() {
    testIsOverrideAllowed(rootOrg, true);
    testIsOverrideAllowed(org, true);
    testIsOverrideAllowed(app, true);

    setRepositoryConfiguration(rootOrg, null, false);
    testIsOverrideAllowed(rootOrg, true);
    testIsOverrideAllowed(org, false);
    testIsOverrideAllowed(app, false);

    setRepositoryConfiguration(rootOrg, null, true);
    setRepositoryConfiguration(org, null, false);
    testIsOverrideAllowed(rootOrg, true);
    testIsOverrideAllowed(org, true);
    testIsOverrideAllowed(app, false);
  }

  private void testIsOverrideAllowed(Owner owner, boolean allowed) {
    InnerSourceRepositoryBaseConfigurationsPage page = visitPage(owner.getType().toString(), owner.getId());
    refresh();
    if (allowed) {
      page.alert().shouldNotBe(visible);
    }
    else {
      page.alert().shouldBe(visible);
    }
  }

  private InnerSourceRepositoryBaseConfigurationsPage visitPage(String ownerType, String ownerId) {
    refreshOrOpen(InnerSourceRepositoryBaseConfigurationsPage.url(ownerType, ownerId));
    InnerSourceRepositoryBaseConfigurationsPage page = new InnerSourceRepositoryBaseConfigurationsPage();
    page.shouldBe(visible);
    return page;
  }

  @Test
  public void testAddEditAndDeleteRepository_RootOrg() {
    testAddEditAndDeleteRepository(rootOrg);
  }

  @Test
  public void testAddEditAndDeleteRepository_Org() {
    testAddEditAndDeleteRepository(org);
  }

  @Test
  public void testAddEditAndDeleteRepository_App() {
    testAddEditAndDeleteRepository(app);
  }

  public void testAddEditAndDeleteRepository(Owner owner) {
    RepositoryConnection repositoryConnection = tempEntity.newRepositoryConnection(owner.getId());
    setRepositoryConfiguration(owner, true, true);
    InnerSourceRepositoryBaseConfigurationsPage page = visitPage(owner.getType().toString(), owner.getId());

    page.add().click();
    InnerSourceRepositoryConfigurationModal addRepositoryModal = new InnerSourceRepositoryConfigurationModal();
    addRepositoryModal.shouldBe(visible);
    addRepositoryModal.cancel().click();

    page.row(repositoryConnection.getId()).edit().click();
    InnerSourceRepositoryConfigurationModal editRepositoryModal = new InnerSourceRepositoryConfigurationModal();
    editRepositoryModal.shouldBe(visible);
    editRepositoryModal.cancel().click();

    page.row(repositoryConnection.getId()).delete().click();
    DeleteModal deleteModal = new DeleteModal();
    deleteModal.shouldBe(visible);
    deleteModal.cancel().click();
    deleteModal.shouldNotBe(visible);

    page.back().click();
    refresh();
    SidebarNavigation.closeNavigationSidebar();
    OwnerSummaryPage.summaryTile().innerSourceRepositoryButton().shouldBe(visible).click();
    OwnerSummaryPage.innerSourceRepositoryTile().editButton().click();
    waitUntilUrl(InnerSourceRepositoryBaseConfigurationsPage.url(owner.getType().toString(), owner.getId()));
  }

  @Test
  public void testUnsavedChanges() {
    setRepositoryConfiguration(org, true, true);
    InnerSourceRepositoryBaseConfigurationsPage page = visitPage(org.getType().toString(), org.getId());

    page.allowOverride().click();
    checkUnsavedChangesModalIsVisible();
    refresh();

    page.disable().click();
    checkUnsavedChangesModalIsVisible();
    refresh();

    setRepositoryConfiguration(org, false, false);
    page = visitPage(org.getType().toString(), org.getId());

    page.allowOverride().click();
    checkUnsavedChangesModalIsVisible();
    refresh();

    page.enable().click();
    checkUnsavedChangesModalIsVisible();
  }

  private void checkUnsavedChangesModalIsVisible() {
    SidebarNavigation.dashboardNavigationButton().click();
    UnsavedModal unsavedChangesModal = new UnsavedModal();
    unsavedChangesModal.shouldBe(visible);
    unsavedChangesModal.cancelButton().click();
  }
}
