/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.io.File;
import java.nio.charset.StandardCharsets;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.NxFormSelect.Option;
import com.sonatype.clm.testing.functional.elements.Tooltip;
import com.sonatype.clm.testing.functional.pages.SamlConfigurationPage;
import com.sonatype.insight.brain.configuration.saml.SamlConfigurationService;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.enabled;
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
    SamlConfigurationPage samlConfigurationPage = new SamlConfigurationPage();
    refreshOrOpen(samlConfigurationPage.url());
    loginAsAdmin();
    samlConfigurationPage.scrollToTop();
  }

  @After
  public void after() {
    logout();
    clearAlerts();
    lookup(SamlConfigurationService.class).delete();
  }

  @Test
  public void testDefaultState() {
    SamlConfigurationPage samlConfigurationPage = new SamlConfigurationPage();
    eyesWatcher.eyesCheck("saml configuration editor top");
    samlConfigurationPage.identityProviderName().shouldBe(value("identity provider"));
    samlConfigurationPage.identityProviderMetadataXmlTextArea().shouldBe(empty);

    samlConfigurationPage.validateResponseSignatureDropdown().shouldBe(text("Default"));
    samlConfigurationPage.validateAssertionSignatureDropdown().shouldBe(text("Default"));

    samlConfigurationPage.scrollToBottom();
    samlConfigurationPage.entityId().shouldHave(value(rootUriBuilder().build() + "api/v2/config/saml/metadata"));

    samlConfigurationPage.usernameAttribute().shouldBe(value("username"));
    samlConfigurationPage.firstNameAttribute().shouldBe(value("firstName"));
    eyesWatcher.eyesCheck("saml configuration editor bottom");
    samlConfigurationPage.lastNameAttribute().shouldBe(value("lastName"));
    samlConfigurationPage.emailAttribute().shouldBe(value("email"));
    samlConfigurationPage.groupsAttribute().shouldBe(value("groups"));

    // Save is shown if there is no existing configuration
    samlConfigurationPage.saveButton().shouldHave(DISABLED);
    samlConfigurationPage.saveButton().shouldBe(text("Save"));

    // samlConfigurationPage.cancelButton().shouldBe(disabled);
    samlConfigurationPage.deleteButton().shouldBe(disabled);

    // If no configuration is saved, the download button is "disabled" and the tooltip shows.
    samlConfigurationPage.scrollToTop();
    samlConfigurationPage.downloadIqMetadataButton().shouldHave(cssClass("disabled"));
    samlConfigurationPage.downloadIqMetadataButton().hover();
    Tooltip.get().shouldBe(visible).shouldBe(text("Nothing to download until a SAML configuration is saved"));

    // The links are as expected.
    samlConfigurationPage.documentationLink()
        .shouldBe(
            attribute("href", "http://links.sonatype.com/products/nxiq/doc/saml-integration"));
    samlConfigurationPage.feedbackWelcomeLink()
        .shouldBe(
            attribute("href", "http://links.sonatype.com/products/nxiq/feedback/saml"));
  }

  @Test
  public void testCancelRevertsAllFields() {
    // Cancel reverts the changed fields back to their original values.
    SamlConfigurationPage samlConfigurationPage = new SamlConfigurationPage();
    samlConfigurationPage.identityProviderName().clear();
    samlConfigurationPage.identityProviderName().sendKeys("My Awesome IdP");
    samlConfigurationPage.loadXmlInput()
        .uploadFromClasspath(
            "com/sonatype/clm/testing/functional/brain/SamlConfigurationTest/identity-provider-metadata.xml");
    samlConfigurationPage.validateResponseSignatureDropdown().chooseOption(new Option(1, "True"));
    samlConfigurationPage.validateAssertionSignatureDropdown().chooseOption(new Option(2, "False"));
    samlConfigurationPage.scrollToBottom();
    samlConfigurationPage.entityId().clear();
    samlConfigurationPage.entityId().sendKeys("http://my-iq-server/entity-id");
    samlConfigurationPage.usernameAttribute().clear();
    samlConfigurationPage.usernameAttribute().sendKeys("my-user-name");
    samlConfigurationPage.firstNameAttribute().clear();
    samlConfigurationPage.firstNameAttribute().sendKeys("my-first-name");
    samlConfigurationPage.lastNameAttribute().clear();
    samlConfigurationPage.lastNameAttribute().sendKeys("my-last-name");
    samlConfigurationPage.emailAttribute().clear();
    samlConfigurationPage.emailAttribute().sendKeys("my-email");
    samlConfigurationPage.groupsAttribute().clear();
    samlConfigurationPage.groupsAttribute().sendKeys("my-groups");

    samlConfigurationPage.cancelButton().click();

    samlConfigurationPage.identityProviderName().shouldBe(value("identity provider"));
    samlConfigurationPage.validateResponseSignatureDropdown().shouldBe(text("Default"));
    samlConfigurationPage.validateAssertionSignatureDropdown().shouldBe(text("Default"));
    samlConfigurationPage.usernameAttribute().shouldBe(value("username"));
    samlConfigurationPage.firstNameAttribute().shouldBe(value("firstName"));
    samlConfigurationPage.lastNameAttribute().shouldBe(value("lastName"));
    samlConfigurationPage.emailAttribute().shouldBe(value("email"));
    samlConfigurationPage.groupsAttribute().shouldBe(value("groups"));
    samlConfigurationPage.identityProviderMetadataXmlTextArea().shouldBe(empty);
  }

  @Test
  public void testCrud() throws Exception {
    SamlConfigurationPage samlConfigurationPage = new SamlConfigurationPage();
    // Indicator for configured/not configured shows as expected.
    samlConfigurationPage.isConfiguredText().shouldBe(text("* Currently not configured"));

    // Saving a new configuration requires Identity Provider XML.
    samlConfigurationPage.saveButton().shouldHave(DISABLED);
    samlConfigurationPage.loadXmlInput()
        .uploadFromClasspath(
            "com/sonatype/clm/testing/functional/brain/SamlConfigurationTest/identity-provider-metadata.xml");
    samlConfigurationPage.saveButton().shouldHave(cssClass("iq-saml-configuration-save-button"));

    samlConfigurationPage.identityProviderName().clear();
    samlConfigurationPage.identityProviderName().sendKeys("My Awesome IdP");
    samlConfigurationPage.validateResponseSignatureDropdown().chooseOption(new Option(2, "False"));
    samlConfigurationPage.validateAssertionSignatureDropdown().chooseOption(new Option(1, "True"));
    samlConfigurationPage.scrollToBottom();
    samlConfigurationPage.entityId().clear();
    samlConfigurationPage.entityId().sendKeys("http://my-iq-server/entity-id");
    samlConfigurationPage.usernameAttribute().clear();
    samlConfigurationPage.usernameAttribute().sendKeys("my-user-name");
    samlConfigurationPage.firstNameAttribute().clear();
    samlConfigurationPage.firstNameAttribute().sendKeys("my-first-name");
    samlConfigurationPage.lastNameAttribute().clear();
    samlConfigurationPage.lastNameAttribute().sendKeys("my-last-name");
    samlConfigurationPage.emailAttribute().clear();
    samlConfigurationPage.emailAttribute().sendKeys("my-email");
    samlConfigurationPage.groupsAttribute().clear();
    samlConfigurationPage.groupsAttribute().sendKeys("my-groups");

    samlConfigurationPage.saveButton().click();

    // Save/Update actually sets the SAML configuration to the saved/updated values.
    samlConfigurationPage.scrollToTop();
    samlConfigurationPage.identityProviderName().shouldBe(value("My Awesome IdP"));
    samlConfigurationPage.validateResponseSignatureDropdown().shouldBe(text("False"));
    samlConfigurationPage.validateAssertionSignatureDropdown().shouldBe(text("True"));
    samlConfigurationPage.entityId().shouldBe(value("http://my-iq-server/entity-id"));
    samlConfigurationPage.usernameAttribute().shouldBe(value("my-user-name"));
    samlConfigurationPage.firstNameAttribute().shouldBe(value("my-first-name"));
    samlConfigurationPage.lastNameAttribute().shouldBe(value("my-last-name"));
    samlConfigurationPage.emailAttribute().shouldBe(value("my-email"));
    samlConfigurationPage.groupsAttribute().shouldBe(value("my-groups"));
    assertThat(samlConfigurationPage.identityProviderMetadataXmlTextArea().getValue()).startsWith("<?xml");

    logout();
    refreshOrOpen(samlConfigurationPage.url());
    loginAsAdmin();

    // Any saved configuration is loaded when going to the page.
    samlConfigurationPage.identityProviderName().shouldBe(value("My Awesome IdP"));
    samlConfigurationPage.validateResponseSignatureDropdown().shouldBe(text("False"));
    samlConfigurationPage.validateAssertionSignatureDropdown().shouldBe(text("True"));
    samlConfigurationPage.entityId().shouldBe(value("http://my-iq-server/entity-id"));
    samlConfigurationPage.usernameAttribute().shouldBe(value("my-user-name"));
    samlConfigurationPage.firstNameAttribute().shouldBe(value("my-first-name"));
    samlConfigurationPage.lastNameAttribute().shouldBe(value("my-last-name"));
    samlConfigurationPage.emailAttribute().shouldBe(value("my-email"));
    samlConfigurationPage.groupsAttribute().shouldBe(value("my-groups"));
    assertThat(samlConfigurationPage.identityProviderMetadataXmlTextArea().getValue()).startsWith("<?xml");

    // Downloading IQ Server's (our) service provider metadata requires a saved configuration.
    samlConfigurationPage.downloadIqMetadataButton().shouldHave(cssClass("nx-btn"));

    // Downloading saves the metadata.xml file directly as expected.
    File download = samlConfigurationPage.downloadIqMetadataButton().download();
    String content = FileUtils.readFileToString(download, StandardCharsets.UTF_8);
    assertThat(content.trim()).startsWith("<?xml").endsWith("EntityDescriptor>");

    // Loading an XML file for the identity provider xml works as expected and overwrites anything already in the box.
    samlConfigurationPage.loadXmlInput()
        .uploadFromClasspath(
            "com/sonatype/clm/testing/functional/brain/SamlConfigurationTest/identity-provider-metadata-modified.xml");
    assertThat(samlConfigurationPage.identityProviderMetadataXmlTextArea().getValue().trim())
        .endsWith("<!--modified-->");

    samlConfigurationPage.saveButton().shouldNotHave(DISABLED);
    samlConfigurationPage.saveButton().shouldBe(text("Save"));

    // Delete button is only enabled if a configuration exists.
    samlConfigurationPage.deleteButton().shouldBe(enabled);
    samlConfigurationPage.deleteButton().click();
    samlConfigurationPage.deleteButtonModal().click();

    // Deleting a configuration empties the identity provider xml and sets default values for all other fields.
    samlConfigurationPage.identityProviderName().shouldBe(value("identity provider"));
    samlConfigurationPage.validateResponseSignatureDropdown().shouldBe(text("Default"));
    samlConfigurationPage.validateAssertionSignatureDropdown().shouldBe(text("Default"));
    samlConfigurationPage.usernameAttribute().shouldBe(value("username"));
    samlConfigurationPage.firstNameAttribute().shouldBe(value("firstName"));
    samlConfigurationPage.lastNameAttribute().shouldBe(value("lastName"));
    samlConfigurationPage.emailAttribute().shouldBe(value("email"));
    samlConfigurationPage.groupsAttribute().shouldBe(value("groups"));
    samlConfigurationPage.identityProviderMetadataXmlTextArea().shouldBe(value(""));
  }

}
