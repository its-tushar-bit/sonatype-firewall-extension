/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.DashboardComponentDetails;
import com.sonatype.clm.testing.functional.elements.DashboardFilters;
import com.sonatype.clm.testing.functional.elements.DashboardViolations.ViolationTile;
import com.sonatype.clm.testing.functional.elements.DashboardViolations.ViolationsHeaders;
import com.sonatype.clm.testing.functional.elements.DashboardViolations.ViolationsResults;
import com.sonatype.clm.testing.functional.pages.ApplicationReportContainerPage;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.OperateStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.service.InsightWork;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.open;
import static com.sonatype.clm.testing.functional.elements.CLM.DISABLED;
import static com.sonatype.clm.testing.functional.elements.DashboardViolations.SEVERE;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.LICENSE;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.SECURITY;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.joda.time.DateTime.now;

public class DashboardViolationsTest
    extends AbstractFunctionalTest
{
  private static final String NO_DATA_MSG = "No data available given the applied filters and available permissions.";

  private static final String MAX_RESULTS_MSG = "First 100 results shown";

  private Application app1, app2;

  private Policy securityPolicy, licensePolicy;

  private PolicyEvaluation buildEvalNow, releaseEval2DaysAgo, operateEval1WeekAgo;

  private ApplicationComponent buildComponent, releaseComponent, operateComponent;

  @BeforeClass
  public static void beforeClass() {
    open(DashboardPage.URL);
    loginAsAdmin();
  }

  @Before
  public void init() throws IOException {
    app1 = tempEntity.newApplicationWithParent("app1", "Violations Test App1");
    app2 = tempEntity.newApplicationWithParent("app2", "Violations Test App2");
    licensePolicy = tempEntity.newPolicy(app1.getParentOwnerId(), "DashboardViolationsTestLicensePolicy");
    securityPolicy = tempEntity.newPolicy(app2.getParentOwnerId(), "DashboardViolationsTestSecurityPolicy");
    buildEvalNow = tempEntity
        .newPolicyEvaluation(app1.getId(), BuildStageType.ID, "now", new Date());
    releaseEval2DaysAgo = tempEntity
        .newPolicyEvaluation(app2.getId(), ReleaseStageType.ID, "2dAgo", now().minusDays(2).toDate());
    operateEval1WeekAgo = tempEntity
        .newPolicyEvaluation(app1.getId(), OperateStageType.ID, "1yAgo", now().minusWeeks(1).toDate());
    buildComponent = tempEntity
        .newApplicationComponent(app1.getId(), BuildStageType.ID, "g1a1v1",
            ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"));
    releaseComponent = tempEntity
        .newApplicationComponent(app2.getId(), ReleaseStageType.ID, randomAlphanumeric(10),
            ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"));
    operateComponent = tempEntity
        .newApplicationComponent(app1.getId(), OperateStageType.ID, randomAlphanumeric(10),
            ComponentIdentifier.createMavenCoordinates("g3", "a3", "v3"));

    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    File reportZip = work.getReportFile(buildEvalNow.getApplicationId(), buildEvalNow.getScanId());
    FileUtils.copyURLToFile(getClass().getResource("/canned-reports/small-report.zip"), reportZip);
  }

  @Test
  public void testViolationsTable() {
    ViolationsResults table = DashboardPage.violationsView().results();

    // no results
    refreshOrOpen(DashboardPage.VIOLATIONS_URL);
    table.noDataMessage().shouldBe(visible).shouldHave(text(NO_DATA_MSG));

    // add a few violations
    tempEntity.newPolicyViolation(releaseEval2DaysAgo, licensePolicy, 1,
        LICENSE, releaseComponent.getComponentIdentifier(), releaseComponent.getHash(), FailActionType.ID);
    tempEntity.newPolicyViolation(operateEval1WeekAgo, licensePolicy, 3,
        LICENSE, operateComponent.getComponentIdentifier(), operateComponent.getHash(), FailActionType.ID);
    tempEntity.newPolicyViolation(releaseEval2DaysAgo, securityPolicy, 10,
        SECURITY, releaseComponent.getComponentIdentifier(), releaseComponent.getHash(), FailActionType.ID);
    tempEntity.newPolicyViolation(buildEvalNow, licensePolicy, 7,
        LICENSE, buildComponent.getComponentIdentifier(), buildComponent.getHash(), FailActionType.ID);

    refreshOrOpen(DashboardPage.VIOLATIONS_URL);
    showLowRiskViolations();

    DashboardPage.dashboardContainer().shouldBe(visible);
    table.maxResultsMessage().shouldNotBe(visible);

    table.violations().shouldHaveSize(4);

    // check the tile details
    ViolationTile firstViolation = table.firstViolation();
    firstViolation.threatBar().shouldHave(SEVERE);
    firstViolation.threatNumber().shouldHave(text("7"));
    firstViolation.policy().shouldHave(text(licensePolicy.getName())).hover();
    DashboardPage.tooltip().shouldBe(visible).shouldHave(text(licensePolicy.getName()));
    firstViolation.application().shouldHave(text(app1.getName())).hover();
    DashboardPage.tooltip().shouldBe(visible).shouldHave(text(app1.getName()));
    firstViolation.component().shouldHave(text("g1 : a1 : v1")).hover();
    DashboardPage.tooltip().shouldBe(visible).shouldHave(text("g1 : a1 : v1"));
    firstViolation.age().shouldHave(text("1min"));
    firstViolation.stageReport().shouldBe(DISABLED).shouldHave(text("Stage report"));
    firstViolation.releaseReport().shouldBe(DISABLED).shouldHave(text("Release report"));
    firstViolation.operateReport().shouldBe(DISABLED).shouldHave(text("Operate report"));

    // check the report link - opens new window
    firstViolation.buildReport().shouldNotBe(DISABLED).shouldHave(text("Build report (1min)")).click();
    switchToWindow(1);
    waitUntilUrl(ApplicationReportContainerPage.url(app1.getPublicId(), buildEvalNow.getScanId()));
    ApplicationReportContainerPage.getReportTitle().shouldHave(text(app1.getName() + now().toString(" - YYYY-MM-dd -") + " Build Report"));
    WebDriverRunner.getWebDriver().close();
    switchToWindow(0);
    waitUntilUrl(DashboardPage.VIOLATIONS_URL);

    // open component details and back
    DashboardComponentDetails dashboardComponentDetails = new DashboardComponentDetails();
    firstViolation.component().click();
    waitUntilUrl(DashboardComponentDetails.url("g1a1v1"));
    DashboardPage.dashboardContainer().shouldNotBe(visible);
    dashboardComponentDetails.header().shouldHave(text("g1 : a1 : v1"));
    Selenide.navigator.back();
    DashboardPage.dashboardContainer().shouldBe(visible);

    ViolationsHeaders headers = DashboardPage.violationsView().headers();

    // should be sorted by age
    firstViolation.shouldHave(text("1min"));
    table.lastViolation().shouldHave(text("7d"));
    headers.ageHeader().click();
    firstViolation.shouldHave(text("7d"));
    table.lastViolation().shouldHave(text("1min"));

    // sort by threat
    headers.threatHeader().click();
    table.violations().shouldHave(texts("10", "7", "3", "1"));
    headers.threatHeader().click();
    table.violations().shouldHave(texts("1", "3", "7", "10"));

    // sort by licensePolicy name
    headers.policyHeader().click();
    firstViolation.shouldHave(text("DashboardViolationsTestLicensePolicy"));
    table.lastViolation().shouldHave(text("DashboardViolationsTestSecurityPolicy"));
    headers.policyHeader().click();
    firstViolation.shouldHave(text("DashboardViolationsTestSecurityPolicy"));
    table.lastViolation().shouldHave(text("DashboardViolationsTestLicensePolicy"));

    // sort by application name
    headers.applicationHeader().click();
    firstViolation.shouldHave(text("Violations Test App1"));
    table.lastViolation().shouldHave(text("Violations Test App2"));
    headers.applicationHeader().click();
    firstViolation.shouldHave(text("Violations Test App2"));
    table.lastViolation().shouldHave(text("Violations Test App1"));

    // sort by component name
    headers.componentHeader().click();
    firstViolation.shouldHave(text("g1 : a1 : v1"));
    table.lastViolation().shouldHave(text("g3 : a3 : v3"));
    headers.componentHeader().click();
    firstViolation.shouldHave(text("g3 : a3 : v3"));
    table.lastViolation().shouldHave(text("g1 : a1 : v1"));
  }


  @Test
  public void testNewestRiskRedirectsToViolations() {
    refreshOrOpen(DashboardPage.NEWEST_RISK_URL);
    waitUntilUrl(DashboardPage.VIOLATIONS_URL);
  }

  @Test
  public void testShouldNotShowMaxResultsMessageWhen100Results() {
    createViolations(100, 5, buildEvalNow);
    refreshOrOpen(DashboardPage.VIOLATIONS_URL);
    DashboardPage.dashboardContainer().shouldBe(visible);
    DashboardPage.violationsView().results().maxResultsMessage().shouldNotBe(visible);
  }

  @Test
  public void testShouldShowMaxResultsMessageWhen101Results() {
    createViolations(101, 5, buildEvalNow);
    refreshOrOpen(DashboardPage.VIOLATIONS_URL);
    DashboardPage.dashboardContainer().shouldBe(visible);
    DashboardPage.violationsView().results().maxResultsMessage().shouldBe(visible).shouldHave(text(MAX_RESULTS_MSG));
  }

  private void createViolations(int numViolations, int threatLevel, PolicyEvaluation policyEvaluation) {
    for (int i = 1; i <= numViolations; i++) {
      String group = "Group" + i;
      String artifact = "artifact" + i;
      String version = "version" + i;
      String hash = randomAlphanumeric(20);

      tempEntity.newPolicyViolation(policyEvaluation, licensePolicy, threatLevel,
          LICENSE, group, artifact, version, hash, FailActionType.ID);
    }
  }

  private void showLowRiskViolations() {
    DashboardFilters.policyThreatLevelFilter().twisty().click();
    DashboardFilters.policyThreatLevelFilter().slider().setValues(0, 10);
    DashboardFilters.applyButton().click();
  }
}
