/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.elements.InlineEditor;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class LdapConfigurationPage
{
  public static String URL = BaseUrl.uriBuilder().fragment("/ldap").build().toString();

  public static SelenideElement root() {
    return $("*[ng-show=ldap]");
  }

  public static SelenideElement connectionTab() {
    return $(".tri-pane li:first-child a");
  }

  public static SelenideElement deleteButton() {
    return $("#ldapName .btn-mini");
  }

  public static SelenideElement deleteConfirmationButton() {
    return $("#delete-ldap-confirmation button.btn-danger");
  }

  public static InlineEditor name() {
    return new InlineEditor($("#ldapName .inline-editor"));
  }

  public static SelenideElement nameCancelButton() {
    return $("#ldapName button.btn:first-child");
  }

  public static SelenideElement nameSaveButton() {
    return $("#ldapName .btn-primary");
  }

  public static SelenideElement userAndGroupSettingsTab() {
    return $(".tri-pane li:nth-child(2) a");
  }
}
