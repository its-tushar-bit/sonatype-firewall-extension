/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.SbomManagerDashboardPage;
import com.sonatype.clm.testing.playwright.pages.SbomManagerDashboardRegressionPage;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.sbom.SbomSpecification;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.CvssV3Severity;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.scan.file.SbomFormat;

import com.microsoft.playwright.assertions.LocatorAssertions;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.ACTIVE;
import static com.sonatype.insight.brain.sbom.SbomTestHelper.mockOriginalSbom;

/**
 * Regression tests for the SBOM Manager Dashboard ({@code #/sbomManager/dashboard}).
 */
public class SbomManagerDashboardPlaywrightTest
    extends AbstractIqUiTest
{
  private static final LocatorAssertions.IsVisibleOptions VISIBLE_OPTS =
      new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS);

  private static final String HEADING_TEXT = "SBOM Manager Dashboard";

  private static final String CPP_ALERT_TEXT = "SBOM Manager now supports C/C++.";

  private static final String FEATURE_DISABLED_ERROR = "The SBOM Manager license feature is not enabled.";

  private static final String SIMPLE_BOM_XML = "simple-bom.xml";

  private static final String SBOM_SPECIFICATION = SbomSpecification.CYCLONEDX.name();

  private static final String SBOM_FILE_FORMAT = SbomFormat.XML.name();

  private static final String SBOM_SPEC_VERSION = "0.0";

  @AfterEach
  public void restoreSbomManagerFeature() {
    setFeatures(LicensedFeature.SBOM_MANAGER);
  }

  @BeforeEach
  public void setupLicenseAndNavigate() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER);
    setFeatures(LicensedFeature.SBOM_MANAGER);
    playwrightRefreshOrOpen(SbomManagerDashboardPage.url());
    playwrightLogin();
  }

  @Test
  @Tag("regression")
  public void testSbomManagerDashboard_pageRendersWithAllTiles() {
    seedSingleSbomWithCriticalVuln();
    playwrightRefreshOrOpen(SbomManagerDashboardRegressionPage.url());

    SbomManagerDashboardRegressionPage dashboard = new SbomManagerDashboardRegressionPage();

    assertThat(dashboard.container()).isVisible(
        VISIBLE_OPTS);
    assertThat(dashboard.heading()).containsText(HEADING_TEXT);
    assertThat(dashboard.tilesContainer()).isVisible();

    assertThat(dashboard.totalSbomsStoredTile()).isVisible();
    assertThat(dashboard.totalSbomsStoredCount()).isVisible();

    assertThat(dashboard.applicationsHistoryTile()).isVisible();
    assertThat(dashboard.applicationsHistoryList()).isVisible();

    assertThat(dashboard.highPriorityVulnerabilitiesTile()).isVisible();
    assertThat(dashboard.highPriorityVulnerabilitiesList()).isVisible();

    assertThat(dashboard.vulnerabilitiesByThreatLevelTile()).isVisible();
    assertThat(dashboard.vulnerabilitiesByThreatLevelList()).isVisible();

    assertThat(dashboard.sbomReleaseStatusTile()).isVisible();
    assertThat(dashboard.sbomReleaseStatusMeterBars()).isVisible();

    assertThat(dashboard.recentlyImportedSbomsTile()).isVisible();
    assertThat(dashboard.recentlyImportedSbomsTableRows().first()).isVisible(
        VISIBLE_OPTS);
  }

  @Test
  @Tag("regression")
  public void testSbomManagerDashboard_cppSupportAlertShownWhenCpeMatchingEnabled() {
    setLicensedProducts(
        ProductLicenseDetails.PRODUCT_SBOM_MANAGER,
        ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION);
    setFeatures(LicensedFeature.SBOM_MANAGER, LicensedFeature.CPE_MATCHING);
    playwrightRefreshOrOpen(SbomManagerDashboardPage.url());

    SbomManagerDashboardPage dashboard = new SbomManagerDashboardPage();

    assertThat(dashboard.container()).isVisible(
        VISIBLE_OPTS);
    assertThat(dashboard.cppSupportAlert()).isVisible();
    assertThat(dashboard.cppSupportAlert()).containsText(CPP_ALERT_TEXT);
    assertThat(dashboard.cppSupportAlertDocLink()).isVisible();
    assertThat(dashboard.cppSupportAlertCloseButton()).isVisible();
  }

  @Test
  @Tag("regression")
  public void testSbomManagerDashboard_cppAlertHiddenForSbomManagerOnlyLicense() {
    // @Before sets only PRODUCT_SBOM_MANAGER; CPE_MATCHING is added to prove
    // isSbomManagerOnlyLicense suppresses the alert regardless of CPE support.
    setFeatures(LicensedFeature.SBOM_MANAGER, LicensedFeature.CPE_MATCHING);
    playwrightRefreshOrOpen(SbomManagerDashboardRegressionPage.url());

    SbomManagerDashboardRegressionPage dashboard = new SbomManagerDashboardRegressionPage();

    assertThat(dashboard.container()).isVisible(
        VISIBLE_OPTS);
    assertThat(dashboard.cppSupportAlert()).isHidden();
  }

  @Test
  @Tag("regression")
  public void testSbomManagerDashboard_cppAlertHiddenWhenCpeMatchingNotSupported() {
    setLicensedProducts(
        ProductLicenseDetails.PRODUCT_SBOM_MANAGER,
        ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION);
    setFeatures(LicensedFeature.SBOM_MANAGER);
    playwrightRefreshOrOpen(SbomManagerDashboardRegressionPage.url());

    SbomManagerDashboardRegressionPage dashboard = new SbomManagerDashboardRegressionPage();

    assertThat(dashboard.container()).isVisible(
        VISIBLE_OPTS);
    assertThat(dashboard.cppSupportAlert()).isHidden();
  }

  @Test
  @Tag("regression")
  public void testSbomManagerDashboard_cppAlertDismissedPersistsViaLocalStorage() {
    setLicensedProducts(
        ProductLicenseDetails.PRODUCT_SBOM_MANAGER,
        ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION);
    setFeatures(LicensedFeature.SBOM_MANAGER, LicensedFeature.CPE_MATCHING);
    playwrightRefreshOrOpen(SbomManagerDashboardPage.url());

    SbomManagerDashboardPage dashboard = new SbomManagerDashboardPage();

    assertThat(dashboard.container()).isVisible(
        VISIBLE_OPTS);
    assertThat(dashboard.cppSupportAlert()).isVisible();

    dashboard.cppSupportAlertCloseButton().click();
    assertThat(dashboard.cppSupportAlert()).isHidden();

    playwrightRefreshOrOpen(SbomManagerDashboardPage.url());
    assertThat(dashboard.container()).isVisible(
        VISIBLE_OPTS);
    assertThat(dashboard.cppSupportAlert()).isHidden();
  }

  @Test
  @Tag("regression")
  public void testSbomManagerDashboard_errorShownWhenSbomManagerFeatureNotEnabled() {
    // Override @Before: omit SBOM_MANAGER so the feature-disabled error state is shown.
    setFeatures();
    playwrightRefreshOrOpen(SbomManagerDashboardRegressionPage.url());

    SbomManagerDashboardRegressionPage dashboard = new SbomManagerDashboardRegressionPage();

    assertThat(dashboard.container()).isVisible(
        VISIBLE_OPTS);
    assertThat(dashboard.loadWrapperError()).isVisible();
    assertThat(dashboard.loadWrapperError()).containsText(FEATURE_DISABLED_ERROR);
    assertThat(dashboard.tilesContainer()).isHidden();
  }

  @Test
  @Tag("regression")
  public void testSbomManagerDashboard_tilesPopulatedWithSbomData() {
    // Seed data after @Before's navigation; reload is needed for tiles to reflect it.
    seedSingleSbomWithCriticalVuln();
    playwrightRefreshOrOpen(SbomManagerDashboardRegressionPage.url());

    SbomManagerDashboardRegressionPage dashboard = new SbomManagerDashboardRegressionPage();

    assertThat(dashboard.container()).isVisible(
        VISIBLE_OPTS);
    assertThat(dashboard.totalSbomsStoredCount()).not().hasText("0");
    assertThat(dashboard.applicationsHistoryList()).isVisible();
    assertThat(dashboard.highPriorityVulnerabilitiesList()).isVisible();
    assertThat(dashboard.vulnerabilitiesByThreatLevelList()).isVisible();
    assertThat(dashboard.sbomReleaseStatusMeterBars()).isVisible();
    assertThat(dashboard.recentlyImportedSbomsTableRows().first()).isVisible(VISIBLE_OPTS);
  }

  private void seedSingleSbomWithCriticalVuln() {
    String id = TemporaryEntity.uuid();

    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());

    InsightWork insightWork = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    Path zippedBom;
    try {
      zippedBom = mockOriginalSbom(
          SbomManagerDashboardPlaywrightTest.class,
          SIMPLE_BOM_XML,
          insightWork.getSbomDir(app.getId()).toPath());
    }
    catch (Exception e) {
      throw new RuntimeException("Failed to mock SBOM", e);
    }

    ThirdPartyFile scannedFile = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(scannedFile);
    ThirdPartySbomMetadata metadata = tempEntity.newThirdPartySbomMetadata(
        scannedFile.getId(),
        app.getId(),
        "v1.0.0-" + id,
        ACTIVE,
        zippedBom.getFileName().toString(),
        SBOM_SPECIFICATION,
        SBOM_FILE_FORMAT,
        SBOM_SPEC_VERSION,
        Date.from(Instant.now()));

    ComponentIdentifier componentId = ComponentIdentifier.createNpmCoordinates("pkg-" + id, "1.0.0");
    PackageUrlIdentifier purl = PackageUrlIdentifier.fromComponentIdentifier(componentId);
    ThirdPartyFileCoordinate coordinate = tempEntity.newThirdPartyFileCoordinate(
        metadata.getThirdPartyFileId(),
        "src-" + id,
        purl.getFormat(),
        purl.getName(),
        purl.getVersion(),
        id.substring(0, 20), // hash column is varchar(20)
        purl.getPackageUrl());

    tempEntity.newThirdPartyCoordinateSecurity(coordinate,
        "CVE-DASHBOARD-" + id, metadata.getId(),
        "Vulnerability-" + id, "http://advisory/" + id,
        CvssV3Severity.CRITICAL.getStartScoreRange(),
        CvssV3Severity.CRITICAL.getDisplayName(), "Patch-" + id);
  }
}
