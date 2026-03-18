/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.configuration;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.MainHeader;
import com.sonatype.clm.testing.functional.elements.SystemConfigMenu;
import com.sonatype.clm.testing.functional.elements.Tooltip;
import com.sonatype.clm.testing.functional.pages.AutomaticApplicationsConfigurationPage;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.clm.testing.functional.pages.WebhookConfigurationPage;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import com.codeborne.selenide.Selenide;
import org.junit.After;
import org.junit.Test;

import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.elements.CLM.DISABLED;

public class SystemConfigurationMenuTest
    extends AbstractFunctionalTest
{
  private final SystemConfigMenu systemConfigMenu = MainHeader.systemConfigMenu();

  @After
  public void clearCookies() {
    Selenide.clearBrowserCookies();
  }

  private User newUser(Permission... perms) {
    User user = tempEntity.newUser();
    Role role = tempEntity.newRole(true, perms);
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, role.getId(), user.getUsername());
    return user;
  }

  @Test
  public void testPermissionAwareness_NoPermissionsAtAll() {
    User user = newUser();

    refreshOrOpen(ReportListPage.url());
    login(user.getUsername(), user.getPassword());

    MainHeader.userMenu().dropdownToggle().click();
    MainHeader.userMenu().userName().shouldNotBe(empty);
    systemConfigMenu.shouldBe(hidden);
  }

  @Test
  public void testPermissionAwareness_Admin() {
    refreshOrOpen(ReportListPage.url());
    loginAsAdmin();

    systemConfigMenu.shouldBe(visible);
    systemConfigMenu.dropdownToggle().click();

    systemConfigMenu.users().shouldBe(visible);
    systemConfigMenu.roles().shouldBe(visible);
    systemConfigMenu.administrators().shouldBe(visible);
    systemConfigMenu.productLicense().shouldBe(visible);
    systemConfigMenu.ldap().shouldBe(visible);
    systemConfigMenu.webhooks().shouldBe(visible);
    systemConfigMenu.systemNotice().shouldBe(visible);
    systemConfigMenu.successMetrics().shouldBe(visible);
    systemConfigMenu.automaticApplications().shouldBe(visible);
    systemConfigMenu.emailConfiguration().shouldBe(visible);
    systemConfigMenu.proxyConfiguration().shouldBe(visible);
    systemConfigMenu.advancedSearchConfiguration().shouldBe(visible);
    systemConfigMenu.baseUrlConfiguration().shouldBe(visible);
    systemConfigMenu.samlConfiguration().shouldBe(visible);
  }

  @Test
  public void testPermissionAwareness_CONFIGURE_SYSTEM() {
    User user = newUser(Permission.CONFIGURE_SYSTEM);

    refreshOrOpen(ReportListPage.url());
    login(user.getUsername(), user.getPassword());

    systemConfigMenu.shouldBe(visible);
    systemConfigMenu.dropdownToggle().click();

    systemConfigMenu.users().shouldBe(visible);
    systemConfigMenu.roles().shouldBe(hidden);
    systemConfigMenu.administrators().shouldBe(visible);
    systemConfigMenu.productLicense().shouldBe(visible);
    systemConfigMenu.ldap().shouldBe(visible);
    systemConfigMenu.webhooks().shouldBe(visible);
    systemConfigMenu.systemNotice().shouldBe(visible);
    systemConfigMenu.successMetrics().shouldBe(visible);
    systemConfigMenu.automaticApplications().shouldBe(hidden);
    systemConfigMenu.emailConfiguration().shouldBe(visible);
    systemConfigMenu.proxyConfiguration().shouldBe(visible);
    systemConfigMenu.advancedSearchConfiguration().shouldBe(visible);
    systemConfigMenu.baseUrlConfiguration().shouldBe(visible);
    systemConfigMenu.samlConfiguration().shouldBe(visible);
    eyesWatcher.eyesCheck();
  }

  @Test
  public void testPermissionAwareness_VIEW_ROLES() {
    User user = newUser(Permission.VIEW_ROLES);

    refreshOrOpen(ReportListPage.url());
    login(user.getUsername(), user.getPassword());

    systemConfigMenu.shouldBe(visible);
    systemConfigMenu.dropdownToggle().click();

    systemConfigMenu.users().shouldBe(hidden);
    systemConfigMenu.roles().shouldBe(visible);
    systemConfigMenu.administrators().shouldBe(hidden);
    systemConfigMenu.productLicense().shouldBe(hidden);
    systemConfigMenu.ldap().shouldBe(hidden);
    systemConfigMenu.webhooks().shouldBe(hidden);
    systemConfigMenu.systemNotice().shouldBe(hidden);
    systemConfigMenu.successMetrics().shouldBe(hidden);
    systemConfigMenu.automaticApplications().shouldBe(hidden);
    systemConfigMenu.emailConfiguration().shouldBe(hidden);
    systemConfigMenu.proxyConfiguration().shouldBe(hidden);
    systemConfigMenu.advancedSearchConfiguration().shouldBe(hidden);
  }

  @Test
  public void testPermissionAwareness_MANAGE_AUTOMATIC_APPLICATION_CREATION() {
    User user = newUser(Permission.MANAGE_AUTOMATIC_APPLICATION_CREATION);

    refreshOrOpen(AutomaticApplicationsConfigurationPage.url());
    login(user.getUsername(), user.getPassword());

    systemConfigMenu.shouldBe(visible);
    systemConfigMenu.dropdownToggle().click();

    systemConfigMenu.users().shouldBe(hidden);
    systemConfigMenu.roles().shouldBe(hidden);
    systemConfigMenu.administrators().shouldBe(hidden);
    systemConfigMenu.productLicense().shouldBe(hidden);
    systemConfigMenu.ldap().shouldBe(hidden);
    systemConfigMenu.webhooks().shouldBe(hidden);
    systemConfigMenu.systemNotice().shouldBe(hidden);
    systemConfigMenu.successMetrics().shouldBe(hidden);
    systemConfigMenu.automaticApplications().shouldBe(visible);
    systemConfigMenu.emailConfiguration().shouldBe(hidden);
    systemConfigMenu.proxyConfiguration().shouldBe(hidden);
    systemConfigMenu.advancedSearchConfiguration().shouldBe(hidden);
  }

  @Test
  public void testMenu_Foundation() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_FOUNDATION);

    refreshOrOpen(ReportListPage.url());
    loginAsAdmin();

    systemConfigMenu.shouldBe(visible);
    systemConfigMenu.dropdownToggle().click();

    systemConfigMenu.users().shouldBe(visible);
    systemConfigMenu.roles().shouldBe(visible);
    systemConfigMenu.administrators().shouldBe(visible);
    systemConfigMenu.productLicense().shouldBe(visible);
    systemConfigMenu.ldap().shouldBe(visible);
    systemConfigMenu.webhooks().shouldBe(visible, DISABLED).hover();
    Tooltip.get()
        .shouldBe(visible)
        .shouldHave(text("Webhooks feature is not supported by your license"));
    systemConfigMenu.systemNotice().shouldBe(visible);
    systemConfigMenu.successMetrics().shouldBe(visible);
    systemConfigMenu.automaticApplications().shouldBe(visible);
    systemConfigMenu.baseUrlConfiguration().shouldBe(visible);

    systemConfigMenu.webhooks().click();
    WebhookConfigurationPage webhookConfigurationPage = new WebhookConfigurationPage();
    webhookConfigurationPage.shouldNotBe(visible);
    systemConfigMenu.emailConfiguration().shouldBe(hidden);
    systemConfigMenu.proxyConfiguration().shouldBe(hidden);
  }
}
