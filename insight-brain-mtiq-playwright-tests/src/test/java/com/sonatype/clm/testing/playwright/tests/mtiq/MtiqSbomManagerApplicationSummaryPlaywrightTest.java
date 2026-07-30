/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests.mtiq;

import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.playwright.categories.MtiqTest;
import com.sonatype.clm.testing.playwright.mtiq.AbstractMtiqUiTest;
import com.sonatype.clm.testing.playwright.pages.MtiqSbomManagerApplicationSummaryPage;
import com.sonatype.clm.testing.playwright.pages.MtiqSbomManagerApplicationSummaryPageAssertions;
import com.sonatype.clm.testing.playwright.pages.SbomManagerBomPage;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus;
import com.sonatype.insight.brain.sbom.SbomSpecification;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.CvssV3Severity;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.scan.file.SbomFormat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static com.sonatype.insight.brain.sbom.SbomTestHelper.mockOriginalSbom;

@Category(MtiqTest.class)
public class MtiqSbomManagerApplicationSummaryPlaywrightTest
    extends AbstractMtiqUiTest
{
  private static final String SIMPLE_BOM_XML = "simple-bom.xml";

  private static final List<String> SBOM_SPEC_LABELS =
      List.of("CycloneDX 1.7", "CycloneDX 1.6", "SPDX 2.3", "SPDX 3.0");

  private static final List<String> SBOM_FORMAT_LABELS = List.of("JSON", "XML");

  private ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  private Organization organization;

  private Application application;

  private MtiqSbomManagerApplicationSummaryPage summaryPage;

  private MtiqSbomManagerApplicationSummaryPageAssertions assertions;

  @Before
  public void setUp() throws Exception {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER_SAAS);
    playwrightRefreshOrOpen("/");
    playwrightLogin();

    thirdPartySbomMetadataDAO = lookup(ThirdPartySbomMetadataDAO.class);
    organization = tempEntity.newOrganization();
    String suffix = tempEntity.uuid();
    application = tempEntity.newApplication(
        "Test Application " + suffix, "test-application-" + suffix, organization.getId());

    createSbomMetadata("test-version", true, new Date());

    summaryPage = new MtiqSbomManagerApplicationSummaryPage();
    assertions = new MtiqSbomManagerApplicationSummaryPageAssertions(summaryPage);
  }

  @Test
  public void testSbomsTile_rendersThreeSeededSboms() throws Exception {
    createSbomMetadata("test-version-a", true, new Date());
    createSbomMetadata("test-version-b", true, new Date());
    playwrightRefreshOrOpen(MtiqSbomManagerApplicationSummaryPage.url(application.getPublicId()));

    assertions.shouldShowSbomsTileWithHeader();
    assertions.shouldShowSbomsTableColumns();
    assertThat(summaryPage.sbomsTableBodyRows()).hasCount(3);
  }

  @Test
  public void testSbomsTile_RenderInvalidSbomIndicator() throws Exception {
    // Offset the invalid SBOM's import date so it deterministically sorts newest — otherwise the
    // millisecond-precision timestamp can tie with @Before's "test-version".
    createSbomMetadata("test-version-2", false, new Date(System.currentTimeMillis() + 1000));
    playwrightRefreshOrOpen(MtiqSbomManagerApplicationSummaryPage.url(application.getPublicId()));

    assertThat(summaryPage.sbomsTableBodyRows()).hasCount(2);
    assertThat(summaryPage.invalidSbomIndicatorInRow(0)).isVisible();
  }

  @Test
  public void testSbomsTile_DeleteSbomReport() {
    playwrightRefreshOrOpen(MtiqSbomManagerApplicationSummaryPage.url(application.getPublicId()));

    summaryPage.sbomActionsDropdownFor("test-version").click();
    summaryPage.sbomActionsMenuItem("Delete SBOM").click();
    assertThat(summaryPage.deleteSbomModal()).isVisible();
    summaryPage.deleteSbomModalPrimaryButton().click();
    assertThat(summaryPage.deleteSbomModal()).isHidden();
    assertions.shouldShowEmptyState();

    playwrightRefreshOrOpen(MtiqSbomManagerApplicationSummaryPage.url(application.getPublicId()));
    assertions.shouldShowEmptyState();
  }

  @Test
  public void testSbomsTile_BOMNavigation() {
    playwrightRefreshOrOpen(MtiqSbomManagerApplicationSummaryPage.url(application.getPublicId()));

    summaryPage.bomNavigationLinkInRow(0).click();

    SbomManagerBomPage bomPage = new SbomManagerBomPage();
    assertThat(bomPage.container()).isVisible();
    summaryPage.playwrightPage()
        .waitForURL(url -> url.contains(application.getPublicId())
            && url.contains("test-version"));
  }

  @Test
  public void testSbomsTile_Pagination() throws Exception {
    seedManySbomsForPagination();
    playwrightRefreshOrOpen(MtiqSbomManagerApplicationSummaryPage.url(application.getPublicId()));

    assertThat(summaryPage.paginationButtons().nth(1)).isVisible();
    // Default desc-by-import-date: original "test-version" (today) is newest. Exact match — the
    // paginated seed rows ("test-version 0".."test-version 9") would satisfy containsText.
    assertThat(summaryPage.sbomsTableBodyRowColumn(0, 0)).hasText("test-version");

    summaryPage.paginationButtons().nth(1).click();
    // Page 2: oldest row is the last-seeded pagination entry.
    assertThat(summaryPage.sbomsTableBodyRowColumn(0, 0)).hasText("test-version 9");

    summaryPage.paginationButtons().nth(0).click();
    assertThat(summaryPage.sbomsTableBodyRowColumn(0, 0)).hasText("test-version");
  }

  @Test
  public void testSbomsTile_ChangeToAnotherApplication() {
    playwrightRefreshOrOpen(MtiqSbomManagerApplicationSummaryPage.url(application.getPublicId()));
    assertions.shouldShowApplicationTitle(application.getName());
    assertThat(summaryPage.sbomsTableBodyRows()).hasCount(1);

    String suffix = tempEntity.uuid();
    Application newApp =
        tempEntity.newApplication("New Application " + suffix, "new-application-" + suffix, organization.getId());
    // Detour route re-mounts the SBOMs tile against newApp; same-document hash nav preserves state.
    playwrightRefreshOrOpen(MtiqSbomManagerApplicationSummaryPage.neutralDetourUrl());
    playwrightRefreshOrOpen(MtiqSbomManagerApplicationSummaryPage.url(newApp.getPublicId()));
    assertions.shouldShowApplicationTitle(newApp.getName());
    assertions.shouldShowEmptyState();
  }

  @Test
  public void testSbomsTile_DownloadDropdownOptions() throws Exception {
    createSbomMetadata("test-version-2", false, new Date(System.currentTimeMillis() + 1000));
    playwrightRefreshOrOpen(MtiqSbomManagerApplicationSummaryPage.url(application.getPublicId()));

    summaryPage.sbomActionsDropdownFor("test-version").click();
    assertThat(summaryPage.sbomActionsMenuItem("Export Original SBOM")).isEnabled();
    assertThat(summaryPage.sbomActionsMenuItem("Additional Export Options")).isEnabled();
    assertThat(summaryPage.sbomActionsMenuItem("Export PDF")).isVisible();
    assertThat(summaryPage.sbomActionsMenuItem("Delete SBOM")).isEnabled();

    summaryPage.sbomActionsMenuItem("Additional Export Options").click();
    assertThat(summaryPage.additionalExportOptionsModal()).isVisible();
    SBOM_SPEC_LABELS.forEach(
        label -> assertThat(summaryPage.additionalExportSpecificationRadio(label)).isVisible());
    SBOM_FORMAT_LABELS.forEach(
        label -> assertThat(summaryPage.additionalExportFormatRadio(label)).isVisible());
    assertThat(summaryPage.additionalExportOptionsModal().locator("input[type=radio]"))
        .hasCount(SBOM_SPEC_LABELS.size() + SBOM_FORMAT_LABELS.size());
    summaryPage.additionalExportOptionsModal()
        .getByRole(AriaRole.BUTTON,
            new Locator.GetByRoleOptions().setName("Cancel"))
        .click();
    assertThat(summaryPage.additionalExportOptionsModal()).isHidden();

    summaryPage.sbomActionsDropdownFor("test-version-2").click();
    assertThat(summaryPage.sbomActionsMenuItem("Export Original SBOM")).isEnabled();
    assertThat(summaryPage.sbomActionsMenuItem("Additional Export Options")).isDisabled();
    assertThat(summaryPage.sbomActionsMenuItem("Delete SBOM")).isEnabled();
  }

  @Test
  public void testSbomsTile_sortByImportDate() throws Exception {
    seedManySbomsForPagination();
    playwrightRefreshOrOpen(MtiqSbomManagerApplicationSummaryPage.url(application.getPublicId()));

    // Default desc: original "test-version" (today) is newest. Exact match — the paginated seed
    // rows ("test-version 0".."test-version 9") would satisfy containsText.
    assertThat(summaryPage.sbomsTableBodyRowColumn(0, 0)).hasText("test-version");

    summaryPage.sbomsTableColumnHeader(4).click();
    // Asc: "test-version 9" (10 days ago) is oldest.
    assertThat(summaryPage.sbomsTableBodyRowColumn(0, 0)).hasText("test-version 9");

    summaryPage.sbomsTableColumnHeader(4).click();
    assertThat(summaryPage.sbomsTableBodyRowColumn(0, 0)).hasText("test-version");
  }

  /** Header clickable + rows still render — full ordering coverage is CLM-42839. */
  @Test
  public void testSbomsTile_releaseStatusHeaderIsClickable() throws Exception {
    seedManySbomsForPagination();
    playwrightRefreshOrOpen(MtiqSbomManagerApplicationSummaryPage.url(application.getPublicId()));

    summaryPage.sbomsTableColumnHeader(2).click();
    assertThat(summaryPage.sbomsTableBodyRows().first()).isVisible();
    summaryPage.sbomsTableColumnHeader(2).click();
    assertThat(summaryPage.sbomsTableBodyRows().first()).isVisible();
  }

  private ThirdPartySbomMetadata createSbomMetadata(
      String sbomVersion,
      boolean isValid,
      Date createdAt) throws Exception
  {
    ThirdPartyFile scannedFile = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(scannedFile);
    InsightWork insightWork = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    Path zippedBom = mockOriginalSbom(MtiqSbomManagerApplicationSummaryPlaywrightTest.class, SIMPLE_BOM_XML,
        insightWork.getSbomDir(application.getId()).toPath());

    ThirdPartySbomMetadata metadata = tempEntity.newThirdPartySbomMetadata(scannedFile.getId(), application.getId(),
        sbomVersion, ThirdPartySbomMetadataStatus.ACTIVE, zippedBom.getFileName().toString(),
        SbomSpecification.CYCLONEDX.name(), SbomFormat.XML.name(), "0.0", createdAt);
    // Factory persists createdAt; update() only needed to persist the isValid flip.
    metadata.setIsValid(isValid);
    thirdPartySbomMetadataDAO.update(metadata);
    return metadata;
  }

  private void seedManySbomsForPagination() throws Exception {
    ThirdPartyFile scannedFile = tempEntity.newThirdPartyFile();
    InsightWork insightWork = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    Instant baseInstant = Instant.now();
    for (int i = 0; i < 10; i++) {
      Path zippedBom = mockOriginalSbom(MtiqSbomManagerApplicationSummaryPlaywrightTest.class, SIMPLE_BOM_XML,
          insightWork.getSbomDir(application.getId()).toPath());
      Date rowDate = Date.from(baseInstant.minus(i + 1, ChronoUnit.DAYS));
      ThirdPartySbomMetadata meta = tempEntity.newThirdPartySbomMetadata(scannedFile.getId(), application.getId(),
          "test-version " + i, ThirdPartySbomMetadataStatus.ACTIVE, zippedBom.getFileName().toString(),
          SbomSpecification.CYCLONEDX.name(), SbomFormat.XML.name(), "0.0", rowDate);

      ComponentIdentifier compId = ComponentIdentifier.createNpmCoordinates("p" + i, "v" + i);
      PackageUrlIdentifier purl = PackageUrlIdentifier.fromComponentIdentifier(compId);
      ThirdPartyFileCoordinate coord = tempEntity.newThirdPartyFileCoordinate(scannedFile.getId(), "s" + i,
          purl.getFormat(), purl.getName(), purl.getVersion(), "h" + i, purl.getPackageUrl());
      tempEntity.newThirdPartyCoordinateSecurity(coord, "cve-" + i, meta.getId(), "d" + i, "l" + i,
          CvssV3Severity.HIGH.getStartScoreRange(), CvssV3Severity.HIGH.getDisplayName(), "f" + i);
    }
  }
}
