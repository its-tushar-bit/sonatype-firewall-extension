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
{
  public LdapConnectionForm(String... selectors) {
    super(selectors);
  }

  public SelenideElement authenticationMethod() {
    return child("#authenticationMethod");
  }

  public SelenideElement connectionTimeout() {
    return child("#connectionTimeout");
  }

  public List<SelenideElement> getRequiredFields() {
    return Arrays.asList(hostname(), searchBase());
  }

  public SelenideElement hostname() {
    return child("#hostname");
  }

  public SelenideElement port() {
    return child("#port");
  }

  public SelenideElement protocol() {
    return child("#protocol");
  }

  public SelenideElement retryDelay() {
    return child("#retryDelay");
  }

  public SelenideElement saslRealm() {
    return child("#saslRealm");
  }

  public SelenideElement searchBase() {
    return child("#searchBase");
  }

  public SelenideElement systemPassword() {
    return child("#systemPassword");
  }

  public SelenideElement systemUsername() {
    return child("#systemUsername");
  }

  public SelenideElement testConnectionButton() {
    return $("#ldap-connection-test");
  }

  public SelenideElement saveButton() {
    return $("#ldap-connection-save");
  }

  public SelenideElement cancelButton() {
    return $("#ldap-connection-cancel");
  }
}
