/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.UserActivityDetailsPage;
import com.sonatype.clm.testing.playwright.pages.UserActivityDetailsPageAssertions;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.security.User;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

public class UserActivityDetailsPlaywrightTest
    extends AbstractIqUiTest
{
  @AfterEach
  public void cleanup() {
    playwrightLogout();
    tempEntity.deleteSystemConfigurationProperty(SystemConfigurationProperty.USER_ACTIVITY_TRACKING);
  }

  @Test
  @Tag("regression")
  public void testUserActivityDetailsPageRenders() {
    lookup(SystemConfigurationPropertyDAO.class).set(SystemConfigurationProperty.USER_ACTIVITY_TRACKING, "true");
    User user = tempEntity.newUser();

    playwrightRefreshOrOpen(UserActivityDetailsPage.url(user.getUsername()));
    playwrightLogin();

    UserActivityDetailsPage detailsPage = new UserActivityDetailsPage();
    UserActivityDetailsPageAssertions detailsAssertions = new UserActivityDetailsPageAssertions(detailsPage);

    detailsAssertions.shouldRenderPageLayoutFor(user.getUsername());
  }

  @Test
  @Tag("regression")
  public void testUserActivityDetails_freshUserShowsDisabledExportAndEmptyState() {
    lookup(SystemConfigurationPropertyDAO.class).set(SystemConfigurationProperty.USER_ACTIVITY_TRACKING, "true");
    User user = tempEntity.newUser();

    playwrightRefreshOrOpen(UserActivityDetailsPage.url(user.getUsername()));
    playwrightLogin();

    UserActivityDetailsPage detailsPage = new UserActivityDetailsPage();
    UserActivityDetailsPageAssertions detailsAssertions = new UserActivityDetailsPageAssertions(detailsPage);

    detailsAssertions.shouldRenderPageLayoutFor(user.getUsername());
    detailsAssertions.shouldShowExportActivityDisabled();
    detailsAssertions.shouldShowEmptyState();
  }
}
