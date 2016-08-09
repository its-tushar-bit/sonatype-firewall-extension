/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class LdapUserAndGroupSettingsForm
    extends BasicElement<LdapUserAndGroupSettingsForm>
{
  public LdapUserAndGroupSettingsForm(String... selectors) {
    super(selectors);
  }

  public SelenideElement userBaseDN() {
    return child("#userBaseDN");
  }

  public SelenideElement userSubtree() {
    return child("#userSubtree");
  }

  public SelenideElement userObjectClass() {
    return child("#userObjectClass");
  }

  public SelenideElement userFilter() {
    return child("#userFilter");
  }

  public SelenideElement userIDAttribute() {
    return child("#userIDAttribute");
  }

  public SelenideElement userRealNameAttribute() {
    return child("#userRealNameAttribute");
  }

  public SelenideElement userEmailAttribute() {
    return child("#userEmailAttribute");
  }

  public SelenideElement useUserPasswordAttribute() {
    return child("#useUserPasswordAttribute");
  }

  public SelenideElement userPasswordAttribute() {
    return child("#userPasswordAttribute");
  }

  public SelenideElement groupMappingType() {
    return child("#groupMappingType");
  }

  public SelenideElement groupBaseDN() {
    return child("#groupBaseDN");
  }

  public SelenideElement groupSubtree() {
    return child("#groupSubtree");
  }

  public SelenideElement groupObjectClass() {
    return child("#groupObjectClass");
  }

  public SelenideElement groupIDAttribute() {
    return child("#groupIDAttribute");
  }

  public SelenideElement groupMemberAttribute() {
    return child("#groupMemberAttribute");
  }

  public SelenideElement groupMemberFormat() {
    return child("#groupMemberFormat");
  }

  public SelenideElement userMemberOfGroupAttribute() {
    return child("#userMemberOfGroupAttribute");
  }

  public SelenideElement checkUserMapping() {
    return $("#ldap-mapping-check");
  }

  public SelenideElement checkUserLogin() {
    return $("#ldap-mapping-checklogin");
  }

  public SelenideElement cancel() {
    return $("#ldap-mapping-cancel");
  }

  public SelenideElement save() {
    return $("#ldap-mapping-save");
  }

  public SelenideElement userMappingDialog() {
    return $("div.modal-ldap");
  }

  public SelenideElement userMappingDialogClose() {
    return $("div.modal-ldap button");
  }

}
