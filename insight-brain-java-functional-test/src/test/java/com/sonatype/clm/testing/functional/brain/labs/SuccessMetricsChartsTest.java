/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.labs;

import java.util.Arrays;
import java.util.HashSet;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.Tooltip;
import com.sonatype.clm.testing.functional.pages.SuccessMetricsReportPage;
import com.sonatype.clm.testing.functional.pages.SuccessMetricsReportPage.ApplicationCountsTile;
import com.sonatype.clm.testing.functional.pages.SuccessMetricsReportPage.ComponentCountsTile;
import com.sonatype.clm.testing.functional.pages.SuccessMetricsReportPage.MttrTile;
import com.sonatype.clm.testing.functional.pages.SuccessMetricsReportPage.SummaryStatementTile;
import com.sonatype.clm.testing.functional.pages.SuccessMetricsReportPage.ViolationAveragesTile;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.successmetrics.SuccessMetricsReport;
import com.sonatype.insight.brain.successmetrics.SuccessMetricsReportScopeDTO;
import com.sonatype.insight.json.store.JsonUtils;

import com.codeborne.selenide.ElementsCollection;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.appear;
import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.pages.SuccessMetricsReportPage.NO_DATA_INFO_TEXT_MONTHLY;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.LICENSE;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.OTHER;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.QUALITY;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.SECURITY;
import static org.joda.time.DateTime.now;

