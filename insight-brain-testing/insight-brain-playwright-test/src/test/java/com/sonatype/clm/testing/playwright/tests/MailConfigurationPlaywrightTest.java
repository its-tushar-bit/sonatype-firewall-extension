/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.pages.MailConfigurationPage;
import com.sonatype.clm.testing.playwright.pages.MailConfigurationPageAssertions;
import com.sonatype.insight.brain.api.v2.service.ApiMailConfigurationService;

import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MailConfigurationPlaywrightTest
    extends AbstractIqUiTest
{
  private static final Logger log = LoggerFactory.getLogger(MailConfigurationPlaywrightTest.class);

  private static final String TEST_HOSTNAME = "smtp.example.com";

  private static final String TEST_PORT = "465";

  private static final String TEST_PASSWORD = "pw-mail-pass";

  private static final String TEST_SYSTEM_EMAIL = "noreply@example.com";

  private static final String TEST_RECIPIENT = "recipient@example.com";

  private MailConfigurationPage mailPage;

  private MailConfigurationPageAssertions mailAssertions;

  @Before
  public void setUp() {
    playwrightRefreshOrOpen(MailConfigurationPage.url());
    playwrightLogin();
    mailPage = new MailConfigurationPage();
    mailAssertions = new MailConfigurationPageAssertions(mailPage);
  }

  @After
  public void cleanup() {
    playwrightLogout();
    try {
      lookup(ApiMailConfigurationService.class).deleteConfiguration();
    }
    catch (NotFoundException e) {
      log.debug("Mail config already absent during cleanup", e);
    }
  }

  @Test
  @Category(RegressionTest.class)
  public void testMailConfigurationPageRenders() {
    mailAssertions.shouldRenderPageLayout();
    mailAssertions.shouldShowTestEmailWidgets();
    mailAssertions.shouldShowDeleteButtonDisabled();
  }

  @Test
  @Category(RegressionTest.class)
  public void testMailConfiguration_saveSuccessfully() {
    fillRequiredFields();

    mailPage.saveButton().click();
    waitForSubmitMaskSuccess();

    navigateToMailPage();
    mailAssertions.shouldHaveHostname(TEST_HOSTNAME);
    mailAssertions.shouldHavePort(TEST_PORT);
    mailAssertions.shouldHaveSystemEmail(TEST_SYSTEM_EMAIL);
    mailAssertions.shouldShowDeleteButtonEnabled();
    mailAssertions.shouldShowTestEmailWidgets();
  }

  @Test
  @Category(RegressionTest.class)
  public void testMailConfiguration_deleteRemovesConfiguration() {
    fillRequiredFields();
    mailPage.saveButton().click();
    waitForSubmitMaskSuccess();

    navigateToMailPage();
    mailAssertions.shouldShowDeleteButtonEnabled();

    mailPage.openDeleteModal();
    mailAssertions.shouldShowDeleteModal();
    mailPage.deleteModalSubmitButton().click();
    waitForSubmitMaskSuccess();

    navigateToMailPage();
    mailAssertions.shouldShowDeleteButtonDisabled();
  }

  @Test
  @Category(RegressionTest.class)
  public void testSendTestEmail_showsErrorWhenHostUnreachable() {
    fillRequiredFields();
    mailPage.saveButton().click();
    waitForSubmitMaskSuccess();

    navigateToMailPage();
    mailPage.testEmailRecipientInput().fill(TEST_RECIPIENT);
    mailPage.sendTestEmailButton().click();

    mailAssertions.shouldShowTestEmailErrorAlert();
  }

  private void fillRequiredFields() {
    mailPage.hostnameInput().fill(TEST_HOSTNAME);
    mailPage.portInput().fill(TEST_PORT);
    mailPage.passwordInput().fill(TEST_PASSWORD);
    mailPage.systemEmailInput().fill(TEST_SYSTEM_EMAIL);
  }

  private void navigateToMailPage() {
    playwrightRefreshOrOpen(MailConfigurationPage.url());
  }
}
