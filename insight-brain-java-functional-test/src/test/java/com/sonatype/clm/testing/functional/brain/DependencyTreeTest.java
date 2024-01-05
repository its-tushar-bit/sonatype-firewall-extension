/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.MainHeader;
import com.sonatype.clm.testing.functional.elements.NxTooltip;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage;
import com.sonatype.clm.testing.functional.pages.ComponentDetailsPage;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.DependencyTreePage;
import com.sonatype.clm.testing.functional.utils.TestReportEvaluator;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDataHelper;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.policy.PolicyExportResult;
import com.sonatype.insight.brain.policy.PolicyImportExport;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.json.store.JsonUtils;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;

public class DependencyTreeTest
    extends AbstractFunctionalTest
{
  private static final String SCAN_ID = "e16caf35769f4b3186a7e416d34c2797";

  private static final String THREAT_CRITICAL_CLASS = "nx-threat-indicator--critical";

  private static final String THREAT_SEVERE_CLASS = "nx-threat-indicator--severe";

  private static final String THREAT_NONE_CLASS = "nx-threat-indicator--none";

  private final DependencyTreePage dependencyTreePage = new DependencyTreePage();

  private Application app;

  private TestReportEvaluator evaluator;

  @BeforeClass
  public static void startup() {
    refreshOrOpen(DashboardPage.url());
    loginAsAdmin();
  }

  @Before
  public void start() throws IOException {
    LicenseThreatGroupDataHelper.createTestLicenseThreatGroups(tempEntity);

    URL referencePolicyUrl = getClass().getResource("/reference-policies-v3.json");
    PolicyExportResult referencePolicies = JsonUtils.parse(referencePolicyUrl.openStream(), PolicyExportResult.class);
    PolicyImportExport policyImportExport = lookup(PolicyImportExport.class);

    Organization org = tempEntity.newOrganization("Test Organization");
    policyImportExport.importOrganization(org, referencePolicies);
    app = tempEntity.newApplication("ApplicationReportTest", "ApplicationReportTest", org.getId());
    URL zippedReport = ReportHelper.zipReport("/canned-reports/report-with-dependency-tree", tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    evaluator = new TestReportEvaluator(app, SCAN_ID, zippedReport, Configuration.baseUrl, work);
    evaluator.evaluatePolicy();

    refreshOrOpen(DependencyTreePage.url(app, SCAN_ID));
  }

  @Test
  public void testDependencyTree() {
    dependencyTreePage.title().shouldBe(visible);
    dependencyTreePage.tree().shouldBe(visible);

    dependencyTreePage.tree().treeItems().get(0).shouldHave(text("ApplicationReportTest"));

    SelenideElement permanentMessage = dependencyTreePage.permanentMessage();
    permanentMessage.shouldHave(text("Only supported ecosystem components are displayed in dependency tree."));

    ElementsCollection treeItems = dependencyTreePage.tree().clickableTreeItems();
    ElementsCollection threatIndicators = dependencyTreePage.tree().threatIndicators();

    treeItems.shouldHaveSize(35);

    treeItems.get(0).shouldHave(text("org.apache.geronimo.framework : geronimo-security : 2.1"));
    threatIndicators.get(0).shouldHave(cssClass(THREAT_CRITICAL_CLASS));

    treeItems.get(1).shouldHave(text("ch.qos.logback : logback-access : 0.6"));
    threatIndicators.get(1).shouldHave(cssClass(THREAT_CRITICAL_CLASS));

    treeItems.get(2).shouldHave(text("org.mortbay.jetty : jetty : 6.1.15"));
    threatIndicators.get(2).shouldHave(cssClass(THREAT_CRITICAL_CLASS));

    treeItems.get(3).shouldHave(text("apache-httpclient : commons-httpclient : 3.1"));
    threatIndicators.get(3).shouldHave(cssClass(THREAT_CRITICAL_CLASS));

    treeItems.get(4).shouldHave(text("org.jclouds.driver : jclouds-enterprise : 1.3.1"));
    threatIndicators.get(4).shouldHave(cssClass(THREAT_SEVERE_CLASS));

    treeItems.get(5).shouldHave(text("org.jclouds.driver : jclouds-bouncycastle : 1.3.1"));
    threatIndicators.get(5).shouldHave(cssClass(THREAT_SEVERE_CLASS));

    treeItems.get(6).shouldHave(text("tomcat : catalina-host-manager : 5.5.23"));
    threatIndicators.get(6).shouldHave(cssClass(THREAT_SEVERE_CLASS));

    treeItems.get(7).shouldHave(text("org.openid4java : openid4java : 0.9.5"));
    threatIndicators.get(7).shouldHave(cssClass(THREAT_SEVERE_CLASS));

    treeItems.get(8).shouldHave(text("edu.ucar : unidatacommon : 4.2.20"));
    threatIndicators.get(8).shouldHave(cssClass(THREAT_SEVERE_CLASS));

    treeItems.get(9).shouldHave(text("org.slf4j : slf4j-api : 1.6.1"));
    threatIndicators.get(9).shouldHave(cssClass(THREAT_SEVERE_CLASS));

    treeItems.get(10).shouldHave(text("net.sf.xradar : xradar : 1.1.2"));
    threatIndicators.get(10).shouldHave(cssClass(THREAT_SEVERE_CLASS));

    treeItems.get(11).shouldHave(text("cobertura : cobertura : 1.6"));
    threatIndicators.get(11).shouldHave(cssClass(THREAT_NONE_CLASS));

    treeItems.get(12).shouldHave(text("javancss : javancss : 29.50"));
    threatIndicators.get(12).shouldHave(cssClass(THREAT_NONE_CLASS));

    treeItems.get(13).shouldHave(text("javancss : javancss : 29.50"));
    threatIndicators.get(13).shouldHave(cssClass(THREAT_NONE_CLASS));

    treeItems.get(14).shouldHave(text("org.apache.lucene : lucene-spellchecker : 2.9.0"));
    threatIndicators.get(14).shouldHave(cssClass(THREAT_SEVERE_CLASS));

    treeItems.get(15).shouldHave(text("org.opencms.modules : com.alkacon.opencms.v8.twitter : 8.0.2"));
    threatIndicators.get(15).shouldHave(cssClass(THREAT_NONE_CLASS));

    treeItems.get(16).shouldHave(text("edu.stanford.ejalbert : browserlauncher2 : 1.3"));
    threatIndicators.get(16).shouldHave(cssClass(THREAT_NONE_CLASS));

    treeItems.get(17).shouldHave(text("geronimo : geronimo-tomcat-builder : 1.1"));
    threatIndicators.get(17).shouldHave(cssClass(THREAT_NONE_CLASS));

    treeItems.get(18).shouldHave(text("geronimo : geronimo-tomcat : 1.0"));
    threatIndicators.get(18).shouldHave(cssClass(THREAT_CRITICAL_CLASS));

    treeItems.get(19).shouldHave(text("commons-beanutils : commons-beanutils : 1.8.3"));
    threatIndicators.get(19).shouldHave(cssClass(THREAT_CRITICAL_CLASS));

    treeItems.get(20).shouldHave(text("tomcat : tomcat-util : 5.5.23"));
    threatIndicators.get(20).shouldHave(cssClass(THREAT_CRITICAL_CLASS));

    treeItems.get(21).shouldHave(text("tomcat : servlets-default : 5.5.4"));
    threatIndicators.get(21).shouldHave(cssClass(THREAT_SEVERE_CLASS));

    treeItems.get(22).shouldHave(text("tomcat : tomcat-util : 5.5.23"));
    threatIndicators.get(22).shouldHave(cssClass(THREAT_CRITICAL_CLASS));

    treeItems.get(23).shouldHave(text("tomcat : servlets-default : 5.5.4"));
    threatIndicators.get(23).shouldHave(cssClass(THREAT_SEVERE_CLASS));

    treeItems.get(24).shouldHave(text("org.apache.flume : flume-ng-node : 1.0.0-incubating"));
    threatIndicators.get(24).shouldHave(cssClass(THREAT_NONE_CLASS));

    treeItems.get(25).shouldHave(text("org.slf4j : slf4j-api : 1.6.1"));
    threatIndicators.get(25).shouldHave(cssClass(THREAT_SEVERE_CLASS));

    treeItems.get(26).shouldHave(text("org.apache.flume : flume-ng-core : 1.0.0-incubating"));
    threatIndicators.get(26).shouldHave(cssClass(THREAT_NONE_CLASS));

    treeItems.get(27).shouldHave(text("org.slf4j : slf4j-api : 1.6.1"));
    threatIndicators.get(27).shouldHave(cssClass(THREAT_SEVERE_CLASS));

    treeItems.get(28).shouldHave(text("org.apache.avro : avro-ipc : 1.5.0"));
    threatIndicators.get(28).shouldHave(cssClass(THREAT_NONE_CLASS));

    treeItems.get(29).shouldHave(text("org.mortbay.jetty : jetty : 6.1.15"));
    threatIndicators.get(29).shouldHave(cssClass(THREAT_CRITICAL_CLASS));

    treeItems.get(30).shouldHave(text("org.slf4j : slf4j-api : 1.6.1"));
    threatIndicators.get(30).shouldHave(cssClass(THREAT_SEVERE_CLASS));

    treeItems.get(31).shouldHave(text("org.apache.flume.flume-ng-channels : flume-jdbc-channel : 1.0.0-incubating"));
    threatIndicators.get(31).shouldHave(cssClass(THREAT_NONE_CLASS));

    treeItems.get(32).shouldHave(text("commons-dbcp : commons-dbcp : 1.4"));
    threatIndicators.get(32).shouldHave(cssClass(THREAT_CRITICAL_CLASS));

    treeItems.get(33).shouldHave(text("commons-pool : commons-pool : 1.4"));
    threatIndicators.get(33).shouldHave(cssClass(THREAT_SEVERE_CLASS));

    treeItems.get(34).shouldHave(text("org.apache.flume : flume-ng-core : 1.0.0-incubating"));
    threatIndicators.get(34).shouldHave(cssClass(THREAT_NONE_CLASS));

    eyesWatcher.eyesCheck("dependency tree page");

    MainHeader.backButton().shouldHave(text("Back to Application Report"));
    MainHeader.backButton().click();
    waitUntilUrl(ApplicationReportPage.url(app, SCAN_ID));
  }

  @Test
  public void testDependencyTree_innerSource() {
    ElementsCollection treeItems = dependencyTreePage.tree().treeItems();

    SelenideElement innerSourceTreeItem = treeItems.get(5);
    innerSourceTreeItem.shouldHave(text("org.jclouds.driver : jclouds-enterprise : 1.3.1"));

    SelenideElement dependencyIndicator = dependencyTreePage.tree().dependencyTypeIndicator(innerSourceTreeItem);
    dependencyIndicator.shouldHave(text("IS"));

    dependencyIndicator.hover();
    NxTooltip tooltip = new NxTooltip();
    tooltip.shouldHave(text("InnerSource"));
  }

  @Test
  public void testDependencyTree_linking() {
    dependencyTreePage.tree().shouldBe(visible);
    ElementsCollection clickableTreeItems = dependencyTreePage.tree().clickableTreeItems();

    clickableTreeItems.shouldHaveSize(35);

    clickableTreeItems.get(0).shouldHave(text("org.apache.geronimo.framework : geronimo-security : 2.1"));
    clickableTreeItems.get(0).click();

    waitUntilUrl(ComponentDetailsPage.urlToOverview(app, SCAN_ID, "848d7549ef7ec13ce546"));
    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();

    SelenideElement title = componentDetailsPage.header().title();
    title.shouldHave(text("org.apache.geronimo.framework : geronimo-security : 2.1"));
  }

  @Test
  public void testDependencyTree_navigateBackFromComponentDetailsPage() {
    dependencyTreePage.tree().shouldBe(visible);
    ElementsCollection clickableTreeItems = dependencyTreePage.tree().clickableTreeItems();

    clickableTreeItems.get(0).click();
    waitUntilUrl(ComponentDetailsPage.urlToOverview(app, SCAN_ID, "848d7549ef7ec13ce546"));

    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();
    componentDetailsPage.footer().shouldNotBe(visible);

    SelenideElement menuBarBackButton = MainHeader.backButton();
    menuBarBackButton.shouldHave(text("Back to Dependency Tree"));
    menuBarBackButton.click();

    waitUntilUrl(DependencyTreePage.url(app, SCAN_ID));
  }

  @Test
  public void testDependencyTree_filterByDisplayName() {
    dependencyTreePage.tree().shouldBe(visible);
    ElementsCollection clickableTreeItems = dependencyTreePage.tree().clickableTreeItems();
    clickableTreeItems.shouldHaveSize(35);

    final String SEARCH_TERM = "geronimo-security";
    SelenideElement filterInput = dependencyTreePage.componentNameFilterInput();
    filterInput.setValue(SEARCH_TERM);

    clickableTreeItems.shouldHaveSize(1);
    SelenideElement highlightedTreeItemPortion =
        clickableTreeItems.first().find(By.cssSelector(".iq-dependency-tree-page__search-match"));
    highlightedTreeItemPortion.shouldHave(text(SEARCH_TERM));

    clickableTreeItems.get(0).click();
    waitUntilUrl(ComponentDetailsPage.urlToOverview(app, SCAN_ID, "848d7549ef7ec13ce546"));
    MainHeader.backButton().click();
    waitUntilUrl(DependencyTreePage.url(app, SCAN_ID));

    filterInput.shouldHave(value(SEARCH_TERM));
    clickableTreeItems.shouldHaveSize(1);

    filterInput.setValue("non existent component");
    clickableTreeItems.shouldHaveSize(0);
  }

  @Test
  public void testDependencyTree_persistBranchStatus() {
    dependencyTreePage.tree().shouldBe(visible);
    ElementsCollection treeItems = dependencyTreePage.tree().treeItems();
    treeItems.shouldHaveSize(36);

    SelenideElement filterInput = dependencyTreePage.componentNameFilterInput();
    filterInput.setValue("geronimo");
    treeItems.shouldHaveSize(9);

    treeItems.forEach(item -> item.shouldHave(attribute("aria-expanded", "true")));

    SelenideElement firstCollapsibleTreeItem = treeItems.get(2);
    dependencyTreePage.tree().collapseIconFor(firstCollapsibleTreeItem).click();
    firstCollapsibleTreeItem.shouldHave(attribute("aria-expanded", "false"));

    filterInput.setValue("");
    firstCollapsibleTreeItem.shouldHave(attribute("aria-expanded", "false"));
  }

  @Test
  public void testDependencyTree_expandAllNodesInFilteredTree() {
    dependencyTreePage.tree().shouldBe(visible);
    ElementsCollection treeItems = dependencyTreePage.tree().treeItems();
    treeItems.shouldHaveSize(36);

    LinkedList<SelenideElement> collapseIcons = new LinkedList<>(dependencyTreePage.tree().collapseIcons());
    collapseIcons.descendingIterator().forEachRemaining(SelenideElement::click);

    SelenideElement filterInput = dependencyTreePage.componentNameFilterInput();
    filterInput.setValue("geronimo");
    treeItems.shouldHaveSize(9);

    treeItems.forEach(item -> item.shouldHave(attribute("aria-expanded", "true")));
  }

  @Test
  public void testDependencyTree_expandAndCollapseAll() {
    dependencyTreePage.tree().shouldBe(visible);
    ElementsCollection treeItems = dependencyTreePage.tree().collapsibleTreeItems();

    treeItems.forEach(item -> item.shouldHave(attribute("aria-expanded", "true")));

    dependencyTreePage.collapseAllButton().click();

    treeItems.forEach(item -> item.shouldHave(attribute("aria-expanded", "false")));

    dependencyTreePage.expandAllButton().click();

    treeItems.forEach(item -> item.shouldHave(attribute("aria-expanded", "true")));
  }

  @Test
  public void testDependencyTree_expandAndCollapseAllInFilteredTree() {
    dependencyTreePage.tree().shouldBe(visible);
    SelenideElement filterInput = dependencyTreePage.componentNameFilterInput();
    ElementsCollection treeItems = dependencyTreePage.tree().collapsibleTreeItems();

    treeItems.forEach(item -> item.shouldHave(attribute("aria-expanded", "true")));

    filterInput.setValue("geronimo-security");
    dependencyTreePage.collapseAllButton().click();
    filterInput.setValue("");

    treeItems.forEach(item -> item.shouldHave(attribute("aria-expanded", "false")));

    filterInput.setValue("geronimo-security");
    dependencyTreePage.expandAllButton().click();
    filterInput.setValue("");

    treeItems.forEach(item -> item.shouldHave(attribute("aria-expanded", "true")));
  }

  public void testDependencyTree_EmptyMessage() throws IOException {
    URL zippedReport = ReportHelper.zipReport("/canned-reports/empty-report", tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    evaluator = new TestReportEvaluator(app, SCAN_ID, zippedReport, Configuration.baseUrl, work);
    evaluator.evaluatePolicy();

    refreshOrOpen(DependencyTreePage.url(app, SCAN_ID));
    SelenideElement emptyMessage = dependencyTreePage.emptyMessage();
    emptyMessage.shouldBe(visible);
    emptyMessage.shouldHave(text("Dependency tree not available."));
  }
}
