/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.Button;
import com.sonatype.clm.testing.functional.elements.Dropdown;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.clm.testing.functional.utils.ScrollUtil;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Selenide.$;

public class SamlConfigurationPage
    extends BasicElement<SamlConfigurationPage>
{
  public static String samlConfigurationUrl() {
    return BaseUrl.resolvePageUrl("/saml");
  }

  public static SelenideElement downloadIqMetadataButton() {
    return $("#saml-iq-server-metadata");
  }

  public static SelenideElement identityProviderMetadataXmlTextArea() {
    return $("#saml-identity-provider-metadata-xml");
  }

  public static SelenideElement entityId() {
    return $("#saml-entity-id");
  }

  public static SelenideElement usernameAttribute() {
    return $("#saml-username-attribute-name");
  }

  public static SelenideElement firstNameAttribute() {
    return $("#saml-first-name-attribute-name");
  }

  public static SelenideElement lastNameAttribute() {
    return $("#saml-last-name-attribute-name");
  }

  public static SelenideElement emailAttribute() {
    return $("#saml-email-attribute-name");
  }

  public static SelenideElement groupsAttribute() {
    return $("#saml-groups-attribute-name");
  }

  public static SelenideElement loadXmlInput() {
    return $("#saml-identity-provider-metadata-xml-load");
  }

  public static Dropdown validateResponseSignatureDropdown() {
    return new Dropdown("#select-validate-response-signature");
  }

  public static Dropdown validateAssertionSignatureDropdown() {
    return new Dropdown("#select-validate-assertion-signature");
  }

  public static Button saveButton() {
    return new Button("#saml-save");
  }

  public static Button cancelButton() {
    return new Button("#saml-cancel");
  }

  public static Button deleteButton() {
    return new Button("#saml-delete");
  }

  // Are you sure you want to delete this SAML configuration?
  public static SelenideElement deleteButtonModal() {
    return $("#delete-saml-confirmation > div.iq-modal-footer > button.btn.btn-primary");
  }

  // Here you can configure SAML integration..
  public static SelenideElement documentationLink() {
    return $("#saml-explanation > a");
  }

  // Feedback is welcome.
  public static SelenideElement feedbackWelcomeLink() {
    return $("#saml-configuration > div.iq-tile-content.iq-load-wrapper > div:nth-child(1) > div > p:nth-child(3) > a");
  }

  public static SelenideElement isConfiguredIcon() {
    return $("#saml-configuration > div.iq-tile-header.iq-tile-header--hrule > div.iq-tile-header__subtitle > i");
  }

  public static SelenideElement isConfiguredText() {
    return $("#saml-configuration > div.iq-tile-header.iq-tile-header--hrule > div.iq-tile-header__subtitle");
  }

  public static void scrollToTop() {
    scrollIntoView(downloadIqMetadataButton());
  }

  public static void scrollToBottom() {
    scrollIntoView(deleteButton().getElement());
  }

  private static void scrollIntoView(SelenideElement element) {
    ScrollUtil.awaitEndOfScrolling(element.should(exist).scrollIntoView(true));
  }
}
