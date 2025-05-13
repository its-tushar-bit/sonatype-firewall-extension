/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.configuration;

import java.util.List;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.Tooltip;
import com.sonatype.clm.testing.functional.elements.NxTextInput;
import com.sonatype.clm.testing.functional.elements.UnsavedModal;
import com.sonatype.clm.testing.functional.pages.ApiPage;
import com.sonatype.clm.testing.functional.pages.IndexPage;
import com.sonatype.clm.testing.functional.pages.ZscalerConfigPage;
import com.sonatype.clm.testing.functional.utils.FormUtils;
import com.sonatype.insight.brain.dataaccess.configuration.ZScalerConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.security.PasswordHandler;

import com.codeborne.selenide.SelenideElement;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.Keys;

import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class ZscalerConfigPageTest
    extends AbstractFunctionalTest
{
  private final ZscalerConfigPage page = new ZscalerConfigPage();

  private final ApiPage apiPage = new ApiPage();

  private ZScalerConfigurationDAO zScalerConfigurationDAO;

  private PasswordHandler passwordHandler;

  private static final String FAKE_PASSWORD = "\u0000\u0000\u0000\u0000\u0000";
  
  private static final String EULA_TEXT_STRING = "I acknowledge that access to and use of Sonatype products is " +  
      "governed by either 1) the terms of company's negotiated license agreement with Sonatype or, in the absence " +
      "of a negotiated license, 2) Sonatype’s End User License Agreement";

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(IndexPage.url());
    loginAsAdmin();
  }

  @Before
  public void setUp() {
    SystemConfigurationPropertyFeature.ZSCALER.setEnabled(true);
    zScalerConfigurationDAO = lookup(ZScalerConfigurationDAO.class);
    passwordHandler = lookup(PasswordHandler.class);
  }

  @Test
  public void testInitialState() {
    refreshOrOpen(ZscalerConfigPage.url());
    page.loadError().shouldNotBe(visible);

    page.zscalerConfigHeader().shouldHave(text("Zscaler Configuration"));
    assertNoZscalerConfig();
    page.save().shouldBe(enabled);
    page.save().shouldHave(text("Save"));
    page.delete().shouldBe(disabled);
    page.cancel().shouldBe(disabled);
    page.testConfig().shouldHave(attribute("aria-disabled", "true"));
    page.eulaCheckbox().label().shouldHave(text(EULA_TEXT_STRING));
    page.eulaCheckbox().shouldNotBe(selected).shouldBe(enabled);
  }

  @Test
  public void testSaveConfig() {
    refreshOrOpen(ZscalerConfigPage.url());
    fillConfigurationFields();
    saveConfiguration();

    // Check that the config is saved in the database
    assertConfigIsSaved("user", "asdf", "https://zsapi.zscalertwo.net", "key");

    // page is updated with new values
    page.username().shouldBe(value("user"));
    page.password().shouldBe(value(FAKE_PASSWORD));
    page.hostname().shouldBe(value("https://zsapi.zscalertwo.net"));
    page.apiKey().shouldBe(value("key"));
    page.eulaCheckbox().shouldBe(selected).shouldBe(disabled);
  }
  
  @Test
  public void testSaveConfig_failsIfEulaCheckboxNotChecked() {
    refreshOrOpen(ZscalerConfigPage.url());
    page.username().setValue("user");
    page.password().setValue("asdf");
    page.hostname().setValue("https://zsapi.zscalertwo.net");
    page.apiKey().setValue("key");
    
    saveConfiguration();
    assertValidationError("Review the highlighted fields for missing information.");
  }

  @Test
  public void testUpdateConfig() {
    refreshOrOpen(ZscalerConfigPage.url());
    fillConfigurationFields();
    saveConfiguration();
    page.save().shouldHave(text("Update"));
    page.eulaCheckbox().shouldBe(selected).shouldBe(disabled);

    page.username().setValue("user1");
    page.password().setValue("foobar");
    page.hostname().setValue("https://zsapi.zscalerthree.net");
    page.apiKey().setValue("key1");
    saveConfiguration();

    // Check that the config is updated in the database
    assertConfigIsSaved("user1", "foobar", "https://zsapi.zscalerthree.net", "key1");

    // page is updated with new values
    page.username().shouldBe(value("user1"));
    page.password().shouldBe(value(FAKE_PASSWORD));
    page.hostname().shouldBe(value("https://zsapi.zscalerthree.net"));
    page.apiKey().shouldBe(value("key1"));
  }

  @Test
  public void testDeleteConfig() {
    refreshOrOpen(ZscalerConfigPage.url());
    fillConfigurationFields();
    saveConfiguration();

    // delete modal is visible and cancel delete
    page.delete().shouldBe(enabled).click();
    page.deleteModal().shouldBe(visible);
    page.deleteModal().cancel().shouldBe(enabled).click();
    page.deleteModal().shouldBe(hidden);

    // Check that the config is not deleted from the database
    assertThat(zScalerConfigurationDAO.get()).isNotNull();

    page.delete().shouldBe(enabled).click();
    page.deleteModal().shouldBe(visible);
    page.deleteModal().ok().shouldBe(enabled).click();
    page.deleteModal().shouldBe(hidden);

    assertNoZscalerConfig();
  }

  @Test
  public void testPasswordIsRequiredWhenUpdatingOtherFields() {
    refreshOrOpen(ZscalerConfigPage.url());
    fillConfigurationFields();
    saveConfiguration();
    page.cancel().shouldBe(disabled);

    List<SelenideElement> inputElements = asList(page.username(), page.hostname(), page.apiKey());
    inputElements.forEach(ele -> ele.setValue("a"));
    saveConfiguration();
    assertValidationError("Password must be re-entered when any fields are modified.");
  }

  @Test
  public void testAllFieldsRequiredFormValidationErrors() {
    refreshOrOpen(ZscalerConfigPage.url());
    saveConfiguration();
    assertValidationError("Username, Password, Hostname and Zscaler API Key are required details.");
  }

  @Test
  public void testFieldsValidationErrors() {
    refreshOrOpen(ZscalerConfigPage.url());
    List<SelenideElement> inputElements = asList(page.username(), page.password(), page.hostname(), page.apiKey());
    inputElements.forEach(ele -> ele.setValue("a"));
    inputElements.forEach(ele -> ele.sendKeys(Keys.BACK_SPACE));
    inputElements.forEach(ele -> new NxTextInput(ele).errorMessage().shouldHave(text("Must be non-empty")));
  }

  @Test
  public void testSendTestConfig() {
    refreshOrOpen(ZscalerConfigPage.url());
    fillConfigurationFields();
    saveConfiguration();
    page.testConfig().shouldHave(attribute("aria-disabled", "true"));
    page.testConfig().click();
    Tooltip.get().shouldBe(visible).shouldHave(text("Password must be re-entered for testing configuration."));

    // Only test the config with dummy values as we don't want to have real credentials in the test
    page.password().setValue("a");
    page.testConfig().shouldBe(enabled).click();
    FormMask.seeAndWaitForDismissal();
    FormUtils.getErrorElement(page).shouldBe(visible)
        .shouldHave(text("Unable to establish the connection to Zscaler as the connection is not configured. " +
        "Test Zscaler configuration failed. Learn more about the Zscaler integration\n Retry"));
  }

  @Test
  public void testUnsavedChangesModal() {
    refreshOrOpen(ZscalerConfigPage.url());
    fillConfigurationFields();

    refreshOrOpen(ApiPage.firewallUrl());

    UnsavedModal unsavedChangesModal = new UnsavedModal();
    unsavedChangesModal.shouldBe(visible);
    unsavedChangesModal.cancelButton().click();

    apiPage.publicTab().shouldNotBe(visible);
    page.zscalerConfigHeader().shouldBe(visible).shouldHave(text("Zscaler Configuration"));

    refreshOrOpen(ApiPage.firewallUrl());

    unsavedChangesModal.shouldBe(visible);
    unsavedChangesModal.continueButton().click();

    apiPage.publicTab().shouldBe(visible);
  }

  private void assertNoZscalerConfig() {
    page.username().shouldBe(empty);
    page.password().shouldBe(empty);
    page.hostname().shouldBe(empty);
    page.apiKey().shouldBe(empty);
    page.eulaCheckbox().shouldNotBe(selected).shouldBe(enabled);
    assertThat(zScalerConfigurationDAO.get()).isNull();
  }

  private void fillConfigurationFields() {
    page.username().setValue("user");
    page.password().setValue("asdf");
    page.hostname().setValue("https://zsapi.zscalertwo.net");
    page.apiKey().setValue("key");
    page.eulaCheckbox().click();
  }

  private void saveConfiguration() {
    page.save().click();
    FormMask.seeAndWaitForDismissal();
  }

  private void assertConfigIsSaved(String username, String password, String hostname, String apiKey) {
    assertThat(zScalerConfigurationDAO.get()).isNotNull();
    assertThat(zScalerConfigurationDAO.get().getUsername()).isEqualTo(username);
    assertThat(passwordHandler.decryptPassword(zScalerConfigurationDAO.get().getPassword())).isEqualTo(password);
    assertThat(zScalerConfigurationDAO.get().getHostname()).isEqualTo(hostname);
    assertThat(zScalerConfigurationDAO.get().getApikey()).isEqualTo(apiKey);
  }

  private void assertValidationError(String errorMessage) {
    FormUtils.getAlertElement(page).shouldBe(visible)
        .shouldHave(text(FormUtils.DEFAULT_VALIDATION_ERRORS_PREFIX + " " + errorMessage));
  }
}
