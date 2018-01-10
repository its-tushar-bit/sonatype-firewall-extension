/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.report;

import java.net.URL;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.VersionsCIP;
import com.sonatype.clm.testing.functional.elements.reports.AddProprietaryMatchersDialog;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.clm.testing.functional.pages.ReportPage;
import com.sonatype.clm.testing.functional.pages.ReportPolicyPage;
import com.sonatype.clm.testing.functional.utils.ReportHelper;
import com.sonatype.clm.testing.functional.utils.TestReportEvaluator;
import com.sonatype.insight.brain.dataaccess.configuration.ProprietaryConfigDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.configuration.ProprietaryConfig;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.CoordinatesConditionType;
import com.sonatype.insight.brain.service.InsightWork;

import com.codeborne.selenide.Configuration;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.elements.CLM.DISABLED;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class AddProprietaryMatchersTest
    extends AbstractFunctionalTest
{
  private static final String POLICY_NAME = "All components";

  private static final String SCAN_ID = "306e0a923df34c64b836358182b1b902";

  private static final InsightWork WORK = new InsightWork(testCLMServer.getCLMServer().getConfiguration());

  private static final String CANNED_TEST_REPORT = "/canned-reports/report-with-unknown-and-proprietary";

  private static final com.codeborne.selenide.Condition ERROR = cssClass("error");

  private ProprietaryConfigDAO proprietaryConfigDAO = new ProprietaryConfigDAO();

  private Application app;

  @BeforeClass
  public static void startup() {
    refreshOrOpen(ReportListPage.URL);
    loginAsAdmin();
  }

  @Before
  public void start() throws Exception {
    app = tempEntity.newApplicationWithParent(AddProprietaryMatchersTest.class.getSimpleName());
    URL zippedReport = ReportHelper.zipReport(CANNED_TEST_REPORT, tempDir);
    TestReportEvaluator evaluator = new TestReportEvaluator(app, SCAN_ID, zippedReport, Configuration.baseUrl, WORK);
    createGavViolatingPolicy(app.getOrganizationId());
    evaluator.evaluatePolicy();
  }

  @Test
  public void testAddProprietaryMatchersDialog() {
    AddProprietaryMatchersDialog modal = new AddProprietaryMatchersDialog();

    refreshOrOpen(ReportPage.url(app, SCAN_ID));
    ReportPage.policyTabButton().shouldBe(visible).click();
    ReportPolicyPage.rows().shouldHaveSize(3);

    // test AddProprietaryButton is not visible if all pathNames are maven coordinates
    ReportPolicyPage.row(2).openCip();
    VersionsCIP.addProprietaryMatchersButton().shouldBe(hidden);

    // test AddProprietaryButton is not visible if already proprietary
    ReportPolicyPage.row(1).openCip();
    VersionsCIP.addProprietaryMatchersButton().shouldBe(hidden);

    // test Cancel button
    ReportPolicyPage.row(0).openCip();
    VersionsCIP.addProprietaryMatchersButton().shouldBe(visible).click();
    modal.cancelButton().shouldBe(visible).click();
    modal.shouldBe(hidden);

    // test init state
    VersionsCIP.addProprietaryMatchersButton().shouldBe(visible).click();
    modal.regexInput().shouldHave(value(""));
    modal.pathMatcherCheckboxes().shouldHaveSize(2);
    modal.pathMatcherCheckboxes().first().shouldBe(selected);
    modal.pathMatcherCheckboxes().last().shouldBe(selected);

    // test link to app config
    String expectedHref = Configuration.baseUrl + "assets/index.html#/management/edit/application/AddProprietaryMatchersTest/proprietary";
    modal.linkToAppConfig().shouldBe(visible).shouldHave(attribute("href", expectedHref));

    // submit all pathNames plus regex
    modal.regexInput().val("foo");
    modal.addButton().shouldNotBe(DISABLED).click();
    modal.shouldBe(hidden);
    ProprietaryConfig config = proprietaryConfigDAO.getByOwnerId(app.getId());
    assertThat(config.getRegexes(), hasSize(3));
    assertThat(config.getRegexes().get(0), is("\\QHelloWorldApp.jar/HelloWorld.jar\\E"));
    assertThat(config.getRegexes().get(1), is("\\QHelloWorld.jar\\E"));
    assertThat(config.getRegexes().get(2), is("foo"));

    // submit same data - config should not change
    VersionsCIP.addProprietaryMatchersButton().shouldBe(visible).click();
    modal.regexInput().val("foo");
    modal.addButton().shouldNotBe(DISABLED).click();
    modal.shouldBe(hidden);
    config = proprietaryConfigDAO.getByOwnerId(app.getId());
    assertThat(config.getRegexes(), hasSize(3));
    assertThat(config.getRegexes().get(0), is("\\QHelloWorldApp.jar/HelloWorld.jar\\E"));
    assertThat(config.getRegexes().get(1), is("\\QHelloWorld.jar\\E"));
    assertThat(config.getRegexes().get(2), is("foo"));

    // nothing selected
    VersionsCIP.addProprietaryMatchersButton().shouldBe(visible).click();
    modal.pathMatcherCheckboxes().first().click();
    modal.addButton().shouldNotBe(DISABLED);
    modal.pathMatcherCheckboxes().last().click();
    modal.addButton().shouldBe(DISABLED);

    // submit invalid regex
    modal.regexInput().val("(foo");
    modal.addButton().shouldNotBe(DISABLED).click();
    modal.shouldBe(visible);
    modal.footer().shouldBe(visible).shouldHave(ERROR);
    modal.retryButton().shouldBe(visible);

    // disabled retry button
    modal.regexInput().val("");
    modal.retryButton().shouldBe(DISABLED);

    // retry with all pathNames and new valid regex - should add new regex
    modal.pathMatcherCheckboxes().first().click();
    modal.pathMatcherCheckboxes().first().shouldBe(selected);
    modal.pathMatcherCheckboxes().last().click();
    modal.pathMatcherCheckboxes().last().shouldBe(selected);
    modal.regexInput().val("bar");
    modal.retryButton().shouldNotBe(DISABLED).click();
    modal.shouldBe(hidden);
    config = proprietaryConfigDAO.getByOwnerId(app.getId());
    assertThat(config.getRegexes(), hasSize(4));
    assertThat(config.getRegexes().get(0), is("\\QHelloWorldApp.jar/HelloWorld.jar\\E"));
    assertThat(config.getRegexes().get(1), is("\\QHelloWorld.jar\\E"));
    assertThat(config.getRegexes().get(2), is("foo"));
    assertThat(config.getRegexes().get(3), is("bar"));
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
