/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.clm.testing.functional.developer;

import java.io.IOException;
import java.net.URL;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.NxDropdown;
import com.sonatype.clm.testing.functional.elements.NxTree;
import com.sonatype.clm.testing.functional.elements.componentdetails.DependencyTreeTile;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage;
import com.sonatype.clm.testing.functional.pages.ComponentDetailsPage;
import com.sonatype.clm.testing.functional.pages.DeveloperReportListPage;
import com.sonatype.clm.testing.functional.pages.PrioritiesPage;
import com.sonatype.clm.testing.functional.utils.ScrollUtil;
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
import com.codeborne.selenide.WebDriverRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selectors.byText;
import static com.sonatype.clm.testing.functional.utils.ScrollUtil.scrollIntoView;

public class DeveloperReportListPageTest
    extends AbstractFunctionalTest
{
  private static final int TOTAL_PRIORITIES_PER_PAGE = 15;

  private final ApplicationReportPage reportPage = new ApplicationReportPage();

  private final ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();

  @Before
  public void before() {
    setUpAppsWithPriorities();
    refreshOrOpen(DeveloperReportListPage.url());
    loginAsAdmin();
  }

  @After
  public void after() {
    logout();
  }

  @Test
  public void testPrioritiesReportPage_shouldEnterReportFromListAndBack() {
    refreshOrOpen(DeveloperReportListPage.url());
    DeveloperReportListPage.reportListTable().findAll(byText("View Priorities")).first().click();
    PrioritiesPage.title().shouldHave(text(title(0)));

    // Go back to the report list page
    PrioritiesPage.backLink().click();
    DeveloperReportListPage.title().shouldHave(text("Priorities"));
  }

  @Test
  public void testPrioritiesReportPage_shouldEnterFullReportAndBack() {
    refreshOrOpen(DeveloperReportListPage.url());
    DeveloperReportListPage.reportListTable().findAll(byText("View Priorities")).first().click();
    PrioritiesPage.title().shouldHave(text(title(0)));
    NxDropdown viewDropdown = PrioritiesPage.viewDropdown();
    viewDropdown.shouldHave(text("View"));
    viewDropdown.click();
    viewDropdown
        .menu()
        .shouldBe(visible)
        .entries()
        .find(text("Lifecycle Report"))
        .shouldBe(visible)
        .click();

    Selenide.switchTo().window(1);
    reportPage.shouldBe(visible);

    // Go back to the priorities page
    NxDropdown optionsDropdown = reportPage.optionsDropdown();
    optionsDropdown.button().shouldHave(text("Options")).click();
    optionsDropdown
        .menu()
        .shouldBe(visible)
        .entries()
        .find(text("Priorities"))
        .shouldBe(visible)
        .click();

    WebDriverRunner.getWebDriver().close();
    Selenide.switchTo().window(0);
    PrioritiesPage.title().shouldHave(text("Priorities"));
  }

  @Test
  public void testPrioritiesReportPage_shouldListViolatingComponentsProperly() {
    refreshOrOpen(DeveloperReportListPage.url());
    DeveloperReportListPage.reportListTable().findAll(byText("View Priorities")).first().click();
    PrioritiesPage.title().shouldHave(text(title(0)));

    ElementsCollection firstPage =
        PrioritiesPage.prioritiesTableRows();

    firstPage.shouldHave(size(TOTAL_PRIORITIES_PER_PAGE));

    firstPage.get(0).find(".iq-priorities-page-components__component")
        .shouldHave(text("com.mycila : license-maven-plugin : 2.11"));
    firstPage.get(0).find(".iq-priorities-page-policy-details__desc-threat")
        .shouldHave(text("10"));

    firstPage.get(1).find(".iq-priorities-page-components__component")
        .shouldHave(text("com.vaadin.addon : vaadin-touchkit-agpl : 3.0.0-beta1"));
    firstPage.get(1).find(".iq-priorities-page-policy-details__desc-threat")
        .shouldHave(text("10"));

    firstPage.get(2).find(".iq-priorities-page-components__component")
        .shouldHave(text("org.springframework.security : spring-security-web : 3.2.4.RELEASE"));
    firstPage.get(2).find(".iq-priorities-page-policy-details__desc-threat")
        .shouldHave(text("9"));

    ScrollUtil.scrollIntoView(PrioritiesPage.lastPageLink());
    PrioritiesPage.lastPageLink().shouldHave(text("2")).click();

    ElementsCollection lastPage =
        PrioritiesPage.prioritiesTableRows();

    lastPage.shouldHave(size(13));
  }

  @Test
  public void testPrioritiesReportPage_shouldOpenComponentDetailsPageFromRow() {
    refreshOrOpen(DeveloperReportListPage.url());
    DeveloperReportListPage.reportListTable().findAll(byText("View Priorities")).get(1).click();
    PrioritiesPage.title().shouldHave(text(title(1)));

    ElementsCollection firstPage =
        PrioritiesPage.prioritiesTableRows();
    firstPage.get(0).find(".iq-priorities-page-components__component").click();

    DependencyTreeTile dependencyTreeTile = componentDetailsPage.dependencyTreeTile();
    ScrollUtil.scrollIntoView(dependencyTreeTile.title());
    dependencyTreeTile.shouldBe(visible);
    dependencyTreeTile.title().shouldHave(text("Dependency Tree"));

    final NxTree nxTree = dependencyTreeTile.tree();
    ElementsCollection clickableTreeItems = nxTree.clickableTreeItems();

    nxTree.treeItems().get(0).shouldHave(text("appName1"));
    scrollIntoView(clickableTreeItems.get(0), true);

    clickableTreeItems.get(0).shouldHave(text("geronimo : geronimo-tomcat-builder : 1.1"));

    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();
    componentDetailsPage.violationsTab().shouldBe(visible).click();
    componentDetailsPage.violationsTabContent().shouldBe(visible);
  }

  private String title(int appId) {
    return "appName" + appId + " - Priorities";
  }

  private void setUpAppsWithPriorities() {
    try {
      setUpMainApp(0, "/canned-reports/large-report");
      setUpMainApp(1, "/canned-reports/report-with-dependency-tree");
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void setUpMainApp(int id, String reportResourceName) throws IOException {
    URL referencePolicyUrl = getClass().getResource("/reference-policies-v3-with-build-fail.json");
    PolicyExportResult referencePolicies = JsonUtils.parse(referencePolicyUrl.openStream(), PolicyExportResult.class);
    PolicyImportExport policyImportExport = lookup(PolicyImportExport.class);

    Organization org = tempEntity.newOrganization();
    policyImportExport.importOrganization(org, referencePolicies);
    Application app = tempEntity.newApplication("appName" + id, "appId" + id, org.getId());
    URL zippedReport = ReportHelper.zipReport(reportResourceName, tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    TestReportEvaluator evaluator =
        new TestReportEvaluator(app, "scan-" + id, zippedReport, baseUrlFromTest, work);
    evaluator.evaluatePolicy();
  }
}
