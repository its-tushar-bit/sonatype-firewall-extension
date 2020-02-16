/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.Dropdown.Option;
import com.sonatype.clm.testing.functional.elements.Tooltip;
import com.sonatype.clm.testing.functional.pages.SamlConfigurationPage;
import com.sonatype.insight.brain.dataaccess.configuration.saml.SamlConfigurationDAO;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.model.configuration.saml.SamlConfiguration;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.Keys;

import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.elements.CLM.DISABLED;
import static com.sonatype.clm.testing.functional.utils.BaseUrl.rootUriBuilder;
import static org.assertj.core.api.Assertions.assertThat;

public class SamlConfigurationPageTest
    extends AbstractFunctionalTest
{
  @Before
  public void before() {
    refreshOrOpen(SamlConfigurationPage.url());
    loginAsAdmin();
    SamlConfigurationPage.scrollToTop();
  }

  @After
  public void after() {
    logout();
    new SamlConfigurationDAO().delete();
  }

  @Test
  public void testDefaultState() {
    eyesWatcher.eyesCheck("saml configuration editor top");
    SamlConfigurationPage.identityProviderName().shouldBe(value("identity provider"));
    SamlConfigurationPage.identityProviderMetadataXmlTextArea().shouldBe(text(""));

    SamlConfigurationPage.validateResponseSignatureDropdown().selectedItem().shouldBe(text("Default"));
    SamlConfigurationPage.validateAssertionSignatureDropdown().selectedItem().shouldBe(text("Default"));

    SamlConfigurationPage.scrollToBottom();
    SamlConfigurationPage.entityId().shouldHave(value(rootUriBuilder().build() + "api/v2/config/saml/metadata"));

    SamlConfigurationPage.usernameAttribute().shouldBe(value("username"));
    SamlConfigurationPage.firstNameAttribute().shouldBe(value("firstName"));
    eyesWatcher.eyesCheck("saml configuration editor bottom");
    SamlConfigurationPage.lastNameAttribute().shouldBe(value("lastName"));
    SamlConfigurationPage.emailAttribute().shouldBe(value("email"));
    SamlConfigurationPage.groupsAttribute().shouldBe(value("groups"));

    // Save is shown if there is no existing configuration
    SamlConfigurationPage.saveButton().shouldHave(DISABLED);
    SamlConfigurationPage.saveButton().shouldBe(text("Save"));

    SamlConfigurationPage.cancelButton().shouldBe(disabled);
    SamlConfigurationPage.deleteButton().shouldBe(disabled);

    // If no configuration is saved, the download button is "disabled" and the popover shows.
    SamlConfigurationPage.scrollToTop();
    SamlConfigurationPage.downloadIqMetadataButton().shouldHave(DISABLED);
    SamlConfigurationPage.downloadIqMetadataButton().hover();
    Tooltip.get().shouldBe(visible).shouldBe(text("Nothing to download until a SAML configuration is saved"));

    // The links are as expected.
    SamlConfigurationPage.documentationLink().shouldBe(
        attribute("href", "http://links.sonatype.com/products/nxiq/doc/saml-integration"));
    SamlConfigurationPage.feedbackWelcomeLink().shouldBe(
        attribute("href", "http://links.sonatype.com/products/nxiq/feedback/saml"));
  }

  @Test
  public void testDefaultValuesSetIfFieldEmptyAndTooltipsAreShown() {
    // If an input field with a default value is empty and loses focus, it gets set to its default value.
    // A popover shows for each input field which has a default value asserting the value of the value.
    SamlConfigurationPage.identityProviderName().hover();
    Tooltip.get().shouldBe(visible).shouldBe(text("If empty will default to \"identity provider\""));
    SamlConfigurationPage.identityProviderName().clear();
    SamlConfigurationPage.entityId().click();
    SamlConfigurationPage.identityProviderName().shouldBe(value("identity provider"));

    SamlConfigurationPage.scrollToBottom();
    String defaultEntityId = rootUriBuilder().build() + "api/v2/config/saml/metadata";
    SamlConfigurationPage.entityId().hover();
    Tooltip.get().shouldBe(visible).shouldBe(text("If empty will default to \"" + defaultEntityId + "\""));
    SamlConfigurationPage.entityId().clear();
    SamlConfigurationPage.usernameAttribute().click();
    SamlConfigurationPage.entityId().shouldBe(value(defaultEntityId));
    SamlConfigurationPage.usernameAttribute().hover();
    Tooltip.get().shouldBe(visible).shouldBe(text("If empty will default to \"username\""));
    SamlConfigurationPage.usernameAttribute().clear();
    SamlConfigurationPage.firstNameAttribute().click();
    SamlConfigurationPage.usernameAttribute().shouldBe(value("username"));

    SamlConfigurationPage.firstNameAttribute().hover();
    Tooltip.get().shouldBe(visible).shouldBe(text("If empty will default to \"firstName\""));
    SamlConfigurationPage.firstNameAttribute().clear();
    SamlConfigurationPage.scrollToBottom();
    SamlConfigurationPage.lastNameAttribute().click();
    SamlConfigurationPage.firstNameAttribute().shouldBe(value("firstName"));

    SamlConfigurationPage.lastNameAttribute().hover();
    Tooltip.get().shouldBe(visible).shouldBe(text("If empty will default to \"lastName\""));
    SamlConfigurationPage.lastNameAttribute().clear();
    SamlConfigurationPage.emailAttribute().click();
    SamlConfigurationPage.lastNameAttribute().shouldBe(value("lastName"));

    SamlConfigurationPage.emailAttribute().hover();
    Tooltip.get().shouldBe(visible).shouldBe(text("If empty will default to \"email\""));
    SamlConfigurationPage.emailAttribute().clear();
    SamlConfigurationPage.groupsAttribute().click();
    SamlConfigurationPage.emailAttribute().shouldBe(value("email"));

    SamlConfigurationPage.groupsAttribute().hover();
    Tooltip.get().shouldBe(visible).shouldBe(text("If empty will default to \"groups\""));
    SamlConfigurationPage.groupsAttribute().clear();
    SamlConfigurationPage.entityId().click();
    SamlConfigurationPage.groupsAttribute().shouldBe(value("groups"));
  }

  @Test
  public void testCancelRevertsAllFields() {
    // Cancel reverts the changed fields back to their original values.
    SamlConfigurationPage.identityProviderName().clear();
    SamlConfigurationPage.identityProviderName().sendKeys("My Awesome IdP");
    SamlConfigurationPage.loadXmlInput().uploadFromClasspath(
        "com/sonatype/clm/testing/functional/brain/SamlConfigurationTest/identity-provider-metadata.xml");
    SamlConfigurationPage.validateResponseSignatureDropdown().chooseOption(new Option(1, "True"));
    SamlConfigurationPage.validateAssertionSignatureDropdown().chooseOption(new Option(2, "False"));
    SamlConfigurationPage.scrollToBottom();
    SamlConfigurationPage.entityId().clear();
    SamlConfigurationPage.entityId().sendKeys("http://my-iq-server/entity-id");
    SamlConfigurationPage.usernameAttribute().clear();
    SamlConfigurationPage.usernameAttribute().sendKeys("my-user-name");
    SamlConfigurationPage.firstNameAttribute().clear();
    SamlConfigurationPage.firstNameAttribute().sendKeys("my-first-name");
    SamlConfigurationPage.lastNameAttribute().clear();
    SamlConfigurationPage.lastNameAttribute().sendKeys("my-last-name");
    SamlConfigurationPage.emailAttribute().clear();
    SamlConfigurationPage.emailAttribute().sendKeys("my-email");
    SamlConfigurationPage.groupsAttribute().clear();
    SamlConfigurationPage.groupsAttribute().sendKeys("my-groups");

    SamlConfigurationPage.cancelButton().click();

    SamlConfigurationPage.identityProviderName().shouldBe(value("identity provider"));
    SamlConfigurationPage.validateResponseSignatureDropdown().selectedItem().shouldBe(text("Default"));
    SamlConfigurationPage.validateAssertionSignatureDropdown().selectedItem().shouldBe(text("Default"));
    SamlConfigurationPage.usernameAttribute().shouldBe(value("username"));
    SamlConfigurationPage.firstNameAttribute().shouldBe(value("firstName"));
    SamlConfigurationPage.lastNameAttribute().shouldBe(value("lastName"));
    SamlConfigurationPage.emailAttribute().shouldBe(value("email"));
    SamlConfigurationPage.groupsAttribute().shouldBe(value("groups"));
    SamlConfigurationPage.identityProviderMetadataXmlTextArea().shouldBe(text(""));
  }

  @Test
  public void testCrud() throws Exception {
    // Indicator for configured/not configured shows as expected.
    SamlConfigurationPage.isConfiguredText().shouldBe(text("not configured"));
    SamlConfigurationPage.isConfiguredIcon().shouldBe(cssClass("gray"));

    // Saving a new configuration requires Identity Provider XML.
    SamlConfigurationPage.saveButton().shouldHave(DISABLED);
    SamlConfigurationPage.loadXmlInput().uploadFromClasspath(
        "com/sonatype/clm/testing/functional/brain/SamlConfigurationTest/identity-provider-metadata.xml");
    SamlConfigurationPage.saveButton().shouldHave(cssClass("iq-btn--primary"));

    SamlConfigurationPage.identityProviderName().clear();
    SamlConfigurationPage.identityProviderName().sendKeys("My Awesome IdP");
    SamlConfigurationPage.validateResponseSignatureDropdown().chooseOption(new Option(2, "False"));
    SamlConfigurationPage.validateAssertionSignatureDropdown().chooseOption(new Option(1, "True"));
    SamlConfigurationPage.scrollToBottom();
    SamlConfigurationPage.entityId().clear();
    SamlConfigurationPage.entityId().sendKeys("http://my-iq-server/entity-id");
    SamlConfigurationPage.usernameAttribute().clear();
    SamlConfigurationPage.usernameAttribute().sendKeys("my-user-name");
    SamlConfigurationPage.firstNameAttribute().clear();
    SamlConfigurationPage.firstNameAttribute().sendKeys("my-first-name");
    SamlConfigurationPage.lastNameAttribute().clear();
    SamlConfigurationPage.lastNameAttribute().sendKeys("my-last-name");
    SamlConfigurationPage.emailAttribute().clear();
    SamlConfigurationPage.emailAttribute().sendKeys("my-email");
    SamlConfigurationPage.groupsAttribute().clear();
    SamlConfigurationPage.groupsAttribute().sendKeys("my-groups");

    SamlConfigurationPage.saveButton().click();

    // Indicator for configured/not configured shows as expected.
    SamlConfigurationPage.scrollToTop();
    SamlConfigurationPage.isConfiguredText().shouldBe(text("configured"));
    SamlConfigurationPage.isConfiguredIcon().shouldHave(cssClass("fa-check-circle"));

    // Update is shown if there is an existing configuration.
    SamlConfigurationPage.scrollToBottom();
    SamlConfigurationPage.saveButton().shouldBe(text("Update"));

    // Save/Update actually sets the SAML configuration to the saved/updated values.
    SamlConfigurationPage.scrollToTop();
    SamlConfigurationPage.identityProviderName().shouldBe(value("My Awesome IdP"));
    SamlConfigurationPage.validateResponseSignatureDropdown().selectedItem().shouldBe(text("False"));
    SamlConfigurationPage.validateAssertionSignatureDropdown().selectedItem().shouldBe(text("True"));
    SamlConfigurationPage.entityId().shouldBe(value("http://my-iq-server/entity-id"));
    SamlConfigurationPage.usernameAttribute().shouldBe(value("my-user-name"));
    SamlConfigurationPage.firstNameAttribute().shouldBe(value("my-first-name"));
    SamlConfigurationPage.lastNameAttribute().shouldBe(value("my-last-name"));
    SamlConfigurationPage.emailAttribute().shouldBe(value("my-email"));
    SamlConfigurationPage.groupsAttribute().shouldBe(value("my-groups"));
    assertThat(SamlConfigurationPage.identityProviderMetadataXmlTextArea().getValue()).startsWith("<?xml");

    logout();
    refreshOrOpen(SamlConfigurationPage.url());
    loginAsAdmin();

    // Any saved configuration is loaded when going to the page.
    SamlConfigurationPage.identityProviderName().shouldBe(value("My Awesome IdP"));
    SamlConfigurationPage.validateResponseSignatureDropdown().selectedItem().shouldBe(text("False"));
    SamlConfigurationPage.validateAssertionSignatureDropdown().selectedItem().shouldBe(text("True"));
    SamlConfigurationPage.entityId().shouldBe(value("http://my-iq-server/entity-id"));
    SamlConfigurationPage.usernameAttribute().shouldBe(value("my-user-name"));
    SamlConfigurationPage.firstNameAttribute().shouldBe(value("my-first-name"));
    SamlConfigurationPage.lastNameAttribute().shouldBe(value("my-last-name"));
    SamlConfigurationPage.emailAttribute().shouldBe(value("my-email"));
    SamlConfigurationPage.groupsAttribute().shouldBe(value("my-groups"));
    assertThat(SamlConfigurationPage.identityProviderMetadataXmlTextArea().getValue()).startsWith("<?xml");

    // Downloading IQ Server's (our) service provider metadata requires a saved configuration.
    SamlConfigurationPage.downloadIqMetadataButton().shouldHave(cssClass("iq-btn"));

    // Downloading saves the metadata.xml file directly as expected.
    File download = SamlConfigurationPage.downloadIqMetadataButton().download();
    String content = FileUtils.readFileToString(download, StandardCharsets.UTF_8);
    assertThat(content.trim()).startsWith("<?xml").endsWith("</EntityDescriptor>");

    // Save/Update are only enabled if valid changes are made.
    SamlConfigurationPage.scrollToBottom();
    SamlConfigurationPage.saveButton().shouldHave(DISABLED);
    SamlConfigurationPage.saveButton().shouldBe(text("Update"));

    // Loading an XML file for the identity provider xml works as expected and overwrites anything already in the box.
    SamlConfigurationPage.loadXmlInput().uploadFromClasspath(
        "com/sonatype/clm/testing/functional/brain/SamlConfigurationTest/identity-provider-metadata-modified.xml");
    assertThat(SamlConfigurationPage.identityProviderMetadataXmlTextArea().getValue().trim())
        .endsWith("<!--modified-->");

    SamlConfigurationPage.saveButton().shouldNotHave(DISABLED);
    SamlConfigurationPage.saveButton().shouldBe(text("Update"));

    // Delete button is only enabled if a configuration exists.
    SamlConfigurationPage.deleteButton().shouldBe(enabled);
    SamlConfigurationPage.deleteButton().click();
    SamlConfigurationPage.deleteButtonModal().click();

    // Deleting a configuration empties the identity provider xml and sets default values for all other fields.
    SamlConfigurationPage.identityProviderName().shouldBe(value("identity provider"));
    SamlConfigurationPage.validateResponseSignatureDropdown().selectedItem().shouldBe(text("Default"));
    SamlConfigurationPage.validateAssertionSignatureDropdown().selectedItem().shouldBe(text("Default"));
    SamlConfigurationPage.usernameAttribute().shouldBe(value("username"));
    SamlConfigurationPage.firstNameAttribute().shouldBe(value("firstName"));
    SamlConfigurationPage.lastNameAttribute().shouldBe(value("lastName"));
    SamlConfigurationPage.emailAttribute().shouldBe(value("email"));
    SamlConfigurationPage.groupsAttribute().shouldBe(value("groups"));
    SamlConfigurationPage.identityProviderMetadataXmlTextArea().shouldBe(value(""));
  }

  @Test
  public void testIdentityProviderName_MaximumLength() {
    SamlConfigurationPage.loadXmlInput().uploadFromClasspath(
        "com/sonatype/clm/testing/functional/brain/SamlConfigurationTest/identity-provider-metadata.xml");
    SamlConfigurationPage.identityProviderName().sendKeys(Keys.HOME, Keys.chord(Keys.SHIFT, Keys.END),
        StringUtils.repeat('a', SamlConfiguration.IDENTITY_PROVIDER_NAME_MAXIMUM_LENGTH));
    popoverViolations(SamlConfigurationPage.identityProviderName()).shouldBe(hidden);
    SamlConfigurationPage.scrollToBottom();
    SamlConfigurationPage.saveButton().shouldNotHave(DISABLED);

    SamlConfigurationPage.scrollToTop();
    SamlConfigurationPage.identityProviderName().sendKeys("a");
    popoverViolations(SamlConfigurationPage.identityProviderName()).shouldBe(visible)
        .shouldHave(text("Maximum length"));
    SamlConfigurationPage.scrollToBottom();
    SamlConfigurationPage.saveButton().shouldHave(DISABLED);
  }

  @Test
  public void testLoadError_Delete() throws Exception {
    try (Connection connection = OperationalDataStoreProvider.getDataSource().getConnection();
         Statement statement = connection.createStatement()) {
      statement.execute("INSERT INTO insight_brain_ods.saml_configuration " +
          "VALUES ('474878d8bfe44d2086ca8387e340692f', '{}', '', '');");
    }
    refreshOrOpen(SamlConfigurationPage.url());
    SamlConfigurationPage.deleteButton().shouldBe(visible, enabled).click();
    SamlConfigurationPage.deleteButtonModal().click();
    SamlConfigurationPage.scrollToBottom();
    SamlConfigurationPage.saveButton().shouldHave(text("Save"));
  }
}
