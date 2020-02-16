/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.elements.IqBackButton;
import com.sonatype.clm.testing.functional.elements.LdapConnectionForm;
import com.sonatype.clm.testing.functional.elements.LdapNameEditor;
import com.sonatype.clm.testing.functional.elements.LdapUserAndGroupSettingsForm;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class LdapConfigurationPage
{
  private static final String ROOT_SELECTOR = "#ldap-configuration-editor";

  public static String urlToEdit(String ldapId) {
    return BaseUrl.resolvePageUrl("/ldap/edit/{ldapId}", ldapId);
  }

  public static String urlToCreate() {
    return BaseUrl.resolvePageUrl("/ldap/create");
  }

  public static SelenideElement root() {
    return $(ROOT_SELECTOR);
  }

  public static IqBackButton backButton() {
    return new IqBackButton(ROOT_SELECTOR);
  }

  public static SelenideElement connectionTab() {
    return $("#tab-connection");
  }

  public static SelenideElement userAndGroupSettingsTab() {
    return $("#tab-user");
  }

  public static LdapConnectionForm ldapConnectionForm() {
    return new LdapConnectionForm(ROOT_SELECTOR, "#ldap-connection-form");
  }

  public static LdapUserAndGroupSettingsForm ldapUserAndGroupSettingsForm() {
    return new LdapUserAndGroupSettingsForm(ROOT_SELECTOR, "#user-group-mapping-form");
  }

  public static LdapNameEditor ldapNameEditor() {
    return new LdapNameEditor("#ldap-name");
  }

  public static SelenideElement discardChangesModalButton() {
    return $("#ldap-unsaved-changes .iq-btn--primary");
  }

  public static SelenideElement deleteButton() {
    return $("#ldap-name #ldap-connection-delete");
  }

  public static SelenideElement deleteConfirmationButton() {
    return $("#delete-ldap-confirmation .btn-primary");
  }
}
