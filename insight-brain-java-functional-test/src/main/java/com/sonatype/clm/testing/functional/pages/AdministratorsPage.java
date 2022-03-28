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

import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class AdministratorsPage
    extends BasicElement<AdministratorsPage>
{
  private static final String OLD_ROOT_SELECTOR = ".iq-tile-content";

  private static final String ROOT_SELECTOR = ".nx-tile";

  public static String url() {
    return BaseUrl.resolvePageUrl("/administrators");
  }

  public AdministratorsPage() {
    super(OLD_ROOT_SELECTOR);
  }

  public static class AdministratorsRoleMappingList
      extends BasicElement<AdministratorsRoleMappingList>
  {
    public AdministratorsRoleMappingList(String... selector) {
      super(selector);
    }

    public ElementsCollection elements() {
      return children(".iq-list__item--expanding");
    }

    public RoleMappingElement element(int num) {
      return new RoleMappingElement(selector, ".iq-list__item--expanding", nthChild(num + 1));
    }

    public static class RoleMappingElement
        extends BasicElement<RoleMappingElement>
    {
      public RoleMappingElement(String... selectors) {
        super(selectors);
      }

      public Content content() {
        return new Content(selector, ".iq-list__row--admin-member");
      }

      public static class Content
          extends BasicElement<Content>
      {
        public Content(String... selectors) {
          super(selectors);
        }

        public SelenideElement editor() {
          return child("app-security-editor > role-membership");
        }

        public SelenideElement cancelButton() {
          return child("button[ng-click=\"cancel()\"]");
        }

        public void search() {
          availableMembersList().shouldBe(visible); // ensure page is ready to process searches
          searchButton().click();
        }

        public SelenideElement searchButton() {
          return child(".test-search-button");
        }

        public SelenideElement availableMembersList() {
          return child(".available-list");
        }
      }
    }
  }

  public static AdministratorsMappingList administratorsMappingList() {
    return new AdministratorsMappingList(ROOT_SELECTOR, "tbody");
  }

  public static class AdministratorsMappingList
      extends BasicElement<AdministratorsMappingList>
  {
    public AdministratorsMappingList(String... selector) {
      super(selector);
    }

    public ElementsCollection rows() {
      return children(".nx-table-row");
    }

    public RoleRow row(int num) {
      return new RoleRow(selector, ".nx-table-row", nthChild(num + 1));
    }

    public static class RoleRow
        extends BasicElement<RoleRow>
    {
      public RoleRow(String... selectors) {
        super(selectors);
      }

      public SelenideElement role() {
        return child(".nx-cell", nthChild(1));
      }

      public SelenideElement members() {
        return child(".nx-cell", nthChild(2));
      }

      public SelenideElement chevron() {
        return child(".fa-chevron-right");
      }
    }
  }
}
