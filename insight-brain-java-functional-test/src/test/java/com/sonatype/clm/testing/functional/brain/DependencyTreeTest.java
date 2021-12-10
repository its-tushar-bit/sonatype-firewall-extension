/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.io.IOException;
import java.net.URL;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.MainHeader;
import com.sonatype.clm.testing.functional.elements.NxTooltip;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage;
import com.sonatype.clm.testing.functional.pages.ComponentDetailsPage;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.DependencyTreePage;
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
import com.codeborne.selenide.SelenideElement;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

public class DependencyTreeTest
    extends AbstractFunctionalTest
{
  private static final String SCAN_ID = "e16caf35769f4b3186a7e416d34c2797";

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
    URL referencePolicyUrl = getClass().getResource("/reference-policies-v3.json");
    PolicyExportResult referencePolicies = JsonUtils.parse(referencePolicyUrl.openStream(), PolicyExportResult.class);
    PolicyImportExport policyImportExport = new PolicyImportExport();

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
    ElementsCollection treeItems = dependencyTreePage.tree().treeItems();

    treeItems.shouldHaveSize(36);

    treeItems.get(0).shouldHave(text("ApplicationReportTest"));

    treeItems.get(1).shouldHave(text("org.jclouds.driver : jclouds-enterprise : 1.3.1"));
    treeItems.get(2).shouldHave(text("org.jclouds.driver : jclouds-bouncycastle : 1.3.1"));
    treeItems.get(3).shouldHave(text("tomcat : catalina-host-manager : 5.5.23"));
    treeItems.get(4).shouldHave(text("org.opencms.modules : com.alkacon.opencms.v8.twitter : 8.0.2"));
    treeItems.get(5).shouldHave(text("edu.stanford.ejalbert : browserlauncher2 : 1.3"));
    treeItems.get(6).shouldHave(text("org.apache.geronimo.framework : geronimo-security : 2.1"));
    treeItems.get(7).shouldHave(text("org.openid4java : openid4java : 0.9.5"));
    treeItems.get(8).shouldHave(text("edu.ucar : unidatacommon : 4.2.20"));
    treeItems.get(9).shouldHave(text("org.slf4j : slf4j-api : 1.6.1"));
    treeItems.get(10).shouldHave(text("geronimo : geronimo-tomcat-builder : 1.1"));
    treeItems.get(11).shouldHave(text("geronimo : geronimo-tomcat : 1.0"));
    treeItems.get(12).shouldHave(text("commons-beanutils : commons-beanutils : 1.8.3"));
    treeItems.get(13).shouldHave(text("tomcat : servlets-default : 5.5.4"));
    treeItems.get(14).shouldHave(text("tomcat : tomcat-util : 5.5.23"));
    treeItems.get(15).shouldHave(text("tomcat : servlets-default : 5.5.4"));
    treeItems.get(16).shouldHave(text("tomcat : tomcat-util : 5.5.23"));
    treeItems.get(17).shouldHave(text("ch.qos.logback : logback-access : 0.6"));
    treeItems.get(18).shouldHave(text("org.mortbay.jetty : jetty : 6.1.15"));
    treeItems.get(19).shouldHave(text("org.apache.flume : flume-ng-node : 1.0.0-incubating"));
    treeItems.get(20).shouldHave(text("org.apache.flume : flume-ng-core : 1.0.0-incubating"));
    treeItems.get(21).shouldHave(text("org.apache.avro : avro-ipc : 1.5.0"));
    treeItems.get(22).shouldHave(text("org.mortbay.jetty : jetty : 6.1.15"));
    treeItems.get(23).shouldHave(text("org.slf4j : slf4j-api : 1.6.1"));
    treeItems.get(24).shouldHave(text("org.slf4j : slf4j-api : 1.6.1"));
    treeItems.get(25).shouldHave(text("org.apache.flume.flume-ng-channels : flume-jdbc-channel : 1.0.0-incubating"));
    treeItems.get(26).shouldHave(text("commons-dbcp : commons-dbcp : 1.4"));
    treeItems.get(27).shouldHave(text("commons-pool : commons-pool : 1.4"));
    treeItems.get(28).shouldHave(text("org.apache.flume : flume-ng-core : 1.0.0-incubating"));
    treeItems.get(29).shouldHave(text("org.slf4j : slf4j-api : 1.6.1"));
    treeItems.get(30).shouldHave(text("net.sf.xradar : xradar : 1.1.2"));
    treeItems.get(31).shouldHave(text("cobertura : cobertura : 1.6"));
    treeItems.get(32).shouldHave(text("javancss : javancss : 29.50"));
    treeItems.get(33).shouldHave(text("javancss : javancss : 29.50"));
    treeItems.get(34).shouldHave(text("org.apache.lucene : lucene-spellchecker : 2.9.0"));
    treeItems.get(35).shouldHave(text("apache-httpclient : commons-httpclient : 3.1"));

    eyesWatcher.eyesCheck("dependency tree page");

    MainHeader.backButton().shouldHave(text("Back to Application Report"));
    MainHeader.backButton().click();
    waitUntilUrl(ApplicationReportPage.url(app, SCAN_ID));
  }

  @Test
  public void testDependencyTree_innerSource() {
    ElementsCollection treeItems = dependencyTreePage.tree().treeItems();

    SelenideElement firstTreeItem = treeItems.get(1);
    firstTreeItem.shouldHave(text("org.jclouds.driver : jclouds-enterprise : 1.3.1"));

    SelenideElement dependencyIndicator = dependencyTreePage.tree().dependencyTypeIndicator(firstTreeItem);
    dependencyIndicator.shouldHave(text("IS"));

    dependencyIndicator.hover();
    NxTooltip tooltip = new NxTooltip();
    tooltip.shouldHave(text("InnerSource"));

    eyesWatcher.eyesCheck("dependency tree innerSource dependency indicator");
  }

  @Test
  public void testDependencyTree_linking() {
    dependencyTreePage.tree().shouldBe(visible);
    ElementsCollection clickableTreeItems = dependencyTreePage.tree().clickableTreeItems();

    clickableTreeItems.shouldHaveSize(35);

    clickableTreeItems.get(0).shouldHave(text("org.jclouds.driver : jclouds-enterprise : 1.3.1"));
    clickableTreeItems.get(0).click();

    waitUntilUrl(ComponentDetailsPage.urlToOverview(app, SCAN_ID, "891b3de68f449f8a1ad2"));
    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();

    SelenideElement title = componentDetailsPage.header().title();
    title.shouldHave(text("org.jclouds.driver : jclouds-enterprise : 1.3.1"));
  }

  @Test
  public void testDependencyTree_navigateBackFromComponentDetailsPage() {
    dependencyTreePage.tree().shouldBe(visible);
    ElementsCollection clickableTreeItems = dependencyTreePage.tree().clickableTreeItems();

    clickableTreeItems.get(0).click();
    waitUntilUrl(ComponentDetailsPage.urlToOverview(app, SCAN_ID, "891b3de68f449f8a1ad2"));

    eyesWatcher.eyesCheck("component details page from dependency tree");

    SelenideElement menuBarBackButton = MainHeader.backButton();
    menuBarBackButton.shouldHave(text("Back to Dependency Tree"));
    menuBarBackButton.click();

    waitUntilUrl(DependencyTreePage.url(app, SCAN_ID));
  }
}
