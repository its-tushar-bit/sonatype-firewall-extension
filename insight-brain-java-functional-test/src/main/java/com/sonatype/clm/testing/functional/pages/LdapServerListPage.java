/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.ActionList;
import com.sonatype.clm.testing.functional.ldap.ReorderLdapModal;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.visible;

public class LdapServerListPage 
    extends BasicElement<LdapServerListPage>
{
  private static final String ROOT_SELECTOR = "#ldap-server-list";

  public static String url() {
    return BaseUrl.resolvePageUrl("/ldap-servers");
  }

  public LdapServerListPage() {
    super(ROOT_SELECTOR);
  }

  public SelenideElement header() {
    return child("h2");
  }

  public SelenideElement subHeader() {
    return child(".iq-tile-header__sub-title");
  }

  public SelenideElement newServerButton() {
    return child("button:last-child");
  }

  public ReorderLdapModal openModalWithAssert() {
    reorderButton().shouldBe(visible).click();

    ReorderLdapModal modal = new ReorderLdapModal();
    modal.shouldBe(visible);

    return modal;
  }

  public SelenideElement reorderButton() {
    return child("button:first-child");
  }

  public ActionList ldapServerList() {
    return new ActionList(childSelector(".iq-list"));
  }
}
