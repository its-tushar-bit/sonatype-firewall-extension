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
import java.util.Collections;
import java.util.Date;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.SbomApplicationsPage;
import com.sonatype.clm.testing.playwright.pages.SbomApplicationsPageAssertions;
import com.sonatype.clm.testing.playwright.pages.SbomComponentDetailsPage;
import com.sonatype.clm.testing.playwright.pages.SbomComponentDetailsPageAssertions;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.ACTIVE;

public class SbomComponentDetailsPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String SBOM_VERSION_ID = "mockVersionId";

  private static final String COMPONENT_HASH = "mockComponentHash";

  private static final String COMPONENT_REF = "96c3fa923cd66782eb2a2747a3453200a2d78fad";

  private static final String COMPONENT_PURL = "pkg:maven/2/3@1.1";

  private static final String REPORT_RESOURCE_DIR = "/sbom/ComponentDetailsTest/reportWithComponentRef";

  private static final String EXPECTED_PAGE_TITLE = "2 : 3 : 1.1";

  private static final String EXPECTED_FORMAT_TAG = "Maven";

  private static final int DISCLOSED_VULNERABILITY_COLUMN_COUNT = 7;

  private static final int SONATYPE_VULNERABILITY_COLUMN_COUNT = 6;

  private static final int EXPECTED_DISCLOSED_ROW_COUNT = 3;

  private static final String EXPECTED_HIGHEST_CVSS_SCORE = "9.6";

  private static final String EXPECTED_SONATYPE_VERIFIED_COUNT = "0 Sonatype Verified";

  private static final String EXPECTED_UNVERIFIED_COUNT = "3 Unverified";

  private static final String EXPECTED_CRITICAL_VIOLATION_COUNT = "1";

  private static final String FIRST_DISCLOSED_VULNERABILITY_CVSS = "5.6";

  private static final String FIRST_DISCLOSED_VULNERABILITY_ISSUE = "ABC-123";

  private static final String FIRST_SONATYPE_VULNERABILITY_CVSS = "9.6";

  private static final String FIRST_SONATYPE_VULNERABILITY_ISSUE = "sonatype-123";

  private static final String VULNERABILITY_DETAILS_POPOVER_TITLE = "Vulnerability Details CVE-4812";

  private static final String POLICY_VIOLATION_THREAT_LEVEL = "9";

  private static final String POLICY_VIOLATION_POLICY_NAME = "Security-High";

  private static final String POLICY_VIOLATION_CONSTRAINT_NAME = "Medium risk CVSS score";

  private static final String POLICY_VIOLATION_CONDITION_TEXT =
      "Found security vulnerability CVE-4812 with severity 5.3.";

  private Application testApplication;

  private Organization testOrganization;

  @BeforeEach
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

  @AfterEach
  public void disableSbomPoliciesFeature() {
    SystemConfigurationPropertyFeature.SBOM_POLICIES.setEnabled(false);
  }

  @Test
  @Tag("sanity")
  public void testComponentSummaryAndVulnerabilityTables() {
    SbomComponentDetailsPage details = new SbomComponentDetailsPage();
    SbomComponentDetailsPageAssertions assertions = new SbomComponentDetailsPageAssertions(details);

    assertions.shouldBeVisible();
    assertions.shouldShowPageTitle(EXPECTED_PAGE_TITLE);
    assertions.shouldShowReportInfoItems(testOrganization.getName(), testApplication.getName());
    assertions.shouldShowFormatTag(EXPECTED_FORMAT_TAG);
    assertions.shouldShowPurlTag(COMPONENT_PURL);
    assertions.shouldHaveTabVisible("Vulnerability");
    assertions.shouldHaveTabVisible("Policy Violations");
    assertions.shouldShowComponentSummary(EXPECTED_HIGHEST_CVSS_SCORE,
        EXPECTED_SONATYPE_VERIFIED_COUNT, EXPECTED_UNVERIFIED_COUNT);
    assertions.shouldShowCriticalViolationCount(EXPECTED_CRITICAL_VIOLATION_COUNT);
    assertions.shouldHaveDisclosedVulnerabilityColumnCount(DISCLOSED_VULNERABILITY_COLUMN_COUNT);
    assertions.shouldHaveDisclosedVulnerabilityRowCount(EXPECTED_DISCLOSED_ROW_COUNT);
    assertions.shouldShowFirstDisclosedVulnerabilityRow(FIRST_DISCLOSED_VULNERABILITY_CVSS,
        FIRST_DISCLOSED_VULNERABILITY_ISSUE);
    assertions.shouldHaveSonatypeVulnerabilityColumnCount(SONATYPE_VULNERABILITY_COLUMN_COUNT);
    assertions.shouldShowFirstSonatypeVulnerabilityRow(FIRST_SONATYPE_VULNERABILITY_CVSS,
        FIRST_SONATYPE_VULNERABILITY_ISSUE);
  }

  @Test
  @Tag("sanity")
  public void testVulnerabilityDetailsPopover() {
    SbomComponentDetailsPage details = new SbomComponentDetailsPage();
    SbomComponentDetailsPageAssertions assertions = new SbomComponentDetailsPageAssertions(details);

    assertions.shouldBeVisible();
    details.clickDisclosedVulnerabilityIssueLink(2);
    assertions.shouldShowVulnerabilityDetailsPopover(VULNERABILITY_DETAILS_POPOVER_TITLE);
  }

  @Test
  @Tag("sanity")
  public void testPolicyViolationsTile() {
    SbomComponentDetailsPage details = new SbomComponentDetailsPage();
    SbomComponentDetailsPageAssertions assertions = new SbomComponentDetailsPageAssertions(details);

    assertions.shouldBeVisible();
    details.clickTab("Policy Violations");
    assertions.shouldShowPolicyViolationsTile();
    assertions.shouldShowFirstPolicyViolationRow(POLICY_VIOLATION_THREAT_LEVEL,
        POLICY_VIOLATION_POLICY_NAME, POLICY_VIOLATION_CONSTRAINT_NAME);
  }

  @Test
  @Tag("sanity")
  public void testPolicyViolationDetailsDrawer() {
    SbomComponentDetailsPage details = new SbomComponentDetailsPage();
    SbomComponentDetailsPageAssertions assertions = new SbomComponentDetailsPageAssertions(details);

    assertions.shouldBeVisible();
    details.clickTab("Policy Violations");
    assertions.shouldShowPolicyViolationsTile();
    assertions.shouldShowFirstPolicyViolationRow(POLICY_VIOLATION_THREAT_LEVEL,
        POLICY_VIOLATION_POLICY_NAME, POLICY_VIOLATION_CONSTRAINT_NAME);
    details.clickPolicyViolationRow(0);
    assertions.shouldShowPolicyViolationDetailsDrawer();
    assertions.shouldShowDrawerConditionText(POLICY_VIOLATION_CONDITION_TEXT);
  }

  private void seedComponentDetails() throws IOException {
    testOrganization = tempEntity.newOrganization("SbomCdpTest-" + TemporaryEntity.uuid());
    testApplication = tempEntity.newApplication("testApp", "testApp", testOrganization.getId());

    ThirdPartyFile scannedFile = tempEntity.newThirdPartyFile();
    ThirdPartyScan scan = tempEntity.newThirdPartyScan(scannedFile);

    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    Path sbomDir = work.getSbomDir(testApplication.getId()).toPath();
    Files.createDirectories(sbomDir);

    ThirdPartySbomMetadata metadata =
        tempEntity.newThirdPartySbomMetadata(
            scannedFile.getId(), testApplication.getId(), SBOM_VERSION_ID, ACTIVE,
            "test-sbom.xml", "CYCLONEDX", "XML", "0.0", new Date());

    ThirdPartyFileCoordinate coordinate = tempEntity.newThirdPartyFileCoordinate(
        metadata.getThirdPartyFileId(),
        COMPONENT_REF,
        "npm", "testComponent", "1.2",
        COMPONENT_HASH, COMPONENT_PURL, COMPONENT_REF);

    seedVulnerabilities(coordinate);
    seedCannedReport(scan);
  }

  private void seedVulnerabilities(ThirdPartyFileCoordinate coordinate) {
    tempEntity.newThirdPartyCoordinateSecurity(coordinate, "ABC-123", null,
        "test vulnerability", "http://123.xyz", 5.6d, "testUser", "source", "v:1",
        "test severity", "123", "m1", "r1", "a1", "SBOM", null, null);
    tempEntity.newThirdPartyCoordinateSecurity(coordinate, "DEF-456", null,
        "test vulnerability2", "http://1234.xyz", 1.6d, "testUser", "source", "v:1",
        "testSeverity", "1234", "m1", "r1", "a1", "SBOM", null, null);
    tempEntity.newThirdPartyCoordinateSecurity(coordinate, "CVE-4812", null,
        "test vulnerability", "http://12345.xyz", 1.5d, "testUser", "source",
        "CVSS:3.0/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H", "testSeverity",
        "12345", "m3", "r3", "a3", "SBOM", "DEEP_DIVE", "PRIMARY");
    tempEntity.newThirdPartyCoordinateSecurity(coordinate, "sonatype-123",
        "test sonatype vulnerability", "http://sonatype.com", 9.6d, "testUser", "SONATYPE",
        "CVSS:1/1/1", "testSeverity", "a", "b", "c", "d", "Sonatype");
    tempEntity.newThirdPartyCoordinateSecurity(coordinate, "sonatype-456",
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
            getClass().getResource("/vulnerabilityDetails/vulnerabilityDetails_CVE-4812.json"))
        .atUri("rest/vulnerability/details/json/CVE-4812");
    testCLMServer.getHdsServer()
        .respondWith(Collections.emptyMap())
        .atUri("rest/vulnerability/details/json");
  }
}
