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
import com.sonatype.clm.testing.functional.elements.ApplicationReportFilter.MatchStateFilter;
import com.sonatype.clm.testing.functional.elements.ApplicationReportFilter.PolicyTypeFilter;
import com.sonatype.clm.testing.functional.elements.ApplicationReportFilter.ProprietaryFilter;
import com.sonatype.clm.testing.functional.elements.DashboardFilters;
import com.sonatype.clm.testing.functional.elements.PolicyThreatLevelFilter;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage.AppReportHeaders;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.utils.TestReportEvaluator;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.policy.PolicyExportResult;
import com.sonatype.insight.brain.policy.PolicyImportExport;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.json.store.JsonUtils;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.Keys;

import static com.codeborne.selenide.CollectionCondition.empty;
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
    PolicyImportExport policyImportExport = new PolicyImportExport();

    Organization org = tempEntity.newOrganization();
    policyImportExport.importOrganization(org, referencePolicies);
    app = tempEntity.newApplication("ApplicationReportFilterTest", "ApplicationReportFilterTest", org.getId());
    URL zippedReport = ReportHelper.zipReport("/canned-reports/large-report", tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    TestReportEvaluator evaluator = new TestReportEvaluator(app, SCAN_ID, zippedReport, Configuration.baseUrl, work);
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

    violations.shouldHaveSize(1);
    violations.shouldHave(texts("Component-Unknown"));
    violations.shouldHave(texts("RegexMatch.dll"));

    headers.componentNameFilterInput().setValue("org.slf4j");

    violations.shouldHaveSize(1);
    violations.shouldHave(texts("No Results"));

    headers.policyNameFilterInput().clear();
    violations.shouldHaveSize(3);
    violations.shouldHave(texts("None", "None", "None"));
    violations.shouldHave(texts("org.slf4j : jcl-over-slf4j", "org.slf4j : slf4j-api", "org.slf4j : slf4j-log4j12"));

    // test filtering across colon-separate fields in component name
    headers.componentNameFilterInput().setValue("org.slf4j : slf4j-");

    violations.shouldHaveSize(2);
    violations.shouldHave(texts("None", "None"));
    violations.shouldHave(texts("org.slf4j : slf4j-api", "org.slf4j : slf4j-log4j12"));

    reportPage.filterToggle().click();
    ProprietaryFilter proprietaryFilter = reportPage.filterPanel().proprietaryFilter();

    proprietaryFilter.counter().shouldHave(text("2"));
    proprietaryFilter.multiSelectList().shouldBe(empty);
    proprietaryFilter.twisty().click();
    proprietaryFilter.multiSelectList().shouldHaveSize(3);
    proprietaryFilter.proprietary().click();

    proprietaryFilter.counter().shouldHave(text("1 of 2"));
    proprietaryFilter.proprietary().shouldBe(selected);
    proprietaryFilter.nonProprietary().shouldNotBe(selected);

    violations.shouldHaveSize(1);
    violations.shouldHave(texts("No Results"));

    headers.componentNameFilterInput().clear();

    violations.shouldHaveSize(3);
    violations.shouldHave(texts("full.jar", "org.apache.tiles : tiles-api", "org.apache.tiles : tiles-core"));

    proprietaryFilter.allItems().click();
    proprietaryFilter.counter().shouldHave(text("2 of 2"));
    proprietaryFilter.proprietary().shouldBe(selected);
    proprietaryFilter.nonProprietary().shouldBe(selected);

    violations.shouldHaveSize(64);

    proprietaryFilter.allItems().click();
    proprietaryFilter.counter().shouldHave(text("2"));
    proprietaryFilter.proprietary().shouldNotBe(selected);
    proprietaryFilter.nonProprietary().shouldNotBe(selected);

    violations.shouldHaveSize(64);

    proprietaryFilter.nonProprietary().click();
    proprietaryFilter.counter().shouldHave(text("1 of 2"));
    proprietaryFilter.proprietary().shouldNotBe(selected);
    proprietaryFilter.nonProprietary().shouldBe(selected);

    violations.shouldHaveSize(61);
    proprietaryFilter.twisty().click();

    // match state filter
    MatchStateFilter matchStateFilter = reportPage.filterPanel().matchStateFilter();
    matchStateFilter.counter().shouldHave(exactText("3"));
    matchStateFilter.multiSelectList().shouldBe(empty);
    matchStateFilter.twisty().click();
    matchStateFilter.multiSelectList().shouldHaveSize(4);

    matchStateFilter.similar().click();
    matchStateFilter.similar().shouldBe(selected);
    matchStateFilter.counter().shouldHave(exactText("1 of 3"));
    violations.shouldHaveSize(1);
    violations.first().shouldHave(text("apache-httpclient : commons-httpclient : 3.1"));

    matchStateFilter.unknown().click();
    matchStateFilter.unknown().shouldBe(selected);
    matchStateFilter.counter().shouldHave(exactText("2 of 3"));
    violations.shouldHaveSize(2);
    violations.shouldHave(texts("apache-httpclient : commons-httpclient : 3.1", "RegexMatch.dll"));

    matchStateFilter.exact().click();
    matchStateFilter.exact().shouldBe(selected);
    matchStateFilter.counter().shouldHave(exactText("3 of 3"));
    violations.shouldHaveSize(61);
    matchStateFilter.twisty().click();

    //policy type filter
    PolicyTypeFilter policyTypeFilter = reportPage.filterPanel().policyTypeFilter();
    policyTypeFilter.counter().shouldHave(exactText("4"));
    policyTypeFilter.multiSelectList().shouldBe(empty);
    policyTypeFilter.twisty().click();
    policyTypeFilter.multiSelectList().shouldHaveSize(5);

    policyTypeFilter.quality().click();
    policyTypeFilter.quality().shouldBe(selected);
    policyTypeFilter.counter().shouldHave(exactText("1 of 4"));
    violations.shouldHaveSize(1);
    violations.first().shouldHave(exactText("No Results"));

    policyTypeFilter.license().click();
    policyTypeFilter.license().shouldBe(selected);
    policyTypeFilter.counter().shouldHave(exactText("2 of 4"));
    violations.shouldHaveSize(3);
    violations.shouldHave(texts(
        "com.mycila : license-maven-plugin : 2.11",
        "com.vaadin.addon : vaadin-touchkit-agpl : 3.0.0-beta1",
        "xpp3 : xpp3_min : 1.1.4c"
    ));

    policyTypeFilter.other().click();
    policyTypeFilter.other().shouldBe(selected);
    policyTypeFilter.counter().shouldHave(exactText("3 of 4"));
    violations.shouldHaveSize(5);
    violations.shouldHave(texts(
        "com.mycila : license-maven-plugin : 2.11",
        "com.vaadin.addon : vaadin-touchkit-agpl : 3.0.0-beta1",
        "xpp3 : xpp3_min : 1.1.4c",
        "RegexMatch.dll",
        "junit : junit : 4.8.1"
    ));

    policyTypeFilter.security().click();
    policyTypeFilter.security().shouldBe(selected);
    policyTypeFilter.counter().shouldHave(exactText("4 of 4"));
    violations.shouldHaveSize(28);

    policyTypeFilter.allItems().click();
    policyTypeFilter.allItems().shouldNotBe(selected);
    violations.shouldHaveSize(61);

    // dependency type filter
    DependencyTypeFilter dependencyTypeFilter = reportPage.filterPanel().dependencyTypeFilter();
    dependencyTypeFilter.counter().shouldHave(exactText("3"));
    dependencyTypeFilter.multiSelectList().shouldBe(empty);
    dependencyTypeFilter.twisty().click();
    dependencyTypeFilter.multiSelectList().shouldHaveSize(4);
    dependencyTypeFilter.unknown().click();
    dependencyTypeFilter.unknown().shouldBe(selected);
    violations.shouldHaveSize(55);
    dependencyTypeFilter.counter().shouldHave(exactText("1 of 3"));

    dependencyTypeFilter.transitive().click();
    violations.shouldHaveSize(59);
    dependencyTypeFilter.counter().shouldHave(exactText("2 of 3"));

    dependencyTypeFilter.direct().click();
    violations.shouldHaveSize(61);
    dependencyTypeFilter.allItems().shouldBe(selected);
    dependencyTypeFilter.counter().shouldHave(exactText("3 of 3"));
    dependencyTypeFilter.allItems().click();
    dependencyTypeFilter.allItems().shouldNotBe(selected);
    dependencyTypeFilter.counter().shouldHave(exactText("3"));
    dependencyTypeFilter.direct().click();
    dependencyTypeFilter.direct().shouldBe(selected);
    dependencyTypeFilter.counter().shouldHave(exactText("1 of 3"));
    violations.shouldHaveSize(2);
    dependencyTypeFilter.allItems().click();
    dependencyTypeFilter.allItems().shouldBe(selected);
    violations.shouldHaveSize(61);
    dependencyTypeFilter.twisty().click();

    // policy threat level filter
    PolicyThreatLevelFilter threatLevelFilter = DashboardFilters.iqPolicyThreatLevelFilter();
    threatLevelFilter.counter().shouldBe(visible).shouldHave(cssClass("iq-counter--active")).shouldHave(text("0 – 10"));
    threatLevelFilter.slider().shouldBe(hidden);
    threatLevelFilter.twisty().click();
    threatLevelFilter.slider().shouldBe(visible);
    threatLevelFilter.slider().setValues(1, 10);
    violations.shouldHaveSize(28);
    threatLevelFilter.slider().setValues(1, 9);
    violations.shouldHaveSize(26);
    threatLevelFilter.slider().setValues(2, 9);
    violations.shouldHaveSize(25);
    threatLevelFilter.slider().setValues(7, 9);
    violations.shouldHaveSize(24);
    threatLevelFilter.slider().setValues(9, 9);
    violations.shouldHaveSize(15);
    threatLevelFilter.slider().setValues(10, 10);
    violations.shouldHaveSize(2);
    threatLevelFilter.slider().setValues(3, 6);
    violations.shouldHaveSize(1);
    violations.shouldHave(texts("No Results"));
    threatLevelFilter.slider().setValues(0, 10);
    violations.shouldHaveSize(61);
    threatLevelFilter.twisty().click();
    threatLevelFilter.slider().shouldBe(hidden);

    reportPage.filterPanel().closeButton().click();
  }

  @Test
  public void testFilterClose() {
    // By close button in panel
    reportPage.filterToggle().shouldBe(visible).click();
    reportPage.filterPanel().shouldBe(visible);
    reportPage.filterPanel().closeButton().click();
    reportPage.filterPanel().shouldNotBe(visible);

    // By escape key
    reportPage.filterToggle().shouldBe(visible).click();
    reportPage.filterPanel().shouldBe(visible);
    pressEscape();
    reportPage.filterPanel().shouldNotBe(visible);

    // By clicking an element off-panel
    reportPage.filterToggle().shouldBe(visible).click();
    reportPage.filterPanel().shouldBe(visible);
    reportPage.headers().componentNameFilterInput().click();
    reportPage.filterPanel().shouldNotBe(visible);
  }

  private void pressEscape() {
    Selenide.actions().sendKeys(Keys.ESCAPE).perform();
  }
}
