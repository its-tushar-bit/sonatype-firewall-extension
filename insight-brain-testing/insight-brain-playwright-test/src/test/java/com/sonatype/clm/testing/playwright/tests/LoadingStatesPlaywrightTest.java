/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.util.regex.Pattern;

import com.microsoft.playwright.options.AriaRole;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.AdministratorsPage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Regression tests for loading-state UI patterns.
 *
 * <p>
 * Uses a never-fulfilled {@code page.route()} intercept on the Administrators endpoint to hold
 * {@code NxTable} in its {@code isLoading=true} state long enough to assert the spinner.
 * Divergence: manual says "any data-heavy page"; Administrators page is used
 * (single interceptable endpoint, no DB seeding required).
 */
public class LoadingStatesPlaywrightTest
    extends AbstractIqUiTest
{
  private static final Pattern ROLE_MEMBERSHIPS_URL =
      Pattern.compile(".*/api/v2/roleMemberships/global/roles.*");

  @BeforeEach
  public void navigateAndLogin() {
    playwrightRefreshOrOpen(AdministratorsPage.url());
    playwrightLogin();
  }

  @AfterEach
  public void unrouteAll() {
    page.unrouteAll();
  }

  /** NxTable loading spinner visible while request is held in-flight. */
  @Test
  @Tag("regression")
  public void testInitialDataFetch_loadingSpinnerVisibleWhileRequestPending() {
    page.route(ROLE_MEMBERSHIPS_URL, route -> {
      // Never fulfilled — keeps NxTable isLoading=true for the assertion.
    });
    playwrightRefreshOrOpen(AdministratorsPage.url());

    AdministratorsPage adminsPage = new AdministratorsPage();
    assertThat(adminsPage.container()).isVisible();
    assertThat(adminsPage.table().getByRole(AriaRole.STATUS)).isVisible();
  }
}
