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

public class AdministratorsEditPage
    extends BasicElement<AdministratorsEditPage>
{
  private static final String ROOT_SELECTOR = ".nx-page-main.iq-administrators-edit";

  public AdministratorsEditPage() {
    super(ROOT_SELECTOR);
  }

  public static String url(String roleId) {
    return BaseUrl.resolvePageUrl("/administrators/{roleId}", roleId);
  }

  public RoleDetails roleDetails() {
    return new RoleDetails(
        this.childSelector(".nx-read-only"));
  }

  public AddMembersForm addMembersForm() {
    return new AddMembersForm(
        this.childSelector("#administrators-add-members-form"));
  }

  public static class RoleDetails
      extends BasicElement<RoleDetails>
  {
    private static final String DEFINITION_ITEM_SELECTOR =
        ".nx-read-only__data";

    private RoleDetails(String selector) {
      super(selector);
    }

    private ElementsCollection getDefinitionPairs() {
      return children(DEFINITION_ITEM_SELECTOR);
    }

    public SelenideElement name() {
      ElementsCollection definitionPairs = getDefinitionPairs();
      return definitionPairs.first();
    }

    public SelenideElement description() {
      ElementsCollection definitionPairs = getDefinitionPairs();
      return definitionPairs.last();
    }
  }

  public static class AddMembersForm
      extends BasicElement<AddMembersForm>
  {
    private AddMembersForm(String selector) {
      super(selector);
    }

    public static final String DISABLED_GROUP_SEARCH_WARNING = "One or more LDAP servers have group search " +
        "disabled, which will affect your results";

    public SelenideElement removeAll() {
      return this.child(".nx-transfer-list__move-all");
    }

    public SelenideElement searchInput() {
      return this.child(".nx-search-dropdown__input .nx-text-input__input");
    }

    public ElementsCollection searchResults() {
      return this.children(".nx-search-dropdown__menu .nx-dropdown-button");
    }

    public ElementsCollection addedItems() {
      return this.children(".nx-transfer-list__item");
    }

    public SelenideElement cancelBtn() {
      return this.child(".nx-form__cancel-btn");
    }

    public SelenideElement submitBtn() {
      return this.child(".nx-form__submit-btn");
    }

    public SelenideElement groupAlert() {
      return this.child("#ldap-servers-alert");
    }

    public SelenideElement addAssociateGroupBtn() {
      return this.child("#add-associate-group-btn");
    }

    public SelenideElement addAssociateGroupInput() {
      return this.child("#add-associate-group-input");
    }

    public SelenideElement addAssociateGroupSublabel() {
      return this.child("#associate-group-form-group .nx-sub-label");
    }
  }
}
