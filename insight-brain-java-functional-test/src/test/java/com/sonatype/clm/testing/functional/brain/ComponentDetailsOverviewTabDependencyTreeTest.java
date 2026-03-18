/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.io.IOException;
import java.net.URL;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.NxTree;
import com.sonatype.clm.testing.functional.elements.componentdetails.DependencyTreeTile;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage;
import com.sonatype.clm.testing.functional.pages.ComponentDetailsPage;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
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
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.utils.ScrollUtil.scrollIntoView;

public class ComponentDetailsOverviewTabDependencyTreeTest
    extends AbstractFunctionalTest
{
  private static final String SCAN_ID = "e16caf35769f4b3186a7e416d34c2797";

  private Organization org;

  private Application app;

  private TestReportEvaluator evaluator;

  @Before
  public void start() throws IOException {
    URL referencePolicyUrl = getClass().getResource("/reference-policies-v3.json");
    PolicyExportResult referencePolicies = JsonUtils.parse(referencePolicyUrl.openStream(), PolicyExportResult.class);
    PolicyImportExport policyImportExport = lookup(PolicyImportExport.class);

    org = tempEntity.newOrganization("Test Organization");
    policyImportExport.importOrganization(org, referencePolicies);
    app = tempEntity.newApplication("ApplicationReportTest", "ApplicationReportTest", org.getId());

    URL zippedReport = ReportHelper.zipReport("/canned-reports/report-with-dependency-tree", tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    evaluator = new TestReportEvaluator(app, SCAN_ID, zippedReport, baseUrlFromTest, work);
    evaluator.evaluatePolicy();
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
  }

  @BeforeClass
  public static void startup() {
    refreshOrOpen(DashboardPage.url());
    loginAsAdmin();
  }

  @Test
  public void testOverviewTab_DependencyTreeTile() {
    refreshOrOpen(
        ComponentDetailsPage.urlToOverview(app, SCAN_ID, "494308fc2d433720c778"));
    waitUntilUrl(
        ComponentDetailsPage.urlToOverview(app, SCAN_ID, "494308fc2d433720c778"));

    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();

    DependencyTreeTile dependencyTreeTile = componentDetailsPage.dependencyTreeTile();
    ScrollUtil.scrollIntoView(dependencyTreeTile.title());
    dependencyTreeTile.shouldBe(visible);
    dependencyTreeTile.title().shouldHave(text("Dependency Tree"));

    final String THREAT_CRITICAL_CLASS = "nx-threat-indicator--critical";
    final String THREAT_NONE_CLASS = "nx-threat-indicator--none";
    final NxTree nxTree = dependencyTreeTile.tree();

    ElementsCollection clickableTreeItems = nxTree.clickableTreeItems();
    ElementsCollection nonClickableTreeItems = nxTree.nonClickableTreeItems();
    ElementsCollection threatIndicators = nxTree.threatIndicators();

    nxTree.treeItems().get(0).shouldHave(text("ApplicationReportTest"));
    clickableTreeItems.shouldHave(size(4));
    scrollIntoView(clickableTreeItems.get(0), true);

    clickableTreeItems.get(0).shouldHave(text("ch.qos.logback : logback-access : 0.6"));
    threatIndicators.get(0).shouldHave(cssClass(THREAT_CRITICAL_CLASS));

    clickableTreeItems.get(1).shouldHave(text("org.apache.flume : flume-ng-node : 1.0.0-incubating"));
    threatIndicators.get(1).shouldHave(cssClass(THREAT_CRITICAL_CLASS));

    clickableTreeItems.get(2).shouldHave(text("org.apache.flume : flume-ng-core : 1.0.0-incubating"));
    threatIndicators.get(2).shouldHave(cssClass(THREAT_NONE_CLASS));

    clickableTreeItems.get(3).shouldHave(text("org.apache.avro : avro-ipc : 1.5.0"));
    threatIndicators.get(3).shouldHave(cssClass(THREAT_NONE_CLASS));

    nonClickableTreeItems.shouldHave(size(2));

    nonClickableTreeItems.get(0).shouldHave(text("org.mortbay.jetty : jetty : 6.1.15"));
    threatIndicators.get(1).shouldHave(cssClass(THREAT_CRITICAL_CLASS));
    nonClickableTreeItems.get(0).shouldNotHave(attribute("a"));

    nonClickableTreeItems.get(1).shouldHave(text("org.mortbay.jetty : jetty : 6.1.15"));
    threatIndicators.get(5).shouldHave(cssClass(THREAT_CRITICAL_CLASS));
    nonClickableTreeItems.get(1).shouldNotHave(attribute("a"));

    eyesWatcher.eyesCheck("Overview tab with dependency tree tile");
  }

  @Test
  public void testOverviewTab_dependencyTreeTile_emptyTree() throws Exception {
    final String SCAN_ID2 = "e16caf35769fab230987fabc43ad801a";
    Application app2 = tempEntity.newApplication("ApplicationReportTest2", "ApplicationReportTest2", org.getId());

    URL zippedReport = ReportHelper.zipReport("/canned-reports/large-report", tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    evaluator = new TestReportEvaluator(app2, SCAN_ID2, zippedReport, baseUrlFromTest, work);
    evaluator.evaluatePolicy();

    refreshOrOpen(
        ComponentDetailsPage.urlToOverview(app2, SCAN_ID2, "dc810b3d25f9e8c930f5"));
    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();

    DependencyTreeTile dependencyTreeTile = componentDetailsPage.dependencyTreeTile();
    ScrollUtil.scrollIntoView(dependencyTreeTile.title());
    dependencyTreeTile.unavailableAlert().shouldBe(visible).shouldHave(text("Dependency tree not available"));
  }

  @Test
  public void testOverviewTab_DependencyTreeTile_InitialState() {
    refreshOrOpen(
        ComponentDetailsPage.urlToOverview(app, SCAN_ID, "ae81d32288bf8419181f"));
    waitUntilUrl(
        ComponentDetailsPage.urlToOverview(app, SCAN_ID, "ae81d32288bf8419181f"));
    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();

    DependencyTreeTile dependencyTreeTile = componentDetailsPage.dependencyTreeTile();
    dependencyTreeTile.shouldBe(visible);
    dependencyTreeTile.title().shouldHave(text("Dependency Tree"));

    final NxTree nxTree = dependencyTreeTile.tree();

    ElementsCollection clickableTreeItems = nxTree.clickableTreeItems();
    ElementsCollection nonClickableTreeItems = nxTree.nonClickableTreeItems();
    ElementsCollection clickableIcons = nxTree.collapseIcons();

    nxTree.treeItems().get(0).shouldHave(text("ApplicationReportTest"));
    clickableTreeItems.shouldHave(size(4));
    scrollIntoView(clickableTreeItems.get(0), true);

    clickableTreeItems.get(0).shouldHave(text("org.apache.flume : flume-ng-node : 1.0.0-incubating"));

    clickableTreeItems.get(1).shouldHave(text("org.apache.flume : flume-ng-core : 1.0.0-incubating"));

    clickableTreeItems.get(0).shouldBe(visible);
    clickableTreeItems.get(1).shouldBe(visible);
    clickableTreeItems.get(2).shouldNotBe(visible);
    clickableTreeItems.get(3).shouldNotBe(visible);

    nonClickableTreeItems.shouldHave(size(1));

    nonClickableTreeItems.get(0).shouldHave(text("org.apache.avro : avro-ipc : 1.5.0"));

    clickableIcons.get(2).click();
    clickableTreeItems.get(2).shouldBe(visible);
    clickableTreeItems.get(3).shouldBe(visible);

    clickableTreeItems.get(2).shouldHave(text("org.mortbay.jetty : jetty : 6.1.15"));

    clickableTreeItems.get(3).shouldHave(text("org.slf4j : slf4j-api : 1.6.1"));
  }

  @Test
  public void testOverviewTab_DependencyTreeTileInitialStateDirectDependency() {
    refreshOrOpen(
        ComponentDetailsPage.urlToOverview(app, SCAN_ID, "ad19001bd021002377c2"));
    waitUntilUrl(
        ComponentDetailsPage.urlToOverview(app, SCAN_ID, "ad19001bd021002377c2"));
    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();

    DependencyTreeTile dependencyTreeTile = componentDetailsPage.dependencyTreeTile();
    ScrollUtil.scrollIntoView(dependencyTreeTile.title());
    dependencyTreeTile.shouldBe(visible);
    dependencyTreeTile.title().shouldHave(text("Dependency Tree"));

    final NxTree nxTree = dependencyTreeTile.tree();

    ElementsCollection clickableTreeItems = nxTree.clickableTreeItems();
    ElementsCollection nonClickableTreeItems = nxTree.nonClickableTreeItems();

    nxTree.treeItems().get(0).shouldHave(text("ApplicationReportTest"));
    clickableTreeItems.shouldHave(size(10));
    scrollIntoView(clickableTreeItems.get(0), true);

    clickableTreeItems.get(0).shouldBe(visible);
    clickableTreeItems.get(1).shouldBe(visible);
    clickableTreeItems.get(2).shouldBe(visible);
    clickableTreeItems.get(4).shouldBe(visible);
    clickableTreeItems.get(5).shouldBe(visible);
    clickableTreeItems.get(6).shouldBe(visible);
    clickableTreeItems.get(7).shouldBe(visible);
    clickableTreeItems.get(8).shouldBe(visible);
    clickableTreeItems.get(9).shouldBe(visible);

    nonClickableTreeItems.shouldHave(size(1));
  }
}
