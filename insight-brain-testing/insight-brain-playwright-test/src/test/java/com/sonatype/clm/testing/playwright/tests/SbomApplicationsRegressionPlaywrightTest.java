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
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.pages.SbomApplicationsRegressionAssertions;
import com.sonatype.clm.testing.playwright.pages.SbomApplicationsRegressionPage;
import com.sonatype.clm.testing.playwright.pages.SbomManagerBomRegressionPage;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.ComplianceStageType;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.sbom.SbomSpecification;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.CvssV3Severity;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.scan.file.SbomFormat;

import com.microsoft.playwright.assertions.LocatorAssertions;

import org.apache.commons.io.FileUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.ACTIVE;
import static com.sonatype.insight.brain.sbom.SbomTestHelper.mockOriginalSbom;

/** Regression tests for the SBOM Manager Applications table ({@code #/sbomManager/applications}). */
public class SbomApplicationsRegressionPlaywrightTest
    extends AbstractIqUiTest
{
  private static final LocatorAssertions.IsVisibleOptions VISIBLE_OPTS =
      new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS);

  private static final int EXPECTED_FIRST_PAGE_ROW_COUNT = 50;

  private static final int EXPECTED_TOTAL_APP_COUNT = 75;

  private static final String NO_MATCH_FILTER = "ZZZ-NO-MATCH-XYZ";

  private String nameSuffix;

  private String firstAlphaAppName;

  private String filterQuerySingleMatch;

  private Application firstApp;

  private Application violationsApp;

  private ThirdPartyScan firstScan;

  @Before
  public void seedAndOpenSbomApplicationsAsAdmin() throws Exception {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER);
    setFeatures(LicensedFeature.SBOM_MANAGER);
    seedApplicationsWithSbomData();
    playwrightRefreshOrOpen(SbomApplicationsRegressionPage.url());
    playwrightLogin();
    new SbomApplicationsRegressionAssertions(new SbomApplicationsRegressionPage()).shouldBeLoaded();
  }

  /** SBOM Applications table — columns have expected headers and sortability; Latest Version link navigates to BOM. */
  @Test
  @Category(RegressionTest.class)
  public void testTableColumns_sortabilityAndLatestVersionLinkNavigatesToBom() {
    SbomApplicationsRegressionPage sbomApps = new SbomApplicationsRegressionPage();
    new SbomApplicationsRegressionAssertions(sbomApps).shouldHaveExpectedColumnsWithSortability();

    sbomApps.clickNameColumnSort();
    sbomApps.latestVersionLink(sbomApps.tableBodyRows().first()).click();

    assertThat(new SbomManagerBomRegressionPage().reportTab()).isVisible(VISIBLE_OPTS);
  }

  /**
   * SBOM Applications table — sort resets after filter narrows to one row; sort button hidden in single-match state.
   */
  @Test
  @Category(RegressionTest.class)
  public void testSortBehavior_multiAppAndSingleApp() {
    SbomApplicationsRegressionPage sbomApps = new SbomApplicationsRegressionPage();
    SbomApplicationsRegressionAssertions assertions = new SbomApplicationsRegressionAssertions(sbomApps);

    sbomApps.clickNameColumnSort();
    assertions.firstRowShouldContainText(firstAlphaAppName);

    sbomApps.clickViolationsColumnHeader();
    sbomApps.filterByName(nameSuffix);
    assertions.shouldHaveRowCount(EXPECTED_FIRST_PAGE_ROW_COUNT);
    assertions.firstRowShouldContainText(firstAlphaAppName);

    sbomApps.clickNameColumnSort();
    assertions.firstRowShouldNotContainText(firstAlphaAppName);

    assertions.nameColumnSortButtonShouldBeVisible();

    sbomApps.filterByName(filterQuerySingleMatch);
    assertions.shouldShowSingleMatchingRow(filterQuerySingleMatch);

    assertions.nameColumnSortButtonShouldBeHidden();
  }

  /** SBOM Applications table — debounced name filter narrows rows; clearing restores full table. */
  @Test
  @Category(RegressionTest.class)
  public void testNameFilter_debounceAndClearRestoresFullTable() {
    SbomApplicationsRegressionPage sbomApps = new SbomApplicationsRegressionPage();
    SbomApplicationsRegressionAssertions assertions = new SbomApplicationsRegressionAssertions(sbomApps);

    sbomApps.typeFilterByName(filterQuerySingleMatch);
    assertions.shouldShowSingleMatchingRow(filterQuerySingleMatch);

    sbomApps.clearFilter();
    sbomApps.filterByName(nameSuffix);
    assertions.shouldHaveRowCount(EXPECTED_FIRST_PAGE_ROW_COUNT);
  }

  /**
   * SBOM Applications table — row cells show vulnerability/violations counters and relative import date; empty-state
   * message when no rows match filter.
   */
  @Test
  @Category(RegressionTest.class)
  public void testRowCells_showCountersRelativeDateAndEmptyMessageWhenNoMatch() {
    SbomApplicationsRegressionPage sbomApps = new SbomApplicationsRegressionPage();
    SbomApplicationsRegressionAssertions assertions = new SbomApplicationsRegressionAssertions(sbomApps);

    sbomApps.clickNameColumnSort();
    sbomApps.filterByName(nameSuffix);
    assertions.firstRowCellsShouldShowCountersAndRelativeDate();
    assertions.shouldShowMultiPagePagination(EXPECTED_FIRST_PAGE_ROW_COUNT, EXPECTED_TOTAL_APP_COUNT);

    sbomApps.filterByName(filterQuerySingleMatch);
    assertions.shouldShowSingleMatchingRowWithViolations(filterQuerySingleMatch);

    sbomApps.clearFilter();
    sbomApps.filterByName(NO_MATCH_FILTER);
    assertions.shouldShowEmptyStateMessage();

    sbomApps.clearFilter();
    sbomApps.filterByName(nameSuffix);
    assertions.shouldHaveRowCount(EXPECTED_FIRST_PAGE_ROW_COUNT);
  }

  private void seedApplicationsWithSbomData() throws Exception {
    InsightWork insightWork = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    ThirdPartyFile scannedFile = tempEntity.newThirdPartyFile();
    firstScan = tempEntity.newThirdPartyScan(scannedFile);
    nameSuffix = "-" + TemporaryEntity.uuid();

    // Seed EXPECTED_TOTAL_APP_COUNT apps so the table spans multiple pages (> EXPECTED_FIRST_PAGE_ROW_COUNT).
    for (int i = 0; i < EXPECTED_TOTAL_APP_COUNT; i++) {
      String appName = "Test App " + i + nameSuffix;
      if (i == 0) {
        firstAlphaAppName = appName;
      }
      if (i == 19) {
        filterQuerySingleMatch = appName;
      }
      Application app = tempEntity.newApplication(
          appName,
          "test-sbom-app-" + i + "-" + TemporaryEntity.uuid(),
          tempEntity.newOrganization().getId());
      if (i == 0) {
        firstApp = app;
      }
      if (i == 19) {
        violationsApp = app;
      }

      Path zippedBom = mockOriginalSbom(
          SbomApplicationsRegressionPlaywrightTest.class,
          "simple-bom.xml",
          insightWork.getSbomDir(app.getId()).toPath());

      Instant date = Instant.now().minus(i + 1, ChronoUnit.DAYS);
      ThirdPartySbomMetadata metadata = tempEntity.newThirdPartySbomMetadata(
          scannedFile.getId(),
          app.getId(),
          "test-version-" + i,
          ACTIVE,
          zippedBom.getFileName().toString(),
          SbomSpecification.CYCLONEDX.name(),
          SbomFormat.XML.name(),
          "0.0",
          Date.from(date));

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
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(
        violationsApp.getId(), ComplianceStageType.ID, TemporaryEntity.uuid());
    Policy policy = tempEntity.newPolicy(violationsApp.getId(), "sbom-policy-" + TemporaryEntity.uuid());
    tempEntity.newPolicyViolation(evaluation, policy);

    seedCannedReport(firstApp, firstScan);
  }

  private void seedCannedReport(Application app, ThirdPartyScan scan) {
    URL zippedReport = ReportHelper.zipReport("/sbom/ComponentDetailsTest/reportWithComponentRef", tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    File reportDestination = work.getReportFile(app.getId(), scan.getScanId());
    try {
      FileUtils.copyURLToFile(zippedReport, reportDestination);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
