/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.clm.testing.functional.developer;

import java.io.IOException;
import java.net.URL;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
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
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.innersource.InnerSourceApplication;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.policy.PolicyExportResult;
import com.sonatype.insight.brain.policy.PolicyImportExport;
import com.sonatype.insight.brain.report.InnerSourceUtils;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.focused;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.sleep;
import static com.sonatype.clm.testing.functional.utils.ScrollUtil.scrollIntoView;
import static org.junit.Assert.assertTrue;

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
    PrioritiesPage prioritiesPage = new PrioritiesPage();
    prioritiesPage.title().shouldHave(text(title(0)));

    // Go back to the report list page
    prioritiesPage.backLink().click();
    DeveloperReportListPage.title().shouldHave(text("Priorities"));
  }

  @Test
  public void testPrioritiesReportPage_shouldEnterFullReportAndBack() {
    refreshOrOpen(DeveloperReportListPage.url());
    DeveloperReportListPage.reportListTable().findAll(byText("View Priorities")).first().click();
    PrioritiesPage prioritiesPage = new PrioritiesPage();
    prioritiesPage.title().shouldHave(text(title(0)));
    NxDropdown viewDropdown = prioritiesPage.viewDropdown();
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
    prioritiesPage.title().shouldHave(text("Priorities"));
  }

  @Test
  public void testPrioritiesReportPage_shouldListViolatingComponentsProperly() {
    refreshOrOpen(DeveloperReportListPage.url());
    DeveloperReportListPage.reportListTable().findAll(byText("View Priorities")).first().click();
    PrioritiesPage prioritiesPage = new PrioritiesPage();
    prioritiesPage.title().shouldHave(text(title(0)));

    prioritiesPage.prioritiesTableRows().shouldHave(size(TOTAL_PRIORITIES_PER_PAGE));

    prioritiesPage.prioritiesTableCell(0, 1).shouldHave(text("com.mycila : license-maven-plugin : 2.11"));

    prioritiesPage.prioritiesTableCell(1, 1).shouldHave(text("com.vaadin.addon : vaadin-touchkit-agpl : 3.0.0-beta1"));

    prioritiesPage.prioritiesTableCell(2, 1)
        .shouldHave(text("org.springframework.security : spring-security-web : 3.2.4.RELEASE"));

    ScrollUtil.scrollIntoView(prioritiesPage.lastPageLink());
    prioritiesPage.lastPageLink().shouldHave(text("2")).click();

    ElementsCollection lastPage = prioritiesPage.prioritiesTableRows();

    lastPage.shouldHave(size(13));
  }

  @Test
  public void testPrioritiesReportPage_shouldOpenComponentDetailsPageFromRow() {
    refreshOrOpen(DeveloperReportListPage.url());
    DeveloperReportListPage.reportListTable().findAll(byText("View Priorities")).get(1).click();
    PrioritiesPage prioritiesPage = new PrioritiesPage();
    prioritiesPage.title().shouldHave(text(title(1)));

    prioritiesPage.rowComponentLink(0).click();

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

  @Test
  public void testPrioritiesReportPage_shouldFilterByComponentNameWhenNotOnFirstPage() {
    refreshOrOpen(DeveloperReportListPage.url());
    DeveloperReportListPage.reportListTable().findAll(byText("View Priorities")).get(1).click();
    PrioritiesPage prioritiesPage = new PrioritiesPage();
    prioritiesPage.title().shouldHave(text(title(1)));

    prioritiesPage.nextPageButton().shouldBe(visible).click();
    prioritiesPage.componentNameFilter().type("http");

    prioritiesPage.prioritiesTableCell(0, 1).shouldHave(text("http"));
  }

  @Test
  public void testComponentFilterInputRetainsFocusWhileTyping() {
    refreshOrOpen(DeveloperReportListPage.url());
    DeveloperReportListPage.reportListTable().findAll(byText("View Priorities")).get(1).click();
    PrioritiesPage page = new PrioritiesPage();
    page.componentNameFilter().shouldBe(visible);
    page.componentNameFilter().click();
    page.componentNameFilter().shouldBe(focused);

    // Type multiple characters and verify focus is retained after each keystroke
    String filterText = "spring";
    for (char c : filterText.toCharArray()) {
      page.componentNameFilter().sendKeys(String.valueOf(c));
      page.componentNameFilter().shouldBe(focused);
      sleep(50); // Small delay between keystrokes to simulate real typing
    }

    // Verify the full text was captured
    page.componentNameFilter().shouldHave(value(filterText));

    // Verify URL updated with filter parameter after debounce
    sleep(500);
    String currentUrl = WebDriverRunner.url();
    assertTrue("URL should contain filter parameter",
        currentUrl.contains("componentNameFilter=" + filterText));

    page.componentNameFilter().shouldBe(focused);
  }

  @Test
  public void testComponentFilterInputRetainsFocusWhenCleared() {
    refreshOrOpen(DeveloperReportListPage.url());
    DeveloperReportListPage.reportListTable().findAll(byText("View Priorities")).get(1).click();
    PrioritiesPage page = new PrioritiesPage();
    page.componentNameFilter().shouldBe(visible);
    page.componentNameFilter().click();
    page.componentNameFilter().shouldBe(focused);

    // Type text in the filter input
    String filterText = "springframework";
    page.componentNameFilter().setValue(filterText);
    page.componentNameFilter().shouldHave(value(filterText));
    page.componentNameFilter().shouldBe(focused);

    // Clear all characters - THIS IS THE CRITICAL TEST
    page.componentNameFilter().clear();

    // Verify focus is STILL on the input when empty
    page.componentNameFilter().shouldBe(focused);

    // Verify input is actually empty
    page.componentNameFilter().shouldHave(value(""));

    // Verify focus remains even after a short delay (after data loads)
    sleep(300);
    page.componentNameFilter().shouldBe(focused);
  }

  @Test
  public void testPrioritiesReportPage_shouldRetainPaginationStateWhenNavigatedToCDPAndBack() {
    refreshOrOpen(DeveloperReportListPage.url());
    DeveloperReportListPage.reportListTable().findAll(byText("View Priorities")).get(1).click();
    PrioritiesPage prioritiesPage = new PrioritiesPage();
    prioritiesPage.title().shouldHave(text(title(1)));

    prioritiesPage.nextPageButton().shouldBe(visible).click();

    prioritiesPage.rowComponentLink(0).click();

    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();
    componentDetailsPage.shouldBe(visible);
    componentDetailsPage.backButton().click();

    prioritiesPage.title().shouldBe(visible);

    prioritiesPage.currentPageButton().shouldHave((text("2")));
  }

  private String title(int appId) {
    return "appName" + appId + " - Priorities";
  }

  private void setUpAppsWithPriorities() {
    try {
      setUpMainApp(0, "/canned-reports/large-report");
      setUpMainApp(1, "/canned-reports/report-with-dependency-tree");
      setUpInnerSourceVersion();
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

  private void setUpInnerSourceVersion() {
    ApplicationDAO applicationDAO = lookup(ApplicationDAO.class);
    Application appId1 = applicationDAO.getByPublicId("appId1");
    // add inner source data
    ComponentIdentifier innersourceDirectComponent =
        ComponentIdentifier.createMavenCoordinates("org.jclouds.driver", "jclouds-enterprise", "1.3.1", "", "jar");
    PackageUrlIdentifier versionlessPurl = InnerSourceUtils.getVersionlessPackageUrl(innersourceDirectComponent);
    InnerSourceApplication innerSourceApplication =
        tempEntity.newInnerSourceApplication(versionlessPurl.getPackageUrl(), appId1);
    tempEntity.newInnerSourceVersion(innerSourceApplication, "1.4.0", StageTypes.BUILD.getId());
  }
}
