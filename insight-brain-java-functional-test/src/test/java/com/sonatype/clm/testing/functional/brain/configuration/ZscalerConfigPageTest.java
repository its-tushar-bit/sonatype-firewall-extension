/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.configuration;

import java.time.Duration;
import java.util.List;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.NxTextInput;
import com.sonatype.clm.testing.functional.elements.Tooltip;
import com.sonatype.clm.testing.functional.elements.UnsavedModal;
import com.sonatype.clm.testing.functional.pages.ApiPage;
import com.sonatype.clm.testing.functional.pages.IndexPage;
import com.sonatype.clm.testing.functional.pages.ZscalerConfigPage;
import com.sonatype.clm.testing.functional.utils.FormUtils;
import com.sonatype.insight.brain.dataaccess.configuration.ZScalerConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ZscalerFormatDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.configuration.ZscalerFormat;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.security.PasswordHandler;

import com.codeborne.selenide.SelenideElement;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.Keys;

import static com.codeborne.selenide.CollectionCondition.size;
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

  private ZscalerFormatDAO zscalerFormatDAO;

  private PasswordHandler passwordHandler;

  private static final String FAKE_PASSWORD = "\u0000\u0000\u0000\u0000\u0000";

  private static final String EULA_TEXT_STRING = """
      By clicking "Save" below, I hereby acknowledge and agree that
      access to and use of Sonatype's Zscaler integration is subject to
      and governed by these License Terms.
      """;

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(IndexPage.url());
    loginAsAdmin();
  }

  @Before
  public void setUp() {
    zScalerConfigurationDAO = lookup(ZScalerConfigurationDAO.class);
    zscalerFormatDAO = lookup(ZscalerFormatDAO.class);
    passwordHandler = lookup(PasswordHandler.class);
    SystemConfigurationPropertyFeature.ZSCALER.setEnabled(true);
  }

  @Test
  public void testUserNotAuthorized() {
    try {
      User user = tempEntity.newUser("username", "foo", "bar", "foo@bar");
      refreshOrOpen(ZscalerConfigPage.url());
      logout();
      login(user.getUsername(), user.getPassword());
      refreshOrOpen(ZscalerConfigPage.url());

      page.loadError()
          .shouldBe(visible)
          .shouldHave(text("An error occurred loading data. It appears you do not have " +
              "permission to access this page. If you believe this to be incorrect please contact your administrator.\n"
              +
              "Retry"));
    }
    finally {
      logout();
      loginAsAdmin();
    }
  }

  @Test
  public void testInitialState() {
    refreshOrOpen(ZscalerConfigPage.url());
    page.loadError().shouldNotBe(visible);

    page.zscalerConfigHeader().shouldHave(text("Zscaler Configuration"));
    assertNoZscalerConfig();
    page.formatTooltipIcon().shouldBe(visible);
    page.save().shouldBe(enabled);
    page.save().shouldHave(text("Save"));
    page.delete().shouldBe(disabled);
    page.cancel().shouldBe(disabled);
    page.testConfig().shouldHave(attribute("aria-disabled", "true"));
  }

  @Test
  public void testSaveConfig() {
    refreshOrOpen(ZscalerConfigPage.url());
    fillConfigurationFields();
    saveConfiguration();

    // Check that the config is saved in the database
    assertConfigIsSaved("user", "asdf", "https://zsapi.zscalertwo.net", "key", true, false, false, false);

    // page is updated with new values
    page.username().shouldBe(value("user"));
    page.password().shouldBe(value(FAKE_PASSWORD));
    page.hostname().shouldBe(value("https://zsapi.zscalertwo.net"));
    page.apiKey().shouldBe(value("key"));
    page.formatDropdownButton().shouldHave(text("1 of 4"));
    page.formatDropdownButton().click();
    page.getFormatCheckboxAt(0).shouldBe(selected);
    page.formatDropdownButton().click();
    page.eulaCheckbox().shouldBe(selected).shouldBe(disabled);
  }

  @Test
  public void testSaveConfig_failsIfEulaCheckboxNotChecked() {
    refreshOrOpen(ZscalerConfigPage.url());
    page.username().setValue("user");
    page.password().setValue("asdf");
    page.hostname().setValue("https://zsapi.zscalertwo.net");
    page.apiKey().setValue("key");
    page.formatDropdownButton().click();
    page.getFormatCheckboxAt(0).click();
    page.formatDropdownButton().click();

    saveConfiguration();
    assertValidationError("Username, Password, Hostname, Zscaler API Key, Configured format and " +
        "End User License Agreement are required details.");
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
    page.formatDropdownButton().click();
    page.getFormatCheckboxAt(0).click();
    page.getFormatCheckboxAt(1).click();
    page.getFormatCheckboxAt(2).click();
    saveConfiguration();

    // Check that the config is updated in the database
    assertConfigIsSaved("user1", "foobar", "https://zsapi.zscalerthree.net", "key1", false, true, true, false);

    // page is updated with new values
    page.username().shouldBe(value("user1"));
    page.password().shouldBe(value(FAKE_PASSWORD));
    page.hostname().shouldBe(value("https://zsapi.zscalerthree.net"));
    page.apiKey().shouldBe(value("key1"));
    page.formatDropdownButton().click();
    page.getFormatCheckboxAt(0).shouldNotBe(selected);
    page.getFormatCheckboxAt(1).shouldBe(selected);
    page.getFormatCheckboxAt(2).shouldBe(selected);
    page.getFormatCheckboxAt(3).shouldNotBe(selected);
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

    List<SelenideElement> inputElements = asList(page.username(), page.apiKey());
    inputElements.forEach(ele -> ele.setValue("a"));
    page.hostname().setValue("https://zsapi.zscalerthree.net");
    saveConfiguration();
    assertValidationError("Password must be re-entered when any fields are modified.");
  }

  @Test
  public void testAllFieldsRequiredFormValidationErrors() {
    refreshOrOpen(ZscalerConfigPage.url());
    saveConfiguration();
    assertValidationError("Username, Password, Hostname, Zscaler API Key, Configured format and " +
        "End User License Agreement are required details.");
  }

  @Test
  public void testFieldsValidationErrors() {
    refreshOrOpen(ZscalerConfigPage.url());
    List<SelenideElement> inputElements = asList(page.username(), page.password(), page.hostname(), page.apiKey());
    inputElements.forEach(ele -> ele.setValue("a"));
    inputElements.forEach(ele -> ele.sendKeys(Keys.BACK_SPACE));

    new NxTextInput(page.username()).errorMessage().shouldHave(text("Must be non-empty"));
    new NxTextInput(page.password()).errorMessage().shouldHave(text("Must be non-empty"));
    new NxTextInput(page.apiKey()).errorMessage().shouldHave(text("Must be non-empty"));
    // Hostname field has specific URL validation message
    new NxTextInput(page.hostname()).errorMessage().shouldHave(text("URL is required"));
  }

  @Test
  public void testFormatDropdown() {
    refreshOrOpen(ZscalerConfigPage.url());
    page.username().setValue("user");
    page.password().setValue("asdf");
    page.hostname().setValue("https://zsapi.zscalertwo.net");
    page.apiKey().setValue("key");
    page.formatDropdownButton().shouldHave(text("Formats"));
    page.formatDropdownButton().click();
    page.getFormatCheckboxAt(0).click();
    page.getFormatCheckboxAt(0).shouldBe(selected);
    page.formatDropdownButton().shouldHave(text("1 of 4"));
    page.getFormatCheckboxAt(1).click();
    page.getFormatCheckboxAt(1).shouldBe(selected);
    page.formatDropdownButton().shouldHave(text("2 of 4"));
    page.getFormatCheckboxAt(2).click();
    page.getFormatCheckboxAt(2).shouldBe(selected);
    page.formatDropdownButton().shouldHave(text("3 of 4"));
    page.getFormatCheckboxAt(3).click();
    page.getFormatCheckboxAt(3).shouldBe(selected);
    page.formatDropdownButton().shouldHave(text("4 of 4"));

    // show validation error when all formats are unchecked
    page.getFormatCheckboxAt(0).click();
    page.getFormatCheckboxAt(1).click();
    page.getFormatCheckboxAt(2).click();
    page.getFormatCheckboxAt(3).click();
    page.formatDropdownButton().shouldHave(text("Formats"));
    page.formatValidationError().shouldBe(visible).shouldHave(text("At least one format must be selected"));

    // show tooltip when hovering over the icon
    page.formatTooltipIcon().shouldBe(visible).hover();
    Tooltip.get()
        .shouldBe(visible)
        .shouldHave(text("URLs pushed to Zscaler are based on official package sources. " +
            "Limiting formats reduces noise and optimizes security rules. Dependencies from unofficial or custom sources "
            +
            "are not fully protected by this integration."));
  }

  @Test
  public void testSendTestConfig() {
    refreshOrOpen(ZscalerConfigPage.url());
    page.testConfig().shouldHave(attribute("aria-disabled", "true"));
    page.testConfig().hover();
    Tooltip.get()
        .shouldBe(visible)
        .shouldHave(text("Username, Password, Hostname and Zscaler API Key are " +
            "required details."));

    fillConfigurationFields();
    saveConfiguration();
    page.testConfig().shouldHave(attribute("aria-disabled", "true"));
    page.testConfig().hover();
    Tooltip.get().shouldBe(visible).shouldHave(text("Password must be re-entered for testing configuration."));

    // Only test the config with dummy values as we don't want to have real credentials in the test
    page.password().setValue("a");
    page.testConfig().shouldBe(enabled).click();
    FormMask.seeAndWaitForDismissal();
    FormUtils.getErrorElement(page).shouldBe(visible);
    FormUtils.getErrorElement(page.zscalerFormSection())
        .shouldBe(visible)
        .shouldHave(text("Test Zscaler configuration failed."));
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

  @Test
  public void testZscalerCustomUrls() {
    refreshOrOpen(ZscalerConfigPage.url());
    page.zscalerCustomUrlsHeader().shouldBe(visible, Duration.ofSeconds(30)).shouldHave(text("Zscaler Custom URLs"));
    page.gridHeaders().shouldHave(size(3));
    page.gridHeaders().get(0).shouldHave(text("Total Purchased"));
    page.gridHeaders().get(1).shouldHave(text("Remaining"));
    page.gridHeaders().get(2).shouldHave(text("Status"));
    page.indicator().shouldBe(visible);
  }

  private void assertNoZscalerConfig() {
    page.username().shouldBe(empty);
    page.password().shouldBe(empty);
    page.hostname().shouldBe(empty);
    page.apiKey().shouldBe(empty);
    page.eulaCheckbox().shouldNotBe(selected).shouldBe(enabled);
    page.eulaCheckbox().label().shouldHave(text(EULA_TEXT_STRING));
    page.formatDropdownButton().shouldHave(text("Formats"));
    page.formatDropdownButton().click();
    page.getFormatCheckboxAt(0).shouldBe(enabled).shouldNotBe(selected).shouldHave(text("Maven"));
    page.getFormatCheckboxAt(1).shouldBe(enabled).shouldNotBe(selected).shouldHave(text("Npm"));
    page.getFormatCheckboxAt(2).shouldBe(enabled).shouldNotBe(selected).shouldHave(text("Nuget"));
    page.getFormatCheckboxAt(3).shouldBe(enabled).shouldNotBe(selected).shouldHave(text("Pypi"));
    assertThat(zScalerConfigurationDAO.get()).isNull();
  }

  private void fillConfigurationFields() {
    page.username().setValue("user");
    page.password().setValue("asdf");
    page.hostname().setValue("https://zsapi.zscalertwo.net");
    page.apiKey().setValue("key");
    page.formatDropdownButton().click();
    page.getFormatCheckboxAt(0).click();
    page.formatDropdownButton().click();
    page.eulaCheckbox().click();
  }

  private void saveConfiguration() {
    page.save().click();
    FormMask.seeAndWaitForDismissal();
  }

  private void assertConfigIsSaved(
      String username,
      String password,
      String hostname,
      String apiKey,
      boolean mavenFormatEnabled,
      boolean npmFormatEnabled,
      boolean nugetFormatEnabled,
      boolean pypiFormatEnabled)
  {
    assertThat(zScalerConfigurationDAO.get()).isNotNull();
    assertThat(zScalerConfigurationDAO.get().getUsername()).isEqualTo(username);
    assertThat(passwordHandler.decryptPassword(zScalerConfigurationDAO.get().getPassword())).isEqualTo(password);
    assertThat(zScalerConfigurationDAO.get().getHostname()).isEqualTo(hostname);
    assertThat(zScalerConfigurationDAO.get().getApikey()).isEqualTo(apiKey);
    List<ZscalerFormat> zscalerFormats = zscalerFormatDAO.getAll();
    for (ZscalerFormat format : zscalerFormats) {
      switch (format.getFormat()) {
        case "maven" -> assertThat(format.isEnabled()).isEqualTo(mavenFormatEnabled);
        case "npm" -> assertThat(format.isEnabled()).isEqualTo(npmFormatEnabled);
        case "nuget" -> assertThat(format.isEnabled()).isEqualTo(nugetFormatEnabled);
        case "pypi" -> assertThat(format.isEnabled()).isEqualTo(pypiFormatEnabled);
        default -> throw new IllegalArgumentException("Unknown format: " + format.getFormat());
      }
    }
  }

  private void assertValidationError(String errorMessage) {
    FormUtils.getAlertElement(page)
        .shouldBe(visible)
        .shouldHave(text(FormUtils.DEFAULT_VALIDATION_ERRORS_PREFIX + " " + errorMessage));
  }
}
