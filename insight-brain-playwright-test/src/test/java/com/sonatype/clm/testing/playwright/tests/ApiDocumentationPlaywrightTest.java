/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.pages.ApiDocumentationPage;
import com.sonatype.clm.testing.playwright.pages.ApiDocumentationPageAssertions;
import com.sonatype.clm.testing.playwright.pages.DashboardPage;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ApiDocumentationPlaywrightTest
    extends AbstractIqUiTest
{
  private ApiDocumentationPage apiPage;

  private ApiDocumentationPageAssertions apiAssertions;

  @Before
  public void setUp() {
    playwrightRefreshOrOpen(DashboardPage.url());
    playwrightLogin();
    playwrightRefreshOrOpen(ApiDocumentationPage.url());
    apiPage = new ApiDocumentationPage();
    apiAssertions = new ApiDocumentationPageAssertions(apiPage);
  }

  @Test
  @Category(RegressionTest.class)
  public void testApiDocumentation_tryItOutExpandsExecuteButton() {
    apiAssertions.shouldShowSwaggerLoaded();

    apiPage.firstOpblockSummary().click();
    apiPage.tryItOutButton().click();

    apiAssertions.shouldShowExecuteButtonReady();
  }
}
