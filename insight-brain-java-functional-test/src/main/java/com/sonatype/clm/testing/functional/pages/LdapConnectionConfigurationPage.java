/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import java.util.Arrays;
import java.util.List;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class LdapConnectionConfigurationPage
    extends LdapConfigurationPage
{

  public static SelenideElement authenticationMethod() {
    return $("#authenticationMethod");
  }

  public static SelenideElement cancelButton() {
    return $("#ldap-connection-cancel");
  }

  public static SelenideElement connectionTimeout() {
    return $("#connectionTimeout");
  }

  public static SelenideElement discardChangesButton() {
    return $("#ldap-unsaved-changes button.btn-primary");
  }

  public static List<SelenideElement> getRequiredFields() {
    return Arrays.asList(hostname(), searchBase());
  }

  public static SelenideElement hostname() {
    return $("#hostname");
  }

  public static SelenideElement port() {
    return $("#port");
  }

  public static SelenideElement protocol() {
    return $("#protocol");
  }

  public static SelenideElement retryDelay() {
    return $("#retryDelay");
  }

  public static SelenideElement saslRealm() {
    return $("#saslRealm");
  }

  public static SelenideElement saveButton() {
    return $("#ldap-connection-save");
  }

  public static SelenideElement searchBase() {
    return $("#searchBase");
  }

  public static SelenideElement systemPassword() {
    return $("#systemPassword");
  }

  public static SelenideElement systemUsername() {
    return $("#systemUsername");
  }

  public static SelenideElement testConnectionButton() {
    return $("#ldap-connection-test");
  }
}
