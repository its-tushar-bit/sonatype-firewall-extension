/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.TileSimpleList;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.SelenideElement;

public class LdapServerListPage 
    extends BasicElement<LdapServerListPage>
{
  private static final String ROOT_SELECTOR = ".ldap-server-list";

  public static String URL = BaseUrl.uriBuilder().fragment("/ldap-servers").build().toString();

  public LdapServerListPage() {
    super(ROOT_SELECTOR);
  }

  public SelenideElement header() {
    return child("h2");
  }

  public SelenideElement subHeader() {
    return child(".sub-header");
  }

  public SelenideElement newServerButton() {
    return child("button");
  }

  public TileSimpleList ldapServerList() {
    return new TileSimpleList(child(".simple-list"));
  }

}
