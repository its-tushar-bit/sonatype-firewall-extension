/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.HeaderComponent;
import com.sonatype.clm.testing.playwright.pages.SamlConfigurationPage;
import com.sonatype.clm.testing.playwright.pages.SamlConfigurationPageAssertions;
import com.sonatype.insight.brain.configuration.saml.SamlConfigurationService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

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

  private static final String VALID_IDP_METADATA_XML =
      readClasspathUtf8(SamlConfigurationPlaywrightTest.class, "/saml/valid-idp-metadata.xml");

  private SamlConfigurationPage samlPage;

  private SamlConfigurationPageAssertions samlAssertions;

  @BeforeEach
  public void openSamlConfigPageAsAdmin() {
    playwrightRefreshOrOpen(SamlConfigurationPage.url());
    playwrightLogin();
    samlPage = new SamlConfigurationPage();
    samlAssertions = new SamlConfigurationPageAssertions(samlPage);
  }

  @AfterEach
  public void cleanupSamlConfiguration() {
    playwrightLogout();
    lookup(SamlConfigurationService.class).delete();
  }

  @Test
  @Tag("sanity")
  public void testDefaultState() {
    navigateToSamlPage();

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
  @Tag("sanity")
  public void testCancelRevertsAllFields() {
    navigateToSamlPage();

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
  @Tag("sanity")
  public void testDocumentationLinks() {
    navigateToSamlPage();

    assertThat(samlPage.documentationLink()).hasAttribute("href", DOCUMENTATION_URL);
    assertThat(samlPage.feedbackLink()).hasAttribute("href", FEEDBACK_URL);
  }

  @Test
  @Tag("regression")
  public void testSamlConfigurationPageRenders() {
    new HeaderComponent().navigateToSystemPreference("SAML");
    assertThat(samlPage.saveButton()).isVisible();

    samlAssertions.shouldRenderPageLayout();
  }

  @Test
  @Tag("regression")
  public void testSaveConfiguration() {
    navigateToSamlPage();

    samlPage.identityProviderMetadataXml().fill(VALID_IDP_METADATA_XML);

    samlAssertions.shouldShowSaveButtonEnabled();
    samlPage.saveButton().click();
    waitForSubmitMaskSuccess();
    samlAssertions.shouldShowDeleteButtonEnabled();

    navigateToSamlPage();
    assertThat(samlPage.identityProviderMetadataXml()).not().isEmpty();
    samlAssertions.shouldShowDeleteButtonEnabled();
    samlAssertions.shouldShowIqServerMetadataDownloadEnabled();
  }

  @Test
  @Tag("regression")
  public void testDeleteConfiguration() {
    navigateToSamlPage();

    samlPage.identityProviderMetadataXml().fill(VALID_IDP_METADATA_XML);
    samlPage.saveButton().click();
    waitForSubmitMask();

    navigateToSamlPage();
    samlAssertions.shouldShowDeleteButtonEnabled();
    samlPage.deleteButton().click();

    samlAssertions.shouldShowDeleteModal();
    samlPage.deleteModalConfirmButton().click();
    waitForSubmitMask();

    navigateToSamlPage();
    samlAssertions.shouldShowDeleteButtonDisabled();
  }

  private void navigateToSamlPage() {
    playwrightRefreshOrOpen(SamlConfigurationPage.url());
    assertThat(samlPage.saveButton()).isVisible();
  }
}
