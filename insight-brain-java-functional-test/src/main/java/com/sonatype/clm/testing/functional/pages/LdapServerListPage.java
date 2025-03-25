/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class LdapServerListPage
    extends BasicElement<LdapServerListPage>
{
  private static final String ROOT_SELECTOR = "#ldap-server-list";

  public String url() {
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

  public SelenideElement reorderButton() {
    return $("#reorder-ldap-list-btn");
  }

  public SelenideElement saveButton() {
    return $(".nx-form__submit-btn");
  }

  public SelenideElement addButton() {
    return $("#add-ldap-server-btn");
  }

  public ElementsCollection listElements() {
    return children(".nx-list__item:not(.nx-list__item--empty)");
  }

  public SelenideElement emptyDescriptor() {
    return child(".nx-list__item--empty");
  }

  public ListRow listRow(int num) {
    return new ListRow(num);
  }

  public static class ListRow
      extends BasicElement<ListRow>
  {
    private static final String ROOT_SELECTOR = ".nx-list__item";

    ListRow(int num) {
      super(ROOT_SELECTOR + ":nth-child(" + num + ")");
    }

    public SelenideElement element() {
      return child(".nx-list__btn");
    }

    public SelenideElement chevron() {
      return child(".fa-angle-right");
    }

    public SelenideElement reorderUp() {
      return child(".fa-arrow-up").parent();
    }

    public SelenideElement reorderDown() {
      return child(".fa-arrow-down").parent();
    }
  }
}
