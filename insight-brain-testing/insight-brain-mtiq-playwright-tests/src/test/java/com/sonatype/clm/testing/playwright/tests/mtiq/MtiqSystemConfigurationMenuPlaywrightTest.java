/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests.mtiq;

import com.sonatype.clm.testing.playwright.categories.MtiqTest;
import com.sonatype.clm.testing.playwright.mtiq.AbstractMtiqUiTest;
import com.sonatype.clm.testing.playwright.pages.HeaderComponent;
import com.sonatype.clm.testing.playwright.pages.SystemConfigMenuComponent;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.User;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@Category(MtiqTest.class)
public class MtiqSystemConfigurationMenuPlaywrightTest
    extends AbstractMtiqUiTest
{
  private static final String LANDING_URL = "/";

  @Test
  public void testPermissionAwareness_NoPermissionsAtAll() {
    User user = newUser();

    playwrightLoginAt(LANDING_URL, user.getUsername(), user.getPassword());

    HeaderComponent header = new HeaderComponent();
    header.userMenuDropdownToggle().click();
    assertThat(header.userName()).not().isEmpty();

    assertThat(new SystemConfigMenuComponent().menu()).isHidden();
  }

  @Test
  public void testPermissionAwareness_AdminOwnIdp() {
    tempEntity.newMailConfigurationWithNoAuthentication();

    // SSO_IDP_MANAGED_BY_SONATYPE stays disabled (default), so the Users item is hidden.
    playwrightLoginAdminAt(LANDING_URL);

    SystemConfigMenuComponent menu = new SystemConfigMenuComponent();
    assertThat(menu.menu()).isVisible();
    menu.open();

    assertThat(menu.users()).isHidden();
    assertThat(menu.roles()).isVisible();
    assertThat(menu.administrators()).isVisible();
    assertThat(menu.webhooks()).isVisible();
    assertThat(menu.automaticScmConfiguration()).isVisible();
    assertThat(menu.emailConfiguration()).isVisible();
    assertThat(menu.advancedSearchConfiguration()).isVisible();
    assertMtiqOmissions(menu);
  }

  @Test
  public void testPermissionAwareness_Admin() {
    tempEntity.newMailConfigurationWithNoAuthentication();
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.SSO_IDP_MANAGED_BY_SONATYPE,
        String.valueOf(true));

    playwrightLoginAdminAt(LANDING_URL);

    SystemConfigMenuComponent menu = new SystemConfigMenuComponent();
    assertThat(menu.menu()).isVisible();
    menu.open();

    assertThat(menu.users()).isVisible();
    assertThat(menu.roles()).isVisible();
    assertThat(menu.administrators()).isVisible();
    assertThat(menu.webhooks()).isVisible();
    assertThat(menu.automaticScmConfiguration()).isVisible();
    assertThat(menu.emailConfiguration()).isVisible();
    assertThat(menu.advancedSearchConfiguration()).isVisible();
    assertMtiqOmissions(menu);
  }

  @Test
  public void testPermissionAwareness_CONFIGURE_SYSTEM() {
    User user = newUser(Permission.CONFIGURE_SYSTEM);

    tempEntity.newMailConfigurationWithNoAuthentication();
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.SAML_ENABLED, "true");
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.SSO_IDP_MANAGED_BY_SONATYPE,
        String.valueOf(true));

    playwrightLoginAt(LANDING_URL, user.getUsername(), user.getPassword());

    SystemConfigMenuComponent menu = new SystemConfigMenuComponent();
    assertThat(menu.menu()).isVisible();
    menu.open();

    assertThat(menu.users()).isVisible();
    assertThat(menu.roles()).isHidden();
    assertThat(menu.administrators()).isVisible();
    assertThat(menu.webhooks()).isVisible();
    assertThat(menu.automaticScmConfiguration()).isHidden();
    assertThat(menu.emailConfiguration()).isVisible();
    assertThat(menu.advancedSearchConfiguration()).isVisible();
    assertThat(menu.samlConfiguration()).isVisible();
    assertMtiqOmissions(menu);
  }

  @Test
  public void testPermissionAwareness_VIEW_ROLES() {
    User user = newUser(Permission.VIEW_ROLES);

    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.SSO_IDP_MANAGED_BY_SONATYPE,
        String.valueOf(true));

    playwrightLoginAt(LANDING_URL, user.getUsername(), user.getPassword());

    SystemConfigMenuComponent menu = new SystemConfigMenuComponent();
    assertThat(menu.menu()).isVisible();
    menu.open();

    assertThat(menu.users()).isHidden();
    assertThat(menu.roles()).isVisible();
    assertThat(menu.administrators()).isHidden();
    assertThat(menu.webhooks()).isHidden();
    assertThat(menu.automaticScmConfiguration()).isHidden();
    assertThat(menu.emailConfiguration()).isHidden();
    assertThat(menu.advancedSearchConfiguration()).isHidden();
    assertMtiqOmissions(menu);
  }

  /** Items that MTIQ never shows regardless of permissions. */
  private void assertMtiqOmissions(SystemConfigMenuComponent menu) {
    assertThat(menu.baseUrlConfiguration()).isHidden();
    assertThat(menu.ldap()).isHidden();
    assertThat(menu.productLicense()).isHidden();
    assertThat(menu.proxyConfiguration()).isHidden();
    assertThat(menu.systemNotice()).isHidden();
    assertThat(menu.successMetrics()).isHidden();
  }
}
