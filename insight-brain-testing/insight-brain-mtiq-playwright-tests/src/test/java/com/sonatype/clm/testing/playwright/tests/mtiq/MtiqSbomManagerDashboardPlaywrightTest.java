/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests.mtiq;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import com.sonatype.clm.testing.playwright.categories.MtiqTest;
import com.sonatype.clm.testing.playwright.mtiq.AbstractMtiqUiTest;
import com.sonatype.clm.testing.playwright.pages.MtiqSbomManagerDashboardPage;
import com.sonatype.clm.testing.playwright.pages.MtiqSbomManagerDashboardPageAssertions;
import com.sonatype.clm.testing.playwright.pages.SbomApplicationsPage;
import com.sonatype.clm.testing.playwright.pages.SbomManagerDashboardPage;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus;
import com.sonatype.insight.brain.sbom.SbomSpecification;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import com.sonatype.insight.scan.file.SbomFormat;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@Category(MtiqTest.class)
public class MtiqSbomManagerDashboardPlaywrightTest
    extends AbstractMtiqUiTest
{
  private MtiqSbomManagerDashboardPage page;

  private MtiqSbomManagerDashboardPageAssertions assertions;

  private Organization org;

  private ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  @Before
  public void setUp() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER_SAAS);
    playwrightRefreshOrOpen(SbomManagerDashboardPage.url());
    playwrightLogin();

    thirdPartySbomMetadataDAO = lookup(ThirdPartySbomMetadataDAO.class);
    org = tempEntity.newOrganization();
    long testDate = System.currentTimeMillis();

    seedSbomWithVulnerability("Z", "low_vulnerability1", 1, new Date(testDate));
    seedSbomWithVulnerability("A", "severe_vulnerability1", 7,
        new Date(testDate - TimeUnit.SECONDS.toMillis(5)));
    seedSbomWithVulnerability("B", "severe_vulnerability2", 8,
        new Date(testDate - TimeUnit.SECONDS.toMillis(10)));
    seedSbomWithVulnerability("C", "critical_vulnerability1", 9,
        new Date(testDate - TimeUnit.SECONDS.toMillis(15)));
    seedSbomWithVulnerability("D", "critical_vulnerability2", 10,
        new Date(testDate - TimeUnit.SECONDS.toMillis(20)));

    page = new MtiqSbomManagerDashboardPage();
    assertions = new MtiqSbomManagerDashboardPageAssertions(page);
  }

  @Test
  public void testDashboard_PageHeader() {
    playwrightRefreshOrOpen(SbomManagerDashboardPage.url());
    assertions.shouldShowDashboardHeader();
  }

  @Test
  public void testDashboard_NavigateToNonSbomPages() {
    playwrightRefreshOrOpen(SbomApplicationsPage.url());
    playwrightRefreshOrOpen(SbomManagerDashboardPage.url());
    assertions.shouldShowDashboardHeader();
  }

  @Test
  public void testDashboard_SbomManagerDisabledRedirectsToLearnMorePage() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS);
    playwrightRefreshOrOpen(SbomManagerDashboardPage.url());

    page.playwrightPage().waitForURL(url -> url.contains("#/sbomManager/learnMore"));
    assertThat(page.playwrightPage()).hasURL(Pattern.compile(".*#/sbomManager/learnMore.*"));
  }

  @Test
  public void testDashboard_AllTilesRender() {
    playwrightRefreshOrOpen(SbomManagerDashboardPage.url());
    assertions.shouldShowAllTiles();
  }

  @Test
  public void testDashboard_ApplicationHistoryTile__Link() {
    playwrightRefreshOrOpen(SbomManagerDashboardPage.url());
    page.tileLink(page.applicationsHistoryTile()).click();

    page.playwrightPage().waitForURL(url -> url.contains("#/sbomManager/applications"));
    assertThat(new SbomApplicationsPage().container()).isVisible();
  }

  @Test
  public void testDashboard_VulnerabilitiesByThreatTile__Link() {
    playwrightRefreshOrOpen(SbomManagerDashboardPage.url());
    page.tileLink(page.vulnerabilitiesByThreatLevelTile()).click();

    page.playwrightPage().waitForURL(url -> url.contains("sortBy=vulnerability") && url.contains("sortDirection=desc"));
    assertThat(new SbomApplicationsPage().container()).isVisible();
  }

  @Test
  public void testDashboard_TotalSBOMsStoreTile_ConfirmTileRenderedCorrectly() {
    playwrightRefreshOrOpen(SbomManagerDashboardPage.url());
    assertThat(page.totalSbomsStoredTile()).containsText("Total SBOMs Stored");
    assertions.shouldShowTotalSbomsStored(5, 50);
  }

  @Test
  public void testDashboard_ApplicationsHistory_ConfirmTileRenderedCorrectly() {
    playwrightRefreshOrOpen(SbomManagerDashboardPage.url());
    assertThat(page.applicationsHistoryTile()).containsText("Applications History");
    assertions.shouldShowApplicationsHistory(5, 5, 5, 5);
  }

  @Test
  public void testDashboard_VulnerabilitiesByThreatTile_ConfirmTileRenderedCorrectly() {
    playwrightRefreshOrOpen(SbomManagerDashboardPage.url());
    assertThat(page.vulnerabilitiesByThreatLevelTile()).containsText("Vulnerabilities by Threat Level");
    assertions.shouldShowVulnerabilitiesTotals(5, 5, 0);
    assertions.shouldShowVulnerabilitiesTableRow("Critical", 2, 0, 2);
    assertions.shouldShowVulnerabilitiesTableRow("High", 2, 0, 2);
    assertions.shouldShowVulnerabilitiesTableRow("Medium", 0, 0, 0);
    assertions.shouldShowVulnerabilitiesTableRow("Low", 1, 0, 1);
  }

  @Test
  public void testDashboard_HighPriorityVulnerabilitiesTile_ConfirmTileRenderedCorrectly() {
    playwrightRefreshOrOpen(SbomManagerDashboardPage.url());
    assertThat(page.highPriorityVulnerabilitiesTile()).containsText("High Priority Vulnerabilities");
    // Severity 7-10 count as high-priority; low_vulnerability (1) is excluded.
    assertions.shouldShowHighPriorityVulnerabilityCount(4);
  }

  @Test
  public void testDashboard_HighPriorityVulnerabilitiesTile_ConfirmPageRedirect() {
    playwrightRefreshOrOpen(SbomManagerDashboardPage.url());
    page.highPriorityVulnerabilityLinkByName("severe_vulnerability1").click();

    page.playwrightPage().waitForURL(url -> url.contains("advancedSearch"));
  }

  @Test
  public void testDashboard_SbomReleaseStatus_ConfirmTileRenderedCorrectly() {
    playwrightRefreshOrOpen(SbomManagerDashboardPage.url());
    assertThat(page.sbomReleaseStatusTile()).containsText("SBOM Release Status");
    // NOTE: per-status count assertion is intentionally not made — the meter renders 3 stacked bars and
    // the raw count element co-exists with meter-inner text so filter-by-text is ambiguous.
    assertions.shouldShowSbomReleaseStatusEntry("Needs Attention");
    assertions.shouldShowSbomReleaseStatusEntry("Partially Annotated");
    assertions.shouldShowSbomReleaseStatusEntry("Release Ready");
  }

  @Test
  public void testDashboard_RecentlyImportedSBOMsTile_ConfirmTileRenderedCorrectly() {
    playwrightRefreshOrOpen(SbomManagerDashboardPage.url());
    assertThat(page.recentlyImportedSbomsTile()).containsText("Recently Imported SBOMs");

    assertions.shouldShowRecentlyImportedSbomsHeader("Application Name");
    assertions.shouldShowRecentlyImportedSbomsHeader("Version");
    assertions.shouldShowRecentlyImportedSbomsHeader("BOM Format");
    assertions.shouldShowRecentlyImportedSbomsHeader("Import Date");
    assertions.shouldShowRecentlyImportedSbomsHeader("Vulnerabilities");
    assertions.shouldShowRecentlyImportedSbomsFirstRowContains("test_app_Z");
  }

  @Test
  public void testDashboard_RecentlyImportedSBOMsTile_TableSorting() {
    playwrightRefreshOrOpen(SbomManagerDashboardPage.url());
    assertions.shouldShowRecentlyImportedSbomsFirstRowContains("test_app_Z");

    page.recentlyImportedSbomsTableHeader("Application Name").click();
    assertions.shouldShowRecentlyImportedSbomsFirstRowContains("test_app_A");

    page.recentlyImportedSbomsTableHeader("Application Name").click();
    assertions.shouldShowRecentlyImportedSbomsFirstRowContains("test_app_Z");
  }

  private void seedSbomWithVulnerability(
      String appIdSuffix,
      String vulnerabilityName,
      int severity,
      Date creationDate)
  {
    Application app = tempEntity.newApplication("test_app_" + appIdSuffix, "test_app_" + appIdSuffix, org.getId());
    ThirdPartyFile scannedFile = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(scannedFile);
    ThirdPartySbomMetadata metadata = tempEntity.newThirdPartySbomMetadata(
        scannedFile.getId(),
        app.getId(),
        "test-version-" + appIdSuffix,
        ThirdPartySbomMetadataStatus.ACTIVE,
        scannedFile.getId(),
        SbomSpecification.CYCLONEDX.name(),
        SbomFormat.XML.name(),
        "0.0",
        creationDate);
    metadata.setCreatedAt(creationDate);
    thirdPartySbomMetadataDAO.update(metadata);

    ThirdPartyFileCoordinate coordinate = tempEntity.newThirdPartyFileCoordinate(
        metadata.getThirdPartyFileId(), "s", "SPDX",
        "n" + appIdSuffix, "v" + appIdSuffix, "h" + appIdSuffix, "u" + appIdSuffix);

    tempEntity.newThirdPartyCoordinateSecurity(coordinate, vulnerabilityName, metadata.getId(),
        "d" + appIdSuffix, "l" + appIdSuffix, severity, "sd" + appIdSuffix, "f" + appIdSuffix);
  }
}
