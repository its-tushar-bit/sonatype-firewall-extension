/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.labs;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.SuccessMetricsChartPage;
import com.sonatype.clm.testing.functional.pages.SuccessMetricsChartPage.MttrTile;
import com.sonatype.clm.testing.functional.pages.SuccessMetricsChartPage.SummaryStatementTile;
import com.sonatype.clm.testing.functional.pages.SuccessMetricsChartPage.ViolationAveragesTile;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.successmetrics.SuccessMetrics;
import com.sonatype.insight.brain.successmetrics.SuccessMetricsScopeDTO;
import com.sonatype.insight.json.store.JsonUtils;

import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.dto.model.component.ComponentIdentifier.createMavenCoordinates;
import static com.sonatype.clm.testing.functional.pages.SuccessMetricsChartPage.NO_DATA_INFO_TEXT;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.SECURITY;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.joda.time.DateTime.now;

public class SuccessMetricsPoCTest
    extends AbstractFunctionalTest
{
  private SuccessMetricsChartPage successMetricsChartsPage = new SuccessMetricsChartPage();

  private Application app;

  private Policy securityPolicy;

  private SuccessMetrics successMetrics;

  private String successMetricsChartsPageUrl;

  @Before
  public void before() {
    app = tempEntity.newApplicationWithParent("pocApp", "SuccessMetricsPoCTestApp");
    securityPolicy = tempEntity.newPolicy(app.getParentOwnerId(), "SuccessMetricsPoCTestSecurityPolicy");

    successMetrics = tempEntity.newSuccessMetrics("admin", "Test Success Metric",
        JsonUtils.format(new SuccessMetricsScopeDTO()));

    successMetricsChartsPageUrl = SuccessMetricsChartPage.getUrl(successMetrics.getId());
  }

  @After
  public void after() {
    DateTimeUtils.setCurrentMillisSystem();
  }

  @Test
  public void testPoCMode() {
    DateTime fakeNow = setTimeTo(now().withDayOfMonth(20));

    refreshOrOpen(successMetricsChartsPageUrl);
    loginAsAdmin();
    successMetricsChartsPage.noDataInfoPane().shouldBe(visible).shouldHave(NO_DATA_INFO_TEXT);

    List<PolicyViolation> existingViolations = new ArrayList<>();
    createCriticalViolations(createEvaluation(fakeNow), 1, existingViolations);

    // The violation was discovered today, so no success metrics should be visible yet.
    refreshOrOpen(successMetricsChartsPageUrl);
    successMetricsChartsPage.noDataInfoPane().shouldBe(visible);

    // It should appear in results the next day.
    fakeNow = setTimeTo(fakeNow.plusDays(1));
    refreshOrOpen(successMetricsChartsPageUrl);
    successMetricsChartsPage.noDataInfoPane().shouldNotBe(visible);
    SummaryStatementTile.months().shouldBe(visible).shouldHave(text("1 month"));
    ViolationAveragesTile.averageEvaluations().shouldHave(text("1"));
    ViolationAveragesTile.averagePolicyViolations().shouldHave(text("1"));
    ViolationAveragesTile.averageCriticalPolicyViolations().shouldHave(text("1"));
    MttrTile.root().shouldNotBe(visible);

    // Roll over to next month.
    fakeNow = setTimeTo(fakeNow.plusMonths(1).withDayOfMonth(15));

    createCriticalViolations(createEvaluation(fakeNow), 3, existingViolations);

    fakeNow = setTimeTo(fakeNow.plusDays(1));
    refreshOrOpen(successMetricsChartsPageUrl);

    SummaryStatementTile.months().shouldBe(visible).shouldHave(text("2 months"));
    ViolationAveragesTile.averageEvaluations().shouldHave(text("1"));
    ViolationAveragesTile.averagePolicyViolations().shouldHave(text("2"));
    ViolationAveragesTile.averageCriticalPolicyViolations().shouldHave(text("2"));
    MttrTile.root().shouldNotBe(visible);

    // More violations this month, but don't look at metrics again until start of calendar month 3.
    createCriticalViolations(createEvaluation(fakeNow), 14, existingViolations);

    // Roll over to next month - out of PoC mode.
    setTimeTo(fakeNow.plusMonths(1).withDayOfMonth(1));

    refreshOrOpen(successMetricsChartsPageUrl);

    // We are now in normal mode, showing last two full months of data, including MTTR chart.
    SummaryStatementTile.months().shouldBe(visible).shouldHave(text("2 months"));
    ViolationAveragesTile.averageEvaluations().shouldHave(text("2"));
    ViolationAveragesTile.averagePolicyViolations().shouldHave(text("9"));
    ViolationAveragesTile.averageCriticalPolicyViolations().shouldHave(text("9"));
    MttrTile.root().shouldBe(visible);
  }

  private PolicyEvaluation createEvaluation(DateTime fakeNow) {
    return tempEntity.newPolicyEvaluation(app.getId(), ReleaseStageType.ID, randomAlphanumeric(10), fakeNow.toDate());

  }

  private void createCriticalViolations(PolicyEvaluation eval,
                                        int numNewViolations,
                                        List<PolicyViolation> existingViolations)
  {
    for (PolicyViolation violation : existingViolations) {
      tempEntity.newPolicyViolation(eval, securityPolicy, violation.getThreatLevel(), SECURITY,
          violation.getComponentIdentifier(), violation.getHash(), FailActionType.ID);
    }
    for (int i = 0; i < numNewViolations; i++) {
      ApplicationComponent component = tempEntity
          .newApplicationComponent(app.getId(), ReleaseStageType.ID, randomAlphanumeric(10),
              createMavenCoordinates("g", "a", randomAlphanumeric(5)));

      existingViolations.add(
          tempEntity.newPolicyViolation(eval, securityPolicy, 10, SECURITY, component.getComponentIdentifier(),
              component.getHash(), FailActionType.ID));
    }
  }

  private DateTime setTimeTo(DateTime fakeNow) {
    DateTimeUtils.setCurrentMillisFixed(fakeNow.getMillis());
    return fakeNow;
  }
}
