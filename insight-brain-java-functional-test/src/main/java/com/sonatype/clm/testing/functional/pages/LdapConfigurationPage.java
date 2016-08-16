/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.elements.LdapConnectionForm;
import com.sonatype.clm.testing.functional.elements.LdapNameEditor;
import com.sonatype.clm.testing.functional.elements.LdapUserAndGroupSettingsForm;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class LdapConfigurationPage
{
  private static final String ROOT_SELECTOR = ".container.config";

  public static String editLdapUrl(String ldapId) {
    return BaseUrl.uriBuilder().fragment("/ldap/edit/{ldapId}").build(ldapId).toString();
  }

  public static String createLdapUrl() {
    return BaseUrl.uriBuilder().fragment("/ldap/create").build().toString();
  }

  public static SelenideElement root() {
    return $(ROOT_SELECTOR);
  }

  public static SelenideElement connectionTab() {
    return $(".tri-pane li:first-child a");
  }

  public static SelenideElement userAndGroupSettingsTab() {
    return $(".tri-pane li:nth-child(2) a");
  }

  public static LdapConnectionForm ldapConnectionForm() {
    return new LdapConnectionForm(ROOT_SELECTOR, "#ldap-connection-form");
  }

  public static LdapUserAndGroupSettingsForm ldapUserAndGroupSettingsForm() {
    return new LdapUserAndGroupSettingsForm(ROOT_SELECTOR, "#user-group-mapping-form");
  }

  public static LdapNameEditor ldapNameEditor() {return new LdapNameEditor("#ldapName");}

  public static SelenideElement discardChangesModalButton() {
    return $("#ldap-unsaved-changes button.btn-primary");
  }

  public static SelenideElement deleteButton() {
    return $("#ldapName .btn-mini");
  }

  public static SelenideElement deleteConfirmationButton() {
    return $("#delete-ldap-confirmation button.btn-primary");
  }
}
