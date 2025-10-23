/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.codeborne.selenide.SelenideElement;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.UserActivityDetailsPage;
import com.sonatype.clm.testing.functional.pages.UserActivityOverviewPage;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.security.Permission;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.sonatype.insight.brain.dataaccess.TemporaryEntity.USER_PASSWORD_CLEAR;
import static org.assertj.core.api.Assertions.assertThat;

public class UserActivityDetailsTest
    extends AbstractFunctionalTest
{
  private UserActivityDetailsPage detailsPage;

  private UserActivityOverviewPage overviewPage;

  private static final String TEST_USERNAME = "admin";

  @BeforeClass
  public static void initialLogin() {
    refreshOrOpen(UserActivityOverviewPage.url());
    loginAsAdmin();
  }

  @Before
  public void setUp() {
    detailsPage = new UserActivityDetailsPage();
    overviewPage = new UserActivityOverviewPage();

    SystemConfigurationPropertyFeature.USER_ACTIVITY_TRACKING.setEnabled(true);
    grantPermissions("admin", Organization.ROOT_ORGANIZATION_ID, Permission.ACCESS_AUDIT_LOG);
    navigateToUserDetailsPage(TEST_USERNAME);
  }

  @After
  public void tearDown() {
    refreshOrOpen("about");
    SystemConfigurationPropertyFeature.USER_ACTIVITY_TRACKING.setEnabled(false);
  }

  @Test
  public void testAdminWithFeatureEnabledCanAccessUserActivityDetails() {
    SystemConfigurationPropertyFeature.USER_ACTIVITY_TRACKING.setEnabled(true);
    navigateToUserDetailsPage(TEST_USERNAME);

    detailsPage.waitForPageLoad();
    detailsPage.pageTitle().shouldBe(visible);
    detailsPage.activityDetailsTable().table().shouldBe(visible);
  }

  @Test
  public void testFeatureDisabledShowsAccessDenied() {
    SystemConfigurationPropertyFeature.USER_ACTIVITY_TRACKING.setEnabled(false);
    navigateToUserDetailsPage(TEST_USERNAME);

    detailsPage.hasFeatureDisabledError();
  }

  @Test
  public void testNonAdminUserCannotAccessUserActivityDetails() {
    try {
      tempEntity.newUser("regularUser", "Regular", "User", "regular@example.com");

      logout();
      login("regularUser", USER_PASSWORD_CLEAR);

      navigateToUserDetailsPage(TEST_USERNAME);

      detailsPage.hasPermissionError();
    }
    finally {
      logout();
      loginAsAdmin();
    }
  }

  @Test
  public void testConsistentPermissionsBetweenOverviewAndDetails() {
    try {
      tempEntity.newUser("adminNoAudit", "Admin", "NoAudit", "admin@example.com");
      grantPermissions("adminNoAudit", Organization.ROOT_ORGANIZATION_ID, Permission.READ);

      logout();
      login("adminNoAudit", USER_PASSWORD_CLEAR);

      refreshOrOpen(UserActivityOverviewPage.url());
      overviewPage.hasPermissionError();

      navigateToUserDetailsPage(TEST_USERNAME);
      detailsPage.hasPermissionError();
    }
    finally {
      logout();
      loginAsAdmin();
    }
  }

  @Test
  public void testPageLoadsWithCorrectElements() {
    detailsPage.waitForPageLoad();

    detailsPage.pageTitle().shouldBe(visible);
    detailsPage.backButton().shouldBe(visible);
    detailsPage.tileTitle().shouldBe(visible);
    detailsPage.exportButton().shouldBe(visible);
    detailsPage.filterButton().shouldBe(visible);
    detailsPage.activityDetailsTable().table().shouldBe(visible);
  }

  @Test
  public void testActivityTableDisplaysData() {
    detailsPage.waitForActivitiesToLoad();

    detailsPage.activityDetailsTable().activityRows().shouldHave(sizeGreaterThan(0));
    detailsPage.activityDetailsTable().timestampHeader().shouldBe(visible);
    detailsPage.activityDetailsTable().domainHeader().shouldBe(visible);
    detailsPage.activityDetailsTable().typeHeader().shouldBe(visible);
    detailsPage.activityDetailsTable().requestUriHeader().shouldBe(visible);
  }

  @Test
  public void testUsernameDisplayedInPageTitle() {
    detailsPage.waitForPageLoad();

    assertThat(detailsPage.getPageTitle()).contains(TEST_USERNAME);
    assertThat(detailsPage.getUsernameFromTitle()).isEqualTo(TEST_USERNAME);
  }

  @Test
  public void testSortingByTimestamp() {
    detailsPage.waitForActivitiesToLoad();

    detailsPage.sortByTimestamp();

    detailsPage.activityDetailsTable().timestampHeader().shouldBe(visible);
    detailsPage.activityDetailsTable().activityRows().shouldHave(sizeGreaterThan(0));
  }

  @Test
  public void testSortingByDomain() {
    detailsPage.waitForActivitiesToLoad();

    detailsPage.sortByDomain();

    detailsPage.activityDetailsTable().domainHeader().shouldBe(visible);
    detailsPage.activityDetailsTable().activityRows().shouldHave(sizeGreaterThan(0));
  }

  @Test
  public void testSortingByType() {
    detailsPage.waitForActivitiesToLoad();

    detailsPage.sortByType();

    detailsPage.activityDetailsTable().typeHeader().shouldBe(visible);
    detailsPage.activityDetailsTable().activityRows().shouldHave(sizeGreaterThan(0));
  }

  @Test
  public void testFilterDrawerOpenAndClose() {
    detailsPage.waitForPageLoad();
    detailsPage.filterButton().shouldBe(visible, enabled);

    detailsPage.filterButton().click();

    $(".nx-drawer").shouldBe(visible);
    $(".nx-drawer button[aria-label*='Close']").shouldBe(visible).click();
  }

  @Test
  public void testPaginationFunctionality() {
    detailsPage.waitForActivitiesToLoad();

    SelenideElement paginationFooter = detailsPage.pagination();

    if (paginationFooter.exists()) {
      SelenideElement nextButton = detailsPage.nextPageButton();

      if (nextButton.exists() && nextButton.isEnabled()) {
        detailsPage.goToNextPage();
        detailsPage.waitForActivitiesToLoad();

        SelenideElement prevButton = detailsPage.previousPageButton();
        if (prevButton.exists() && prevButton.isEnabled()) {
          detailsPage.goToPreviousPage();
          detailsPage.waitForActivitiesToLoad();
        }
      }
    }
  }

  @Test
  public void testExportFunctionality() {
    detailsPage.waitForActivitiesToLoad();

    detailsPage.exportButton().shouldBe(enabled);
    detailsPage.clickExportButton();
    detailsPage.exportButton().shouldBe(enabled);
  }

  @Test
  public void testActivityDataDisplay() {
    detailsPage.waitForActivitiesToLoad();

    assertThat(detailsPage.getFirstActivityTimestamp()).isNotEmpty();
    assertThat(detailsPage.getFirstActivityDomain()).isNotEmpty();
    assertThat(detailsPage.getFirstActivityType()).isNotEmpty();
  }

  @Test
  public void testEmptyStateWhenNoActivities() {
    tempEntity.newUser("userWithNoActivity", "No", "Activity", "noactivity@example.com");
    navigateToUserDetailsPage("userWithNoActivity");

    detailsPage.activityDetailsTable().emptyMessage().shouldBe(visible);
  }

  private void navigateToUserDetailsPage(String username) {
    refreshOrOpen(UserActivityDetailsPage.url(username));
  }
}
