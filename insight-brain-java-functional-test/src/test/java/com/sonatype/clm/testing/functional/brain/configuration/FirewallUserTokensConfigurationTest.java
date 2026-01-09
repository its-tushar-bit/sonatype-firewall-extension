/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.configuration;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.MainHeader;
import com.sonatype.clm.testing.functional.elements.SidebarNavigation;
import com.sonatype.clm.testing.functional.elements.SystemConfigMenu;
import com.sonatype.clm.testing.functional.pages.FirewallPage;
import com.sonatype.clm.testing.functional.pages.UserTokensConfigurationPage;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.clm.testing.functional.utils.FormUtils;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.Keys;

import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.checked;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.utils.FormUtils.DEFAULT_VALIDATION_ERRORS_PREFIX;

public class FirewallUserTokensConfigurationTest
    extends AbstractFunctionalTest
{
  private static final String FIREWALL_USER_TOKENS_URL =
      BaseUrl.resolvePageUrl("/firewall/userTokensConfiguration");

  private final SystemConfigMenu systemConfigMenu = MainHeader.systemConfigMenu();

  private final UserTokensConfigurationPage configPage =
      new UserTokensConfigurationPage();

  @Before
  public void before() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_FIREWALL_V2);
    refreshOrOpen(FirewallPage.url());
    loginAsAdmin();
  }

  @Test
  public void testFirewallPageLogo() {
    systemConfigMenu.dropdownToggle().click();
    systemConfigMenu.userTokensConfiguration().shouldBe(visible).click();
    waitUntilUrl(FIREWALL_USER_TOKENS_URL);

    SidebarNavigation.productLogo().shouldHave(attribute("alt", "sonatype firewall"));
  }

  @Test
  public void testUserTokensConfiguration() {
    systemConfigMenu.dropdownToggle().click();
    systemConfigMenu.userTokensConfiguration().shouldNotHave(cssClass("active"));
    systemConfigMenu.userTokensConfiguration().shouldBe(visible).shouldHave(text("User Tokens Configuration")).click();
    waitUntilUrl(FIREWALL_USER_TOKENS_URL);

    systemConfigMenu.dropdownToggle().click();
    systemConfigMenu.userTokensConfiguration().shouldHave(cssClass("active"));

    systemConfigMenu.dropdownToggle().click();
    systemConfigMenu.userTokensConfiguration().shouldBe(hidden);

    configPage.pageTitle().shouldHave(text("User Tokens"));
    configPage.pageDescription().shouldHave(text("Manage user token configuration"));
    configPage.tileHeaderTitle().shouldHave(text("Token Configuration"));
    configPage.explanation().shouldHave(text("The user tokens feature allows users to authenticate securely"));

    configPage.userTokensEnabledToggle().input().shouldBe(checked);
    configPage.userTokensEnabledToggle().input().shouldBe(disabled);

    configPage.expirationEnabledToggle().input().shouldNotBe(checked);
    configPage.expirationDaysInput().shouldBe(disabled);
    configPage.expirationDaysInput().shouldHave(value("30"));

    configPage.cancel().shouldBe(disabled);
    configPage.update().shouldBe(enabled);

    eyesWatcher.eyesCheck();

    configPage.update().click();
    FormUtils.getAlertElement(configPage)
        .shouldHave(text(DEFAULT_VALIDATION_ERRORS_PREFIX + " There are no changes to update."));

    configPage.expirationEnabledToggle().click();
    configPage.expirationEnabledToggle().input().shouldBe(checked);
    configPage.expirationDaysInput().shouldBe(enabled);

    configPage.cancel().shouldNotBe(disabled).click();
    configPage.expirationEnabledToggle().input().shouldNotBe(checked);
    configPage.expirationDaysInput().shouldBe(disabled);

    configPage.expirationEnabledToggle().click();
    configPage.expirationDaysInput().setValue("90");
    configPage.expirationDaysInput().shouldHave(value("90"));

    configPage.update().shouldBe(enabled).click();

    refresh();
    configPage.expirationEnabledToggle().input().shouldBe(checked);
    configPage.expirationDaysInput().shouldHave(value("90"));

    configPage.expirationDaysInput().setValue("500");
    configPage.validationError().shouldHave(text("Must be at most 365 days"));

    configPage.expirationDaysInput().setValue("abc");
    configPage.validationError().shouldHave(text("Must be a valid integer"));

    configPage.expirationDaysInput().setValue("0");
    configPage.validationError().shouldHave(text("Must be at least 1 day"));

    configPage.expirationDaysInput().doubleClick();
    configPage.expirationDaysInput().sendKeys(Keys.BACK_SPACE);
    configPage.expirationDaysInput().pressTab();
    configPage.validationError().shouldHave(text("Must be non-empty"));

    configPage.expirationDaysInput().setValue("60");
    configPage.update().click();

    refresh();
    configPage.expirationDaysInput().shouldHave(value("60"));

    configPage.expirationEnabledToggle().click();
    configPage.update().click();

    refresh();
    configPage.expirationEnabledToggle().input().shouldNotBe(checked);
    configPage.expirationDaysInput().shouldBe(disabled);
    configPage.expirationDaysInput().shouldHave(value("30"));
  }

  @Test
  public void testCancelButton() {
    refreshOrOpen(FIREWALL_USER_TOKENS_URL);
    waitUntilUrl(FIREWALL_USER_TOKENS_URL);

    configPage.cancel().shouldBe(disabled);

    configPage.expirationEnabledToggle().click();
    configPage.cancel().shouldBe(enabled);
    configPage.expirationDaysInput().setValue("100");

    configPage.cancel().click();

    configPage.expirationEnabledToggle().input().shouldNotBe(checked);
    configPage.expirationDaysInput().shouldBe(disabled);
    configPage.expirationDaysInput().shouldHave(value("30"));
    configPage.cancel().shouldBe(disabled);
  }

  @Test
  public void testManageUserTokenLink() {
    refreshOrOpen(FIREWALL_USER_TOKENS_URL);
    waitUntilUrl(FIREWALL_USER_TOKENS_URL);

    configPage.manageUserTokenLink().shouldBe(visible).shouldHave(text("Manage User Token"));
    configPage.manageUserTokenLink().click();

    configPage.modal().shouldBe(visible);
  }
}
