/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.applicationreport;

import java.net.URL;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.AddProprietaryComponentMatchersPopover;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage;
import com.sonatype.clm.testing.functional.pages.ComponentDetailsPage;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.clm.testing.functional.utils.FormUtils;
import com.sonatype.clm.testing.functional.utils.TestReportEvaluator;
import com.sonatype.insight.brain.dataaccess.configuration.ProprietaryConfigDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.configuration.ProprietaryConfig;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.CoordinatesConditionType;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.SelenideElement;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.elements.CLM.DISABLED;
import static com.sonatype.clm.testing.functional.elements.CLM.NX_RADIO_SELECTED;
import static com.sonatype.clm.testing.functional.utils.FormUtils.DEFAULT_VALIDATION_ERRORS_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;

public class ProprietaryMatchersTest
    extends AbstractFunctionalTest
{
  private static final String POLICY_NAME = "All components";

  private static final String SCAN_ID = "306e0a923df34c64b836358182b1b902";

  private static final InsightWork WORK = new InsightWork(testCLMServer.getCLMServer().getConfiguration());

  private static final String CANNED_TEST_REPORT = "/canned-reports/report-with-unknown-and-proprietary";

  private final ApplicationReportPage reportPage = new ApplicationReportPage();

  private ProprietaryConfigDAO proprietaryConfigDAO;

  private Application app;

  @BeforeClass
  public static void startup() {
    refreshOrOpen(ReportListPage.url());
    loginAsAdmin();
  }

  @Before
  public void start() throws Exception {
    proprietaryConfigDAO = lookup(ProprietaryConfigDAO.class);

    app = tempEntity.newApplicationWithParent("AddProprietaryMatchersTest", "AddProprietaryMatchersTest");
    URL zippedReport = ReportHelper.zipReport(CANNED_TEST_REPORT, tempDir);
    TestReportEvaluator evaluator = new TestReportEvaluator(app, SCAN_ID, zippedReport, baseUrlFromTest, WORK);
    createGavViolatingPolicy(app.getOrganizationId());
    evaluator.evaluatePolicy();
  }

  @Test
  public void testAddProprietaryMatchersDialog() {
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
    reportPage.resultRows().shouldHave(size(3));
    reportPage.resultRow(3).click();

    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();
    waitUntilUrl(ComponentDetailsPage.url(app, SCAN_ID, "289cba71ada5a0811e57"));
    // test AddProprietaryButton is not visible if all pathNames are maven coordinates
    componentDetailsPage.unknownComponentAlert().shouldBe(visible);
    componentDetailsPage.addProprietarypComponentMatchersBtn().shouldBe(hidden);
    componentDetailsPage.backButton().shouldBe(visible).click();
    waitUntilUrl(ApplicationReportPage.url(app, SCAN_ID));

    // test AddProprietaryButton is not visible if already proprietary
    reportPage.resultRow(2).click();
    componentDetailsPage.addProprietarypComponentMatchersBtn().shouldBe(hidden);
    componentDetailsPage.backButton().shouldBe(visible).click();
    waitUntilUrl(ApplicationReportPage.url(app, SCAN_ID));

    // test Cancel button
    reportPage.resultRow(1).click();
    componentDetailsPage.unknownComponentAlert().shouldBe(visible);
    componentDetailsPage.addProprietarypComponentMatchersBtn().shouldBe(visible);
    componentDetailsPage.addProprietarypComponentMatchersBtn().click();

    AddProprietaryComponentMatchersPopover proprietaryComponentMatchersPopover =
        new AddProprietaryComponentMatchersPopover();
    proprietaryComponentMatchersPopover.regexInput().shouldBe(visible);
    proprietaryComponentMatchersPopover.cancelBtn().shouldBe(visible).click();
    proprietaryComponentMatchersPopover.shouldBe(hidden);

    // test init state
    componentDetailsPage.addProprietarypComponentMatchersBtn().shouldBe(visible).click();
    proprietaryComponentMatchersPopover.shouldBe(visible);
    proprietaryComponentMatchersPopover.regexInput().shouldHave(value(""));
    proprietaryComponentMatchersPopover.matchers().shouldHave(size(2));
    proprietaryComponentMatchersPopover.matchers().first().shouldBe(NX_RADIO_SELECTED);
    proprietaryComponentMatchersPopover.matchers().last().shouldBe(NX_RADIO_SELECTED);

    // test link to app config
    String expectedHref = Configuration.baseUrl +
        "assets/index.html#/management/edit/application/AddProprietaryMatchersTest/proprietary";
    SelenideElement configLink = proprietaryComponentMatchersPopover.alerts().first().find("a");
    configLink.shouldBe(visible).shouldHave(attribute("href", expectedHref));

    // submit all pathNames plus regex
    proprietaryComponentMatchersPopover.regexInput().val("foo");
    proprietaryComponentMatchersPopover.addBtn().shouldBe(enabled).click();
    proprietaryComponentMatchersPopover.shouldBe(hidden);

    ProprietaryConfig config = proprietaryConfigDAO.getByOwnerId(app.getId());
    assertThat(config.getRegexes()).containsExactly("\\QHelloWorldApp.jar/HelloWorld.jar\\E", "\\QHelloWorld.jar\\E",
        "foo");

    // submit same data - config should not change
    componentDetailsPage.addProprietarypComponentMatchersBtn().shouldBe(visible).click();
    proprietaryComponentMatchersPopover.shouldBe(visible);
    proprietaryComponentMatchersPopover.regexInput().val("foo");
    proprietaryComponentMatchersPopover.addBtn().shouldNotBe(DISABLED).click();
    proprietaryComponentMatchersPopover.shouldBe(hidden);

    config = proprietaryConfigDAO.getByOwnerId(app.getId());
    assertThat(config.getRegexes()).containsExactly("\\QHelloWorldApp.jar/HelloWorld.jar\\E", "\\QHelloWorld.jar\\E",
        "foo");

    // nothing selected will show validation errors
    componentDetailsPage.addProprietarypComponentMatchersBtn().shouldBe(visible).click();
    proprietaryComponentMatchersPopover.shouldBe(visible);
    proprietaryComponentMatchersPopover.matchers().first().click();
    proprietaryComponentMatchersPopover.matchers().last().click();
    proprietaryComponentMatchersPopover.addBtn().click();
    FormUtils.getAlertElement(proprietaryComponentMatchersPopover)
        .shouldHave(text(DEFAULT_VALIDATION_ERRORS_PREFIX + " Unable to add: Fields with invalid or missing data."));

    // submit invalid regex
    proprietaryComponentMatchersPopover.regexInput().val("(foo");
    proprietaryComponentMatchersPopover.addBtn().shouldNotBe(DISABLED).click();
    proprietaryComponentMatchersPopover.shouldBe(visible);

    // retry with all pathNames and new valid regex - should add new regex
    SelenideElement retryButton = proprietaryComponentMatchersPopover.alerts().last().find(".nx-load-error__retry");
    proprietaryComponentMatchersPopover.matchers().first().click();
    proprietaryComponentMatchersPopover.matchers().last().click();
    proprietaryComponentMatchersPopover.regexInput().val("bar");
    retryButton.shouldNotBe(DISABLED).click();
    proprietaryComponentMatchersPopover.shouldBe(hidden);

    config = proprietaryConfigDAO.getByOwnerId(app.getId());
    assertThat(config.getRegexes()).containsExactly("\\QHelloWorldApp.jar/HelloWorld.jar\\E", "\\QHelloWorld.jar\\E",
        "foo", "bar");
  }

  private void createGavViolatingPolicy(String ownerId) {
    Condition condition = new Condition(CoordinatesConditionType.ID, "match", "maven:*");
    Constraint constraint = new Constraint();
    constraint.setName("All coordinates");
    constraint.addCondition(condition);
    Policy policy = new Policy();
    policy.setName(POLICY_NAME);
    policy.addConstraint(constraint);
    policy.setOwnerId(ownerId);
    tempEntity.newPolicy(policy);
  }
}
