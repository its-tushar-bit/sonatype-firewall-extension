/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.ComponentLegalOverviewPage;
import com.sonatype.clm.testing.playwright.pages.ComponentLegalOverviewPageAssertions;
import com.sonatype.clm.testing.playwright.pages.LegalDashboardPage;
import com.sonatype.clm.testing.playwright.pages.LegalDashboardPageAssertions;
import com.sonatype.clm.testing.playwright.testdatamanager.TestDataManager;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerComponent;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.license.model.LicensedFeature;

import com.microsoft.playwright.Locator.WaitForOptions;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class LegalDashboardPlaywrightTest
    extends AbstractIqUiTest
{
  private record LegalDashboardData(
      String stageTypeId,
      String orgAlphaName,
      String orgBetaName,
      String tagName,
      String appPrefix,
      int totalAppCount,
      int taggedAppCount,
      int applicationsPageSize,
      int componentsPageSize,
      String componentAlphaName,
      String componentBetaName,
      String componentGammaName,
      String licenseAlphaId,
      String licenseBetaId,
      String licenseGammaId,
      int expectedFirstPageRowCount,
      int expectedSecondPageRowCount,
      int expectedComponentsFirstPageRowCount,
      int expectedTotalComponentCount,
      String applicationNameSortColumn,
      String lastScanTimeSortColumn,
      String appCategoriesSortColumn,
      String attributionTooltipOnComponentsTab,
      String shortSearchTerm,
      String searchValidationError)
  {
  }

  private static final LegalDashboardData DATA =
      TestDataManager.load("legal-dashboard", LegalDashboardData.class);

  private LegalDashboardPage legalDashboard;

  private LegalDashboardPageAssertions assertions;

  @BeforeEach
  public void setUp() {
    setFeatures(LicensedFeature.values());
    seed();
    stubHdsEndpoints();
    playwrightRefreshOrOpen(LegalDashboardPage.url());
    playwrightLogin();
    legalDashboard = new LegalDashboardPage();
    assertions = new LegalDashboardPageAssertions(legalDashboard);
  }

  @Test
  @org.junit.jupiter.api.Tag("regression")
  public void testLegalDashboardLoadsWithApplicationsTabActive() {
    assertions.shouldBeVisible();
    legalDashboard.waitForApplicationsLoaded();

    assertThat(legalDashboard.componentsTab()).isVisible();
    assertions.shouldShowAllApplicationsColumns();
  }

  @Test
  @org.junit.jupiter.api.Tag("regression")
  public void testApplicationsTabSortByApplicationName() {
    assertions.shouldBeVisible();
    legalDashboard.waitForApplicationsLoaded();
    legalDashboard.applicationNameColumnHeader().click();
    assertions.shouldHaveColumnSortDir(
        legalDashboard.applicationNameColumnHeader(), "ascending");
    legalDashboard.applicationNameColumnHeader().click();
    assertions.shouldHaveColumnSortDir(
        legalDashboard.applicationNameColumnHeader(), "descending");
  }

  @Test
  @org.junit.jupiter.api.Tag("regression")
  public void testApplicationsTabSortByLastScanTime() {
    assertions.shouldBeVisible();
    legalDashboard.waitForApplicationsLoaded();
    legalDashboard.lastScanTimeColumnHeader().click();
    assertions.shouldHaveColumnSortDir(
        legalDashboard.lastScanTimeColumnHeader(), "ascending");
    legalDashboard.lastScanTimeColumnHeader().click();
    assertions.shouldHaveColumnSortDir(
        legalDashboard.lastScanTimeColumnHeader(), "descending");
  }

  @Test
  @org.junit.jupiter.api.Tag("regression")
  public void testApplicationsTabSortByAppCategories() {
    assertions.shouldBeVisible();
    legalDashboard.waitForApplicationsLoaded();
    assertions.shouldShowAppCategoriesColumn();
    legalDashboard.appCategoriesColumnHeader().click();
    assertions.shouldHaveColumnSortDir(
        legalDashboard.appCategoriesColumnHeader(), "ascending");
    legalDashboard.appCategoriesColumnHeader().click();
    assertions.shouldHaveColumnSortDir(
        legalDashboard.appCategoriesColumnHeader(), "descending");
  }

  @Test
  @org.junit.jupiter.api.Tag("regression")
  public void testApplicationsTabPagination() {
    legalDashboard.container().waitFor();
    assertions.shouldBeVisible();
    legalDashboard.waitForApplicationsLoaded();
    assertions.shouldShowPagination();
    assertions.shouldShowApplicationsTableWithRowCount(DATA.expectedFirstPageRowCount());

    legalDashboard.paginationNextButton().click();
    assertions.shouldShowApplicationsTableWithRowCount(DATA.expectedSecondPageRowCount());
    legalDashboard.paginationPreviousButton().click();
    assertions.shouldShowApplicationsTableWithRowCount(DATA.expectedFirstPageRowCount());

    legalDashboard.paginationPageButton(2).click();
    assertions.shouldShowApplicationsTableWithRowCount(DATA.expectedSecondPageRowCount());
    legalDashboard.paginationPageButton(1).click();
    assertions.shouldShowApplicationsTableWithRowCount(DATA.expectedFirstPageRowCount());
  }

  @Test
  @org.junit.jupiter.api.Tag("regression")
  public void testApplicationsTabClickRowNavigatesToDetails() {
    assertions.shouldBeVisible();
    legalDashboard.waitForApplicationsLoaded();
    legalDashboard.applicationsTableRows().first().click();
    playwrightWaitUntilUrlContains("/legal/application/");
  }

  @Test
  @org.junit.jupiter.api.Tag("regression")
  public void testComponentsTabLoadsAndDisplaysTable() {
    assertions.shouldBeVisible();
    legalDashboard.waitForApplicationsLoaded();
    legalDashboard.switchToComponentsTab();
    assertions.shouldShowComponentsTabActive();
    assertions.shouldShowComponentsTableWithRows();

    assertions.shouldShowAllComponentsColumns();

    assertions.shouldShowComponentsPagination();
    assertions.shouldShowComponentsTableWithRowCount(DATA.expectedComponentsFirstPageRowCount());
    legalDashboard.paginationPageButton(2).click();
    assertThat(legalDashboard.componentsTableRows().first()).isVisible();
    assertThat(legalDashboard.componentsTableRows()).hasCount(
        Math.min(DATA.componentsPageSize(),
            DATA.expectedTotalComponentCount() - DATA.expectedComponentsFirstPageRowCount()));
  }

  @Test
  @org.junit.jupiter.api.Tag("regression")
  public void testComponentsTabSortByComponentName() {
    legalDashboard.switchToComponentsTab();
    assertions.shouldShowComponentsTableWithRows();
    legalDashboard.componentNameColumnHeader().click();
    assertions.shouldHaveColumnSortDir(
        legalDashboard.componentNameColumnHeader(), "ascending");
    legalDashboard.componentNameColumnHeader().click();
    assertions.shouldHaveColumnSortDir(
        legalDashboard.componentNameColumnHeader(), "descending");
  }

  @Test
  @org.junit.jupiter.api.Tag("regression")
  public void testComponentsTabSortByLicense() {
    legalDashboard.switchToComponentsTab();
    assertions.shouldShowComponentsTableWithRows();
    legalDashboard.licenseColumnHeader().click();
    assertions.shouldHaveColumnSortDir(
        legalDashboard.licenseColumnHeader(), "ascending");
    legalDashboard.licenseColumnHeader().click();
    assertions.shouldHaveColumnSortDir(
        legalDashboard.licenseColumnHeader(), "descending");
  }

  @Test
  @org.junit.jupiter.api.Tag("regression")
  public void testComponentsTabSortByApplicationCount() {
    legalDashboard.switchToComponentsTab();
    assertions.shouldShowComponentsTableWithRows();
    legalDashboard.applicationCountColumnHeader().click();
    assertions.shouldHaveColumnSortDir(
        legalDashboard.applicationCountColumnHeader(), "ascending");
    legalDashboard.applicationCountColumnHeader().click();
    assertions.shouldHaveColumnSortDir(
        legalDashboard.applicationCountColumnHeader(), "descending");
  }

  @Test
  @org.junit.jupiter.api.Tag("regression")
  public void testComponentsTabSearch() {
    legalDashboard.switchToComponentsTab();
    assertions.shouldShowComponentsTableWithRows();

    assertThat(legalDashboard.componentSearchButton()).isEnabled();

    legalDashboard.searchComponent(DATA.shortSearchTerm());
    assertThat(legalDashboard.componentSearchButton()).isDisabled();
    assertThat(legalDashboard.componentSearchValidationError())
        .hasText(DATA.searchValidationError());

    legalDashboard.searchComponent(DATA.componentAlphaName());
    assertThat(legalDashboard.componentSearchButton()).isEnabled();
    legalDashboard.componentSearchButton().click();
    assertions.shouldShowComponentsTableWithRows();
    assertThat(legalDashboard.componentsTable()).containsText(DATA.componentAlphaName());
    assertThat(legalDashboard.componentsTable()).not().containsText(DATA.componentBetaName());
    assertThat(legalDashboard.componentsTable()).not().containsText(DATA.componentGammaName());
  }

  @Test
  @org.junit.jupiter.api.Tag("regression")
  public void testFilterDrawerOpensAndCloses() {
    assertions.shouldBeVisible();
    legalDashboard.openFilterDrawer();
    assertThat(legalDashboard.filterOrgAppGroup()).isVisible();
    assertThat(legalDashboard.filterCategoryGroup()).isVisible();
    assertThat(legalDashboard.filterStageGroup()).isVisible();
    assertThat(legalDashboard.filterProgressGroup()).isVisible();
    legalDashboard.closeFilterDrawer();
  }

  @Test
  @org.junit.jupiter.api.Tag("regression")
  public void testFilterApplyUpdatesTable() {
    assertions.shouldBeVisible();
    legalDashboard.waitForApplicationsLoaded();
    assertions.shouldShowApplicationsTableWithRowCount(DATA.expectedFirstPageRowCount());

    legalDashboard.openFilterDrawer();
    legalDashboard.expandOrganizationsAndApplicationsGroups();
    legalDashboard.filterOrgAppGroupFirstCheckbox().click();

    legalDashboard.filterProgressGroupExpandTrigger().click();
    legalDashboard.filterProgressGroupCheckboxAt(0).click();

    legalDashboard.filterApplyButton().click();
    assertions.shouldShowFilterDirtyAsterisk();
    assertions.shouldShowApplicationsTableWithRows();

    legalDashboard.closeFilterDrawer();
    assertions.shouldShowFilterDirtyAsterisk();
  }

  @Test
  @org.junit.jupiter.api.Tag("regression")
  public void testFilterSaveAndRevert() {
    assertions.shouldBeVisible();
    legalDashboard.waitForApplicationsLoaded();
    legalDashboard.openFilterDrawer();
    legalDashboard.filterProgressGroupExpandTrigger().click();
    legalDashboard.filterProgressGroupCheckboxAt(1).click();
    legalDashboard.filterApplyButton().click();
    assertions.shouldShowFilterDirtyAsterisk();
    legalDashboard.filterSaveButton().click();
    assertThat(legalDashboard.saveFilterModal()).isVisible();
    legalDashboard.saveFilterModalNameInput().fill("TestFilter");
    legalDashboard.saveFilterModalSaveButton().click();
    legalDashboard.saveFilterModal()
        .waitFor(
            new WaitForOptions().setState(WaitForSelectorState.HIDDEN));
    assertions.shouldNotShowFilterDirtyAsterisk();
    assertThat(legalDashboard.filterRevertButton()).isDisabled();
    legalDashboard.closeFilterDrawer();
    page.reload();
    assertions.shouldBeVisible();
    legalDashboard.waitForApplicationsLoaded();
    assertThat(legalDashboard.filterToggleButton()).containsText("TestFilter");
  }

  @Test
  @org.junit.jupiter.api.Tag("regression")
  public void testCreateAttributionReportModalOpens() {
    assertions.shouldBeVisible();
    legalDashboard.waitForApplicationsLoaded();

    legalDashboard.clickCreateAttributionReport();
    assertions.shouldShowAttributionReportModal();
    legalDashboard.createReportCancelButton().click();
    assertThat(legalDashboard.createReportCancelButton()).isHidden();

    legalDashboard.switchToComponentsTab();
    assertions.shouldShowComponentsTabActive();
    assertions.shouldShowComponentsTableWithRows();
    assertions.shouldShowCreateAttributionReportDisabled();

    legalDashboard.applicationsTab().click();
    legalDashboard.waitForApplicationsLoaded();

    legalDashboard.openFilterDrawer();
    legalDashboard.expandOrganizationsAndApplicationsGroups();
    legalDashboard.filterApplicationsGroupCheckboxAt(1).click();
    legalDashboard.filterApplyButton().click();
    assertions.shouldShowFilterDirtyAsterisk();
    legalDashboard.closeFilterDrawer();
    assertions.shouldShowApplicationsTableWithRowCount(1);

    legalDashboard.clickCreateAttributionReport();
    page.waitForURL(Pattern.compile(".*/legal/application/attributionReport.*"));
  }

  @Test
  @org.junit.jupiter.api.Tag("regression")
  public void testComponentLegalOverviewRendersFromComponentsTab() {
    assertions.shouldBeVisible();
    legalDashboard.switchToComponentsTab();
    assertions.shouldShowComponentsTabActive();

    legalDashboard.searchComponent(DATA.componentAlphaName());
    legalDashboard.componentSearchButton().click();
    assertions.shouldShowComponentsTableWithRows();

    legalDashboard.componentsTableRows().first().click();

    ComponentLegalOverviewPage overviewPage = new ComponentLegalOverviewPage();
    ComponentLegalOverviewPageAssertions overviewAssertions = new ComponentLegalOverviewPageAssertions(overviewPage);
    overviewAssertions.shouldBeLoaded();
    overviewAssertions.shouldShowLicenseObligationsTile();
    overviewAssertions.shouldShowAttributionSummaryTile();
  }

  private void seed() {
    Organization orgAlpha = tempEntity.newOrganization(DATA.orgAlphaName());
    Organization orgBeta = tempEntity.newOrganization(DATA.orgBetaName());

    Tag tag = tempEntity.newTag(orgAlpha.getId(), DATA.tagName());

    for (int i = 1; i <= DATA.totalAppCount(); i++) {
      Organization parentOrg = (i <= DATA.totalAppCount() / 2) ? orgAlpha : orgBeta;
      Application app = tempEntity.newApplication(
          DATA.appPrefix() + "-" + String.format("%02d", i),
          DATA.appPrefix() + "-" + String.format("%02d", i),
          parentOrg.getId());

      tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, TemporaryEntity.uuid());

      if (i <= 3) {
        tempEntity.newApplicationTag(app.getId(), tag.getId());
      }

      seedComponent(app, "com.example",
          DATA.componentAlphaName(), "1.0." + i, DATA.licenseAlphaId());

      if (i <= 6) {
        seedComponent(app, "org.example",
            DATA.componentBetaName(), "2.0." + i, DATA.licenseBetaId());
      }

      if (i <= 3) {
        seedComponent(app, "io.example",
            DATA.componentGammaName(), "3.0." + i, DATA.licenseGammaId());
      }
    }
  }

  private void stubHdsEndpoints() {
    try {
      testCLMServer.getHdsServer()
          .respondWith(IOUtils.toString(
              getClass().getResourceAsStream("/legal/legalLicenseMetadataHdsResponse.json"),
              StandardCharsets.UTF_8))
          .atUri("/rest/license/metadata");

      testCLMServer.getHdsServer()
          .respondWith("[]")
          .atUri("/rest/legal/comment");

      testCLMServer.getHdsServer()
          .respondWith("[]")
          .atUri("/rest/legal/file");

      testCLMServer.getHdsServer()
          .respondWith("[]")
          .atUri("/rest/legal/source-link");
    }
    catch (Exception e) {
      throw new RuntimeException("Failed to stub HDS endpoints", e);
    }
  }

  private void seedComponent(
      Application application,
      String groupId,
      String artifactId,
      String version,
      String licenseId)
  {
    String hash = TemporaryEntity.uuid().replace("-", "").substring(0, 20);
    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates(groupId, artifactId, version);
    OwnerComponent component = tempEntity.newApplicationComponent(
        application.getId(),
        BuildStageType.ID,
        hash,
        componentIdentifier);
    tempEntity.newApplicationComponentLicense(component.getId(), licenseId);
  }
}
