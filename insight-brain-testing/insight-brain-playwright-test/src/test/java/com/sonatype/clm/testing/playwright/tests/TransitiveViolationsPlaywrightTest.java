/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.sonatype.clm.testing.playwright.utils.TestReportEvaluator;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.ApplicationReportPage;
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.clm.testing.playwright.pages.TransitiveViolationsPage;
import com.sonatype.clm.testing.playwright.testdatamanager.TestDataManager;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class TransitiveViolationsPlaywrightTest
    extends AbstractIqUiTest
{
  private static final Data DATA = TestDataManager.load("transitive-violations", Data.class);

  private TransitiveViolationsPage tvPage;

  private String appPublicId;

  private Application application;

  private Policy policy;

  @BeforeEach
  public void seedReportAndLogin() throws IOException {
    tvPage = new TransitiveViolationsPage();

    seed();

    playwrightRefreshOrOpen(DashboardPage.url());
    playwrightLogin();
  }

  @Test
  @Tag("regression")
  public void testPageRenders_headerAndInnerSourceTag() {
    openTransitiveViolationsPage(DATA.directComponentHash());

    assertThat(tvPage.container()).isVisible();
    assertThat(tvPage.pageTitle()).containsText(DATA.directComponentDisplayName());
    assertThat(tvPage.reportInfo()).isVisible();
    assertThat(tvPage.innerSourceTag(DATA.innerSourceTagTestId())).isVisible();
    assertThat(tvPage.innerSourceTag(DATA.innerSourceTagTestId())).containsText(DATA.innerSourceTagText());
  }

  @Test
  @Tag("regression")
  public void testActionButtonsDisabled_whenNoViolations() {
    openTransitiveViolationsPage(DATA.noTransitiveDepsHash());

    assertThat(tvPage.container()).isVisible();
    assertThat(tvPage.requestWaiverButton()).isDisabled();
    assertThat(tvPage.waiveButton()).isDisabled();
  }

  @Test
  @Tag("regression")
  public void testActionPopovers_requestWaiverAndWaiveAndViewWaivers() {
    openTransitiveViolationsPage(DATA.directComponentHash());

    assertThat(tvPage.container()).isVisible();
    assertThat(tvPage.transitiveViolationRows()).hasCount(DATA.expectedViolationCount());

    tvPage.requestWaiverButton().click();
    assertThat(tvPage.requestWaiverPopover()).isVisible();
    tvPage.requestWaiverPopoverCloseButton().click();
    tvPage.requestWaiverPopover()
        .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));

    tvPage.waiveButton().click();
    assertThat(tvPage.waivePopover()).isVisible();
    tvPage.waivePopoverCancelButton().click();
    tvPage.waivePopover()
        .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));

    seedTransitiveWaiver();
    tvPage.viewWaiversButton().click();
    assertThat(tvPage.componentWaiversPopover()).isVisible();
    assertThat(tvPage.componentWaiversPopoverTitle()).containsText(DATA.transitiveComponentWaiversTitle());
  }

  @Test
  @Tag("regression")
  public void testDeleteWaiver_fromTransitiveWaiversPopover() {
    seedTransitiveWaiver();
    openTransitiveViolationsPage(DATA.directComponentHash());

    assertThat(tvPage.container()).isVisible();

    tvPage.viewWaiversButton().click();
    assertThat(tvPage.componentWaiversPopover()).isVisible();
    assertThat(tvPage.componentWaiversDeleteButtons()).hasCount(1);

    tvPage.componentWaiversDeleteButtons().first().click();
    tvPage.deleteWaiverConfirmButton().click();
    assertThat(tvPage.componentWaiversDeleteButtons()).hasCount(0);
  }

  /**
   * Badge renders only when aggregate-by-component is ON and the component is innerSource —
   * both hold for the seeded direct component (aggregation defaults to ON).
   */
  @Test
  @Tag("regression")
  public void testApplicationReport_aggregatedRowShowsTransitiveCountBadge() {
    playwrightRefreshOrOpen(ApplicationReportPage.url(appPublicId, DATA.scanId()));

    ApplicationReportPage report = new ApplicationReportPage();
    assertThat(report.appReportMain()).isVisible();

    assertThat(report.aggregateByComponentToggleInput()).isChecked();
    Locator directRow = report.violationRowForComponent(DATA.directComponentDisplayName());
    assertThat(report.transitiveViolationsBadgeIn(directRow)).isVisible();
    assertThat(report.transitiveViolationsBadgeIn(directRow)).containsText("transitive violation");
  }

  @Test
  @Tag("regression")
  public void testBackButton_hiddenWhenParamsMissing() {
    openTransitiveViolationsPage(DATA.directComponentHash());

    assertThat(tvPage.container()).isVisible();
    assertThat(tvPage.backButton()).isVisible();

    playwrightRefreshOrOpen("/assets/index.html#///" + DATA.directComponentHash() + "/transitiveViolations");
    playwrightRefresh();
    assertThat(tvPage.backButton()).not().isVisible();
  }

  private void openTransitiveViolationsPage(String hash) {
    String url = TransitiveViolationsPage.url(appPublicId, DATA.scanId(), hash);
    playwrightRefreshOrOpen(url);
    tvPage.container().waitFor();
  }

  private void seed() throws IOException {
    String suffix = TemporaryEntity.uuid();
    String orgName = DATA.orgNamePrefix() + "-" + suffix;
    String appName = DATA.appNamePrefix() + "-" + suffix;
    appPublicId = DATA.appPublicIdPrefix() + "-" + suffix;

    Organization org = tempEntity.newOrganization(orgName);
    application = tempEntity.newApplication(appName, appPublicId, org.getId());

    seedPolicy();
    evaluateReport();

    testCLMServer.getHdsServer()
        .respondWith(Collections.emptyMap())
        .atUri("rest/vulnerability/details/json");
  }

  private void seedPolicy() {
    Data.PolicySpec spec = DATA.policy();
    Policy p = new Policy(null, spec.name());
    p.setThreatLevel(spec.threatLevel());
    p.setOwnerId(Organization.ROOT_ORGANIZATION_ID);

    Constraint constraint = new Constraint(null, spec.name() + " constraint", LogicalOperator.AND);
    constraint.setConditions(Collections.singletonList(
        new Condition(spec.conditionTypeId(), spec.operator(), spec.value())));
    p.setConstraints(Collections.singletonList(constraint));

    policy = tempEntity.newPolicy(p);
  }

  private void evaluateReport() throws IOException {
    URL zippedReport = ReportHelper.zipReport(DATA.cannedReportClasspathDir(), tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    new TestReportEvaluator(application, DATA.scanId(), zippedReport, baseUrlFromTest, work)
        .evaluatePolicy();
  }

  private void seedTransitiveWaiver() {
    List<String> hashes = DATA.transitiveComponentHashes();
    tempEntity.newWaiver(
        hashes.get(0),
        policy.getId(),
        application.getId(),
        Collections.emptyList(),
        DATA.waiverComment());
  }

  private record Data(
      String cannedReportClasspathDir,
      String scanId,
      String orgNamePrefix,
      String appNamePrefix,
      String appPublicIdPrefix,
      PolicySpec policy,
      String directComponentHash,
      String directComponentDisplayName,
      String noTransitiveDepsHash,
      List<String> transitiveComponentHashes,
      String innerSourceTagTestId,
      String innerSourceTagText,
      String transitiveComponentWaiversTitle,
      String waiverComment,
      int expectedViolationCount)
  {
    record PolicySpec(String name, int threatLevel, String conditionTypeId, String operator, String value)
    {
    }
  }
}
