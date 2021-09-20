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

  public NxFormSelect authenticationMethod() {
    return new NxFormSelect(childSelector("#method-selector"));
  }

  public SelenideElement connectionTimeout() {
    return child("#connection");
  }

  @Override
  public List<SelenideElement> requiredFields() {
    return Arrays.asList(hostname(), searchBase());
  }

  public SelenideElement hostname() {
    return child("#hostname");
  }

  public SelenideElement port() {
    return child("#port");
  }

  public NxFormSelect protocol() {
    return new NxFormSelect(childSelector("#protocol-selector"));
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

  public NxToggle ignoreReferrals() {
    return new NxToggle(childSelector("#ignore-referrals-toggle"));
  }

  public SelenideElement systemPassword() {
    return child("#password");
  }

  public SelenideElement passwordNeedsEntryMessage() {
    return child(".nx-alert.nx-alert--error");
  }

  public SelenideElement systemUsername() {
    return child("#username");
  }

  public SelenideElement successAlertBox() {
    return child(".nx-alert--success");
  }

  public SelenideElement testConnectionButton() {
    return $("#test-connection");
  }

  @Override
  public SelenideElement saveButton() {
    return $(".nx-form__submit-btn");
  }

  @Override
  public SelenideElement cancelButton() {
    return $(".nx-form__cancel-btn");
  }
}
