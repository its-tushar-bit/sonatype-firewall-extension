/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.mtiq.sbom;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.codeborne.selenide.WebDriverRunner;
import com.sonatype.clm.testing.functional.elements.NxSortingHeader;
import com.sonatype.clm.testing.functional.elements.sbom.dashboard.ApplicationsHistoryTile;
import com.sonatype.clm.testing.functional.elements.sbom.dashboard.HighPriorityVulnerabilitiesTile;
import com.sonatype.clm.testing.functional.elements.sbom.dashboard.HighPriorityVulnerabilitiesTile.VulnerabilityList;
import com.sonatype.clm.testing.functional.elements.sbom.dashboard.RecentlyImportedSBOMsTile;
import com.sonatype.clm.testing.functional.elements.sbom.dashboard.RecentlyImportedSBOMsTile.SbomTable;
import com.sonatype.clm.testing.functional.elements.sbom.dashboard.RecentlyImportedSBOMsTile.TableRow;
import com.sonatype.clm.testing.functional.elements.sbom.dashboard.SbomReleaseStatusTile;
import com.sonatype.clm.testing.functional.elements.sbom.dashboard.TotalSBOMsStoredTile;
import com.sonatype.clm.testing.functional.elements.sbom.dashboard.VulnerabilitiesThreatLevelTile;
import com.sonatype.clm.testing.functional.elements.sbom.dashboard.VulnerabilitiesThreatLevelTile.TileLabels;
import com.sonatype.clm.testing.functional.elements.sbom.dashboard.VulnerabilitiesThreatLevelTile.TileTable;
import com.sonatype.clm.testing.functional.mtiq.AbstractMtiqFunctionalTest;
import com.sonatype.clm.testing.functional.pages.AdvancedSearchPage;
import com.sonatype.clm.testing.functional.pages.IndexPage;
import com.sonatype.clm.testing.functional.pages.SourceControlEditorPage;
import com.sonatype.clm.testing.functional.pages.sbom.LearnMoreSbomManagerPage;
import com.sonatype.clm.testing.functional.pages.sbom.SbomApplicationsPage;
import com.sonatype.clm.testing.functional.pages.sbom.SbomManagerDashboardPage;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyDependencyType;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.sbom.SbomSpecification;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import com.sonatype.insight.scan.file.SbomFormat;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.ACTIVE;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static org.assertj.core.api.Assertions.assertThat;

