/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.labs;

import java.util.Collections;
import java.util.HashSet;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.SuccessMetricsChartPage;
import com.sonatype.clm.testing.functional.pages.SuccessMetricsChartPage.ApplicationCountsTile;
import com.sonatype.clm.testing.functional.pages.SuccessMetricsChartPage.ComponentCountsTile;
import com.sonatype.clm.testing.functional.pages.SuccessMetricsChartPage.MttrTile;
import com.sonatype.clm.testing.functional.pages.SuccessMetricsChartPage.SummaryStatementTile;
import com.sonatype.clm.testing.functional.pages.SuccessMetricsChartPage.ViolationAveragesTile;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.successmetrics.SuccessMetrics;
import com.sonatype.insight.brain.successmetrics.SuccessMetricsScopeDTO;
import com.sonatype.insight.json.store.JsonUtils;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.appear;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.LICENSE;
import static org.joda.time.DateTime.now;

public class SuccessMetricsChartsSingleApplicationTest
    extends AbstractFunctionalTest
{
  private static final DateTime threeMonthsAgo = now().minusMonths(3);

  private static String successMetricsChartsPageUrl;

  @BeforeClass
  public static void startup() {
    Application app1 = staticTempEntity.newApplicationWithParent("app1", "SuccessMetricsChart Test App1");

    Policy licensePolicy = staticTempEntity.newPolicy(app1.getParentOwnerId(), "SuccessMetricsChartTestLicensePolicy");

    PolicyEvaluation buildEval3MonthsAgo = staticTempEntity
        .newPolicyEvaluation(app1.getId(), BuildStageType.ID, "threeMonthsAgo", threeMonthsAgo.toDate());

    ApplicationComponent buildComponent = staticTempEntity
        .newApplicationComponent(app1.getId(), BuildStageType.ID, "logbackhash",
            ComponentIdentifier.createMavenCoordinates("ch.qos.logback", "logback-access", "0.6"));
    
    // add a violation from a few months ago so we can see the mttr chart/break out of PoC mode
    staticTempEntity.newPolicyViolation(buildEval3MonthsAgo, licensePolicy, 6,
        LICENSE, buildComponent.getComponentIdentifier(), buildComponent.getHash(), FailActionType.ID);

    SuccessMetricsScopeDTO successMetricsScope = new SuccessMetricsScopeDTO();
    successMetricsScope.applicationIds = new HashSet<>(Collections.singleton(app1.getId()));

    SuccessMetrics successMetrics = staticTempEntity.newSuccessMetrics("admin", "Test",
        JsonUtils.format(successMetricsScope));

    successMetricsChartsPageUrl = SuccessMetricsChartPage.getUrl(successMetrics.getId());

    refreshOrOpen(successMetricsChartsPageUrl);
    loginAsAdmin();
  }

  @Before
  public void navigate() {
    refreshOrOpen(successMetricsChartsPageUrl);
  }

  @Test
  public void testSummaryStatementTile() {
    SuccessMetricsChartPage successMetricsChartsPage = new SuccessMetricsChartPage();

    successMetricsChartsPage.should(appear);
    SummaryStatementTile.root().shouldBe(visible);
    SummaryStatementTile.title().shouldHave(text("Test"));
    SummaryStatementTile.averages().shouldHave(text("Over the last 3 months, Lifecycle evaluated 1 application."));
  }

  @Test
  public void testViolationAveragesTile() {
    SuccessMetricsChartPage successMetricsChartsPage = new SuccessMetricsChartPage();

    successMetricsChartsPage.should(appear);
    ViolationAveragesTile.root().shouldBe(visible);
    ViolationAveragesTile.title()
        .shouldHave(text("Average Number of Violations Discovered Per Month"));
    ViolationAveragesTile.averages().shouldHave(text(
        "On average Lifecycle performed 0 evaluations per month, finding 0 policy violations, of which 0 were critical."));
  }

  @Test
  public void testApplicationCountsTile() {
    SuccessMetricsChartPage successMetricsChartsPage = new SuccessMetricsChartPage();
    
    successMetricsChartsPage.should(appear);
    ApplicationCountsTile.root().shouldNot(exist);
  }

  @Test
  public void testMttrTile() {
    SuccessMetricsChartPage successMetricsChartsPage = new SuccessMetricsChartPage();

    successMetricsChartsPage.should(appear);

    MttrTile.root().scrollTo();
    MttrTile.root().shouldBe(visible);
    MttrTile.chart().shouldBe(visible);
  }

  @Test
  public void testComponentCountsTile() {
    SuccessMetricsChartPage successMetricsChartsPage = new SuccessMetricsChartPage();

    successMetricsChartsPage.should(appear);

    ComponentCountsTile.root().scrollTo();
    ComponentCountsTile.root().shouldBe(visible);

    ComponentCountsTile.averages()
        .shouldHave(text("SuccessMetricsChart Test App1 contains 1 components."));
  }
}
