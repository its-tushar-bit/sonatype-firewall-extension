/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.labs;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.RootOrganizationSuccessMetricsPage;
import com.sonatype.clm.testing.functional.pages.RootOrganizationSuccessMetricsPage.MttrTile;
import com.sonatype.clm.testing.functional.pages.RootOrganizationSuccessMetricsPage.SummaryStatementTile;
import com.sonatype.clm.testing.functional.pages.RootOrganizationSuccessMetricsPage.ViolationAveragesTile;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;

import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.dto.model.component.ComponentIdentifier.createMavenCoordinates;
import static com.sonatype.clm.testing.functional.pages.RootOrganizationSuccessMetricsPage.NO_DATA_INFO_TEXT;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.SECURITY;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.joda.time.DateTime.now;

public class RootOrgSuccessMetricsPoCTest
    extends AbstractFunctionalTest
{
  private RootOrganizationSuccessMetricsPage rootOrganizationSuccessMetricsPage = new RootOrganizationSuccessMetricsPage();

  private Application app;
  private Policy securityPolicy;

  @Before
  public void before() {
    app = tempEntity.newApplicationWithParent("pocApp", "SuccessMetricsPoCTestApp");
    securityPolicy = tempEntity.newPolicy(app.getParentOwnerId(), "SuccessMetricsPoCTestSecurityPolicy");
  }

  @After
  public void after() {
    DateTimeUtils.setCurrentMillisSystem();
  }

  @Test
  public void testPoCMode() {
    DateTime fakeNow = setTimeTo(now().withDayOfMonth(20));

    refreshOrOpen(RootOrganizationSuccessMetricsPage.URL);
    loginAsAdmin();
    rootOrganizationSuccessMetricsPage.noDataInfoPane().shouldBe(visible).shouldHave(NO_DATA_INFO_TEXT);

    fakeNow = setTimeTo(fakeNow.plusDays(1));

    List<PolicyViolation> existingViolations = new ArrayList<>();
    createModerateViolations(createEvaluation(fakeNow), 1, existingViolations);

    refreshOrOpen(RootOrganizationSuccessMetricsPage.URL);
    rootOrganizationSuccessMetricsPage.noDataInfoPane().shouldNotBe(visible);

    SummaryStatementTile.months().shouldBe(visible).shouldHave(text("1 month"));
    ViolationAveragesTile.averageEvaluations().shouldHave(text("1"));
    ViolationAveragesTile.averagePolicyViolations().shouldHave(text("1"));
    ViolationAveragesTile.averageCriticalPolicyViolations().shouldHave(text("0"));
    MttrTile.root().shouldNotBe(visible);

    // More violations this month, but don't look at metrics until middle of next month.
    createModerateViolations(createEvaluation(fakeNow), 3, existingViolations);

    fakeNow = setTimeTo(fakeNow.plusMonths(1).withDayOfMonth(15));

    createCriticalViolations(createEvaluation(fakeNow), 2, existingViolations);

    refreshOrOpen(RootOrganizationSuccessMetricsPage.URL);

    SummaryStatementTile.months().shouldBe(visible).shouldHave(text("2 months"));
    ViolationAveragesTile.averageEvaluations().shouldHave(text("2"));
    ViolationAveragesTile.averagePolicyViolations().shouldHave(text("3"));
    ViolationAveragesTile.averageCriticalPolicyViolations().shouldHave(text("1"));
    MttrTile.root().shouldNotBe(visible);

    // More violations this month, but don't look at metrics until start of calendar month 3.
    createCriticalViolations(createEvaluation(fakeNow), 15, existingViolations);
    setTimeTo(fakeNow.plusMonths(1).withDayOfMonth(1));

    refreshOrOpen(RootOrganizationSuccessMetricsPage.URL);
    // We are now in normal mode, showing last two full months of data.

    SummaryStatementTile.months().shouldBe(visible).shouldHave(text("2 months"));
    ViolationAveragesTile.averageEvaluations().shouldHave(text("2"));
    ViolationAveragesTile.averagePolicyViolations().shouldHave(text("11"));
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
    createViolations(eval, numNewViolations, 10, existingViolations);
  }

  private void createModerateViolations(PolicyEvaluation eval,
                                        int numNewViolations,
                                        List<PolicyViolation> existingViolations)
  {
    createViolations(eval, numNewViolations, 5, existingViolations);
  }

  private void createViolations(PolicyEvaluation eval,
                                int numViolations,
                                int threatLevel,
                                List<PolicyViolation> existingViolations)
  {
    for (PolicyViolation violation : existingViolations) {
      tempEntity.newPolicyViolation(eval, securityPolicy, violation.getThreatLevel(), SECURITY,
          violation.getComponentIdentifier(), violation.getHash(), FailActionType.ID);
    }
    for (int i = 0; i < numViolations; i++) {
      ApplicationComponent component = tempEntity
          .newApplicationComponent(app.getId(), ReleaseStageType.ID, randomAlphanumeric(10),
              createMavenCoordinates("g", "a", randomAlphanumeric(5)));

      existingViolations.add(
          tempEntity.newPolicyViolation(eval, securityPolicy, threatLevel, SECURITY, component.getComponentIdentifier(),
              component.getHash(), FailActionType.ID));
    }
  }

  private DateTime setTimeTo(DateTime fakeNow) {
    DateTimeUtils.setCurrentMillisFixed(fakeNow.getMillis());
    return fakeNow;
  }
}
