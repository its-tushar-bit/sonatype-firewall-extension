/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.labs;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.SuccessMetricsReportPage;
import com.sonatype.clm.testing.functional.pages.SuccessMetricsReportPage.ApplicationCountsTile;
import com.sonatype.clm.testing.functional.pages.SuccessMetricsReportPage.ComponentCountsTile;
import com.sonatype.clm.testing.functional.pages.SuccessMetricsReportPage.Header;
import com.sonatype.clm.testing.functional.pages.SuccessMetricsReportPage.MttrTile;
import com.sonatype.clm.testing.functional.pages.SuccessMetricsReportPage.ViolationAveragesTile;
import com.sonatype.clm.testing.functional.pages.SuccessMetricsReportPage.ViolationsByCategoryTile;
import com.sonatype.clm.testing.functional.utils.ScrollUtil;
import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.successmetrics.SuccessMetricsReport;
import com.sonatype.insight.brain.successmetrics.SuccessMetricsReportScopeDTO;
import com.sonatype.insight.json.store.JsonUtils;

import org.joda.time.DateTime;
import org.junit.After;
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

  @BeforeClass
  public static void startup() {
    refreshOrOpen(DashboardPage.url());
    loginAsAdmin();
  }

  @Before
  public void navigate() {
    Application app1 = tempEntity.newApplicationWithParent("app1", "SuccessMetricsChart Test App1");

    Policy licensePolicy = tempEntity.newPolicy(app1.getParentOwnerId());

    PolicyEvaluation buildEval3MonthsAgo = tempEntity
        .newPolicyEvaluation(app1.getId(), BuildStageType.ID, "threeMonthsAgo", threeMonthsAgo.toDate());

    ApplicationComponent buildComponent = tempEntity
        .newApplicationComponent(app1.getId(), BuildStageType.ID, "logbackhash",
            ComponentIdentifier.createMavenCoordinates("ch.qos.logback", "logback-access", "0.6"));

    // add a violation from a few months ago so we can see the mttr chart/break out of PoC mode
    tempEntity.newPolicyViolation(buildEval3MonthsAgo, licensePolicy, 6,
        LICENSE, buildComponent.getComponentIdentifier(), buildComponent.getHash(), FailActionType.ID);

    SuccessMetricsReportScopeDTO successMetricsScope = new SuccessMetricsReportScopeDTO();
    successMetricsScope.applicationIds = new HashSet<>(Collections.singleton(app1.getId()));

    SuccessMetricsReport successMetrics = tempEntity.newSuccessMetricsReport("admin", "Test",
        JsonUtils.format(successMetricsScope));

    refreshOrOpen(SuccessMetricsReportPage.url(successMetrics.getId()));
  }

  @After
  public void after() {
    unsetSuccessMetricsStageId();
  }

  @Test
  public void testHeader_whenStageIdNull() {
    SuccessMetricsReportPage successMetricsReportPage = new SuccessMetricsReportPage();

    successMetricsReportPage.should(appear);
    Header.root().shouldBe(visible);
    Header.title().shouldHave(text("Test"));
    Header.description()
        .shouldHave(text("This report contains data for 1 application, evaluated over the "
            + "past 3 months, aggregated and deduplicated over the source, build, stage release, release, and operate "
            + "stages."));
  }

  @Test
  public void testHeader_whenStageIdSet() {
    setSuccessMetricsStageId("build");
    SuccessMetricsReportPage successMetricsReportPage = new SuccessMetricsReportPage();

    successMetricsReportPage.should(appear);
    Header.root().shouldBe(visible);
    Header.title().shouldHave(text("Test"));
    Header.description()
        .shouldHave(text("This report contains data for 1 application, evaluated over the past 3 months, for " +
            "evaluations of the build stage."));
  }

  @Test
  public void testViolationCountsTile() {
    SuccessMetricsReportPage successMetricsReportPage = new SuccessMetricsReportPage();

    successMetricsReportPage.should(appear);

    ScrollUtil.scrollIntoView(ViolationsByCategoryTile.root());
    ViolationsByCategoryTile.root().shouldBe(visible);
    ViolationsByCategoryTile.chart().shouldBe(visible);
  }

  @Test
  public void testViolationAveragesTile() {
    SuccessMetricsReportPage successMetricsReportPage = new SuccessMetricsReportPage();

    successMetricsReportPage.should(appear);
    ViolationAveragesTile.root().shouldBe(visible);
    ViolationAveragesTile.title()
        .shouldHave(text("Average Number of Violations Discovered Per Month"));
    ViolationAveragesTile.averages()
        .shouldHave(text("Lifecycle performed an average of 0 evaluations per month on 1 " +
            "application over the past 3 months. Lifecycle found an average of 0 policy violations, 0 of which were " +
            "critical."));
  }

  @Test
  public void testApplicationCountsTile() {
    SuccessMetricsReportPage successMetricsReportPage = new SuccessMetricsReportPage();

    successMetricsReportPage.should(appear);
    ApplicationCountsTile.root().shouldNot(exist);
  }

  @Test
  public void testMttrTile() {
    SuccessMetricsReportPage successMetricsReportPage = new SuccessMetricsReportPage();

    successMetricsReportPage.should(appear);

    ScrollUtil.scrollIntoView(MttrTile.root());
    MttrTile.root().shouldBe(visible);
    MttrTile.chart().shouldBe(visible);
  }

  @Test
  public void testComponentCountsTile() {
    SuccessMetricsReportPage successMetricsReportPage = new SuccessMetricsReportPage();

    successMetricsReportPage.should(appear);

    ScrollUtil.scrollIntoView(ComponentCountsTile.root());
    ComponentCountsTile.root().shouldBe(visible);

    ComponentCountsTile.averages()
        .shouldHave(text("SuccessMetricsChart Test App1 contains 1 components."));
  }

  private void setSuccessMetricsStageId(final String stageId) {
    ApiConfigurationService configurationService =
        testCLMServer.getCLMServer().getInstance(ApiConfigurationService.class);
    configurationService.setConfigurationNoAuthz(
        SystemConfigurationProperty.SUCCESS_METRICS_STAGE_ID,
        stageId);
  }

  private void unsetSuccessMetricsStageId() {
    ApiConfigurationService configurationService =
        testCLMServer.getCLMServer().getInstance(ApiConfigurationService.class);
    configurationService.deleteConfigurationNoAuthz(Set.of(SystemConfigurationProperty.SUCCESS_METRICS_STAGE_ID));
  }
}
