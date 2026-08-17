/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.BaseUrlConfigurationPage;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Playwright migration of the Selenide {@code BaseUrlConfigurationPageTest}.
 * <p>
 * Authoring rules: see {@code TestAuthourskill.md}. Backend access is encapsulated in the nested
 * {@link BaseUrlConfigSeeder} (§3c).
 */
public class BaseUrlConfigurationPlaywrightTest
    extends AbstractIqUiTest
{
  /** Transient value typed into the input to verify the Cancel button reverts unsaved edits. */
  private static final String TRANSIENT_INPUT_VALUE = "test";

  private String baseUrl = "";

  @BeforeEach
  public void openBaseUrlConfigAsAdmin() {
    baseUrl = readCurrentBaseUrl();

    playwrightRefreshOrOpen(BaseUrlConfigurationPage.url());
    playwrightLogin();
  }

  @Test
  @Tag("sanity")
  public void testDefaultState() {
    assertDefaultFormState();
  }

  /**
   * Save the existing base URL, log out + log back in, and verify the round-trip preserves the
   * value plus the form's button states. Then open the delete-confirmation modal and verify
   * its primary action is enabled and Cancel dismisses the modal.
   */
  @Test
  @Tag("sanity")
  public void testCRUD() {
    BaseUrlConfigurationPage configPage = new BaseUrlConfigurationPage();

    configPage.baseUrlAttribute().clear();
    configPage.baseUrlAttribute().fill(baseUrl);
    configPage.saveButton().click();

    playwrightLogout();
    playwrightRefreshOrOpen(BaseUrlConfigurationPage.url());
    playwrightLogin();

    assertThat(configPage.baseUrlAttribute()).hasValue(baseUrl);
    assertThat(configPage.deleteButton()).isEnabled();
    assertThat(configPage.saveButton()).isEnabled();
    assertThat(configPage.cancelButton()).isDisabled();

    configPage.openDeleteModal();
    assertThat(configPage.deleteModal()).isVisible();
    assertThat(configPage.deleteModalSubmitButton()).isEnabled();

    configPage.cancelDeleteModal();
    assertThat(configPage.deleteModal()).isHidden();
  }

  /**
   * Type a transient value into the input, hit Cancel, and verify the form returns to its
   * default state — i.e. Cancel is a no-op against the persisted base URL.
   */
  @Test
  @Tag("sanity")
  public void testCancel() {
    BaseUrlConfigurationPage configPage = new BaseUrlConfigurationPage();

    configPage.baseUrlAttribute().clear();
    configPage.baseUrlAttribute().fill(TRANSIENT_INPUT_VALUE);
    assertThat(configPage.cancelButton()).isEnabled();
    configPage.cancelButton().click();

    assertDefaultFormState();
    assertThat(configPage.deleteButton()).isEnabled();
  }

  @Test
  @Tag("regression")
  public void testBaseUrlNotSetNotice_routeShowsNotice() {
    lookup(SystemConfigurationPropertyDAO.class).set(SystemConfigurationProperty.BASE_URL, "");

    playwrightRefreshOrOpen(BaseUrlConfigurationPage.url());

    BaseUrlConfigurationPage configPage = new BaseUrlConfigurationPage();
    assertThat(configPage.notSetNoticeBanner()).isVisible();
    assertThat(configPage.notSetNoticeBanner()).containsText("The Base URL is not configured");
  }

  @AfterEach
  public void restoreBaseUrl() {
    lookup(SystemConfigurationPropertyDAO.class).set(SystemConfigurationProperty.BASE_URL, baseUrl);
  }

  private void assertDefaultFormState() {
    BaseUrlConfigurationPage configPage = new BaseUrlConfigurationPage();
    assertThat(configPage.saveButton()).isEnabled();
    assertThat(configPage.cancelButton()).isDisabled();
    assertThat(configPage.baseUrlAttribute()).hasValue(baseUrl);
    assertThat(configPage.deleteButton()).isEnabled();
  }

  private String readCurrentBaseUrl() {
    SystemConfigurationPropertyDAO dao = lookup(SystemConfigurationPropertyDAO.class);
    return dao.get(SystemConfigurationProperty.BASE_URL);
  }
}
