/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

/**
 * Playwright page object for the SAML Configuration page.
 */
public class SamlConfigurationPage
    extends BasePage
{
  public SamlConfigurationPage() {
    super();
  }

  public static String url() {
    return "/assets/index.html#/saml";
  }

  public Locator idpUrl() {
    return byLabel("Identity Provider Metadata URL");
  }

  public Locator entityId() {
    return byLabel("Entity ID");
  }

  public Locator usernameAttribute() {
    return byLabel("Username Attribute");
  }

  public Locator firstNameAttribute() {
    return byLabel("First Name Attribute");
  }

  public Locator lastNameAttribute() {
    return byLabel("Last Name Attribute");
  }

  public Locator emailAttribute() {
    return byLabel("Email Attribute");
  }

  public Locator groupsAttribute() {
    return byLabel("Groups Attribute");
  }

  public Locator validateCertificateToggle() {
    return byRole(AriaRole.CHECKBOX, "Validate Response Signature");
  }

  public Locator saveButton() {
    return byRole(AriaRole.BUTTON, "Save");
  }

  public Locator cancelButton() {
    return byRole(AriaRole.BUTTON, "Cancel");
  }

  public Locator deleteButton() {
    return byRole(AriaRole.BUTTON, "Delete");
  }

  public Locator identityProviderName() {
    return byLabel("Identity Provider Name");
  }

  public Locator identityProviderMetadataXml() {
    return byLabel("Identity Provider Metadata XML");
  }

  public Locator documentationLink() {
    return locator("#saml-explanation");
  }

  public Locator feedbackLink() {
    return locator("#saml-feedback-link");
  }
}
