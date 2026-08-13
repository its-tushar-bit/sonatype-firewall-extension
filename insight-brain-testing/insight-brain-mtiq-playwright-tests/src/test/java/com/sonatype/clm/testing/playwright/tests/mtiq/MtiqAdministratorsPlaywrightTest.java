/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests.mtiq;

import com.sonatype.clm.testing.playwright.categories.MtiqTest;
import com.sonatype.clm.testing.playwright.mtiq.AbstractMtiqUiTest;
import com.sonatype.clm.testing.playwright.pages.MtiqAdministratorsPage;
import com.sonatype.clm.testing.playwright.pages.MtiqAdministratorsPageAssertions;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(MtiqTest.class)
public class MtiqAdministratorsPlaywrightTest
    extends AbstractMtiqUiTest
{
  // MTIQ Administrators page lists global-scoped roles only — Owner/Developer are org/app-scoped.
  private static final String ROLE_SYSTEM_ADMINISTRATOR = "System Administrator";

  private static final String ROLE_POLICY_ADMINISTRATOR = "Policy Administrator";

  private static final String BUILT_IN_ADMIN_DISPLAY = "Admin BuiltIn";

  private MtiqAdministratorsPage administrators;

  private MtiqAdministratorsPageAssertions assertions;

  @Before
  public void seedTenantAndLogin() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS, ProductLicenseDetails.PRODUCT_FOUNDATION);
    playwrightRefreshOrOpen("/");
    playwrightLogin();
    administrators = new MtiqAdministratorsPage();
    assertions = new MtiqAdministratorsPageAssertions(administrators);
  }

  @Test
  public void testAdministrators_defaultRolesRender_localPasswordFieldsHidden() {
    playwrightRefreshOrOpen(MtiqAdministratorsPage.listUrl());

    assertions.shouldShowListPageLayout();
    assertions.shouldShowRoleRow(ROLE_SYSTEM_ADMINISTRATOR);
    assertions.shouldShowRoleRow(ROLE_POLICY_ADMINISTRATOR);
    assertions.shouldShowRoleMembers(ROLE_SYSTEM_ADMINISTRATOR, BUILT_IN_ADMIN_DISPLAY);

    administrators.clickRoleRow(ROLE_SYSTEM_ADMINISTRATOR);
    assertions.shouldShowEditPageLayout();
    assertions.shouldHideLocalUsernameAndPasswordInputs();
  }

  /** External-group flow — Auth0-side effect is covered by insight-brain-api-regression-test. */
  @Test
  public void testAdministrators_addExternalGroup_persistsAsRoleMember() {
    playwrightRefreshOrOpen(MtiqAdministratorsPage.listUrl());
    administrators.clickRoleRow(ROLE_SYSTEM_ADMINISTRATOR);

    assertions.shouldShowEditPageLayout();
    assertions.shouldShowAssociatedMember(BUILT_IN_ADMIN_DISPLAY);

    String externalGroupName = "mtiq-admins-" + tempEntity.uuid();
    administrators.externalGroupInput().fill(externalGroupName);
    administrators.externalGroupAddButton().click();

    assertions.shouldShowAssociatedMember(externalGroupName + " (Group)");
    assertions.shouldEnableSubmitButton();
    administrators.submitButton().click();

    page.waitForURL(url -> url.endsWith("#/administrators"));
    assertions.shouldShowListPageLayout();
    assertions.shouldShowRoleMembers(ROLE_SYSTEM_ADMINISTRATOR, externalGroupName);
  }

  /** Search-dropdown UI slice — Auth0 side-effect is covered by insight-brain-api-regression-test. */
  @Test
  public void testAdministrators_groupSearch_showsMatchingUserAndSelectAddsToTransferList() {
    String suffix = tempEntity.uuid();
    String username = "isaac-asimov-" + suffix;
    String firstName = "Isaac";
    String lastName = "Asimov " + suffix;
    User user = tempEntity.newUser(username, firstName, lastName, username + "@void.com");
    String displayName = user.calculateDisplayName();

    playwrightRefreshOrOpen(MtiqAdministratorsPage.listUrl());
    administrators.clickRoleRow(ROLE_SYSTEM_ADMINISTRATOR);
    assertions.shouldShowEditPageLayout();

    // MTIQ search requires '*' wildcard suffix (on-screen hint: "use '*' as wildcard").
    administrators.typeSearch(firstName + "*");
    assertions.shouldShowSearchMatchInDropdown(displayName);

    administrators.searchMatchOption(displayName).click();
    assertions.shouldShowAssociatedMember(displayName);
    assertions.shouldEnableSubmitButton();
  }
}
