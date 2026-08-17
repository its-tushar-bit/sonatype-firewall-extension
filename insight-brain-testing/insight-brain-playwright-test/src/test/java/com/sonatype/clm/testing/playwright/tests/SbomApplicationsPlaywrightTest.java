/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.SbomApplicationsPage;
import com.sonatype.clm.testing.playwright.pages.SbomApplicationsPageAssertions;
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.ACTIVE;
import static com.sonatype.insight.brain.sbom.SbomTestHelper.mockOriginalSbom;
import static org.assertj.core.api.Assertions.assertThat;

public class SbomApplicationsPlaywrightTest
    extends AbstractIqUiTest
{
  private static final int EXPECTED_COLUMN_COUNT = 6;

  private static final int EXPECTED_FIRST_PAGE_ROW_COUNT = 50;

  private static final int EXPECTED_TOTAL_APP_COUNT = 75;

  private static final String FILTER_QUERY_SINGLE_MATCH = "Test App 19";

  private static final String FILTER_QUERY_RESET_CHAR = " ";

  private static final String FILTER_QUERY_SECOND_SINGLE_MATCH = "Test App 21";

  @BeforeEach
  public void seedAndOpenSbomApplicationsAsAdmin() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER);
    setFeatures(LicensedFeature.SBOM_MANAGER);
    seedApplicationsWithSbomData();
    playwrightRefreshOrOpen(SbomApplicationsPage.url());
    playwrightLogin();
    new SbomApplicationsPageAssertions(new SbomApplicationsPage()).shouldBeLoaded();
  }

  @Test
  @Tag("sanity")
  public void testTableStructure() {
    SbomApplicationsPage sbomApps = new SbomApplicationsPage();
    SbomApplicationsPageAssertions assertions = new SbomApplicationsPageAssertions(sbomApps);

    assertions.shouldBeVisible();
    assertions.shouldShowTitle();
    assertions.shouldHaveColumnCount(EXPECTED_COLUMN_COUNT);
  }

  @Test
  @Tag("sanity")
  public void testTableFilter() {
    SbomApplicationsPage sbomApps = new SbomApplicationsPage();
    SbomApplicationsPageAssertions assertions = new SbomApplicationsPageAssertions(sbomApps);

    assertions.shouldHaveRowCount(EXPECTED_FIRST_PAGE_ROW_COUNT);
    sbomApps.filterByName(FILTER_QUERY_SINGLE_MATCH);
    assertions.shouldHaveRowCount(1);
    assertions.firstRowShouldContainText(FILTER_QUERY_SINGLE_MATCH);

    sbomApps.clearFilter();
    sbomApps.filterByName(FILTER_QUERY_RESET_CHAR);
    assertions.shouldHaveRowCount(EXPECTED_FIRST_PAGE_ROW_COUNT);

    sbomApps.clearFilter();
    sbomApps.filterByName(FILTER_QUERY_SECOND_SINGLE_MATCH);
    assertions.shouldHaveRowCount(1);
    assertions.firstRowShouldContainText(FILTER_QUERY_SECOND_SINGLE_MATCH);
  }

  @Test
  @Tag("sanity")
  public void testTablePagination() {
    SbomApplicationsPage sbomApps = new SbomApplicationsPage();
    SbomApplicationsPageAssertions assertions = new SbomApplicationsPageAssertions(sbomApps);

    assertions.shouldHaveRowCount(EXPECTED_FIRST_PAGE_ROW_COUNT);
    assertions.shouldShowPaginationStatus(
        "Showing " + EXPECTED_FIRST_PAGE_ROW_COUNT + " of " + EXPECTED_TOTAL_APP_COUNT + " applications");

    sbomApps.paginationButtonByLabel("goto last page").click();
    assertions.shouldShowPaginationStatus(
        "Showing " + EXPECTED_TOTAL_APP_COUNT + " of " + EXPECTED_TOTAL_APP_COUNT + " applications");
  }

  @Test
  @Tag("sanity")
  public void testTableSorting() {
    SbomApplicationsPage sbomApps = new SbomApplicationsPage();
    SbomApplicationsPageAssertions assertions = new SbomApplicationsPageAssertions(sbomApps);

    sbomApps.clickNameColumnSort();
    assertions.firstRowShouldContainText("Test App 0");
    String firstRowAsc = sbomApps.tableBodyRows().first().textContent();

    sbomApps.clickNameColumnSort();
    assertions.firstRowShouldNotContainText("Test App 0");
    String firstRowDesc = sbomApps.tableBodyRows().first().textContent();

    assertThat(firstRowAsc).isNotEqualTo(firstRowDesc);
  }

  private void seedApplicationsWithSbomData() {
    Organization org = tempEntity.newOrganization("SbomApps-" + TemporaryEntity.uuid());
    InsightWork insightWork = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    ThirdPartyFile scannedFile = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(scannedFile);

    for (int i = 0; i < 75; i++) {
      Application app = tempEntity.newApplication(
          "Test App " + i,
          "test-sbom-app-" + i + "-" + TemporaryEntity.uuid(),
          org.getId());

      Path zippedBom;
      try {
        zippedBom = mockOriginalSbom(
            SbomApplicationsPlaywrightTest.class,
            "simple-bom.xml",
            insightWork.getSbomDir(app.getId()).toPath());
      }
      catch (Exception e) {
        throw new RuntimeException("Failed to mock SBOM for app " + i, e);
      }

      LocalDateTime date = LocalDateTime.now(ZoneId.systemDefault()).minusDays(i + 1);
      ThirdPartySbomMetadata metadata = tempEntity.newThirdPartySbomMetadata(
          scannedFile.getId(),
          app.getId(),
          "test-version-" + i,
          ACTIVE,
          zippedBom.getFileName().toString(),
          SbomSpecification.CYCLONEDX.name(),
          SbomFormat.XML.name(),
          "0.0",
          Date.from(date.atZone(ZoneId.systemDefault()).toInstant()));

      ComponentIdentifier componentId = ComponentIdentifier.createNpmCoordinates("p" + i, "v1");
      PackageUrlIdentifier purl = PackageUrlIdentifier.fromComponentIdentifier(componentId);
      ThirdPartyFileCoordinate coordinate = tempEntity.newThirdPartyFileCoordinate(
          metadata.getThirdPartyFileId(),
          "s" + i,
          purl.getFormat(),
          purl.getName(),
          purl.getVersion(),
          "h" + i,
          purl.getPackageUrl());

      tempEntity.newThirdPartyCoordinateSecurity(coordinate,
          "cve-" + i + "-1", metadata.getId(), "description1", "link1",
          CvssV3Severity.LOW.getStartScoreRange(),
          CvssV3Severity.LOW.getDisplayName(), "fix1");
      tempEntity.newThirdPartyCoordinateSecurity(coordinate,
          "cve-" + i + "-2", metadata.getId(), "description2", "link2",
          CvssV3Severity.LOW.getStartScoreRange() + 0.2f,
          CvssV3Severity.LOW.getDisplayName(), "fix2");
      tempEntity.newThirdPartyCoordinateSecurity(coordinate,
          "cve-" + i + "-3", metadata.getId(), "description3", "link3",
          CvssV3Severity.LOW.getStartScoreRange() + 1f,
          CvssV3Severity.LOW.getDisplayName(), "fix3");
      tempEntity.newThirdPartyCoordinateSecurity(coordinate,
          "cve-" + i + "-4", metadata.getId(), "description4", "link4",
          CvssV3Severity.LOW.getEndScoreRange(),
          CvssV3Severity.LOW.getDisplayName(), "fix4");
      tempEntity.newThirdPartyCoordinateSecurity(coordinate,
          "cve-" + i + "-5", metadata.getId(), "description5", "link5",
          CvssV3Severity.LOW.getEndScoreRange() - 0.1f,
          CvssV3Severity.LOW.getDisplayName(), "fix5");
      tempEntity.newThirdPartyCoordinateSecurity(coordinate,
          "cve-" + i + "-6", metadata.getId(), "description6", "link6",
          CvssV3Severity.LOW.getEndScoreRange(),
          CvssV3Severity.LOW.getDisplayName(), "fix6");
    }
  }
}
