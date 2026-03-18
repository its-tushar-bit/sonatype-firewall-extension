/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.applicationreport;

import java.io.IOException;
import java.net.URL;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.ApplicationReportFilter.DependencyTypeFilter;
import com.sonatype.clm.testing.functional.elements.ApplicationReportFilter.InnerSourceFilter;
import com.sonatype.clm.testing.functional.elements.ApplicationReportFilter.MatchStateFilter;
import com.sonatype.clm.testing.functional.elements.ApplicationReportFilter.PolicyTypeFilter;
import com.sonatype.clm.testing.functional.elements.ApplicationReportFilter.ProprietaryFilter;
import com.sonatype.clm.testing.functional.elements.DashboardFilters;
import com.sonatype.clm.testing.functional.elements.PolicyThreatLevelFilter;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage.AppReportHeaders;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.utils.InputUtils;
import com.sonatype.clm.testing.functional.utils.TestReportEvaluator;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.policy.PolicyExportResult;
import com.sonatype.insight.brain.policy.PolicyImportExport;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.json.store.JsonUtils;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.Keys;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

public class ApplicationReportFilterTest
    extends AbstractFunctionalTest
{
  public static final String SCAN_ID = "e16caf35769f4b3186a7e416d34c2797";

  private static final int EXPECTED_VIOLATIONS_COUNT = 62;

  private final ApplicationReportPage reportPage = new ApplicationReportPage();

  private Application app;

  @BeforeClass
  public static void startup() {
    refreshOrOpen(DashboardPage.url());
    loginAsAdmin();
  }

  @Before
  public void start() throws IOException {
    URL referencePolicyUrl = getClass().getResource("/reference-policies-v3.json");
    PolicyExportResult referencePolicies = JsonUtils.parse(referencePolicyUrl.openStream(), PolicyExportResult.class);
    PolicyImportExport policyImportExport = lookup(PolicyImportExport.class);

    Organization org = tempEntity.newOrganization();
    policyImportExport.importOrganization(org, referencePolicies);
    app = tempEntity.newApplication("ApplicationReportFilterTest", "ApplicationReportFilterTest", org.getId());
    URL zippedReport = ReportHelper.zipReport("/canned-reports/large-report", tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    TestReportEvaluator evaluator = new TestReportEvaluator(app, SCAN_ID, zippedReport, baseUrlFromTest, work);
    evaluator.evaluatePolicy();
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
  }

  @After
  public void afterEachTestEnds() {
    if (reportPage.filterPanel().getElement().is(visible)) {
      reportPage.filterPanel().closeButton().click();
    }
  }

  @Test
  public void testFiltering() {
    AppReportHeaders headers = reportPage.headers();
    ElementsCollection violations = reportPage.resultRows();

    headers.policyNameFilterInput().setValue("unk");

    violations.shouldHave(size(1));
    violations.shouldHave(texts("Component-Unknown"));
    violations.shouldHave(texts("RegexMatch.dll"));

    headers.componentNameFilterInput().setValue("org.slf4j");

    violations.shouldHave(size(1));
    violations.shouldHave(texts("No Results"));

    InputUtils.clearInput(headers.policyNameFilterInput());
    violations.shouldHave(size(3));
    violations.shouldHave(texts("None", "None", "None"));
    violations.shouldHave(texts("org.slf4j : jcl-over-slf4j", "org.slf4j : slf4j-api", "org.slf4j : slf4j-log4j12"));

    // test filtering across colon-separate fields in component name
    headers.componentNameFilterInput().setValue("org.slf4j : slf4j-");

    violations.shouldHave(size(2));
    violations.shouldHave(texts("None", "None"));
    violations.shouldHave(texts("org.slf4j : slf4j-api", "org.slf4j : slf4j-log4j12"));

    reportPage.filterToggle().click();
    ProprietaryFilter proprietaryFilter = reportPage.filterPanel().proprietaryFilter();

    proprietaryFilter.counter().shouldHave(text("2"));
    proprietaryFilter.multiSelectList().forEach(child -> child.shouldNotBe(visible));
    proprietaryFilter.twisty().click();
    proprietaryFilter.multiSelectList().shouldHave(size(3));
    proprietaryFilter.multiSelectList().forEach(child -> child.shouldBe(visible));
    proprietaryFilter.proprietary().click();

    proprietaryFilter.counter().shouldHave(text("1 of 2"));
    proprietaryFilter.proprietary().shouldBe(selected);
    proprietaryFilter.nonProprietary().shouldNotBe(selected);

    violations.shouldHave(size(1));
    violations.shouldHave(texts("No Results"));

    InputUtils.clearInput(headers.componentNameFilterInput());

    violations.shouldHave(size(3));
    violations.shouldHave(texts("full.jar", "org.apache.tiles : tiles-api", "org.apache.tiles : tiles-core"));

    proprietaryFilter.allItems().click();
    proprietaryFilter.counter().shouldHave(text("2 of 2"));
    proprietaryFilter.proprietary().shouldBe(selected);
    proprietaryFilter.nonProprietary().shouldBe(selected);

    violations.shouldHave(size(65));

    proprietaryFilter.allItems().click();
    proprietaryFilter.counter().shouldHave(text("2"));
    proprietaryFilter.proprietary().shouldNotBe(selected);
    proprietaryFilter.nonProprietary().shouldNotBe(selected);

    violations.shouldHave(size(65));

    proprietaryFilter.nonProprietary().click();
    proprietaryFilter.counter().shouldHave(text("1 of 2"));
    proprietaryFilter.proprietary().shouldNotBe(selected);
    proprietaryFilter.nonProprietary().shouldBe(selected);

    violations.shouldHave(size(EXPECTED_VIOLATIONS_COUNT));
    proprietaryFilter.twisty().click();

    // InnerSource filter
    InnerSourceFilter innerSourceFilter = reportPage.filterPanel().innerSourceFilter();
    innerSourceFilter.shouldHave(text("InnerSource"));
    innerSourceFilter.counter().shouldHave(exactText("2"));
    innerSourceFilter.multiSelectList().forEach(child -> child.shouldNotBe(visible));
    innerSourceFilter.twisty().click();
    innerSourceFilter.multiSelectList().shouldHave(size(3));
    innerSourceFilter.multiSelectList().forEach(child -> child.shouldBe(visible));

    innerSourceFilter.innerSource().click();
    innerSourceFilter.innerSource().shouldBe(selected);
    innerSourceFilter.nonInnerSource().shouldNotBe(selected);
    innerSourceFilter.counter().shouldHave(exactText("1 of 2"));
    violations.shouldHave(size(3));
    violations.first().shouldHave(text("apache-taglibs : standard : 1.1.2"));

    innerSourceFilter.nonInnerSource().click();
    innerSourceFilter.nonInnerSource().shouldBe(selected);
    innerSourceFilter.counter().shouldHave(exactText("2 of 2"));
    violations.shouldHave(size(EXPECTED_VIOLATIONS_COUNT));
    violations.first().shouldHave(text("com.mycila : license-maven-plugin : 2.11"));
    innerSourceFilter.twisty().click();

    // match state filter
    MatchStateFilter matchStateFilter = reportPage.filterPanel().matchStateFilter();
    matchStateFilter.counter().shouldHave(exactText("3"));
    matchStateFilter.multiSelectList().forEach(child -> child.shouldNotBe(visible));
    matchStateFilter.twisty().click();
    matchStateFilter.multiSelectList().shouldHave(size(4));
    matchStateFilter.multiSelectList().forEach(child -> child.shouldBe(visible));

    matchStateFilter.similar().click();
    matchStateFilter.similar().shouldBe(selected);
    matchStateFilter.counter().shouldHave(exactText("1 of 3"));
    violations.shouldHave(size(1));
    violations.first().shouldHave(text("apache-httpclient : commons-httpclient : 3.1"));

    matchStateFilter.unknown().click();
    matchStateFilter.unknown().shouldBe(selected);
    matchStateFilter.counter().shouldHave(exactText("2 of 3"));
    violations.shouldHave(size(2));
    violations.shouldHave(texts("apache-httpclient : commons-httpclient : 3.1", "RegexMatch.dll"));

    matchStateFilter.exact().click();
    matchStateFilter.exact().shouldBe(selected);
    matchStateFilter.counter().shouldHave(exactText("3 of 3"));
    violations.shouldHave(size(EXPECTED_VIOLATIONS_COUNT));
    matchStateFilter.twisty().click();

    // policy type filter
    PolicyTypeFilter policyTypeFilter = reportPage.filterPanel().policyTypeFilter();
    policyTypeFilter.counter().shouldHave(exactText("4"));
    policyTypeFilter.multiSelectList().forEach(child -> child.shouldNotBe(visible));
    policyTypeFilter.twisty().click();
    policyTypeFilter.multiSelectList().shouldHave(size(5));
    policyTypeFilter.multiSelectList().forEach(child -> child.shouldBe(visible));

    policyTypeFilter.quality().click();
    policyTypeFilter.quality().shouldBe(selected);
    policyTypeFilter.counter().shouldHave(exactText("1 of 4"));
    violations.shouldHave(size(1));
    violations.first().shouldHave(exactText("No Results"));

    policyTypeFilter.license().click();
    policyTypeFilter.license().shouldBe(selected);
    policyTypeFilter.counter().shouldHave(exactText("2 of 4"));
    violations.shouldHave(size(3));
    violations.shouldHave(texts(
        "com.mycila : license-maven-plugin : 2.11",
        "com.vaadin.addon : vaadin-touchkit-agpl : 3.0.0-beta1",
        "xpp3 : xpp3_min : 1.1.4c"));

    policyTypeFilter.other().click();
    policyTypeFilter.other().shouldBe(selected);
    policyTypeFilter.counter().shouldHave(exactText("3 of 4"));
    violations.shouldHave(size(5));
    violations.shouldHave(texts(
        "com.mycila : license-maven-plugin : 2.11",
        "com.vaadin.addon : vaadin-touchkit-agpl : 3.0.0-beta1",
        "xpp3 : xpp3_min : 1.1.4c",
        "RegexMatch.dll",
        "junit : junit : 4.8.1"));

    policyTypeFilter.security().click();
    policyTypeFilter.security().shouldBe(selected);
    policyTypeFilter.counter().shouldHave(exactText("4 of 4"));
    violations.shouldHave(size(28));

    policyTypeFilter.allItems().click();
    policyTypeFilter.allItems().shouldNotBe(selected);
    violations.shouldHave(size(EXPECTED_VIOLATIONS_COUNT));

    // dependency type filter
    DependencyTypeFilter dependencyTypeFilter = reportPage.filterPanel().dependencyTypeFilter();
    dependencyTypeFilter.counter().shouldHave(exactText("3"));
    dependencyTypeFilter.multiSelectList().forEach(child -> child.shouldNotBe(visible));
    dependencyTypeFilter.twisty().click();
    dependencyTypeFilter.multiSelectList().shouldHave(size(4));
    dependencyTypeFilter.multiSelectList().forEach(child -> child.shouldBe(visible));
    dependencyTypeFilter.unknown().click();
    dependencyTypeFilter.unknown().shouldBe(selected);
    violations.shouldHave(size(56));
    dependencyTypeFilter.counter().shouldHave(exactText("1 of 3"));

    dependencyTypeFilter.transitive().click();
    violations.shouldHave(size(60));
    dependencyTypeFilter.counter().shouldHave(exactText("2 of 3"));

    dependencyTypeFilter.direct().click();
    violations.shouldHave(size(EXPECTED_VIOLATIONS_COUNT));
    dependencyTypeFilter.allItems().shouldBe(selected);
    dependencyTypeFilter.counter().shouldHave(exactText("3 of 3"));
    dependencyTypeFilter.allItems().click();
    dependencyTypeFilter.allItems().shouldNotBe(selected);
    dependencyTypeFilter.counter().shouldHave(exactText("3"));
    dependencyTypeFilter.direct().click();
    dependencyTypeFilter.direct().shouldBe(selected);
    dependencyTypeFilter.counter().shouldHave(exactText("1 of 3"));
    violations.shouldHave(size(2));
    dependencyTypeFilter.allItems().click();
    dependencyTypeFilter.allItems().shouldBe(selected);
    violations.shouldHave(size(EXPECTED_VIOLATIONS_COUNT));
    dependencyTypeFilter.twisty().click();

    // policy threat level filter
    PolicyThreatLevelFilter threatLevelFilter = DashboardFilters.iqPolicyThreatLevelFilter();
    threatLevelFilter.counter().shouldBe(visible).shouldHave(cssClass("nx-counter--active")).shouldHave(text("0 – 10"));
    threatLevelFilter.slider().shouldBe(hidden);
    threatLevelFilter.twisty().click();
    threatLevelFilter.slider().shouldBe(visible);
    threatLevelFilter.slider().setValues(1, 10);
    violations.shouldHave(size(28));
    threatLevelFilter.slider().setValues(1, 9);
    violations.shouldHave(size(26));
    threatLevelFilter.slider().setValues(2, 9);
    violations.shouldHave(size(25));
    threatLevelFilter.slider().setValues(7, 9);
    violations.shouldHave(size(24));
    threatLevelFilter.slider().setValues(3, 6);
    violations.shouldHave(size(1));
    violations.shouldHave(texts("No Results"));
    threatLevelFilter.slider().setValues(0, 10);
    violations.shouldHave(size(EXPECTED_VIOLATIONS_COUNT));
    threatLevelFilter.twisty().click();
    threatLevelFilter.slider().shouldBe(hidden);

    reportPage.filterPanel().closeButton().click();
  }

  @Test
  public void testFilterClose() {
    // By close button in panel
    reportPage.filterToggle().shouldBe(visible).click();
    DashboardPage.waitForDrawerAnimation();
    reportPage.filterPanel().shouldBe(visible);
    reportPage.filterPanel().closeButton().click();
    DashboardPage.waitForDrawerAnimation();
    reportPage.filterPanel().shouldNotBe(visible);

    // By escape key
    reportPage.filterToggle().shouldBe(visible).click();
    DashboardPage.waitForDrawerAnimation();
    reportPage.filterPanel().shouldBe(visible);
    pressEscape();
    // Wait for the panel to close
    DashboardPage.waitForDrawerAnimation();
    reportPage.filterPanel().shouldNotBe(visible);

    // By clicking an element off-panel
    reportPage.filterToggle().shouldBe(visible).click();
    DashboardPage.waitForDrawerAnimation();
    reportPage.filterPanel().shouldBe(visible);
    reportPage.headers().componentNameFilterInput().click();
    DashboardPage.waitForDrawerAnimation();
    reportPage.filterPanel().shouldNotBe(visible);
  }

  private void pressEscape() {
    Selenide.actions().sendKeys(Keys.ESCAPE).perform();
  }
}