public class SbomManagerDashboardPageTest
    extends AbstractMtiqFunctionalTest
{
  private final SbomManagerDashboardPage sbomManagerDashboardPage = new SbomManagerDashboardPage();

  private Organization org;

  private ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  long testDate = System.currentTimeMillis();

  private final SimpleDateFormat testDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  @Before
  public void before() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER_SAAS);
    refreshOrOpen(IndexPage.url());
    loginAsAdmin();

    thirdPartySbomMetadataDAO = lookup(ThirdPartySbomMetadataDAO.class);
    org = tempEntity.newOrganization("test-organization");

    generateMockDataEntry("Z", "low_vulnerability1", 1, new Date(testDate));
    generateMockDataEntry("A", "severe_vulnerability1", 7, new Date(testDate - TimeUnit.SECONDS.toMillis(5)));
    generateMockDataEntry("B", "severe_vulnerability2", 8, new Date(testDate - TimeUnit.SECONDS.toMillis(10)));
    generateMockDataEntry("C", "critical_vulnerability1", 9, new Date(testDate - TimeUnit.SECONDS.toMillis(15)));
    generateMockDataEntry("D", "critical_vulnerability2", 10, new Date(testDate - TimeUnit.SECONDS.toMillis(20)));
  }

  @Test
  public void testDashboard_PageHeader() {
    refreshOrOpen(SbomManagerDashboardPage.url());

    sbomManagerDashboardPage.title()
        .shouldBe(visible)
        .shouldHave(text("SBOM Manager Dashboard"));
  }

  @Test
  public void testDashboard_NavigateToNonSbomPages() {
    Application application = tempEntity.newApplicationWithParent("test-app");
    refreshOrOpen(SourceControlEditorPage.url(OwnerType.APPLICATION.toString(), application.getPublicId()));
    waitUntilUrl(SbomManagerDashboardPage.url());

    sbomManagerDashboardPage.title()
        .shouldBe(visible)
        .shouldHave(text("SBOM Manager Dashboard"));
  }

  @Test
  public void testDashboard_SbomManagerDisabledRedirectsToLearnMorePage() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS);
    refresh();

    refreshOrOpen(SbomManagerDashboardPage.url());
    waitUntilUrl(LearnMoreSbomManagerPage.url());

    LearnMoreSbomManagerPage learnMoreSbomManagerPage = new LearnMoreSbomManagerPage();
    learnMoreSbomManagerPage.infoAlert()
        .shouldHave(text("SBOM Manager is currently not enabled for your " +
            "organization. Learn more about SBOM Manager."));
  }

  @Test
  public void testDashboard_AllTiles_ConfirmTooltipFunctionality() {
    refreshOrOpen(SbomManagerDashboardPage.url());
    TotalSBOMsStoredTile totalStoreDashboardTile =
        SbomManagerDashboardPage.totalSBOMsStoredTile();
    ApplicationsHistoryTile applicationsHistoryTile =
        SbomManagerDashboardPage.applicationsHistoryTile();
    HighPriorityVulnerabilitiesTile highPriorityVulnerabilitiesTile =
        SbomManagerDashboardPage.highPriorityVulnerabilitiesTile();
    SbomReleaseStatusTile sbomReleaseStatusTile =
        SbomManagerDashboardPage.sbomReleaseStatusTile();
    RecentlyImportedSBOMsTile recentlyImportedSBOMsTile = SbomManagerDashboardPage.recentlyImportedSBOMsTile();
    NxSortingHeader applicationNameTableHeader = recentlyImportedSBOMsTile.sbomTable().applicationNameTableHeader();

    ElementsCollection totalStoreDashboardTileToolTips = totalStoreDashboardTile.allInfoIcons();
    totalStoreDashboardTileToolTips.get(0).hover();
    sbomManagerDashboardPage.toolTip()
        .shouldBe(visible)
        .shouldHave(text("Each application version counts toward the total SBOMs Analyzed."));
    totalStoreDashboardTileToolTips.get(1).hover();
    sbomManagerDashboardPage.toolTip()
        .shouldBe(visible)
        .shouldHave(text("Shows how many SBOMs you have analyzed within the limits of your purchased license."));
    applicationsHistoryTile.infoIcon().hover();
    sbomManagerDashboardPage.toolTip()
        .shouldBe(visible)
        .shouldHave(text("Track the number of applications with updated SBOMs."));
    highPriorityVulnerabilitiesTile.infoIcon().hover();
    sbomManagerDashboardPage.toolTip()
        .shouldBe(visible)
        .shouldHave(text("High severity vulnerabilities found in the most recent SBOM scans or import."));
    sbomReleaseStatusTile.infoIcon().hover();
    sbomManagerDashboardPage.toolTip()
        .shouldBe(visible)
        .shouldHave(text("Shows breakdown of SBOMs based on the annotations completed."));
    applicationNameTableHeader.hover();
    sbomManagerDashboardPage.toolTip()
        .shouldBe(visible)
        .shouldHave(text("Application Name unsorted"));
    applicationNameTableHeader.click();
    sbomManagerDashboardPage.toolTip()
        .shouldBe(visible)
        .shouldHave(text("Application Name ascending"));
    applicationNameTableHeader.click();
    sbomManagerDashboardPage.toolTip()
        .shouldBe(visible)
        .shouldHave(text("Application Name descending"));
  }

  @Test
  public void testDashboard_ApplicationHistoryTile__Link() {
    refreshOrOpen(SbomManagerDashboardPage.url());
    ApplicationsHistoryTile applicationsHistoryTile =
        SbomManagerDashboardPage.applicationsHistoryTile();

    applicationsHistoryTile.link().click();

    SbomApplicationsPage sbomApplicationsPage = new SbomApplicationsPage();
    sbomApplicationsPage.container().shouldBe(visible);

    String currentUrl = WebDriverRunner.getWebDriver().getCurrentUrl();
    assertThat(currentUrl).doesNotContain("sortBy");
    assertThat(currentUrl).doesNotContain("sortDirection");
  }

  @Test
  public void testDashboard_VulnerabilitiesByThreatTile__Link() {
    refreshOrOpen(SbomManagerDashboardPage.url());
    VulnerabilitiesThreatLevelTile vulnerabilitiesThreatLevelTile =
        SbomManagerDashboardPage.vulnerabilitiesThreatLevelTile();

    vulnerabilitiesThreatLevelTile.link().click();

    SbomApplicationsPage sbomApplicationsPage = new SbomApplicationsPage();
    sbomApplicationsPage.container().shouldBe(visible);

    String currentUrl = WebDriverRunner.getWebDriver().getCurrentUrl();
    assertThat(currentUrl).contains("sortBy=vulnerability");
    assertThat(currentUrl).contains("sortDirection=desc");
  }

  @Test
  public void testDashboard_TotalSBOMsStoreTile_ConfirmTileRenderedCorrectly() {
    refreshOrOpen(SbomManagerDashboardPage.url());
    TotalSBOMsStoredTile totalSBOMsStoredTile = SbomManagerDashboardPage.totalSBOMsStoredTile();
    totalSBOMsStoredTile.header().shouldHave(text("Total SBOMs Stored"));
    totalSBOMsStoredTile.totalSBOMsStored().shouldHave(text("5 (all time)"));
    totalSBOMsStoredTile.sbomProgressBarLabel().shouldHave(text("SBOM License Usage"));
    totalSBOMsStoredTile.sbomProgressBar().shouldBe(visible);
    assertThat(totalSBOMsStoredTile.sbomProgressBar().getAttribute("value")).isEqualTo("10");
    totalSBOMsStoredTile.sbomsAddedMetricLabel().shouldHave(text("5 SBOMs added"));
    totalSBOMsStoredTile.sbomThresholdLabel().shouldHave(text("50 Threshold"));
  }

  @Test
  public void testDashboard_ApplicationsHistory_ConfirmTileRenderedCorrectly() {
    refreshOrOpen(SbomManagerDashboardPage.url());
    ApplicationsHistoryTile applicationsHistoryTile = SbomManagerDashboardPage.applicationsHistoryTile();
    applicationsHistoryTile.header().shouldHave(text("Applications History"));
    applicationsHistoryTile.applicationsList().listLabel(0).shouldHave(text("Total scanned applications (all time)"));
    applicationsHistoryTile.applicationsList().listValue(0).shouldHave(text("5"));
    applicationsHistoryTile.applicationsList().listLabel(1).shouldHave(text("Applications updated last year"));
    applicationsHistoryTile.applicationsList().listValue(1).shouldHave(text("5"));
    applicationsHistoryTile.applicationsList().listLabel(2).shouldHave(text("Applications updated last month"));
    applicationsHistoryTile.applicationsList().listValue(2).shouldHave(text("5"));
    applicationsHistoryTile.applicationsList().listLabel(3).shouldHave(text("Applications updated last week"));
    applicationsHistoryTile.applicationsList().listValue(3).shouldHave(text("5"));
  }

  @Test
  public void testDashboard_VulnerabilitiesByThreatTile_ConfirmTileRenderedCorrectly() {
    refreshOrOpen(SbomManagerDashboardPage.url());
    VulnerabilitiesThreatLevelTile vulnerabilitiesThreatLevelTile =
        SbomManagerDashboardPage.vulnerabilitiesThreatLevelTile();
    TileLabels tileLabels = vulnerabilitiesThreatLevelTile.tileLabels();
    SelenideElement pieChart = vulnerabilitiesThreatLevelTile.tilePieChart();
    TileTable tileTable = vulnerabilitiesThreatLevelTile.tileTable();
    vulnerabilitiesThreatLevelTile.header().shouldHave(text("Vulnerabilities by Threat Level"));
    tileLabels.label(0).shouldHave(text("Total: 5"));
    tileLabels.label(1).shouldHave(text("Unannotated: 5"));
    tileLabels.label(2).shouldHave(text("Annotated: 0"));
    pieChart.shouldBe(visible);
    tileTable.tableHeaders().header(0).shouldHave(text("Threat Level"));
    tileTable.tableHeaders().header(1).shouldHave(text("Unannotated"));
    tileTable.tableHeaders().header(2).shouldHave(text("Annotated"));
    tileTable.tableHeaders().header(3).shouldHave(text("Total"));
    tileTable.tableRow(0).shouldHaveCorrectThreatLevelAndMetrics("Critical", 2, 0, 2);
    tileTable.tableRow(1).shouldHaveCorrectThreatLevelAndMetrics("High", 2, 0, 2);
    tileTable.tableRow(2).shouldHaveCorrectThreatLevelAndMetrics("Medium", 0, 0, 0);
    tileTable.tableRow(3).shouldHaveCorrectThreatLevelAndMetrics("Low", 1, 0, 1);
  }

  @Test
  public void testDashboard_HighPriorityVulnerabilitiesTile_ConfirmTileRenderedCorrectly() {
    refreshOrOpen(SbomManagerDashboardPage.url());
    HighPriorityVulnerabilitiesTile highPriorityVulnerabilitiesTile =
        SbomManagerDashboardPage.highPriorityVulnerabilitiesTile();
    highPriorityVulnerabilitiesTile.header().shouldHave(text("High Priority Vulnerabilities"));
    VulnerabilityList vulnerabilityList = highPriorityVulnerabilitiesTile.vulnerabilityList();
    vulnerabilityList.listItem(0).shouldHaveCorrectSeverityAndName(7, "severe_vulnerability1", "a few seconds ago");
    vulnerabilityList.listItem(1).shouldHaveCorrectSeverityAndName(8, "severe_vulnerability2", "a few seconds ago");
    vulnerabilityList.listItem(2).shouldHaveCorrectSeverityAndName(9, "critical_vulnerability1", "a few seconds ago");
    vulnerabilityList.listItem(3).shouldHaveCorrectSeverityAndName(10, "critical_vulnerability2", "a few seconds ago");
  }

  @Test
  public void testDashboard_HighPriorityVulnerabilitiesTile_ConfirmPageRedirect() {
    refreshOrOpen(SbomManagerDashboardPage.url());
    HighPriorityVulnerabilitiesTile highPriorityVulnerabilitiesTile =
        SbomManagerDashboardPage.highPriorityVulnerabilitiesTile();
    highPriorityVulnerabilitiesTile.header().shouldHave(text("High Priority Vulnerabilities"));
    VulnerabilityList vulnerabilityList = highPriorityVulnerabilitiesTile.vulnerabilityList();
    vulnerabilityList.listItem(0).vulnerabilityNameLink().shouldHave(text("severe_vulnerability1"));
    vulnerabilityList.listItem(0).vulnerabilityNameLink().click();
    final AdvancedSearchPage advancedSearchPage = new AdvancedSearchPage();
    advancedSearchPage.advancedSearchPageTitle().shouldBe(visible).shouldHave(text("Advanced Search"));
    advancedSearchPage.searchInput().setValue("severe_vulnerability1");
  }

  @Test
  public void testDashboard_SbomReleaseStatus_ConfirmTileRenderedCorrectly() {
    refreshOrOpen(SbomManagerDashboardPage.url());
    SbomReleaseStatusTile sbomReleaseStatusTile = SbomManagerDashboardPage.sbomReleaseStatusTile();
    sbomReleaseStatusTile.header().shouldHave(text("SBOM Release Status (all time)"));
    sbomReleaseStatusTile.tileLabels().get(0).shouldHave(text("Needs Attention"));
    sbomReleaseStatusTile.tileMeterBars().get(0).shouldBe(visible);
    assertThat(sbomReleaseStatusTile.tileMeterBars().get(0).getAttribute("value")).isEqualTo("4");
    sbomReleaseStatusTile.tileLabelValues().get(0).shouldHave(text("4"));
    sbomReleaseStatusTile.tileLabels().get(1).shouldHave(text("Partially Annotated"));
    sbomReleaseStatusTile.tileMeterBars().get(1).shouldBe(visible);
    assertThat(sbomReleaseStatusTile.tileMeterBars().get(1).getAttribute("value")).isEqualTo("0");
    sbomReleaseStatusTile.tileLabelValues().get(1).shouldHave(text("0"));
    sbomReleaseStatusTile.tileLabels().get(2).shouldHave(text("Release Ready"));
    sbomReleaseStatusTile.tileMeterBars().get(2).shouldBe(visible);
    assertThat(sbomReleaseStatusTile.tileMeterBars().get(2).getAttribute("value")).isEqualTo("2");
    sbomReleaseStatusTile.tileLabelValues().get(2).shouldHave(text("2"));
  }

  @Test
  public void testDashboard_RecentlyImportedSBOMsTile_ConfirmTileRenderedCorrectly() {
    refreshOrOpen(SbomManagerDashboardPage.url());
    RecentlyImportedSBOMsTile recentlyImportedSBOMsTile = SbomManagerDashboardPage.recentlyImportedSBOMsTile();
    recentlyImportedSBOMsTile.header().shouldHave(text("Recently Imported SBOMs"));
    SbomTable sbomTable = recentlyImportedSBOMsTile.sbomTable();

    sbomTable.tableHeader(0).shouldHave(text("Application Name"));
    sbomTable.tableHeader(1).shouldHave(text("Version"));
    sbomTable.tableHeader(2).shouldHave(text("BOM Format"));
    sbomTable.tableHeader(3).shouldHave(text("Import Date"));
    sbomTable.tableHeader(4).shouldHave(text("Vulnerabilities"));
    sbomTable.tableRow(0).applicationName().shouldHave(text("test_app_Z"));
    sbomTable.tableRow(0).sbomVersion().shouldHave(text("test-version-Z"));
    sbomTable.tableRow(0).bomFormat().shouldHave(text("CYCLONEDX"));
    sbomTable.tableRow(0).importDate().shouldHave(text(testDateFormat.format(testDate)));
    sbomTable.tableRow(0).threatCounters().criticalThreatCounter().shouldHave(text("0"));
    sbomTable.tableRow(0).threatCounters().severeThreatCounter().shouldHave(text("0"));
    sbomTable.tableRow(0).threatCounters().moderateThreatCounter().shouldHave(text("0"));
    sbomTable.tableRow(0).threatCounters().lowThreatCounter().shouldHave(text("1"));
  }

  @Test
  public void testDashboard_RecentlyImportedSBOMsTile_TableSorting() {
    refreshOrOpen(SbomManagerDashboardPage.url());
    RecentlyImportedSBOMsTile recentlyImportedSBOMsTile = SbomManagerDashboardPage.recentlyImportedSBOMsTile();
    NxSortingHeader applicationNameTableHeader = recentlyImportedSBOMsTile.sbomTable().applicationNameTableHeader();
    TableRow recentlyImportedSBOMsTileTableFirstRow = recentlyImportedSBOMsTile.sbomTable().firstRow();
    recentlyImportedSBOMsTileTableFirstRow.applicationName()
        .shouldHave(text("test_app_Z"));
    applicationNameTableHeader.click();
    applicationNameTableHeader.sortArrows().shouldBeUp();
    recentlyImportedSBOMsTileTableFirstRow.applicationName()
        .shouldHave(text("test_app_A"));
    applicationNameTableHeader.click();
    applicationNameTableHeader.sortArrows().shouldBeDown();
    recentlyImportedSBOMsTileTableFirstRow.applicationName()
        .shouldHave(text("test_app_Z"));
  }

  public void generateMockDataEntry(
      String appId,
      String vulnerabilityName,
      int vulnerabilitySeverity,
      Date creationDate)
  {
    Application app = tempEntity.newApplication("test_app_" + appId, "test_app_" + appId, org.getId());
    ThirdPartyScan thirdPartyScan = tempEntity.newThirdPartyScan();
    ThirdPartySbomMetadata sbomMetadata = tempEntity.newThirdPartySbomMetadata(
        thirdPartyScan.getThirdPartyFileId(),
        app.getId(),
        "test-version-" + appId,
        ACTIVE,
        thirdPartyScan.getScanId(),
        SbomSpecification.CYCLONEDX.name(),
        SbomFormat.XML.name(),
        "0.0");
    sbomMetadata.setCreatedAt(creationDate);
    thirdPartySbomMetadataDAO.update(sbomMetadata);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate = tempEntity.newThirdPartyFileCoordinate(
        thirdPartyScan.getThirdPartyFileId(),
        "s", "SPDX", "n" + appId, "v" + appId, "h" + appId, "u" + appId,
        ThirdPartyDependencyType.DIRECT);
    tempEntity.newThirdPartyCoordinateSecurity(
        thirdPartyFileCoordinate,
        vulnerabilityName,
        sbomMetadata.getId(),
        "d" + appId, "l" + appId,
        vulnerabilitySeverity,
        "sd" + appId,
        "f" + appId);
  }
}
