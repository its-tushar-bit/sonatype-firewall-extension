/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.nio.charset.StandardCharsets;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.LegalApplicationDetailsPage;
import com.sonatype.clm.testing.playwright.pages.LegalApplicationDetailsPageAssertions;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerComponent;
import com.sonatype.insight.brain.model.legal.ObligationStatus;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.categories.SanityTest;
import org.junit.experimental.categories.Category;

public class LegalApplicationDetailsPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String STAGE_TYPE_ID = "build";

  private static final int EXPECTED_COMPONENT_COUNT = 3;

  private static final List<String> TABLE_COLUMNS =
      List.of("Component", "Licenses", "Completed Obligations", "Review Status");

  private static final String STAGE_SUBTITLE_TEXT = "Build";

  private static final String COMPONENT_FILTER_TERM = "component1";

  private static final int COMPONENT_FILTER_EXPECTED_COUNT = 2;

  private static final String LICENSE_FILTER_TERM = "Apache";

  private static final int LICENSE_FILTER_EXPECTED_COUNT = 1;

  private Application app;

  @Before
  public void openLegalApplicationDetailsAsAdmin() throws Exception {
    app = seed();
    stubHdsEndpoints();
    playwrightRefreshOrOpen(LegalApplicationDetailsPage.url(app, STAGE_TYPE_ID));
    playwrightLogin();
  }

  @Test
  @Category(SanityTest.class)
  public void testLegalApplicationDetailsDisplay() {
    LegalApplicationDetailsPage legalPage = new LegalApplicationDetailsPage();
    LegalApplicationDetailsPageAssertions legalAssertions = new LegalApplicationDetailsPageAssertions(legalPage);

    legalAssertions.shouldShowTitle(app.getName());
    legalAssertions.shouldShowTableWithRowCount(EXPECTED_COMPONENT_COUNT);
    for (String column : TABLE_COLUMNS) {
      legalAssertions.shouldShowColumnHeader(column);
    }

  }

  @Test
  @Category(SanityTest.class)
  public void testFilterButtonAndFilterSidebar() {
    LegalApplicationDetailsPage legalPage = new LegalApplicationDetailsPage();
    LegalApplicationDetailsPageAssertions legalAssertions = new LegalApplicationDetailsPageAssertions(legalPage);

    legalAssertions.shouldShowFilterSidebarClosed();

    assertThat(legalPage.filterButton()).isVisible();
    assertThat(legalPage.filterButton()).containsText("Filter");
    assertThat(legalPage.filterDirtyAsterisk()).not().isVisible();

    legalPage.openFilterSidebar();
    assertThat(legalPage.reviewStatusFilterGroup()).isVisible();
    assertThat(legalPage.licenseThreatGroupFilterGroup()).isVisible();

    legalPage.expandReviewStatusFilter();
    legalPage.selectReviewStatusOption("Flagged");

    legalAssertions.shouldShowTableWithRowCount(1);

  }

  @Test
  @Category(RegressionTest.class)
  public void testLegalApplicationDetailsPageRenders() {
    LegalApplicationDetailsPage legalPage = new LegalApplicationDetailsPage();
    LegalApplicationDetailsPageAssertions assertions = new LegalApplicationDetailsPageAssertions(legalPage);

    assertions.shouldShowTitle(app.getName());
    assertions.shouldShowStageSubtitle(STAGE_SUBTITLE_TEXT);
    assertions.shouldShowBackButton();
    legalPage.clickBackButton();
  }

  @Test
  @Category(RegressionTest.class)
  public void testFilterByComponentAndLicenseName() {
    LegalApplicationDetailsPage legalPage = new LegalApplicationDetailsPage();
    LegalApplicationDetailsPageAssertions assertions = new LegalApplicationDetailsPageAssertions(legalPage);

    assertions.shouldShowTableWithRowCount(EXPECTED_COMPONENT_COUNT);

    legalPage.filterByComponent(COMPONENT_FILTER_TERM);
    assertions.shouldShowTableWithRowCount(COMPONENT_FILTER_EXPECTED_COUNT);

    legalPage.clearComponentFilter();
    assertions.shouldShowTableWithRowCount(EXPECTED_COMPONENT_COUNT);

    legalPage.filterByLicense(LICENSE_FILTER_TERM);
    assertions.shouldShowTableWithRowCount(LICENSE_FILTER_EXPECTED_COUNT);
  }

  @Test
  @Category(RegressionTest.class)
  public void testSortByColumnHeaders() {
    LegalApplicationDetailsPage legalPage = new LegalApplicationDetailsPage();
    LegalApplicationDetailsPageAssertions assertions = new LegalApplicationDetailsPageAssertions(legalPage);

    legalPage.clickComponentSort();
    assertions.shouldHaveComponentSortDir("ascending");

    legalPage.clickComponentSort();
    assertions.shouldHaveComponentSortDir("descending");

    legalPage.clickLicensesSort();
    assertions.shouldHaveLicensesSortDir("ascending");

    legalPage.clickCompletedObligationsSort();
    assertions.shouldHaveCompletedObligationsSortDir("ascending");
  }

  @Test
  @Category(RegressionTest.class)
  public void testCreateAttributionReportNavigation() {
    LegalApplicationDetailsPage legalPage = new LegalApplicationDetailsPage();

    assertThat(legalPage.createAttributionReportButton()).isVisible();
    legalPage.clickCreateAttributionReport();
    legalPage.waitForAttributionReportNavigation();
  }

  private Application seed() {
    Application application = tempEntity.newApplicationWithParent();

    seedComponent(application, "org.package", "component1", "1.0",
        "Apache-2.0", ObligationStatus.FULFILLED);
    seedComponent(application, "org.package", "component2", "2.0",
        "BSD-3-Clause", ObligationStatus.FULFILLED);
    seedComponent(application, "com.package", "component1", "3.0",
        "BSD-2-Clause", ObligationStatus.FLAGGED);

    return application;
  }

  private void seedComponent(
      Application application,
      String groupId,
      String artifactId,
      String version,
      String licenseId,
      ObligationStatus obligationStatus)
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

    tempEntity.newComponentObligation(
        componentIdentifier,
        application.getId(),
        "Inclusion of Notice",
        "test comment",
        obligationStatus,
        hash);
  }

  private void stubHdsEndpoints() throws Exception {
    testCLMServer.getHdsServer()
        .respondWith(IOUtils.toString(
            getClass().getResourceAsStream("/legal/legalLicenseMetadataHdsResponse.json"),
            StandardCharsets.UTF_8))
        .atUri("/rest/license/metadata");

    testCLMServer.getHdsServer()
        .respondWith(IOUtils.toString(
            getClass().getResourceAsStream("/legal/legalCommentHdsResponse.json"),
            StandardCharsets.UTF_8))
        .atUri("/rest/legal/comment");

    testCLMServer.getHdsServer()
        .respondWith(IOUtils.toString(
            getClass().getResourceAsStream("/legal/legalFileHdsResponse.json"),
            StandardCharsets.UTF_8))
        .atUri("/rest/legal/file");

    testCLMServer.getHdsServer()
        .respondWith("[]")
        .atUri("/rest/legal/source-link");
  }
}
