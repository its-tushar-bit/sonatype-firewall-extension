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
import java.util.Collections;
import java.util.Date;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.pages.SbomApplicationsPage;
import com.sonatype.clm.testing.playwright.pages.SbomApplicationsPageAssertions;
import com.sonatype.clm.testing.playwright.pages.SbomComponentDetailsPage;
import com.sonatype.clm.testing.playwright.pages.SbomComponentDetailsPageAssertions;
import com.sonatype.clm.testing.playwright.pages.SbomComponentDetailsRegressionPage;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import com.microsoft.playwright.assertions.LocatorAssertions;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.ACTIVE;

/** Regression tests for the SBOM Manager Component Details page annotation modals. */
public class SbomComponentDetailsRegressionPlaywrightTest
    extends AbstractIqUiTest
{
  private static final LocatorAssertions.IsVisibleOptions VISIBLE_OPTS =
      new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS);

  private static final String SBOM_VERSION_ID = "mockVersionId";

  private static final String COMPONENT_HASH = "mockComponentHash";

  private static final String COMPONENT_REF = "96c3fa923cd66782eb2a2747a3453200a2d78fad";

  private static final String COMPONENT_PURL = "pkg:maven/2/3@1.1";

  private static final String REPORT_RESOURCE_DIR = "/sbom/ComponentDetailsTest/reportWithComponentRef";

  private static final String VULNERABILITY_ISSUE = "ABC-123";

  private static final String VEX_STATUS = "resolved";

  private static final String VEX_JUSTIFICATION = "code_not_reachable";

  private static final String PREV_SBOM_VERSION_ID = "bom-regression-prev";

  private static final long PREV_SBOM_AGE_MS = 60_000L;

  private static final String PREV_ANNOTATION_DETAIL = "Prev annotation detail";

  private static final String TEST_ANNOTATION_DETAIL = "Test annotation detail";

  private static final String DEF_VULN_ISSUE = "DEF-456";

  private static final String CVE_VULN_ISSUE = "CVE-4812";

  private static final String SONATYPE_VULN_ISSUE_1 = "sonatype-123";

  private static final String SONATYPE_VULN_ISSUE_2 = "sonatype-456";

  private static final String SBOM_SPECIFICATION = "CYCLONEDX";

  private static final String SBOM_FILE_FORMAT = "XML";

  private static final String SBOM_SPEC_VERSION = "0.0";

  private static final String COMPONENT_PACKAGE_TYPE = "npm";

  private static final String COMPONENT_NAME = "testComponent";

  private static final String COMPONENT_VERSION = "1.2";

  private static final String COPY_ANNOTATION_MODAL_TITLE = "Copy annotation for " + VULNERABILITY_ISSUE;

  private static final String DELETE_ANNOTATION_MODAL_TITLE = "Delete annotation for " + VULNERABILITY_ISSUE;

  private Application testApplication;

  private ThirdPartyCoordinateSecurity abcCoordinateSecurity;

  @Before
  public void seedAndOpenComponentDetailsAsAdmin() throws IOException {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER);
    setFeatures(LicensedFeature.SBOM_MANAGER, LicensedFeature.APPLICATION_EVALUATION,
        LicensedFeature.SUCCESS_METRICS);
    SystemConfigurationPropertyFeature.SBOM_POLICIES.setEnabled(true);

    seedComponentDetails();
    stubHdsEndpoints();

    playwrightRefreshOrOpen(SbomApplicationsPage.url());
    playwrightLogin();
    new SbomApplicationsPageAssertions(new SbomApplicationsPage()).shouldBeLoaded();

    String componentUrl = SbomComponentDetailsPage.url(
        testApplication.getPublicId(), SBOM_VERSION_ID, COMPONENT_HASH);
    playwrightSpaNavigateToHashFragment(componentUrl.substring(componentUrl.indexOf('#')));
    playwrightWaitUntilUrlContains("/componentDetails/");
    new SbomComponentDetailsPageAssertions(new SbomComponentDetailsPage()).shouldBeLoaded();
  }

  @After
  public void disableSbomPoliciesFeature() {
    SystemConfigurationPropertyFeature.SBOM_POLICIES.setEnabled(false);
  }

  /** Precondition: a prior SBOM version must have a VEX annotation so {@code latestPreviousAnnotation} is non-null. */
  @Test
  @Category(RegressionTest.class)
  public void testCopyAnnotationModal_openOnCopyAnnotationClick() {
    ThirdPartyFile prevFile = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(prevFile);
    ThirdPartySbomMetadata prevMetadata = tempEntity.newThirdPartySbomMetadata(
        prevFile.getId(), testApplication.getId(), PREV_SBOM_VERSION_ID,
        ACTIVE, "prev-sbom.xml", SBOM_SPECIFICATION, SBOM_FILE_FORMAT, SBOM_SPEC_VERSION,
        Date.from(Instant.now().minusMillis(PREV_SBOM_AGE_MS)));
    ThirdPartyFileCoordinate prevCoordinate = tempEntity.newThirdPartyFileCoordinate(
        prevMetadata.getThirdPartyFileId(), COMPONENT_REF,
        COMPONENT_PACKAGE_TYPE, COMPONENT_NAME, COMPONENT_VERSION,
        COMPONENT_HASH, COMPONENT_PURL, COMPONENT_REF);
    ThirdPartyCoordinateSecurity prevSecurity = tempEntity.newThirdPartyCoordinateSecurity(
        prevCoordinate, VULNERABILITY_ISSUE, null, "prev vulnerability",
        "http://123.xyz", 5.6d, "testUser", "source", "v:1",
        "testSeverity", "123", "m1", "r1", "a1", "SBOM", null, null);
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(
        prevSecurity, VULNERABILITY_ISSUE, VEX_STATUS, VEX_JUSTIFICATION, null, PREV_ANNOTATION_DETAIL);

    playwrightRefresh();

    SbomComponentDetailsRegressionPage regressionPage = new SbomComponentDetailsRegressionPage();
    new SbomComponentDetailsPageAssertions(new SbomComponentDetailsPage()).shouldBeLoaded();

    regressionPage.clickVulnerabilityOptionsButton(VULNERABILITY_ISSUE);
    regressionPage.clickCopyAnnotationButton();

    assertThat(regressionPage.copyAnnotationModal())
        .isVisible(VISIBLE_OPTS);
    assertThat(regressionPage.copyAnnotationModalTitle())
        .containsText(COPY_ANNOTATION_MODAL_TITLE);
  }

  /** Precondition: {@code abcCoordinateSecurity} must have a VEX annotation so the delete button is rendered. */
  @Test
  @Category(RegressionTest.class)
  public void testDeleteAnnotationModal_openOnDeleteAnnotationClick() {
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(
        abcCoordinateSecurity, VULNERABILITY_ISSUE, VEX_STATUS, VEX_JUSTIFICATION, null, TEST_ANNOTATION_DETAIL);

    playwrightRefresh();

    SbomComponentDetailsRegressionPage regressionPage = new SbomComponentDetailsRegressionPage();
    new SbomComponentDetailsPageAssertions(new SbomComponentDetailsPage()).shouldBeLoaded();

    regressionPage.clickVulnerabilityOptionsButton(VULNERABILITY_ISSUE);
    regressionPage.clickDeleteAnnotationButton();

    assertThat(regressionPage.deleteAnnotationModal())
        .isVisible(VISIBLE_OPTS);
    assertThat(regressionPage.deleteAnnotationModalTitle())
        .containsText(DELETE_ANNOTATION_MODAL_TITLE);
  }

  private void seedComponentDetails() throws IOException {
    Organization testOrganization = tempEntity.newOrganization();
    testApplication = tempEntity.newApplication(testOrganization.getId());

    ThirdPartyFile scannedFile = tempEntity.newThirdPartyFile();
    ThirdPartyScan scan = tempEntity.newThirdPartyScan(scannedFile);

    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    Path sbomDir = work.getSbomDir(testApplication.getId()).toPath();
    Files.createDirectories(sbomDir);

    ThirdPartySbomMetadata metadata =
        tempEntity.newThirdPartySbomMetadata(
            scannedFile.getId(), testApplication.getId(), SBOM_VERSION_ID, ACTIVE,
            "test-sbom.xml", SBOM_SPECIFICATION, SBOM_FILE_FORMAT, SBOM_SPEC_VERSION, Date.from(Instant.now()));

    ThirdPartyFileCoordinate coordinate = tempEntity.newThirdPartyFileCoordinate(
        metadata.getThirdPartyFileId(),
        COMPONENT_REF,
        COMPONENT_PACKAGE_TYPE, COMPONENT_NAME, COMPONENT_VERSION,
        COMPONENT_HASH, COMPONENT_PURL, COMPONENT_REF);

    seedVulnerabilities(coordinate);
    seedCannedReport(scan);
  }

  private void seedVulnerabilities(ThirdPartyFileCoordinate coordinate) {
    abcCoordinateSecurity = tempEntity.newThirdPartyCoordinateSecurity(coordinate, VULNERABILITY_ISSUE, null,
        "test vulnerability", "http://123.xyz", 5.6d, "testUser", "source", "v:1",
        "test severity", "123", "m1", "r1", "a1", "SBOM", null, null);
    tempEntity.newThirdPartyCoordinateSecurity(coordinate, DEF_VULN_ISSUE, null,
        "test vulnerability2", "http://1234.xyz", 1.6d, "testUser", "source", "v:1",
        "testSeverity", "1234", "m1", "r1", "a1", "SBOM", null, null);
    tempEntity.newThirdPartyCoordinateSecurity(coordinate, CVE_VULN_ISSUE, null,
        "test vulnerability", "http://12345.xyz", 1.5d, "testUser", "source",
        "CVSS:3.0/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H", "testSeverity",
        "12345", "m3", "r3", "a3", "SBOM", "DEEP_DIVE", "PRIMARY");
    tempEntity.newThirdPartyCoordinateSecurity(coordinate, SONATYPE_VULN_ISSUE_1,
        "test sonatype vulnerability", "http://sonatype.com", 9.6d, "testUser", "SONATYPE",
        "CVSS:1/1/1", "testSeverity", "a", "b", "c", "d", "Sonatype");
    tempEntity.newThirdPartyCoordinateSecurity(coordinate, SONATYPE_VULN_ISSUE_2,
        "test sonatype vulnerability2", "http://sonatype2.com", 4.6d, "testUser", "SONATYPE",
        "CVSS:1/1/1", "testSeverity", "a", "b", "c", "d", "Sonatype");
  }

  private void seedCannedReport(ThirdPartyScan scan) throws IOException {
    URL zippedReport = ReportHelper.zipReport(REPORT_RESOURCE_DIR, tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    File reportDestination = work.getReportFile(testApplication.getId(), scan.getScanId());
    try {
      FileUtils.copyURLToFile(zippedReport, reportDestination);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void stubHdsEndpoints() {
    testCLMServer.getHdsServer()
        .respondWith(
            getClass().getResource("/vulnerabilityDetails/vulnerabilityDetails_" + CVE_VULN_ISSUE + ".json"))
        .atUri("rest/vulnerability/details/json/" + CVE_VULN_ISSUE);
    testCLMServer.getHdsServer()
        .respondWith(Collections.emptyMap())
        .atUri("rest/vulnerability/details/json");
  }
}
