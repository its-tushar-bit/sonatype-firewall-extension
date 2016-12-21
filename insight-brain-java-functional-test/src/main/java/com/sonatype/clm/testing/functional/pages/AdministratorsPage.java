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

import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class AdministratorsPage
    extends BasicElement<AdministratorsPage>
{
  private static final String ROOT_SELECTOR = ".container.administrators";

  public static String URL = BaseUrl.uriBuilder().fragment("/administrators").build().toString();

  public AdministratorsPage() {
    super(ROOT_SELECTOR);
  }

  public static AdministratorsRoleMappingList administratorsRoleMappingList() {
    return new AdministratorsRoleMappingList(ROOT_SELECTOR, ".roles");
  }

  public static class AdministratorsRoleMappingList
      extends BasicElement<AdministratorsRoleMappingList>
  {
    public AdministratorsRoleMappingList(String... selector) {
      super(selector);
    }

    public ElementsCollection elements() {
      return children(".accordion-group");
    }

    public RoleMappingElement element(int num) {
      return new RoleMappingElement(selector, ".accordion-group", nthChild(num));
    }

    public static class RoleMappingElement
        extends BasicElement<RoleMappingElement>
    {
      public RoleMappingElement(String... selectors) {
        super(selectors);
      }

      public Content content() {
        return new Content(selector, ".member-content");
      }

      public SelenideElement editButton() {
        return child("button");
      }

      public static class Content
          extends BasicElement<Content>
      {
        public static final String MIXED_GROUP_SEARCH_WARNING = "One or more LDAP servers have group search " +
            "disabled, which will affect your results";

        public Content(String... selectors) {
          super(selectors);
        }

        public SelenideElement groupSearchWarning() {
          return child(".group-search-warning");
        }
      }
    }
  }
}
