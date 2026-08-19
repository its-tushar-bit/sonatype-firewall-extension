/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.io.IOException;
import java.net.URL;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.ApplicationReportPage;
import com.sonatype.clm.testing.playwright.pages.DependencyTreePage;
import com.sonatype.clm.testing.playwright.pages.SastScanPage;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;
import com.sonatype.clm.testing.playwright.utils.TestReportEvaluator;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.sast.SastFinding;
import com.sonatype.insight.brain.model.sast.SastFindingSeverity;
import com.sonatype.insight.brain.model.sast.SastScan;
import com.sonatype.insight.brain.policy.PolicyExportResult;
import com.sonatype.insight.brain.policy.PolicyImportExport;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.json.store.JsonUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Regression tests for the Dependency Tree tab ({@code applicationReport.dependencyTree} route)
 * and the SAST Scan page. Sanity coverage for the application report lives in
 * {@link ApplicationReportPlaywrightTest}.
 */
public class DependencyTreePlaywrightTest
    extends AbstractIqUiTest
{
  private static final String SCAN_ID = "e16caf35769f4b3186a7e416d34c2797";

  private static final String REPORT_DIR = "/canned-reports/large-report";

  private static final String DEP_TREE_HEADING = "Dependency Tree";

  private static final String SAST_RULE_NAME = "Test SAST Rule";

  private static final String SAST_CWE = "CWE-89";

  private static final String SAST_FINDING_DESCRIPTION = "Test finding description";

  private static final int SAST_FINDING_LINE_NUMBER = 42;

  private static final String SAST_FINDING_COORDINATE = "{\"name\":\"TestClass\"}";

  private static final String SAST_PAGE_HEADING = "SAST Scan";

  private static final String SAST_FINDINGS_HEADING = "SAST Findings";

  private Application app;

  // @BeforeAll not used: seeding — including evaluatePolicy(), an expensive full-scan cycle —
  // depends on the instance-level TemporaryEntity, which is inaccessible from a static
  // @BeforeAll context. evaluatePolicy() therefore runs once before each test.
  @BeforeEach
  public void seedAndLogin() throws IOException {
    String suffix = TemporaryEntity.uuid();
    Organization org = tempEntity.newOrganization("DependencyTreeTestOrg-" + suffix);
    PolicyExportResult referencePolicies = JsonUtils.parse(
        getClass().getResource("/reference-policies-v3.json").openStream(),
        PolicyExportResult.class);
    lookup(PolicyImportExport.class).importOrganization(org, referencePolicies);
    app = tempEntity.newApplication(
        "DependencyTreeTestApp-" + suffix,
        "DependencyTreeTestApp-" + suffix,
        org.getId());
    URL zippedReport = ReportHelper.zipReport(REPORT_DIR, tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    new TestReportEvaluator(app, SCAN_ID, zippedReport, baseUrlFromTest, work).evaluatePolicy();
    playwrightRefreshOrOpen(DependencyTreePage.url(app, SCAN_ID));
    playwrightLogin();
  }

  /** Opens the application report, clicks the Dependency Tree tab, and verifies full page render. */
  @Test
  @Tag("regression")
  public void testDependencyTree_pageRendersWithTreeAndControls() {
    playwrightRefreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
    ApplicationReportPage reportPage = new ApplicationReportPage();
    assertThat(reportPage.appReportMain()).isVisible(PlaywrightTiming.VISIBLE_OPTS);

    reportPage.navigateToDependencyTree();

    DependencyTreePage depTreePage = new DependencyTreePage();
    assertThat(depTreePage.container()).isVisible(PlaywrightTiming.VISIBLE_OPTS);
    assertThat(depTreePage.heading()).containsText(DEP_TREE_HEADING);
    assertThat(depTreePage.expandAllButton()).isVisible();
    assertThat(depTreePage.collapseAllButton()).isVisible();
    assertThat(depTreePage.tree()).isVisible();
    assertThat(depTreePage.treeRootLabel()).containsText(app.getName());
    assertThat(depTreePage.treeChildItems().first()).isVisible();
  }

  /**
   * "Collapse All" drives the tree to a known state; then a per-node toggle expand/collapses
   * a child node.
   */
  @Test
  @Tag("regression")
  public void testDependencyTree_expandCollapseNodes() {
    DependencyTreePage depTreePage = new DependencyTreePage();

    assertThat(depTreePage.container()).isVisible(PlaywrightTiming.VISIBLE_OPTS);

    depTreePage.collapseAllButton().click();
    assertThat(depTreePage.firstCollapsibleChildPanel()).isHidden();

    depTreePage.firstCollapsibleChildToggle().click();
    assertThat(depTreePage.firstCollapsibleChildPanel()).isVisible();

    depTreePage.collapseAllButton().click();
    assertThat(depTreePage.firstCollapsibleChildPanel()).isHidden();
  }

  /** Verifies tree nodes with violations render NxThreatIndicators at the correct severity. */
  @Test
  @Tag("regression")
  public void testDependencyTree_violationIndicators() {
    DependencyTreePage depTreePage = new DependencyTreePage();

    assertThat(depTreePage.container()).isVisible(PlaywrightTiming.VISIBLE_OPTS);

    assertThat(depTreePage.treeViolationIndicators().first()).isVisible();
    assertThat(depTreePage.treeCriticalIndicators().first()).isVisible();
    assertThat(depTreePage.treeSevereIndicators().first()).isVisible();
  }

  /** SAST Scan page renders the heading, findings section, and at least one finding row. */
  @Test
  @Tag("regression")
  public void testSastScan_pageRendersWithFindings() {
    SastScan sastScan = tempEntity.newSastScan(app.getId());
    SastFinding sastFinding = new SastFinding();
    sastFinding.setSastScanId(sastScan.getId());
    sastFinding.setRuleName(SAST_RULE_NAME);
    sastFinding.setSeverity(SastFindingSeverity.HIGH);
    sastFinding.setCwe(SAST_CWE);
    sastFinding.setDescription(SAST_FINDING_DESCRIPTION);
    sastFinding.setLineNumber(SAST_FINDING_LINE_NUMBER);
    sastFinding.setCoordinate(SAST_FINDING_COORDINATE);
    tempEntity.newSastFinding(sastFinding);

    playwrightRefreshOrOpen(SastScanPage.url(app.getPublicId(), sastScan.getId()));
    SastScanPage sastPage = new SastScanPage();

    assertThat(sastPage.findingsContainer()).isVisible(PlaywrightTiming.VISIBLE_OPTS);
    assertThat(sastPage.pageHeading()).containsText(SAST_PAGE_HEADING);
    assertThat(sastPage.findingsHeading()).containsText(SAST_FINDINGS_HEADING);
    assertThat(sastPage.findingRows().first()).isVisible();
  }
}
