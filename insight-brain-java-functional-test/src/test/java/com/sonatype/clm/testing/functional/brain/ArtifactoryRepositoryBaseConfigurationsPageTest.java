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
import com.sonatype.clm.testing.functional.pages.ArtifactoryRepositoryBaseConfigurationsPage;
import com.sonatype.clm.testing.functional.pages.ArtifactoryRepositoryBaseConfigurationsPage.ArtifactoryConnectionRow;
import com.sonatype.clm.testing.functional.pages.ArtifactoryRepositoryBaseConfigurationsPage.DeleteModal;
import com.sonatype.clm.testing.functional.pages.ArtifactoryRepositoryConfigurationModal;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.artifactory.ArtifactoryConnection;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
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

public class ArtifactoryRepositoryBaseConfigurationsPageTest
    extends AbstractFunctionalTest
{
  @Rule
  public WireMockRule artifactoryMockServer = new WireMockRule(wireMockConfig().dynamicPort());

  private OrganizationDAO organizationDAO;

  private ApplicationDAO applicationDAO;

  private Organization org;

  private Application app;

  private Organization rootOrg;

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

    SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.setEnabled(true);
    rootOrg.setArtifactoryConnectionEnabled(null);
    rootOrg.setAllowArtifactoryConnectionOverride(true);
    organizationDAO.update(rootOrg);
    org = tempEntity.newOrganization();
    app = tempEntity.newApplication(org.getId());
  }

  @Test
  public void testLoad_ArtifactoryConfiguration_RootOrg() {
    testLoad_ArtifactoryConfiguration(rootOrg);
  }

  @Test
  public void testLoad_ArtifactoryConfiguration_Org() {
    testLoad_ArtifactoryConfiguration(org);
  }

  @Test
  public void testLoad_ArtifactoryConfiguration_App() {
    testLoad_ArtifactoryConfiguration(app);
  }

  private void testLoad_ArtifactoryConfiguration(Owner owner) {
    testLoad_ArtifactoryConfiguration(owner, null, true);
    testLoad_ArtifactoryConfiguration(owner, false, true);
    testLoad_ArtifactoryConfiguration(owner, true, true);
    if (OwnerType.ORGANIZATION.equals(owner.getType())) {
      testLoad_ArtifactoryConfiguration(owner, null, false);
      testLoad_ArtifactoryConfiguration(owner, false, false);
      testLoad_ArtifactoryConfiguration(owner, true, false);
    }
  }

  private void testLoad_ArtifactoryConfiguration(
      Owner owner,
      Boolean enabled,
      boolean allowOverride)
  {
    setArtifactoryConfiguration(owner, enabled, allowOverride);
    ArtifactoryRepositoryBaseConfigurationsPage page = visitPage(owner.getType().toString(), owner.getId());
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

  private void setArtifactoryConfiguration(
      Owner owner,
      Boolean enabled,
      boolean allowOverride)
  {
    switch (owner.getType()) {
      case APPLICATION: {
        Application application = (Application) owner;
        application.setArtifactoryConnectionEnabled(enabled);
        applicationDAO.update(application);
        break;
      }
      case ORGANIZATION: {
        Organization organization = (Organization) owner;
        organization.setArtifactoryConnectionEnabled(enabled);
        organization.setAllowArtifactoryConnectionOverride(allowOverride);
        organizationDAO.update(organization);
        break;
      }
      default: {
        fail("Unknown owner type.");
      }
    }
  }

  @Test
  public void testLoad_ArtifactoryConnection_RootOrg() {
    testLoad_ArtifactoryConnection(rootOrg);
  }

  @Test
  public void testLoad_ArtifactoryConnection_Org() {
    testLoad_ArtifactoryConnection(org);

    eyesWatcher.eyesCheck();
  }

  @Test
  public void testLoad_ArtifactoryConnection_App() {
    testLoad_ArtifactoryConnection(app);
  }

  private void testLoad_ArtifactoryConnection(Owner owner) {
    ArtifactoryConnection artifactoryConnection = tempEntity.newArtifactoryConnection(owner.getId(),
        "baseUrl", null, null);
    ArtifactoryRepositoryBaseConfigurationsPage page =
        visitPage(owner.getType().toString(), owner.getId());

    if (!owner.getId().equals(Organization.ROOT_ORGANIZATION_ID)) {
      page.inherit().click();
      page.row(artifactoryConnection.getId()).shouldNotBe(visible);
    }
    page.disable().click();
    page.row(artifactoryConnection.getId()).shouldNotBe(visible);
    page.enable().click();
    ArtifactoryConnectionRow artifactoryConnectionRow = page.row(artifactoryConnection.getId()).shouldBe(visible);
    artifactoryConnectionRow.text().shouldHave(text(artifactoryConnection.getBaseUrl()));
  }

  @Test
  public void testSave_ArtifactoryConfiguration_RootOrg() {
    testSave_ArtifactoryConfiguration(rootOrg);
  }

  @Test
  public void testSave_ArtifactoryConfiguration_Org() {
    testSave_ArtifactoryConfiguration(org);
  }

  @Test
  public void testSave_ArtifactoryConfiguration_App() {
    testSave_ArtifactoryConfiguration(app);
  }

  private void testSave_ArtifactoryConfiguration(Owner owner) {
    setArtifactoryConfiguration(owner, true, false);
    testSave_ArtifactoryConfiguration(owner, null, true);

    setArtifactoryConfiguration(owner, true, false);
    testSave_ArtifactoryConfiguration(owner, false, true);

    setArtifactoryConfiguration(owner, false, false);
    testSave_ArtifactoryConfiguration(owner, true, true);

    if (OwnerType.ORGANIZATION.equals(owner.getType())) {
      setArtifactoryConfiguration(owner, true, true);
      testSave_ArtifactoryConfiguration(owner, null, false);

      setArtifactoryConfiguration(owner, true, true);
      testSave_ArtifactoryConfiguration(owner, false, false);

      setArtifactoryConfiguration(owner, false, true);
      testSave_ArtifactoryConfiguration(owner, true, false);
    }
  }

  private void testSave_ArtifactoryConfiguration(Owner owner, Boolean enabled, boolean allowOverride) {
    ArtifactoryRepositoryBaseConfigurationsPage page = visitPage(owner.getType().toString(), owner.getId());
    if (OwnerType.ORGANIZATION.equals(owner.getType()) && page.allowOverride().input().is(checked) != allowOverride) {
      page.allowOverride().click();
    }
    if (enabled == null) {
      if (!owner.getId().equals(Organization.ROOT_ORGANIZATION_ID)) {
        page.inherit().click();
      }
      else {
        enabled = ((Organization) owner).isArtifactoryConnectionEnabled();
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
    checkArtifactoryBaseConfiguration(owner, enabled, allowOverride);
  }

  private void checkArtifactoryBaseConfiguration(
      Owner owner,
      Boolean enabled,
      boolean allowOverride)
  {
    switch (owner.getType()) {
      case APPLICATION: {
        Application application = applicationDAO.getById(owner.getId());
        assertThat(application.isArtifactoryConnectionEnabled()).isEqualTo(enabled);
        break;
      }
      case ORGANIZATION: {
        Organization organization = organizationDAO.getById(owner.getId());
        assertThat(organization.isArtifactoryConnectionEnabled()).isEqualTo(enabled);
        assertThat(organization.isAllowArtifactoryConnectionOverride()).isEqualTo(allowOverride);
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
    setArtifactoryConfiguration(owner, true, true);
    ArtifactoryRepositoryBaseConfigurationsPage page = visitPage(owner.getType().toString(), owner.getId());

    page.add().click();
    ArtifactoryRepositoryConfigurationModal modal = new ArtifactoryRepositoryConfigurationModal();
    modal.should(appear);
    modal.cancel().shouldBe(visible, enabled).click();

    ArtifactoryConnection artifactoryConnection = tempEntity.newArtifactoryConnection(owner.getId(),
        "baseUrl", null, null);
    page = visitPage(owner.getType().toString(), owner.getId());

    page.row(artifactoryConnection.getId()).edit().click();
    modal.cancel().click();
    modal.shouldNotBe(visible);

    page.back().click();
    waitUntilUrl(OwnerSummaryPage.url(owner));
    refresh();
    SidebarNavigation.closeNavigationSidebar();
    OwnerSummaryPage.summaryTile().artifactoryRepositoryButton().shouldBe(visible).click();
    OwnerSummaryPage.artifactoryRepositoryTile().editButton().click();
    waitUntilUrl(ArtifactoryRepositoryBaseConfigurationsPage.url(owner.getType().toString(), owner.getId()));
  }

  @Test
  public void testOverrideNotAllowed() {
    testIsOverrideAllowed(rootOrg, true);
    testIsOverrideAllowed(org, true);
    testIsOverrideAllowed(app, true);

    setArtifactoryConfiguration(rootOrg, null, false);
    testIsOverrideAllowed(rootOrg, true);
    testIsOverrideAllowed(org, false);
    testIsOverrideAllowed(app, false);

    setArtifactoryConfiguration(rootOrg, null, true);
    setArtifactoryConfiguration(org, null, false);
    testIsOverrideAllowed(rootOrg, true);
    testIsOverrideAllowed(org, true);
    testIsOverrideAllowed(app, false);
  }

  private void testIsOverrideAllowed(Owner owner, boolean allowed) {
    ArtifactoryRepositoryBaseConfigurationsPage page = visitPage(owner.getType().toString(), owner.getId());
    refresh();
    if (allowed) {
      page.alert().shouldNotBe(visible);
    }
    else {
      page.alert().shouldBe(visible);
    }
  }

  private ArtifactoryRepositoryBaseConfigurationsPage visitPage(String ownerType, String ownerId) {
    refreshOrOpen(ArtifactoryRepositoryBaseConfigurationsPage.url(ownerType, ownerId));
    ArtifactoryRepositoryBaseConfigurationsPage page = new ArtifactoryRepositoryBaseConfigurationsPage();
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
    setArtifactoryConfiguration(owner, true, true);
    ArtifactoryRepositoryBaseConfigurationsPage page = visitPage(owner.getType().toString(), owner.getId());

    page.add().click();
    ArtifactoryRepositoryConfigurationModal addRepositoryModal = new ArtifactoryRepositoryConfigurationModal();
    addRepositoryModal.shouldBe(visible);
    addRepositoryModal.cancel().click();

    ArtifactoryConnection artifactoryConnection = tempEntity.newArtifactoryConnection(owner.getId(),
        "baseUrl", null, null);
    refresh();
    page.row(artifactoryConnection.getId()).edit().click();
    ArtifactoryRepositoryConfigurationModal editRepositoryModal = new ArtifactoryRepositoryConfigurationModal();
    editRepositoryModal.shouldBe(visible);
    editRepositoryModal.cancel().click();

    page.row(artifactoryConnection.getId()).delete().click();
    DeleteModal deleteModal = new DeleteModal();
    deleteModal.shouldBe(visible);
    deleteModal.cancel().click();
    deleteModal.shouldNotBe(visible);

    page.back().click();
    refresh();
    SidebarNavigation.closeNavigationSidebar();
    OwnerSummaryPage.summaryTile().artifactoryRepositoryButton().shouldBe(visible).click();
    OwnerSummaryPage.artifactoryRepositoryTile().editButton().click();
    waitUntilUrl(ArtifactoryRepositoryBaseConfigurationsPage.url(owner.getType().toString(), owner.getId()));
  }

  @Test
  public void testUnsavedChanges() {
    setArtifactoryConfiguration(org, true, true);
    ArtifactoryRepositoryBaseConfigurationsPage page = visitPage(org.getType().toString(), org.getId());

    page.allowOverride().click();
    checkUnsavedChangesModalIsVisible();
    refresh();

    page.disable().click();
    checkUnsavedChangesModalIsVisible();
    refresh();

    setArtifactoryConfiguration(org, false, false);
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
