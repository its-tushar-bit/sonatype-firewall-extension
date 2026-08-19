/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.HeaderComponent;
import com.sonatype.clm.testing.playwright.pages.ReportListPage;
import com.sonatype.clm.testing.playwright.pages.RoutingErrorBoxComponent;
import com.sonatype.clm.testing.playwright.pages.RoutingErrorBoxComponentAssertions;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;
import com.sonatype.insight.brain.model.Organization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Playwright test for routing errors.
 */
public class RoutingErrorPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String OWNER_SUMMARY_URL =
      "/assets/index.html#/management/view/organization/" + Organization.ROOT_ORGANIZATION_ID;

  private String invalidUrl() {
    return baseUrlFromTest + "/assets/index.html#/foo";
  }

  /**
   * After {@code page.navigate} to an invalid hash route, the document {@code load} event can
   * fire before the SPA router runs {@code otherwise} and mounts {@code .iq-alert--error}.
   * Wait for DOM readiness, then for the URL to actually reflect the invalid hash.
   */
  private void navigateToInvalidSpaRoute() {
    page.navigate(invalidUrl());
    page.waitForLoadState(LoadState.DOMCONTENTLOADED);
    page.waitForURL(
        url -> url != null && url.contains("#/foo"),
        new Page.WaitForURLOptions().setTimeout(PlaywrightTiming.URL_SUBSTRING_TIMEOUT_MS));
    // Stabilize on the error box + message before callers assert (router may clear then re-set error).
    RoutingErrorBoxComponent errorBox = new RoutingErrorBoxComponent();
    new RoutingErrorBoxComponentAssertions(errorBox).shouldHaveErrorText("Unknown Address");
  }

  @BeforeEach
  public void startup() {
    playwrightRefreshOrOpen(OWNER_SUMMARY_URL);
    playwrightLogin();
  }

  @Test
  @Tag("sanity")
  public void validRoutesDoNotShowError() {
    playwrightRefreshOrOpen(OWNER_SUMMARY_URL);

    RoutingErrorBoxComponent errorBox = new RoutingErrorBoxComponent();
    new RoutingErrorBoxComponentAssertions(errorBox).shouldBeHidden();
  }

  @Test
  @Tag("sanity")
  public void invalidRoutesShowErrorThenHiddenOnOriginalValidRoute() {
    navigateToInvalidSpaRoute();

    RoutingErrorBoxComponent errorBox = new RoutingErrorBoxComponent();
    RoutingErrorBoxComponentAssertions errorBoxAssertions = new RoutingErrorBoxComponentAssertions(errorBox);
    errorBoxAssertions.shouldHaveErrorText("Unknown Address");

    playwrightRefreshOrOpen(OWNER_SUMMARY_URL);
    errorBoxAssertions.shouldBeHidden();
  }

  @Test
  @Tag("sanity")
  public void invalidRoutesShowErrorThenHiddenOnNewValidRoute() {
    navigateToInvalidSpaRoute();

    RoutingErrorBoxComponent errorBox = new RoutingErrorBoxComponent();
    RoutingErrorBoxComponentAssertions errorBoxAssertions = new RoutingErrorBoxComponentAssertions(errorBox);
    errorBoxAssertions.shouldHaveErrorText("Unknown Address");

    HeaderComponent header = new HeaderComponent();
    assertThat(header.menuBar()).isVisible();

    playwrightRefreshOrOpen(ReportListPage.url());
    errorBoxAssertions.shouldBeHidden();
  }
}
