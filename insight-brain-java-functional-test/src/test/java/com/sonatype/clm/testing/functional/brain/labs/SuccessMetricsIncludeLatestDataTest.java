/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.labs;

import java.util.Date;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.AddSuccessMetricsModal;
import com.sonatype.clm.testing.functional.pages.SuccessMetricsReportListPage;
import com.sonatype.clm.testing.functional.pages.SuccessMetricsReportPage;
import com.sonatype.clm.testing.functional.pages.SuccessMetricsReportPage.MttrTile;
import com.sonatype.clm.testing.functional.pages.SuccessMetricsReportPage.SummaryStatementTile;
import com.sonatype.clm.testing.functional.pages.SuccessMetricsReportPage.ViolationAveragesTile;
import com.sonatype.insight.brain.dataaccess.successmetrics.SuccessMetricsReportDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.successmetrics.SuccessMetricsReport;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.pages.SuccessMetricsReportPage.NO_DATA_INFO_TEXT_LATEST;

public class SuccessMetricsIncludeLatestDataTest
    extends AbstractFunctionalTest
{
  private SuccessMetricsReportPage successMetricsChartsPage = new SuccessMetricsReportPage();

  private SuccessMetricsReportListPage successMetricsReportListPage = new SuccessMetricsReportListPage();

  private AddSuccessMetricsModal addSuccessMetricsModal = new AddSuccessMetricsModal();

  private Application app;

  private Policy policy;

  @Before
  public void before() {
    app = tempEntity.newApplicationWithParent("pocApp", "SuccessMetricsPoCTestApp");
    policy = tempEntity.newPolicy(app.getParentOwnerId());
  }

  @After
  public void after() {
    SuccessMetricsReportDAO dao = new SuccessMetricsReportDAO();
    for (SuccessMetricsReport successMetricsReport : dao.getByUsername("admin")) {
      dao.delete(successMetricsReport);
    }
  }

  @Test
  public void testIncludeLatestData() {
    refreshOrOpen(SuccessMetricsReportListPage.url());
    loginAsAdmin();

    // Create Success Metrics report with latest data.
    successMetricsReportListPage.addSuccessMetricsBtn().shouldBe(visible).click();
    addSuccessMetricsModal.shouldBe(visible);
    addSuccessMetricsModal.name().setValue("Test Latest Data");
    addSuccessMetricsModal.byMostRecentWarning().shouldBe(hidden);
    addSuccessMetricsModal.includingMostRecentEvaluations().shouldNotBe(selected).click();
    addSuccessMetricsModal.byMostRecentWarning().shouldBe(visible)
        .shouldHave(AddSuccessMetricsModal.ON_LOAD_WARNING_TEXT);
    addSuccessMetricsModal.createBtn().click();

    successMetricsReportListPage.successMetricsChartActionItems().elements().shouldHaveSize(1);
    successMetricsReportListPage.successMetricsChartActionItems().element(0).shouldHave(text("Test Latest Data"))
        .click();

    successMetricsChartsPage.shouldBe(visible);
    successMetricsChartsPage.noDataInfoPane().shouldBe(visible).shouldHave(NO_DATA_INFO_TEXT_LATEST);

    PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(app.getId(), ReleaseStageType.ID, "abc", new Date());
    tempEntity.newPolicyViolation(eval1, policy);

    // With latest data flag the new violation should appear immediately.
    refresh();
    successMetricsChartsPage.noDataInfoPane().shouldBe(hidden);
    SummaryStatementTile.months().shouldBe(visible).shouldHave(text("1 month"));
    ViolationAveragesTile.averageEvaluations().shouldHave(text("1"));
    ViolationAveragesTile.averagePolicyViolations().shouldHave(text("1"));
    MttrTile.root().shouldBe(visible);
  }
}
