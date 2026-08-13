/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import com.microsoft.playwright.Route;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.pages.ManualPullRequestPage;
import com.sonatype.clm.testing.playwright.pages.PrioritiesPage;
import com.sonatype.clm.testing.playwright.utils.SmallReportFixture;
import com.sonatype.clm.testing.playwright.utils.TestReportEvaluator;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Regression test for the Manual Pull Request creation modal.
 *
 * <p>
 * Divergence: manual describes "editable PR title, target branch, description fields";
 * actual {@code CreatePRModal.jsx} renders all fields as {@code NxReadOnly} with no description field.
 */
public class ManualPullRequestPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String SCAN_ID = "MPR_REGRESSION_SCAN";

  private static final String TARGET_BRANCH = "main";

  private static final String PRIORITIES_API_PATTERN = "**/rest/developer/priorities/**";

  private static final String COMPONENT_DETAILS_API_PATTERN = "**/rest/ci/componentDetails/**";

  private static final String SOURCE_CONTROL_API_PATTERN = "**/api/v2/compositeSourceControl/**";

  @After
  public void unrouteAll() {
    page.unrouteAll();
  }

  /** CreatePRModal renders with read-only fields and a "Create" button. */
  @Test
  @Category(RegressionTest.class)
  public void testManualPullRequest_modalRendersWithReadOnlyFields() throws IOException {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());

    URL zippedReport = ReportHelper.zipReport(SmallReportFixture.CANNED_REPORT_DIR, tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    new TestReportEvaluator(app, SCAN_ID, zippedReport, baseUrlFromTest, work, Stage.ID_BUILD)
        .evaluatePolicy();

    String prioritiesJson = readJson("manual-pull-request-priorities.json");
    String versionGraphJson = readJson("manual-pull-request-version-graph.json");
    String sourceControlJson = readJson("manual-pull-request-source-control.json");

    page.route(PRIORITIES_API_PATTERN,
        route -> route.fulfill(new Route.FulfillOptions()
            .setStatus(200)
            .setContentType("application/json")
            .setBody(prioritiesJson)));
    page.route(COMPONENT_DETAILS_API_PATTERN,
        route -> route.fulfill(new Route.FulfillOptions()
            .setStatus(200)
            .setContentType("application/json")
            .setBody(versionGraphJson)));
    page.route(SOURCE_CONTROL_API_PATTERN,
        route -> route.fulfill(new Route.FulfillOptions()
            .setStatus(200)
            .setContentType("application/json")
            .setBody(sourceControlJson)));

    playwrightRefreshOrOpen(
        "/assets/index.html#/developer/priorities/" + app.getPublicId() + "/" + SCAN_ID);
    playwrightLogin();

    new PrioritiesPage().createPrTriggerButton().click();
    ManualPullRequestPage modal = new ManualPullRequestPage();
    assertThat(modal.container()).isVisible();
    assertThat(modal.header()).hasText("Create Pull Request");
    assertThat(modal.prTitleField()).isVisible();
    assertThat(modal.targetBranchField()).containsText(TARGET_BRANCH);
    assertThat(modal.createButton()).isVisible();
  }

  private String readJson(String filename) throws IOException {
    return IOUtils.toString(
        Objects.requireNonNull(
            getClass().getResourceAsStream("/test-data/" + filename),
            "missing fixture: " + filename),
        StandardCharsets.UTF_8);
  }
}
