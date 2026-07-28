/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests.mtiq;

import java.util.regex.Pattern;

import com.sonatype.clm.testing.playwright.categories.MtiqTest;
import com.sonatype.clm.testing.playwright.mtiq.AbstractMtiqUiTest;
import com.sonatype.clm.testing.playwright.pages.MtiqGettingStartedPage;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@Category(MtiqTest.class)
public class MtiqGettingStartedPlaywrightTest
    extends AbstractMtiqUiTest
{
  private static final Pattern EXPIRY_DATE_PATTERN = Pattern.compile("[a-zA-Z]+ [0-9]+, 2[0-9]{3}");

  private static final Pattern DAYS_TO_EXPIRATION_PATTERN = Pattern.compile("^\\d+$");

  // DOM text is lower-case "and"; the tile applies CSS text-transform: capitalize (presentational).
  private static final String[] LEARNING_TOPIC_TITLES = {"Policies", "Hierarchy and Inheritance", "Dashboard"};

  @Test
  public void testMtiqGettingStartedPage() {
    setLicensedProducts(
        ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION,
        ProductLicenseDetails.PRODUCT_FIREWALL,
        ProductLicenseDetails.PRODUCT_LIFECYCLE_CLOUD);

    MtiqGettingStartedPage gettingStarted = new MtiqGettingStartedPage();

    playwrightRefreshOrOpen(MtiqGettingStartedPage.url());
    User user = createUser();
    playwrightLogin(user.getUsername(), user.getPassword());

    // Force HDS ping failures so the connectivity warning renders.
    testCLMServer.getHdsServer().respondWith("").andStatus(503).atUri("ping");
    playwrightRefreshOrOpen(MtiqGettingStartedPage.url());

    // Permissionless user: only the HDS connectivity warning and learning-topics tile.
    assertThat(gettingStarted.hdsConnectivityWarning()).isVisible();
    assertThat(gettingStarted.productLicenseSummary()).not().isVisible();
    assertThat(gettingStarted.systemSetup()).not().isVisible();
    assertThat(gettingStarted.learningTopics()).isVisible();
    assertNonAdminOmissions(gettingStarted);

    grantPermissions(user.getUsername(), MembershipMapping.GLOBAL_CONTEXT_ID, Permission.ADD_APPLICATION);
    page.reload();

    // Can add applications: HDS warning, system setup and learning-topics tiles (still no license tile).
    assertThat(gettingStarted.hdsConnectivityWarning()).isVisible();
    assertThat(gettingStarted.productLicenseSummary()).not().isVisible();
    assertThat(gettingStarted.systemSetup()).isVisible();
    assertThat(gettingStarted.learningTopics()).isVisible();
    assertAdminOmissions(gettingStarted);

    grantPermissions(user.getUsername(), MembershipMapping.GLOBAL_CONTEXT_ID, Permission.CONFIGURE_SYSTEM);
    page.reload();

    // Non-default admin user: all tiles, including the product-license summary with details.
    assertThat(gettingStarted.hdsConnectivityWarning()).isVisible();
    assertThat(gettingStarted.productLicenseSummary()).isVisible();
    assertLicenseSummaryContent(gettingStarted);
    assertThat(gettingStarted.systemSetup()).isVisible();
    assertThat(gettingStarted.learningTopics()).isVisible();
    assertAdminOmissions(gettingStarted);

    // HDS reachable again: the connectivity warning disappears.
    testCLMServer.getHdsServer().respondWith("alive").atUri("ping");
    page.reload();
    assertThat(gettingStarted.hdsConnectivityWarning()).not().isVisible();
  }

  private void assertLicenseSummaryContent(MtiqGettingStartedPage page) {
    assertThat(page.licenseExpiryDate()).isVisible();
    assertThat(page.licenseExpiryDate()).hasText(EXPIRY_DATE_PATTERN);
    assertThat(page.licenseDaysToExpiration()).isVisible();
    assertThat(page.licenseDaysToExpiration()).hasText(DAYS_TO_EXPIRATION_PATTERN);
    assertThat(page.licenseProducts()).hasText(new Pattern[]{
      Pattern.compile("Sonatype Lifecycle Cloud"),
      Pattern.compile("Sonatype Developer"),
      Pattern.compile("Sonatype Lifecycle"),
      Pattern.compile("Sonatype Repository Firewall")});
    // NOTE: emdashes between label and value are added via CSS and don't appear in the DOM text.
    assertThat(page.licensedDevelopersRows()).hasText(new Pattern[]{
      Pattern.compile("Lifecycle50"),
      Pattern.compile("Firewall45")});
  }

  private void assertNonAdminOmissions(MtiqGettingStartedPage page) {
    assertThat(page.licenseFingerprint()).not().isVisible();
    assertThat(page.learningTopicsSectionTopics()).hasText(LEARNING_TOPIC_TITLES);
  }

  private void assertAdminOmissions(MtiqGettingStartedPage page) {
    assertThat(page.licenseFingerprint()).not().isVisible();
    assertThat(page.systemSetupSections()).hasText(new String[]{"Adding Users", "Onboarding Applications"});
    assertThat(page.addingUsersTopics()).hasText("INVITE USERS");
    assertThat(page.learningTopicsSectionTopics()).hasText(LEARNING_TOPIC_TITLES);
  }
}
