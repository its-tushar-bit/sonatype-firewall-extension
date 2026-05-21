/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.sonatype.clm.testing.playwright.categories.SanityTest;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.clm.testing.playwright.pages.SamlConfigurationPage;
import com.sonatype.insight.brain.configuration.saml.SamlConfigurationService;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class SamlConfigurationPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String DEFAULT_IDP_NAME = "identity provider";

  private static final String DEFAULT_USERNAME_ATTR = "username";

  private static final String DEFAULT_FIRST_NAME_ATTR = "firstName";

  private static final String DEFAULT_LAST_NAME_ATTR = "lastName";

  private static final String DEFAULT_EMAIL_ATTR = "email";

  private static final String DEFAULT_GROUPS_ATTR = "groups";

  private static final String EDIT_IDP_NAME = "My Awesome IdP";

  private static final String EDIT_USERNAME_ATTR = "my-user-name";

  private static final String EDIT_FIRST_NAME_ATTR = "my-first-name";

  private static final String EDIT_LAST_NAME_ATTR = "my-last-name";

  private static final String EDIT_EMAIL_ATTR = "my-email";

  private static final String EDIT_GROUPS_ATTR = "my-groups";

  private static final String DOCUMENTATION_URL =
      "http://links.sonatype.com/products/nxiq/doc/saml-integration";

  private static final String FEEDBACK_URL = "http://links.sonatype.com/products/nxiq/feedback/saml";

  @Before
  public void openSamlConfigPageAsAdmin() {
    playwrightLoginAdminAt(DashboardPage.url());
  }

  @After
  public void cleanupSamlConfiguration() {
    playwrightLogout();
    lookup(SamlConfigurationService.class).delete();
  }

  @Test
  @Category(SanityTest.class)
  public void testDefaultState() {
    playwrightRefreshOrOpen(SamlConfigurationPage.url());

    SamlConfigurationPage samlPage = new SamlConfigurationPage();
    assertThat(samlPage.identityProviderName()).hasValue(DEFAULT_IDP_NAME);
    assertThat(samlPage.identityProviderMetadataXml()).isEmpty();
    assertThat(samlPage.usernameAttribute()).hasValue(DEFAULT_USERNAME_ATTR);
    assertThat(samlPage.firstNameAttribute()).hasValue(DEFAULT_FIRST_NAME_ATTR);
    assertThat(samlPage.lastNameAttribute()).hasValue(DEFAULT_LAST_NAME_ATTR);
    assertThat(samlPage.emailAttribute()).hasValue(DEFAULT_EMAIL_ATTR);
    assertThat(samlPage.groupsAttribute()).hasValue(DEFAULT_GROUPS_ATTR);
    assertThat(samlPage.saveButton()).isDisabled();
    assertThat(samlPage.deleteButton()).isDisabled();
  }

  @Test
  @Category(SanityTest.class)
  public void testCancelRevertsAllFields() {
    playwrightRefreshOrOpen(SamlConfigurationPage.url());

    SamlConfigurationPage samlPage = new SamlConfigurationPage();
    samlPage.identityProviderName().fill(EDIT_IDP_NAME);
    samlPage.usernameAttribute().fill(EDIT_USERNAME_ATTR);
    samlPage.firstNameAttribute().fill(EDIT_FIRST_NAME_ATTR);
    samlPage.lastNameAttribute().fill(EDIT_LAST_NAME_ATTR);
    samlPage.emailAttribute().fill(EDIT_EMAIL_ATTR);
    samlPage.groupsAttribute().fill(EDIT_GROUPS_ATTR);

    samlPage.cancelButton().click();

    assertThat(samlPage.identityProviderName()).hasValue(DEFAULT_IDP_NAME);
    assertThat(samlPage.usernameAttribute()).hasValue(DEFAULT_USERNAME_ATTR);
    assertThat(samlPage.firstNameAttribute()).hasValue(DEFAULT_FIRST_NAME_ATTR);
    assertThat(samlPage.lastNameAttribute()).hasValue(DEFAULT_LAST_NAME_ATTR);
    assertThat(samlPage.emailAttribute()).hasValue(DEFAULT_EMAIL_ATTR);
    assertThat(samlPage.groupsAttribute()).hasValue(DEFAULT_GROUPS_ATTR);
  }

  @Test
  @Category(SanityTest.class)
  public void testDocumentationLinks() {
    playwrightRefreshOrOpen(SamlConfigurationPage.url());

    SamlConfigurationPage samlPage = new SamlConfigurationPage();
    assertThat(samlPage.documentationLink()).hasAttribute("href", DOCUMENTATION_URL);
    assertThat(samlPage.feedbackLink()).hasAttribute("href", FEEDBACK_URL);
  }
}
