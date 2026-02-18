/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.mtiq;

import com.sonatype.clm.testing.functional.elements.MainHeader;
import com.sonatype.clm.testing.functional.elements.SystemConfigMenu;
import com.sonatype.clm.testing.functional.pages.GettingStartedPage;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;

import org.junit.Test;

import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.visible;

public class MtiqSystemConfigurationMenuTest
    extends AbstractMtiqFunctionalTest
{
  private final SystemConfigMenu mtiqSystemConfigMenu = MainHeader.systemConfigMenu();

  private User newUser(Permission... perms) {
    User user = tempEntity.newUser();
    Role role = tempEntity.newRole(true, perms);
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, role.getId(), user.getUsername());
    return user;
  }

  @Test
  public void testPermissionAwareness_NoPermissionsAtAll() {
    User user = newUser();

    refreshOrOpen(GettingStartedPage.url());
    login(user.getUsername(), user.getPassword());

    MainHeader.userMenu().dropdownToggle().click();
    MainHeader.userMenu().userName().shouldNotBe(empty);
    mtiqSystemConfigMenu.shouldBe(hidden);
  }

  @Test
  public void testPermissionAwareness_AdminOwnIdp() {
    tempEntity.newMailConfigurationWithNoAuthentication();

    refreshOrOpen(GettingStartedPage.url());
    loginAsAdmin();

    // do not enable the SSO_IDP_MANAGED_BY_SONATYPE_FLAG (disabled by default)
    mtiqSystemConfigMenu.shouldBe(visible);
    mtiqSystemConfigMenu.dropdownToggle().click();

    mtiqSystemConfigMenu.users().shouldBe(hidden);
    mtiqSystemConfigMenu.roles().shouldBe(visible);
    mtiqSystemConfigMenu.administrators().shouldBe(visible);
    mtiqSystemConfigMenu.webhooks().shouldBe(visible);
    mtiqSystemConfigMenu.automaticScmConfiguration().shouldBe(visible);
    mtiqSystemConfigMenu.emailConfiguration().shouldBe(visible);
    mtiqSystemConfigMenu.advancedSearchConfiguration().shouldBe(visible);
    checkMtiqOmissions();
  }

  @Test
  public void testPermissionAwareness_Admin() {
    tempEntity.newMailConfigurationWithNoAuthentication();
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.SSO_IDP_MANAGED_BY_SONATYPE,
        String.valueOf(true));

    refreshOrOpen(GettingStartedPage.url());
    loginAsAdmin();

    mtiqSystemConfigMenu.shouldBe(visible);
    mtiqSystemConfigMenu.dropdownToggle().click();

    mtiqSystemConfigMenu.users().shouldBe(visible);
    mtiqSystemConfigMenu.roles().shouldBe(visible);
    mtiqSystemConfigMenu.administrators().shouldBe(visible);
    mtiqSystemConfigMenu.webhooks().shouldBe(visible);
    mtiqSystemConfigMenu.automaticScmConfiguration().shouldBe(visible);
    mtiqSystemConfigMenu.emailConfiguration().shouldBe(visible);
    mtiqSystemConfigMenu.advancedSearchConfiguration().shouldBe(visible);
    checkMtiqOmissions();
  }

  @Test
  public void testPermissionAwareness_CONFIGURE_SYSTEM() {
    User user = newUser(Permission.CONFIGURE_SYSTEM);

    tempEntity.newMailConfigurationWithNoAuthentication();
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.SAML_ENABLED, "true");
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.SSO_IDP_MANAGED_BY_SONATYPE,
        String.valueOf(true));

    refreshOrOpen(GettingStartedPage.url());
    login(user.getUsername(), user.getPassword());

    mtiqSystemConfigMenu.shouldBe(visible);
    mtiqSystemConfigMenu.dropdownToggle().click();

    mtiqSystemConfigMenu.users().shouldBe(visible);
    mtiqSystemConfigMenu.roles().shouldBe(hidden);
    mtiqSystemConfigMenu.administrators().shouldBe(visible);
    mtiqSystemConfigMenu.webhooks().shouldBe(visible);
    mtiqSystemConfigMenu.automaticScmConfiguration().shouldBe(hidden);
    mtiqSystemConfigMenu.emailConfiguration().shouldBe(visible);
    mtiqSystemConfigMenu.advancedSearchConfiguration().shouldBe(visible);
    mtiqSystemConfigMenu.samlConfiguration().shouldBe(visible);
    checkMtiqOmissions();
  }

  @Test
  public void testPermissionAwareness_VIEW_ROLES() {
    User user = newUser(Permission.VIEW_ROLES);

    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.SSO_IDP_MANAGED_BY_SONATYPE,
        String.valueOf(true));

    refreshOrOpen(GettingStartedPage.url());
    login(user.getUsername(), user.getPassword());

    mtiqSystemConfigMenu.shouldBe(visible);
    mtiqSystemConfigMenu.dropdownToggle().click();

    mtiqSystemConfigMenu.users().shouldBe(hidden);
    mtiqSystemConfigMenu.roles().shouldBe(visible);
    mtiqSystemConfigMenu.administrators().shouldBe(hidden);
    mtiqSystemConfigMenu.webhooks().shouldBe(hidden);
    mtiqSystemConfigMenu.automaticScmConfiguration().shouldBe(hidden);
    mtiqSystemConfigMenu.emailConfiguration().shouldBe(hidden);
    mtiqSystemConfigMenu.advancedSearchConfiguration().shouldBe(hidden);
    checkMtiqOmissions();
  }

  private void checkMtiqOmissions() {
    mtiqSystemConfigMenu.baseUrlConfiguration().shouldBe(hidden);
    mtiqSystemConfigMenu.ldap().shouldBe(hidden);
    mtiqSystemConfigMenu.productLicense().shouldBe(hidden);
    mtiqSystemConfigMenu.proxyConfiguration().shouldBe(hidden);
    mtiqSystemConfigMenu.systemNotice().shouldBe(hidden);
    mtiqSystemConfigMenu.successMetrics().shouldBe(hidden);
  }
}