public class SuccessMetricsChartsTest
    extends AbstractFunctionalTest
{
  private static final String FILL_ATTRIBUTE = "fill";

  private static final String STROKE_ATTRIBUTE = "stroke";

  private static final String ALL_COLOR = "rgb(82, 121, 199)";

  private static final String CRITICAL_COLOR = "rgb(253, 55, 62)";

  private static final DateTime fourMonthsAgo = now().minusMonths(4);

  private static final DateTime threeMonthsAgo = now().minusMonths(3);

  private static final DateTime twoMonthsAgo = now().minusMonths(2);

  private static final DateTime oneMonthAgo = now().minusMonths(1);

  private static String successMetricsChartsPageUrl;

  @BeforeClass
  public static void startup() {
    Application app1 = staticTempEntity.newApplicationWithParent("app1", "SuccessMetricsChart Test App1");
    Application app2 = staticTempEntity.newApplicationWithParent("app2", "SuccessMetricsChart Test App2");
    Application app3 = staticTempEntity.newApplicationWithParent("app3", "SuccessMetricsChart Test App3");

    Policy licensePolicy = staticTempEntity.newPolicy(app1.getParentOwnerId(), "SuccessMetricsChartTestLicensePolicy");
    Policy securityPolicy = staticTempEntity.newPolicy(app2.getParentOwnerId(),
        "SuccessMetricsChartTestSecurityPolicy");
    Policy qualityPolicy = staticTempEntity.newPolicy(app2.getParentOwnerId(), "SuccessMetricsChartTestQualityPolicy");
    Policy otherPolicy = staticTempEntity.newPolicy(app2.getParentOwnerId(), "SuccessMetricsChartTestOtherPolicy");
    Policy app3Policy = staticTempEntity.newPolicy(app3.getParentOwnerId(), "SuccessMetricsChartApp3Policy");

    PolicyEvaluation buildEval4MonthsAgo = staticTempEntity
        .newPolicyEvaluation(app1.getId(), BuildStageType.ID, "fourMonthsAgo", fourMonthsAgo.toDate());
    PolicyEvaluation releaseEval3MonthsAgo = staticTempEntity
        .newPolicyEvaluation(app2.getId(), ReleaseStageType.ID, "threeMonthsAgo", threeMonthsAgo.toDate());
    PolicyEvaluation buildEval2MonthsAgo = staticTempEntity
        .newPolicyEvaluation(app2.getId(), BuildStageType.ID, "twoMonthsAgo", twoMonthsAgo.toDate());
    PolicyEvaluation releaseEval2MonthsAgo = staticTempEntity
        .newPolicyEvaluation(app2.getId(), ReleaseStageType.ID, "twoMonthsAgo", twoMonthsAgo.toDate());
    PolicyEvaluation releaseEval1MonthAgo = staticTempEntity
        .newPolicyEvaluation(app2.getId(), ReleaseStageType.ID, "oneMonthAgo", oneMonthAgo.toDate());
    PolicyEvaluation app3Eval1 = staticTempEntity
        .newPolicyEvaluation(app3.getId(), BuildStageType.ID, "app3Eval1", fourMonthsAgo.toDate());
    staticTempEntity.newPolicyEvaluation(app3.getId(), BuildStageType.ID, "app3Eval2", threeMonthsAgo.toDate());

    ApplicationComponent buildComponent = staticTempEntity
        .newApplicationComponent(app1.getId(), BuildStageType.ID, "shortnamehash",
            ComponentIdentifier.createMavenCoordinates("short", "name", "0.6"));
    ApplicationComponent releaseComponent = staticTempEntity
        .newApplicationComponent(app2.getId(), ReleaseStageType.ID, "longnamehash",
            ComponentIdentifier.createMavenCoordinates("long.component.name.should.cause.tooltip", "artifact",
              "1.2.3.4"));

    // add a few violations
    staticTempEntity.newPolicyViolation(buildEval4MonthsAgo, licensePolicy, 7,
        LICENSE, buildComponent.getComponentIdentifier(), buildComponent.getHash(), FailActionType.ID);
    staticTempEntity.newPolicyViolation(releaseEval3MonthsAgo, securityPolicy, 8,
        SECURITY, releaseComponent.getComponentIdentifier(), releaseComponent.getHash(), FailActionType.ID);
    staticTempEntity.newPolicyViolation(releaseEval3MonthsAgo, licensePolicy, 1,
        LICENSE, releaseComponent.getComponentIdentifier(), releaseComponent.getHash(), FailActionType.ID);
    staticTempEntity.newPolicyViolation(releaseEval3MonthsAgo, securityPolicy, 9,
        SECURITY, releaseComponent.getComponentIdentifier(), releaseComponent.getHash(), FailActionType.ID);
    staticTempEntity.newPolicyViolation(releaseEval3MonthsAgo, securityPolicy, 10,
        SECURITY, releaseComponent.getComponentIdentifier(), releaseComponent.getHash(), FailActionType.ID);
    staticTempEntity.newPolicyViolation(releaseEval2MonthsAgo, securityPolicy, 9,
        SECURITY, releaseComponent.getComponentIdentifier(), releaseComponent.getHash(), FailActionType.ID);
    staticTempEntity.newPolicyViolation(releaseEval2MonthsAgo, securityPolicy, 10,
        SECURITY, releaseComponent.getComponentIdentifier(), releaseComponent.getHash(), FailActionType.ID);
    staticTempEntity.newPolicyViolation(buildEval2MonthsAgo, licensePolicy, 10,
        LICENSE, buildComponent.getComponentIdentifier(), buildComponent.getHash(), FailActionType.ID);
    staticTempEntity.newPolicyViolation(buildEval2MonthsAgo, licensePolicy, 7,
        LICENSE, buildComponent.getComponentIdentifier(), buildComponent.getHash(), FailActionType.ID);
    staticTempEntity.newPolicyViolation(buildEval2MonthsAgo, qualityPolicy, 7,
        QUALITY, buildComponent.getComponentIdentifier(), buildComponent.getHash(), FailActionType.ID);
    staticTempEntity.newPolicyViolation(buildEval2MonthsAgo, otherPolicy, 7,
        OTHER, buildComponent.getComponentIdentifier(), buildComponent.getHash(), FailActionType.ID);
    staticTempEntity.newPolicyViolation(releaseEval1MonthAgo, securityPolicy, 10,
        SECURITY, releaseComponent.getComponentIdentifier(), releaseComponent.getHash(), FailActionType.ID);
    staticTempEntity.newPolicyViolation(app3Eval1, app3Policy, 10,
        SECURITY, releaseComponent.getComponentIdentifier(), releaseComponent.getHash(), FailActionType.ID);

    SuccessMetricsReportScopeDTO successMetricsScope = new SuccessMetricsReportScopeDTO();
    successMetricsScope.organizationIds = new HashSet<>(Arrays.asList(app1.getParentOwnerId()));
    successMetricsScope.applicationIds = new HashSet<>(Arrays.asList(app1.getId(), app2.getId()));

    // Include app2 using its app id and app1 using its parent org id. Do not include app3.
    SuccessMetricsReport successMetricsReport = staticTempEntity.newSuccessMetricsReport("admin", "Test",
        JsonUtils.format(successMetricsScope));

    successMetricsChartsPageUrl = SuccessMetricsReportPage.getUrl(successMetricsReport.getId());

    refreshOrOpen(successMetricsChartsPageUrl);
    loginAsAdmin();
  }

  @Before
  public void navigate() {
    refreshOrOpen(successMetricsChartsPageUrl);
  }

  @Test
  public void testSummaryStatementTile() {
    SuccessMetricsReportPage successMetricsChartsPage = new SuccessMetricsReportPage();

    successMetricsChartsPage.should(appear);
    SummaryStatementTile.root().shouldBe(visible);
    SummaryStatementTile.title().shouldHave(text("Test"));
    String startOfMonth = DateTimeFormat.forPattern("MMM d, YYYY").print(LocalDate.now().withDayOfMonth(1));
    SummaryStatementTile.averages().shouldHave(
        text("Over the last 4 months, Lifecycle evaluated 2 applications. Last updated " + startOfMonth + "."));
  }

  @Test
  public void testViolationAveragesTile() {
    SuccessMetricsReportPage successMetricsChartsPage = new SuccessMetricsReportPage();

    successMetricsChartsPage.should(appear);
    ViolationAveragesTile.root().shouldBe(visible);
    ViolationAveragesTile.title()
        .shouldHave(text("Average Number of Violations Discovered Per Month, Per Application"));
    ViolationAveragesTile.averages().shouldHave(text(
        "On average Lifecycle performed 1 evaluations per month, finding 2 policy violations per application, of which 1 were critical."));
  }

  @Test
  public void testApplicationCountsTile() throws Exception {
    SuccessMetricsReportPage successMetricsChartsPage = new SuccessMetricsReportPage();

    ApplicationCountsTile.root().scrollTo();

    successMetricsChartsPage.should(appear);
    ApplicationCountsTile.root().shouldHave(visible);
    ApplicationCountsTile.activeApplicationsCount().shouldBe(visible).shouldHave(text("2"));
    ApplicationCountsTile.totalViolatingApplicationsCount().shouldBe(visible).shouldHave(text("2"));
    ApplicationCountsTile.totalCriticalViolatingApplicationsCount().shouldBe(visible).shouldHave(text("1"));
    ApplicationCountsTile.securityViolatingApplicationsCount().shouldBe(visible).shouldHave(text("1"));
    ApplicationCountsTile.securityCriticalViolatingApplicationsCount().shouldBe(visible).shouldHave(text("1"));
    ApplicationCountsTile.licenseViolatingApplicationsCount().shouldBe(visible).shouldHave(text("2"));
    ApplicationCountsTile.licenseCriticalViolatingApplicationsCount().shouldBe(visible).shouldHave(text("1"));
    ApplicationCountsTile.qualityViolatingApplicationsCount().shouldBe(visible).shouldHave(text("1"));
    ApplicationCountsTile.qualityCriticalViolatingApplicationsCount().shouldBe(visible).shouldHave(text("0"));
    ApplicationCountsTile.otherViolatingApplicationsCount().shouldBe(visible).shouldHave(text("1"));
    ApplicationCountsTile.otherCriticalViolatingApplicationsCount().shouldBe(visible).shouldHave(text("0"));
  }

  @Test
  public void testMttrTile() {
    SuccessMetricsReportPage successMetricsChartsPage = new SuccessMetricsReportPage();

    MttrTile.root().scrollTo();

    successMetricsChartsPage.should(appear);
    MttrTile.root().shouldBe(visible);
    MttrTile.chart().shouldBe(visible);

    ElementsCollection points = MttrTile.mttrPoints();
    points.shouldHaveSize(4);
    points.get(0).should(visible).shouldHave(attribute(FILL_ATTRIBUTE, ALL_COLOR));
    points.get(1).should(visible).shouldHave(attribute(FILL_ATTRIBUTE, ALL_COLOR));
    points.get(2).should(visible).shouldHave(attribute(FILL_ATTRIBUTE, CRITICAL_COLOR));
    points.get(3).should(visible).shouldHave(attribute(FILL_ATTRIBUTE, CRITICAL_COLOR));

    ElementsCollection lines = MttrTile.mttrLines();
    lines.shouldHaveSize(2);
    lines.get(0).should(visible).shouldHave(attribute(STROKE_ATTRIBUTE, ALL_COLOR));
    lines.get(1).should(visible).shouldHave(attribute(STROKE_ATTRIBUTE, CRITICAL_COLOR));

    ElementsCollection months = MttrTile.mttrXAxisLabels();
    months.shouldHaveSize(12);

    DateTime mttrMonth = now().minusMonths(12);
    for (int i = 0; i < 12; i++) {
      months.get(0).shouldBe(visible).shouldHave(text(mttrMonth.toString("MMM")));
      mttrMonth.plusMonths(1);
    }
  }

  @Test
  public void testComponentCountsTile() throws Exception {
    SuccessMetricsReportPage successMetricsChartsPage = new SuccessMetricsReportPage();

    ComponentCountsTile.root().scrollTo();

    successMetricsChartsPage.should(appear);
    ComponentCountsTile.root().shouldBe(visible);

    ComponentCountsTile.averages()
        .shouldHave(text("On average, there are 1 components per application."));
    ElementsCollection componentsInMostApplications = ComponentCountsTile.componentsInMostApplications();
    componentsInMostApplications.shouldHaveSize(2);
    ElementsCollection componentsWithMostViolations = ComponentCountsTile.componentsWithMostViolations();
    componentsWithMostViolations.shouldHaveSize(2);

    String[] componentGroupIdsInMostApplications = {
      "long.component.name.should.cause.tooltip : artifact : 1.2.3.4", "short : name : 0.6"
    };
    String expectedApplicationText = "1applications";
    componentsInMostApplications.shouldHave(texts(componentGroupIdsInMostApplications));
    componentsInMostApplications.shouldHave(
        texts(expectedApplicationText, expectedApplicationText));

    componentsInMostApplications.get(0)
        .shouldHave(text("long.component.name.should.cause.tooltip : artifact : 1.2.3.4")).hover();
    Tooltip.get().shouldBe(visible).shouldHave(text("long.component.name.should.cause.tooltip : artifact : 1.2.3.4"));

    componentsInMostApplications.get(1)
        .shouldHave(text("short : name : 0.6")).hover();

    // Tooltip is configured to appear after 300ms, so we need to wait at least that long to really make sure its
    // not going to appear.  Without this sleep we'd just be testing that it hasn't appeared _yet_.
    Thread.sleep(1000);
    Tooltip.get().shouldNotBe(visible);

    String[] componentGroupIdsWithMostViolations = {
      "short : name : 0.6", "long.component.name.should.cause.tooltip : artifact : 1.2.3.4",
    };
    componentsWithMostViolations.shouldHave(texts(componentGroupIdsWithMostViolations));
    componentsWithMostViolations.shouldHave(texts("5violations", "1violations"));
  }

  /**
   * Test that navigating to a SuccessMetricsReport that has a specific app/org selection, but where that app/org
   * selection has only invalid or unauthorized apps/orgs, causes "No Data" and not a totally unfiltered chart
   */
  @Test
  public void testNonMatchSuccessMetrics() {
    // create a SuccessMetricsReport with only non-existant app and org ids
    SuccessMetricsReportScopeDTO invalidScopeDTO = new SuccessMetricsReportScopeDTO();
    invalidScopeDTO.applicationIds = new HashSet<>(Arrays.asList("non-existent-app"));
    invalidScopeDTO.organizationIds = new HashSet<>(Arrays.asList("non-existent-org"));
    SuccessMetricsReport successMetricsReport = tempEntity.newSuccessMetricsReport("admin", "invalid metrics",
        JsonUtils.format(invalidScopeDTO));

    refreshOrOpen(SuccessMetricsReportPage.getUrl(successMetricsReport.getId()));

    SuccessMetricsReportPage successMetricsChartsPage = new SuccessMetricsReportPage();
    successMetricsChartsPage.should(appear);
    successMetricsChartsPage.noDataInfoPane().shouldBe(visible).shouldHave(NO_DATA_INFO_TEXT_MONTHLY);
  }
}
