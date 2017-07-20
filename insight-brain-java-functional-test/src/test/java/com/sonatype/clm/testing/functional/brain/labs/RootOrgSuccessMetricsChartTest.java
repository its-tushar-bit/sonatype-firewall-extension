/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.labs;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.RootOrganizationSuccessMetricsPage;
import com.sonatype.clm.testing.functional.pages.RootOrganizationSuccessMetricsPage.ApplicationCountsTile;
import com.sonatype.clm.testing.functional.pages.RootOrganizationSuccessMetricsPage.MttrTile;
import com.sonatype.clm.testing.functional.pages.RootOrganizationSuccessMetricsPage.SummaryStatementTile;
import com.sonatype.clm.testing.functional.pages.RootOrganizationSuccessMetricsPage.ViolationAveragesTile;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;

import com.codeborne.selenide.ElementsCollection;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.appear;
import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.LICENSE;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.OTHER;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.QUALITY;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.SECURITY;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.joda.time.DateTime.now;

public class RootOrgSuccessMetricsChartTest
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

  @BeforeClass
  public static void startup() {
    Application app1 = staticTempEntity.newApplicationWithParent("app1", "SuccessMetricsChart Test App1");
    Application app2 = staticTempEntity.newApplicationWithParent("app2", "SuccessMetricsChart Test App2");
    Policy licensePolicy = staticTempEntity.newPolicy(app1.getParentOwnerId(), "SuccessMetricsChartTestLicensePolicy");
    Policy securityPolicy = staticTempEntity.newPolicy(app2.getParentOwnerId(),
        "SuccessMetricsChartTestSecurityPolicy");
    Policy qualityPolicy = staticTempEntity.newPolicy(app2.getParentOwnerId(), "SuccessMetricsChartTestQualityPolicy");
    Policy otherPolicy = staticTempEntity.newPolicy(app2.getParentOwnerId(), "SuccessMetricsChartTestOtherPolicy");

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


    ApplicationComponent buildComponent = staticTempEntity
        .newApplicationComponent(app1.getId(), BuildStageType.ID, "g1a1v1",
            ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"));
    ApplicationComponent releaseComponent = staticTempEntity
        .newApplicationComponent(app2.getId(), ReleaseStageType.ID, randomAlphanumeric(10),
            ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"));

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

    refreshOrOpen(RootOrganizationSuccessMetricsPage.URL);
    loginAsAdmin();
  }

  @Before
  public void navigate() {
    refreshOrOpen(RootOrganizationSuccessMetricsPage.URL);
  }

  @Test
  public void testSummaryStatementTile() {
    RootOrganizationSuccessMetricsPage rootOrganizationSuccessMetricsPage = new RootOrganizationSuccessMetricsPage();

    rootOrganizationSuccessMetricsPage.should(appear);
    SummaryStatementTile.root().shouldBe(visible);
    SummaryStatementTile.activeApplicationsCount().shouldBe(visible).shouldBe(text("2"));
    SummaryStatementTile.months().shouldBe(visible).shouldHave(text("4 months"));
  }

  @Test
  public void testViolationAveragesTile() {
    RootOrganizationSuccessMetricsPage rootOrganizationSuccessMetricsPage = new RootOrganizationSuccessMetricsPage();

    rootOrganizationSuccessMetricsPage.should(appear);
    ViolationAveragesTile.root().shouldBe(visible);
    ViolationAveragesTile.averageEvaluations().shouldBe(visible).shouldBe(text("1"));
    ViolationAveragesTile.averagePolicyViolations().shouldBe(visible).shouldBe(text("2"));
    ViolationAveragesTile.averageCriticalPolicyViolations().shouldBe(visible).shouldBe(text("1"));
  }

  @Test
  public void testApplicationCountsTile() throws Exception {
    RootOrganizationSuccessMetricsPage rootOrganizationSuccessMetricsPage = new RootOrganizationSuccessMetricsPage();

    ApplicationCountsTile.root().scrollTo();

    rootOrganizationSuccessMetricsPage.should(appear);
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
    RootOrganizationSuccessMetricsPage rootOrganizationSuccessMetricsPage = new RootOrganizationSuccessMetricsPage();

    MttrTile.root().scrollTo();

    rootOrganizationSuccessMetricsPage.should(appear);
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
}
