/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.SbomApplicationsRegressionAssertions;
import com.sonatype.clm.testing.playwright.pages.SbomApplicationsRegressionPage;
import com.sonatype.clm.testing.playwright.pages.SbomManagerBomRegressionPage;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.sbom.SbomSpecification;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.scan.file.SbomFormat;

import com.microsoft.playwright.Download;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Route;
import com.microsoft.playwright.assertions.LocatorAssertions;

import org.assertj.core.api.Assertions;

import org.apache.commons.io.FileUtils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.ACTIVE;
import static com.sonatype.insight.brain.sbom.SbomTestHelper.mockOriginalSbom;

/**
 * Regression tests for the SBOM Manager BOM page
 * ({@code #/sbomManager/management/view/application/{id}/bom/{v}/overview}).
 */
public class SbomManagerBomPlaywrightTest
    extends AbstractIqUiTest
{
  private static final LocatorAssertions.IsVisibleOptions VISIBLE_OPTS =
      new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS);

  private static final String SBOM_VERSION_ID = "bom-regression-v1";

  private static final String SBOM_VERSION_ID_V2 = "bom-regression-v2";

  private static final String SBOM_VERSION_ID_INVALID = "bom-regression-invalid";

  private static final String COMPONENT_SEARCH_QUERY = "alpha";

  private static final String ORIGINAL_BOM_VIEWER_TITLE = "Original Bill of Material Data";

  private static final String INVALID_SBOM_ALERT_HEADING = "Invalid SBOM Detected";

  private static final String EXPORT_BUTTON_VALID_LABEL = "Export SBOM";

  private static final String EXPORT_BUTTON_INVALID_LABEL = "Export Original SBOM";

  private static final String ADDITIONAL_EXPORT_OPTIONS_TITLE = "Additional Export Options";

  private static final String POLICY_VIOLATION_SUMMARY_HEADING = "Policy Violation Summary";

  private static final String SUMMARY_API_PATTERN = "**/rest/application/services/summary/**";

  private static final String CYCLONEDX_SPEC = SbomSpecification.CYCLONEDX.name();

  private static final String XML_FORMAT = SbomFormat.XML.name();

  private static final String JSON_FORMAT = SbomFormat.JSON.name();

  private static final String SBOM_VERSION_ID_JSON = "bom-regression-json-v1";

  private static final String SBOM_VERSION_ID_ALIASES = "bom-regression-aliases-v1";

  private static final String SIMPLE_BOM_JSON = "simple-bom.json";

  private static final String BOM_SPEC_VERSION = "0.0";

  private static final String SIMPLE_BOM_XML = "simple-bom.xml";

  private static final String SIMPLE_BOM_ALIASES_XML = "simple-bom-aliases.xml";

  private static final String REPORT_RESOURCE_DIR = "/sbom/ComponentDetailsTest/reportWithComponentRef";

  private Application seedApp;

  @BeforeEach
  public void seedAndOpenBomPageAsAdmin() {
    SystemConfigurationPropertyFeature.SBOM_POLICIES.setEnabled(false);
    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER);
    setFeatures(LicensedFeature.SBOM_MANAGER);
    seedBomPage();

    playwrightRefreshOrOpen(SbomApplicationsRegressionPage.url());
    playwrightLogin();
    new SbomApplicationsRegressionAssertions(new SbomApplicationsRegressionPage()).shouldBeLoaded();

    playwrightRefreshOrOpen(SbomManagerBomRegressionPage.url(seedApp.getPublicId(), SBOM_VERSION_ID));
    assertThat(new SbomManagerBomRegressionPage().reportTab())
        .isVisible(VISIBLE_OPTS);
  }

  @Test
  @Tag("regression")
  public void testBomPageRenders_tabsComponentSearchAndOriginalBomViewer() {
    SbomManagerBomRegressionPage page = new SbomManagerBomRegressionPage();

    assertThat(page.selectedReportTab()).isVisible();
    assertThat(page.originalBomTab()).isVisible();
    assertThat(page.componentTable()).isVisible();

    page.searchComponents(COMPONENT_SEARCH_QUERY);
    assertThat(page.componentTableBodyRowsContaining(COMPONENT_SEARCH_QUERY))
        .hasCount(1, new LocatorAssertions.HasCountOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));

    page.clickOriginalBomTab();
    assertThat(page.originalBomViewerTitle())
        .containsText(ORIGINAL_BOM_VIEWER_TITLE,
            new LocatorAssertions.ContainsTextOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
  }

  @Test
  @Tag("regression")
  public void testOriginalBom_searchProducesMatchCountForHitsAndZeroForNoMatch() {
    SbomManagerBomRegressionPage page = new SbomManagerBomRegressionPage();
    page.clickOriginalBomTab();
    assertThat(page.originalBomViewerTitle()).containsText(ORIGINAL_BOM_VIEWER_TITLE);

    // "components" is a top-level key in every spec-conformant CycloneDX BOM.
    page.originalBomSearchInput().fill("components");
    assertThat(page.originalBomSearchResultsCount())
        .hasText(Pattern.compile("[1-9][0-9]* results? found"));

    page.originalBomSearchInput().fill("zzz-nonsense-" + TemporaryEntity.uuid());
    assertThat(page.originalBomSearchResultsCount()).hasText("0 results found");
  }

  @Test
  @Tag("regression")
  public void testVersionDropdown_navigatesToSelectedVersion() {
    SbomManagerBomRegressionPage page = new SbomManagerBomRegressionPage();

    assertThat(page.versionDropdown())
        .isVisible(VISIBLE_OPTS);

    page.clickVersionDropdownToggle();
    assertThat(page.versionDropdownItem(SBOM_VERSION_ID_V2))
        .isVisible(VISIBLE_OPTS);

    page.clickVersionDropdownItem(SBOM_VERSION_ID_V2);
    playwrightWaitUntilUrlContains("/bom/" + SBOM_VERSION_ID_V2);
  }

  @Test
  @Tag("regression")
  public void testInvalidSbomAlert_dismissRevealsPersistentIndicator() {
    SbomManagerBomRegressionPage page = new SbomManagerBomRegressionPage();

    playwrightRefreshOrOpen(SbomManagerBomRegressionPage.url(seedApp.getPublicId(), SBOM_VERSION_ID_INVALID));

    assertThat(page.invalidSbomAlert())
        .isVisible(VISIBLE_OPTS);
    assertThat(page.invalidSbomAlert()).containsText(INVALID_SBOM_ALERT_HEADING);

    page.clickInvalidSbomAlertClose();

    assertThat(page.invalidSbomAlert()).isHidden();
    assertThat(page.invalidSbomIndicator())
        .isVisible(VISIBLE_OPTS);
  }

  @Test
  @Tag("regression")
  public void testExportButton_dropdownItemsModalAndInvalidBomDisabled() {
    SbomManagerBomRegressionPage page = new SbomManagerBomRegressionPage();

    assertThat(page.exportButton())
        .isVisible(VISIBLE_OPTS);
    assertThat(page.exportButtonPrimary()).containsText(EXPORT_BUTTON_VALID_LABEL);

    page.clickExportButtonDropdownToggle();
    assertThat(page.exportDropdownExportOriginalSbomButton())
        .isVisible(VISIBLE_OPTS);
    assertThat(page.exportDropdownAdditionalExportOptions()).isVisible();
    assertThat(page.exportDropdownPdfLink()).isVisible();

    page.clickAdditionalExportOptions();
    assertThat(page.additionalExportOptionsModal())
        .isVisible(VISIBLE_OPTS);
    assertThat(page.additionalExportOptionsModalTitle())
        .containsText(ADDITIONAL_EXPORT_OPTIONS_TITLE);
    assertThat(page.additionalExportOptionsSpecificationFieldset()).isVisible();
    assertThat(page.additionalExportOptionsFormatFieldset()).isVisible();

    playwrightRefreshOrOpen(SbomManagerBomRegressionPage.url(seedApp.getPublicId(), SBOM_VERSION_ID_INVALID));
    assertThat(page.exportButton())
        .isVisible(VISIBLE_OPTS);
    assertThat(page.exportButtonPrimary()).containsText(EXPORT_BUTTON_INVALID_LABEL);

    page.clickExportButtonDropdownToggle();
    assertThat(page.exportDropdownAdditionalExportOptions()).isDisabled();
    assertThat(page.exportDropdownPdfLink()).isDisabled();
  }

  @Test
  @Tag("regression")
  public void testExportSbom_downloadFiresAndProducesNonEmptyFile() throws IOException {
    playwrightRefreshOrOpen(SbomManagerBomRegressionPage.url(seedApp.getPublicId(), SBOM_VERSION_ID_INVALID));
    SbomManagerBomRegressionPage page = new SbomManagerBomRegressionPage();
    assertThat(page.exportButton()).isVisible(VISIBLE_OPTS);

    Download download = page.clickExportPrimaryAndWaitForDownload();
    Assertions.assertThat(download.suggestedFilename()).isNotBlank();

    Path savedPath = tempDir.resolve("exported-sbom");
    download.saveAs(savedPath);
    Assertions.assertThat(savedPath.toFile().length()).isGreaterThan(0);
  }

  @Test
  @Tag("regression")
  public void testSummaryTile_withPoliciesSupported_showsPolicyViolationSection() {
    SystemConfigurationPropertyFeature.SBOM_POLICIES.setEnabled(true);

    playwrightRefresh();

    SbomManagerBomRegressionPage page = new SbomManagerBomRegressionPage();
    assertThat(page.reportTab())
        .isVisible(VISIBLE_OPTS);

    assertThat(page.policyViolationSummarySection())
        .isVisible(VISIBLE_OPTS);
    assertThat(page.policyViolationSummarySection())
        .containsText(POLICY_VIOLATION_SUMMARY_HEADING);
  }

  @Test
  @Tag("regression")
  public void testLoadError_retryReloadsPage() {
    page.route(SUMMARY_API_PATTERN, route -> route.fulfill(new Route.FulfillOptions()
        .setStatus(500)
        .setContentType("application/json")
        .setBody("{\"message\":\"Simulated server error\"}")));

    playwrightRefresh();

    SbomManagerBomRegressionPage bomPage = new SbomManagerBomRegressionPage();
    assertThat(bomPage.loadError())
        .isVisible(VISIBLE_OPTS);

    page.unroute(SUMMARY_API_PATTERN);
    bomPage.clickRetry();

    assertThat(bomPage.selectedReportTab())
        .isVisible(VISIBLE_OPTS);
  }

  @AfterEach
  public void disableSbomPoliciesFlag() {
    page.unroute(SUMMARY_API_PATTERN);
    SystemConfigurationPropertyFeature.SBOM_POLICIES.setEnabled(false);
  }

  @Test
  @Tag("regression")
  public void testOriginalBom_expandCollapseNode() {
    SbomManagerBomRegressionPage page = new SbomManagerBomRegressionPage();
    page.clickOriginalBomTab();
    assertThat(page.originalBomViewerTitle())
        .containsText(ORIGINAL_BOM_VIEWER_TITLE,
            new LocatorAssertions.ContainsTextOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));

    Locator firstCollapsible = page.originalBomCollapsibleItems().first();
    assertThat(firstCollapsible).isVisible(VISIBLE_OPTS);
    assertThat(firstCollapsible).hasAttribute("aria-expanded", "true",
        new LocatorAssertions.HasAttributeOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));

    page.firstCollapsibleItemToggle().click();
    assertThat(firstCollapsible).hasAttribute("aria-expanded", "false",
        new LocatorAssertions.HasAttributeOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));

    page.firstCollapsibleItemToggle().click();
    assertThat(firstCollapsible).hasAttribute("aria-expanded", "true",
        new LocatorAssertions.HasAttributeOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
  }

  @Test
  @Tag("regression")
  public void testOriginalBom_smallSbomAutoExpandsOnLoad() {
    SbomManagerBomRegressionPage page = new SbomManagerBomRegressionPage();
    page.clickOriginalBomTab();
    assertThat(page.originalBomViewerTitle())
        .containsText(ORIGINAL_BOM_VIEWER_TITLE,
            new LocatorAssertions.ContainsTextOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));

    Locator firstCollapsible = page.originalBomCollapsibleItems().first();
    assertThat(firstCollapsible).isVisible(VISIBLE_OPTS);
    assertThat(firstCollapsible).hasAttribute("aria-expanded", "true",
        new LocatorAssertions.HasAttributeOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));

    assertThat(page.originalBomViewer().locator(".nx-tree__item--collapsible[aria-expanded='false']"))
        .hasCount(0, new LocatorAssertions.HasCountOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
  }

  @Test
  @Tag("regression")
  public void testOriginalBom_searchShowsCountAndHighlightsMatchingText() {
    SbomManagerBomRegressionPage page = new SbomManagerBomRegressionPage();
    page.clickOriginalBomTab();
    assertThat(page.originalBomViewerTitle())
        .containsText(ORIGINAL_BOM_VIEWER_TITLE,
            new LocatorAssertions.ContainsTextOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));

    page.originalBomSearchInput().fill("guava");
    assertThat(page.originalBomSearchResultsCount())
        .hasText(Pattern.compile("[1-9][0-9]* results? found"),
            new LocatorAssertions.HasTextOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
    assertThat(page.originalBomHighlightMarks().first()).isVisible(VISIBLE_OPTS);
    assertThat(page.originalBomHighlightMarks().first())
        .containsText("guava", new LocatorAssertions.ContainsTextOptions().setIgnoreCase(true));

    page.originalBomSearchInput().fill("zzz-no-match-" + TemporaryEntity.uuid());
    assertThat(page.originalBomSearchResultsCount())
        .hasText("0 results found",
            new LocatorAssertions.HasTextOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
    assertThat(page.originalBomHighlightMarks())
        .hasCount(0, new LocatorAssertions.HasCountOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
  }

  @Test
  @Tag("regression")
  public void testOriginalBom_arrayNodesShowIntelligentTitles() {
    SbomManagerBomRegressionPage page = new SbomManagerBomRegressionPage();
    page.clickOriginalBomTab();
    assertThat(page.originalBomViewerTitle())
        .containsText(ORIGINAL_BOM_VIEWER_TITLE,
            new LocatorAssertions.ContainsTextOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));

    assertThat(page.originalBomKeyLabels()
        .filter(new Locator.FilterOptions().setHasText("guava@30.1-jre"))
        .first()).isVisible(VISIBLE_OPTS);
  }

  @Test
  @Tag("regression")
  public void testOriginalBom_jsonFormatSbomRendersAndSearchWorks() {
    playwrightRefreshOrOpen(SbomManagerBomRegressionPage.url(seedApp.getPublicId(), SBOM_VERSION_ID_JSON));
    SbomManagerBomRegressionPage page = new SbomManagerBomRegressionPage();
    assertThat(page.reportTab()).isVisible(VISIBLE_OPTS);

    page.clickOriginalBomTab();
    assertThat(page.originalBomViewerTitle())
        .containsText(ORIGINAL_BOM_VIEWER_TITLE,
            new LocatorAssertions.ContainsTextOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));

    assertThat(page.originalBomCollapsibleItems().first()).isVisible(VISIBLE_OPTS);

    page.originalBomSearchInput().fill("alpha");
    assertThat(page.originalBomSearchResultsCount())
        .hasText(Pattern.compile("[1-9][0-9]* results? found"),
            new LocatorAssertions.HasTextOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
    assertThat(page.originalBomHighlightMarks().first()).isVisible(VISIBLE_OPTS);
  }

  private void seedBomPage() {
    String appSuffix = TemporaryEntity.uuid();
    Organization org = tempEntity.newOrganization("SbomBom-" + appSuffix);
    seedApp = tempEntity.newApplication(
        "BomTestApp-" + appSuffix, "bom-test-app-" + appSuffix, org.getId());

    InsightWork insightWork = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    ThirdPartyFile scannedFile = tempEntity.newThirdPartyFile();
    ThirdPartyScan scan = tempEntity.newThirdPartyScan(scannedFile);
    Date now = Date.from(Instant.now());
    Path zippedBom = mockSbomForApp(insightWork);

    ThirdPartySbomMetadata metadata = tempEntity.newThirdPartySbomMetadata(
        scannedFile.getId(), seedApp.getId(), SBOM_VERSION_ID, ACTIVE,
        zippedBom.getFileName().toString(), CYCLONEDX_SPEC, XML_FORMAT, BOM_SPEC_VERSION, now);

    String fileId = metadata.getThirdPartyFileId();
    seedNpmComponent(fileId, "alpha", "1.0", "alpha-ref", "alpha-hash");
    seedNpmComponent(fileId, "beta", "2.0", "beta-ref", "beta-hash");

    // v2 — DB record only; the version-dropdown test asserts the URL change, not page content.
    ThirdPartyFile scannedFile2 = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartySbomMetadata(
        scannedFile2.getId(), seedApp.getId(), SBOM_VERSION_ID_V2, ACTIVE,
        "v2-bom-" + appSuffix + ".xml.gz", CYCLONEDX_SPEC, XML_FORMAT, BOM_SPEC_VERSION, now);

    // Invalid BOM — isValid=false changes export button label and disables dropdown items.
    ThirdPartyFile invalidFile = tempEntity.newThirdPartyFile();
    ThirdPartyScan invalidScan = tempEntity.newThirdPartyScan(invalidFile);
    tempEntity.newThirdPartySbomMetadata(
        invalidFile.getId(), seedApp.getId(), SBOM_VERSION_ID_INVALID, ACTIVE,
        mockSbomForApp(insightWork).getFileName().toString(),
        CYCLONEDX_SPEC, XML_FORMAT, BOM_SPEC_VERSION, now, false);

    // JSON format BOM — tests cross-format rendering and search in the Original BOM viewer.
    // Uses CYCLONEDX_SPEC (name() = "CYCLONEDX") rather than SbomSpecification.CYCLONEDX.toString()
    // ("CycloneDx") because this seed does not need SbomSpecification.fromValue matching. The aliases
    // BOM at seedAliasedVulnSbom() intentionally uses toString() to exercise that exact path.
    ThirdPartyFile jsonFile = tempEntity.newThirdPartyFile();
    ThirdPartyScan jsonScan = tempEntity.newThirdPartyScan(jsonFile);
    tempEntity.newThirdPartySbomMetadata(
        jsonFile.getId(), seedApp.getId(), SBOM_VERSION_ID_JSON, ACTIVE,
        mockSbomForApp(insightWork, SIMPLE_BOM_JSON).getFileName().toString(),
        CYCLONEDX_SPEC, JSON_FORMAT, BOM_SPEC_VERSION, now);

    seedCannedReport(scan);
    seedCannedReport(invalidScan);
    seedCannedReport(jsonScan);

    seedAliasedVulnSbom();
  }

  private void seedAliasedVulnSbom() {
    InsightWork insightWork = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    ThirdPartyFile aliasFile = tempEntity.newThirdPartyFile();
    ThirdPartyScan aliasScan = tempEntity.newThirdPartyScan(aliasFile);
    Date now = Date.from(Instant.now());
    Path zippedBom = mockSbomForApp(insightWork, SIMPLE_BOM_ALIASES_XML);

    // SbomSpecification.fromValue matches "CycloneDx" (toString), not "CYCLONEDX" (name()).
    ThirdPartySbomMetadata aliasesMeta = tempEntity.newThirdPartySbomMetadata(
        aliasFile.getId(), seedApp.getId(), SBOM_VERSION_ID_ALIASES, ACTIVE,
        zippedBom.getFileName().toString(),
        SbomSpecification.CYCLONEDX.toString(), XML_FORMAT, "1.4", now);

    ThirdPartyFileCoordinate fc = tempEntity.newThirdPartyFileCoordinate(
        aliasFile.getId(), "log4j-core-ref", "maven", "log4j-core", "2.14.1",
        "log4j-hash-001", "pkg:maven/org.apache.logging.log4j/log4j-core@2.14.1");

    // TemporaryEntity.newThirdPartyCoordinateSecurity() has no vulnIds parameter and
    // ThirdPartyCoordinateSecurityDAO has no update() method, so vulnIds must be set before the
    // initial insert. The direct DAO insert is safe: TemporaryEntity.after() calls
    // thirdPartyCoordinateSecurityDAO.getAll() to delete every coordinate_security row
    // regardless of how it was inserted (TemporaryEntity.java ~line 1053).
    ThirdPartyCoordinateSecurity cs = new ThirdPartyCoordinateSecurity(
        fc.getId(), "CVE-2021-44228", aliasesMeta.getId(),
        "Apache Log4j2 remote code execution vulnerability",
        "https://nvd.nist.gov/vuln/detail/CVE-2021-44228", 10.0d, "2.17.1");
    cs.setVulnIdsFromList(List.of("CVE-2021-44228", "GHSA-jfh8-c2jp-hdp2"));
    lookup(ThirdPartyCoordinateSecurityDAO.class).insert(cs);

    seedCannedReport(aliasScan);
  }

  private Path mockSbomForApp(InsightWork insightWork) {
    return mockSbomForApp(insightWork, SIMPLE_BOM_XML);
  }

  private Path mockSbomForApp(InsightWork insightWork, String fileName) {
    try {
      return mockOriginalSbom(
          SbomManagerBomPlaywrightTest.class,
          fileName,
          insightWork.getSbomDir(seedApp.getId()).toPath());
    }
    catch (Exception e) {
      throw new RuntimeException("Failed to mock original SBOM for " + fileName, e);
    }
  }

  private void seedNpmComponent(String thirdPartyFileId, String name, String version, String ref, String hash) {
    PackageUrlIdentifier purl = PackageUrlIdentifier.fromComponentIdentifier(
        ComponentIdentifier.createNpmCoordinates(name, version));
    tempEntity.newThirdPartyFileCoordinate(
        thirdPartyFileId, ref,
        purl.getFormat(), purl.getName(), purl.getVersion(),
        hash, purl.getPackageUrl());
  }

  @Test
  @Tag("regression")
  public void testExportSbom_enrichedExportContainsVulnAliasUrls() throws IOException {
    playwrightRefreshOrOpen(
        SbomManagerBomRegressionPage.url(seedApp.getPublicId(), SBOM_VERSION_ID_ALIASES));
    SbomManagerBomRegressionPage page = new SbomManagerBomRegressionPage();
    assertThat(page.reportTab()).isVisible(VISIBLE_OPTS);

    Download download = page.clickExportPrimaryAndWaitForDownload();
    Path savedPath = tempDir.resolve("exported-sbom-aliases.xml");
    download.saveAs(savedPath);

    String content = Files.readString(savedPath);
    Assertions.assertThat(content).contains("https://github.com/advisories/GHSA-jfh8-c2jp-hdp2");
    Assertions.assertThat(content).contains("https://links.sonatype.com/products/clm/vulnerability/CVE-2021-44228");
  }

  private void seedCannedReport(ThirdPartyScan scan) {
    URL zippedReport = ReportHelper.zipReport(REPORT_RESOURCE_DIR, tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    File reportDestination = work.getReportFile(seedApp.getId(), scan.getScanId());
    try {
      FileUtils.copyURLToFile(zippedReport, reportDestination);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
