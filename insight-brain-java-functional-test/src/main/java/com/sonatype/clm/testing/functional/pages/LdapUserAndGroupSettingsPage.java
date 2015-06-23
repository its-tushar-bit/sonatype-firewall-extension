/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class LdapUserAndGroupSettingsPage
    extends LdapConfigurationPage
{
  public static SelenideElement userBaseDN() {
    return $("#userBaseDN");
  }

  public static SelenideElement userSubtree() {
    return $("#userSubtree");
  }

  public static SelenideElement userObjectClass() {
    return $("#userObjectClass");
  }

  public static SelenideElement userFilter() {
    return $("#userFilter");
  }

  public static SelenideElement userIDAttribute() {
    return $("#userIDAttribute");
  }

  public static SelenideElement userRealNameAttribute() {
    return $("#userRealNameAttribute");
  }

  public static SelenideElement userEmailAttribute() {
    return $("#userEmailAttribute");
  }

  public static SelenideElement useUserPasswordAttribute() {
    return $("#useUserPasswordAttribute");
  }

  public static SelenideElement userPasswordAttribute() {
    return $("#userPasswordAttribute");
  }

  public static SelenideElement groupMappingType() {
    return $("#groupMappingType");
  }

  public static SelenideElement groupBaseDN() {
    return $("#groupBaseDN");
  }

  public static SelenideElement groupSubtree() {
    return $("#groupSubtree");
  }

  public static SelenideElement groupObjectClass() {
    return $("#groupObjectClass");
  }

  public static SelenideElement groupIDAttribute() {
    return $("#groupIDAttribute");
  }

  public static SelenideElement groupMemberAttribute() {
    return $("#groupMemberAttribute");
  }

  public static SelenideElement groupMemberFormat() {
    return $("#groupMemberFormat");
  }

  public static SelenideElement userMemberOfGroupAttribute() {
    return $("#userMemberOfGroupAttribute");
  }

  public static SelenideElement checkUserMapping() {
    return $("#ldap-mapping-check");
  }

  public static SelenideElement checkUserLogin() {
    return $("#ldap-mapping-checklogin");
  }

  public static SelenideElement cancel() {
    return $("#ldap-mapping-cancel");
  }

  public static SelenideElement save() {
    return $("#ldap-mapping-save");
  }

  public static SelenideElement userMappingDialog() {
    return $("div.modal-ldap");
  }

  public static SelenideElement userMappingDialogClose() {
    return $("div.modal-ldap button");
  }

}
