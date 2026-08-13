/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.pages.ApplicationLatestEvaluationsPage;
import com.sonatype.clm.testing.playwright.pages.ApplicationLatestEvaluationsPageAssertions;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;

import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Regression coverage for the Application Latest Evaluations page — a direct-URL landing test
 * (the manual walkthrough via Actions → View Stage Report is a UI-Router affordance; the SPA
 * mounts the same component for the direct route, which is what this test verifies).
 */
public class ApplicationLatestEvaluationsPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String APP_NAME_PREFIX = "pw-le-app";

  private static final String APP_ID_PREFIX = "pw-le-app-id";

  private static final String ORG_NAME_PREFIX = "pw-le-org";

  @Test
  @Category(RegressionTest.class)
  public void testApplicationLatestEvaluationsPage_renders() {
    String suffix = TemporaryEntity.uuid();
    Organization org = tempEntity.newOrganization(ORG_NAME_PREFIX + "-" + suffix);
    Application application = tempEntity.newApplication(
        APP_NAME_PREFIX + "-" + suffix, APP_ID_PREFIX + "-" + suffix, org.getId());

    playwrightRefreshOrOpen(
        ApplicationLatestEvaluationsPage.url(application.getPublicId(), StageTypes.BUILD.getId()));
    playwrightLogin();

    ApplicationLatestEvaluationsPage page = new ApplicationLatestEvaluationsPage();
    ApplicationLatestEvaluationsPageAssertions assertions = new ApplicationLatestEvaluationsPageAssertions(page);
    assertions.shouldBeLoaded();
    assertions.shouldHaveHeadingContaining("Latest Evaluations");
    assertions.shouldShowStageDescription();
  }
}
