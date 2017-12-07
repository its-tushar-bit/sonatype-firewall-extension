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

import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class AdministratorsPage
    extends BasicElement<AdministratorsPage>
{
  private static final String ROOT_SELECTOR = ".iq-tile-content";

  public static String URL = BaseUrl.uriBuilder().fragment("/administrators").build().toString();

  public AdministratorsPage() {
    super(ROOT_SELECTOR);
  }

  public static AdministratorsRoleMappingList administratorsRoleMappingList() {
    return new AdministratorsRoleMappingList(ROOT_SELECTOR, ".iq-action-list.accordion");
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
      return new RoleMappingElement(selector, ".accordion-group", nthChild(num + 1));
    }

    public static class RoleMappingElement
        extends BasicElement<RoleMappingElement>
    {
      public RoleMappingElement(String... selectors) {
        super(selectors);
      }

      public Content content() {
        return new Content(selector, ".iq-action-list__row--admin-member");
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

        public SelenideElement editor() {
          return child("app-security-editor > role-membership");
        }

        public SelenideElement groupSearchWarning() {
          return child("#mixed-group-search-warning");
        }

        public SelenideElement cancelButton() {
          return child("button[ng-click=\"cancel()\"]");
        }

        public SelenideElement queryInput() {
          return child("#access-user-search-input");
        }

        public void search() {
          availableMembersList().shouldBe(visible); // ensure page is ready to process searches
          searchButton().click();
        }

        public SelenideElement searchButton() {
          return child("#user-search-button");
        }

        public ElementsCollection members() {
          return children(".member-list");
        }

        public SelenideElement availableMembersList() {
          return child(".available-list");
        }

        public ElementsCollection availableMembers() {
          return children(".available-list label");
        }

        public SelenideElement availableMember(String text) {
          return availableMembers().find(exactText(text));
        }

        public SelenideElement appliedMembersList() {
          return child(".picked-list");
        }

        public ElementsCollection appliedMembers() {
          return children(".picked-list label");
        }

        public SelenideElement appliedMember(String text) {
          return appliedMembers().find(exactText(text));
        }

        public SelenideElement unpickButton() {
          return child("double-column-picker button > .fa-arrow-left");
        }

        public SelenideElement pickButton() {
          return child("double-column-picker button > .fa-arrow-right");
        }

        public SelenideElement confirmButton() {
          return child("button.btn-primary");
        }
      }
    }
  }
}
