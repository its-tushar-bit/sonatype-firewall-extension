/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.clm.testing.playwright.pages.DashboardWaiversComponent;
import com.sonatype.clm.testing.playwright.pages.HeaderRegressionComponent;
import com.sonatype.clm.testing.playwright.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.playwright.pages.OwnerSummaryRegressionPage;
import com.sonatype.clm.testing.playwright.pages.UserManagementPage;
import com.sonatype.clm.testing.playwright.pages.ViolationDetailsPage;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Regression tests for permission-based UI visibility.
 * Divergences from the manual suite are documented in each test method's Javadoc.
 */
public class PermissionBasedRegressionPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String PERM_TEST_POLICY_NAME = "Perm-Test-Policy";

  private static final int PERM_TEST_THREAT_LEVEL = 5;

  private static final String PERM_TEST_SCAN_ID = "perm-test-scan-001";

  private static final String PERM_TEST_GROUP_ID = "com.example";

  private static final String PERM_TEST_ARTIFACT_ID = "perm-test-artifact";

  private static final String PERM_TEST_VERSION = "1.0.0";

  private static final String PERM_TEST_HASH = "perm-test-hash-001";

  /**
   * user without CONFIGURE_SYSTEM: container renders but LoadWrapper shows NxLoadError; Create User button
   * absent (divergence: no route isAuthorized guard).
   */
  @Test
  @Tag("regression")
  public void testUserManagement_userWithoutConfigureSystem_containerRendersWithLoadError() {
    User user = tempEntity.newUser(TemporaryEntity.uuid());

    playwrightLoginAt(UserManagementPage.url(), user.getUsername(), TemporaryEntity.USER_PASSWORD_CLEAR);

    UserManagementPage userMgmt = new UserManagementPage();
    assertThat(userMgmt.container()).isVisible();
    assertThat(userMgmt.loadError()).isVisible();
    assertThat(userMgmt.createUserButton()).isHidden();
  }

  /**
   * User with VIEW_ROLES only sees the gear button but CONFIGURE_SYSTEM items are hidden.
   * VIEW_ROLES is the minimum to make the gear visible (counted by hasAnyPermissions in MenuBar.jsx).
   */
  @Test
  @Tag("regression")
  public void testSystemPreferencesMenu_userWithoutConfigureSystem_menuButtonVisibleButItemsHidden() {
    User user = tempEntity.newUser(TemporaryEntity.uuid());
    Role viewRolesRole = tempEntity.newRole(true, Permission.VIEW_ROLES);
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, viewRolesRole.getId(), user.getUsername());

    playwrightLoginAt(DashboardPage.url(), user.getUsername(), TemporaryEntity.USER_PASSWORD_CLEAR);

    HeaderRegressionComponent header = new HeaderRegressionComponent();
    assertThat(header.systemConfigMenuButton()).isVisible();

    header.systemConfigMenuButton().click();

    // VIEW_ROLES grants visibility of the Roles link; CONFIGURE_SYSTEM items remain hidden.
    assertThat(header.systemConfigMenuItem("roles")).isVisible();
    assertThat(header.systemConfigMenuItem("users")).isHidden();
    assertThat(header.systemConfigMenuItem("administrators")).isHidden();
    assertThat(header.systemConfigMenuItem("product-license")).isHidden();
    assertThat(header.systemConfigMenuItem("user-tokens")).isHidden();
  }

  /**
   * read-only user: all three Actions dropdown items remain visible (divergence: ActionDropdown.jsx renders
   * them unconditionally).
   */
  @Test
  @Tag("regression")
  public void testOrgActionsDropdown_readOnlyUser_dropdownItemsAlwaysVisible() {
    Organization org = tempEntity.newOrganization();
    User user = tempEntity.newUser(TemporaryEntity.uuid());
    grantPermissions(user.getUsername(), org.getId(), Permission.READ);

    playwrightLoginAt(
        OwnerSummaryPage.url(org.getId()), user.getUsername(), TemporaryEntity.USER_PASSWORD_CLEAR);

    OwnerSummaryRegressionPage ownerSummary = new OwnerSummaryRegressionPage();
    ownerSummary.openOwnerActionsDropdown();

    assertThat(ownerSummary.applicationOrgLink()).isVisible();
    assertThat(ownerSummary.deleteOwnerLink()).isVisible();
    assertThat(ownerSummary.ownerMoveLink()).isVisible();
  }

  /**
   * both Waivers sub-tabs present for admin (divergence: tab gated on product feature, not role; sub-tabs
   * unconditionally rendered).
   */
  @Test
  @Tag("regression")
  public void testDashboardWaiversTab_existingAndRequestedWaiversSubTabsPresent() {
    playwrightRefreshOrOpen(DashboardPage.urlToWaivers());
    playwrightLogin();

    DashboardWaiversComponent waivers = new DashboardWaiversComponent();
    assertThat(waivers.container()).isVisible();
    assertThat(waivers.existingWaiversTab()).isVisible();
    assertThat(waivers.requestedWaiversTab()).isVisible();
  }

  /**
   * user without WAIVE_POLICY_VIOLATIONS: "Request Waiver" visible, "Add Waiver" hidden.
   * WAIVER_REQUEST_WORKFLOW_ENABLED has enabledWhenAbsent=true in the test server.
   */
  @Test
  @Tag("regression")
  public void testViolationDetailPage_userWithoutWaivePermission_addWaiverButtonHidden() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());

    Policy policy = tempEntity.newPolicy(
        Organization.ROOT_ORGANIZATION_ID, PERM_TEST_POLICY_NAME, PERM_TEST_THREAT_LEVEL);
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(
        app.getId(), StageTypes.BUILD.getId(), PERM_TEST_SCAN_ID, false, false,
        Date.from(Instant.now().minus(2, ChronoUnit.DAYS)));
    PolicyViolation violation = tempEntity.newPolicyViolation(
        evaluation, policy,
        PERM_TEST_GROUP_ID, PERM_TEST_ARTIFACT_ID, PERM_TEST_VERSION,
        PERM_TEST_HASH, null);

    User user = tempEntity.newUser(TemporaryEntity.uuid());
    grantPermissions(user.getUsername(), org.getId(), Permission.READ);

    playwrightLoginAt(
        ViolationDetailsPage.url(violation.getId()),
        user.getUsername(),
        TemporaryEntity.USER_PASSWORD_CLEAR);

    ViolationDetailsPage violationPage = new ViolationDetailsPage();
    assertThat(violationPage.container()).isVisible();
    assertThat(violationPage.addWaiverButton()).isHidden();
    assertThat(violationPage.requestWaiverButton()).isVisible();
  }
}
