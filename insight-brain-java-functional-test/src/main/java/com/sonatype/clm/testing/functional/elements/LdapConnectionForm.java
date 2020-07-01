/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import java.util.Arrays;
import java.util.List;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class LdapConnectionForm
    extends BasicElement<LdapConnectionForm>
    implements ILdapForm
{
  public LdapConnectionForm(String... selectors) {
    super(selectors);
  }

  public Dropdown authenticationMethod() {
    return new Dropdown(childSelector("#ldap-authentication-method"));
  }

  public SelenideElement connectionTimeout() {
    return child("#ldap-connection-timeout");
  }

  @Override
  public List<SelenideElement> requiredFields() {
    return Arrays.asList(hostname(), searchBase());
  }

  public SelenideElement hostname() {
    return child("#ldap-hostname");
  }

  public SelenideElement port() {
    return child("#ldap-port");
  }

  public Dropdown protocol() {
    return new Dropdown(childSelector("#ldap-protocol"));
  }

  public SelenideElement retryDelay() {
    return child("#ldap-retry-delay");
  }

  public SelenideElement saslRealm() {
    return child("#ldap-sasl-realm");
  }

  public SelenideElement searchBase() {
    return child("#ldap-search-base");
  }

  public Toggle ignoreReferrals() {
    return new Toggle(childSelector("#ldap-ignore-referrals"));
  }

  public SelenideElement systemPassword() {
    return child("#ldap-system-password");
  }

  public SelenideElement passwordNeedsEntryMessage() {
    return child("#ldap-password-needs-entry-message");
  }

  public SelenideElement systemUsername() {
    return child("#ldap-system-username");
  }

  public SelenideElement successAlertBox() {
    return child(".alert-success");
  }

  public SelenideElement testConnectionButton() {
    return $("#ldap-connection-test");
  }

  @Override
  public SelenideElement saveButton() {
    return $("#ldap-connection-save");
  }

  @Override
  public SelenideElement cancelButton() {
    return $("#ldap-connection-cancel");
  }
}
