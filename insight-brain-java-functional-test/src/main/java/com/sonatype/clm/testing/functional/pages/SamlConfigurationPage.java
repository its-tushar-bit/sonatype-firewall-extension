/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.Button;
import com.sonatype.clm.testing.functional.elements.NxFormSelect;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.clm.testing.functional.utils.ScrollUtil;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Selenide.$;

public class SamlConfigurationPage
    extends BasicElement<SamlConfigurationPage>
{
  public String url() {
    return BaseUrl.resolvePageUrl("/saml");
  }

  public SelenideElement downloadIqMetadataButton() {
    return $("#saml-iq-server-metadata");
  }

  public SelenideElement identityProviderName() {
    return $("#saml-identity-provider-name input");
  }

  public SelenideElement identityProviderMetadataXmlTextArea() {
    return $("#saml-identity-provider-metadata-xml");
  }

  public SelenideElement entityId() {
    return $("#saml-entity-id input");
  }

  public SelenideElement usernameAttribute() {
    return $("#saml-username-attribute-name input");
  }

  public SelenideElement firstNameAttribute() {
    return $("#saml-first-name-attribute-name input");
  }

  public SelenideElement lastNameAttribute() {
    return $("#saml-last-name-attribute-name input");
  }

  public SelenideElement emailAttribute() {
    return $("#saml-email-attribute-name input");
  }

  public SelenideElement groupsAttribute() {
    return $("#saml-groups-attribute-name input");
  }

  public SelenideElement loadXmlInput() {
    return $("#saml-identity-provider-metadata-xml-load");
  }

  public NxFormSelect validateResponseSignatureDropdown() {
    return new NxFormSelect(childSelector("#select-validate-response-signature"));
  }

  public NxFormSelect validateAssertionSignatureDropdown() {
    return new NxFormSelect(childSelector("#select-validate-assertion-signature"));
  }

  public Button saveButton() {
    return new Button(".iq-saml-configuration-save-button");
  }

  public Button cancelButton() {
    return new Button("#saml-cancel");
  }

  public Button deleteButton() {
    return new Button("#saml-delete");
  }

  // Are you sure you want to delete this SAML configuration?
  public SelenideElement deleteButtonModal() {
    return $("#saml-configuration-delete-modal > .nx-footer > .nx-btn-bar > .nx-btn--secondary");
  }

  // Here you can configure SAML integration..
  public SelenideElement documentationLink() {
    return $("#saml-explanation");
  }

  // Feedback is welcome.
  public SelenideElement feedbackWelcomeLink() {
    return $("#saml-feedback-link");
  }

  public SelenideElement isConfiguredText() {
    return $("#saml-is-configured");
  }

  public void scrollToTop() {
    scrollIntoView(downloadIqMetadataButton());
  }

  public void scrollToBottom() {
    scrollIntoView(deleteButton().getElement());
  }

  private void scrollIntoView(SelenideElement element) {
    ScrollUtil.awaitEndOfScrolling(element.should(exist).scrollIntoView(true));
  }
}
