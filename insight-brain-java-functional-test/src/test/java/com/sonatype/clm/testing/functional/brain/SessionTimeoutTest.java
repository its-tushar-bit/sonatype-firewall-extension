/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.LoginDialog;
import com.sonatype.clm.testing.functional.elements.SystemConfigMenu;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.WebhookConfigurationPage;

import com.codeborne.selenide.Selenide;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;

public class SessionTimeoutTest
    extends AbstractFunctionalTest
{
  @BeforeClass
  public static void setup() {
    Selenide.open(DashboardPage.URL);
  }

  /**
   * Test that when the session expires (simulated by deleting the cookie), that the next authentication-requiring
   * HTTP request causes the login prompt to appear, with the username box already filled in and disabled.
   */
  @Test
  public void testReloginPromptOnSessionExpiration() {
    loginAsAdmin();
    hardreset();

    // try to open the Webhooks page. Since the session cookie has been deleted this should trigger the session
    // timeout detection
    SystemConfigMenu systemConfigMenu = new SystemConfigMenu();
    systemConfigMenu.menu().click();
    systemConfigMenu.webhooks().click();

    // verify that username is pre-filled and disabled
    LoginDialog.root().shouldBe(visible);
    LoginDialog.username().shouldHave(value("admin"));
    LoginDialog.username().shouldBe(disabled);
    LoginDialog.password().shouldHave(value(""));
    LoginDialog.password().shouldBe(enabled);

    LoginDialog.password().setValue("admin123");
    LoginDialog.loginButton().click();
    LoginDialog.root().shouldNotBe(visible);

    // confirm that we got to the webhooks page after logging back in
    new WebhookConfigurationPage().newWebhook().shouldBe(visible);

    logout();

    // verify that after logging out properly, the login dialog is fully useable again
    LoginDialog.root().shouldBe(visible);
    LoginDialog.username().shouldHave(value(""));
    LoginDialog.username().shouldBe(enabled);
    LoginDialog.password().shouldHave(value(""));
    LoginDialog.password().shouldBe(enabled);
  }
}
