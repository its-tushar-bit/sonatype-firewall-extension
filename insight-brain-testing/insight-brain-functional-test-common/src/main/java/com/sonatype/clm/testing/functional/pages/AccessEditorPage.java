/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.NxDeleteModal;
import com.sonatype.clm.testing.functional.elements.NxFormSelect;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebElementCondition;

import static com.codeborne.selenide.Condition.text;

public class AccessEditorPage
    extends BasicElement<AccessEditorPage>
{
  private static final String ROOT_SELECTOR = "#create-edit-access-page";

  public AccessEditorPage() {
    super(ROOT_SELECTOR);
  }

  public static final WebElementCondition NEW_TITLE_TEXT = text("New Role");

  public static final WebElementCondition DROPDOWN_DEFAULT_TEXT = text("Select Role");

  public static final WebElementCondition CONFIRM_REMOVAL_HEADER_TEXT = text("Delete Role");

  public static final String DISABLED_GROUP_SEARCH_WARNING =
      "One or more LDAP servers have group search disabled, which will affect your results";

  public static String urlToEdit(Owner owner, String accessRoleId) {
    String ownerId = owner.getType().equals(OwnerType.REPOSITORY) ? owner.getId() : owner.getPublicId();
    return urlToEdit(owner.getType(), ownerId, accessRoleId);
  }

  public static String urlToEdit(OwnerType ownerType, String ownerId, String accessRoleId) {
    return urlToCreate(ownerType, ownerId) + "/" + accessRoleId;
  }

  public static String urlToCreate(Owner owner) {
    String ownerId = owner.getType().equals(OwnerType.REPOSITORY) ? owner.getId() : owner.getPublicId();
    return urlToCreate(owner.getType(), ownerId);
  }

  public static String urlToCreate(OwnerType ownerType, String ownerId) {
    return BaseUrl.resolvePageUrl("/management/edit/{ownerType}/{ownerId}/access", ownerType, ownerId);
  }

  public SelenideElement title() {
    return child(".nx-page-title");
  }

  public AddMembersForm addMembersForm() {
    return new AddMembersForm(
        childSelector("#access-add-members-form"));
  }

  public class AddMembersForm
      extends BasicElement<AddMembersForm>
  {
    private AddMembersForm(String selector) {
      super(selector);
    }

    public NxFormSelect roleSelect() {
      return new NxFormSelect(childSelector("select.nx-form-select__select"));
    }

    public SelenideElement addGroupBox() {
      return child(".nx-form-row .nx-text-input__input");
    }

    public SelenideElement addGroupButton() {
      return child("#add-associate-group-btn");
    }

    public SelenideElement addGroupInput() {
      return this.child("#add-associate-group-input");
    }

    public SelenideElement addGroupSublabel() {
      return this.child("#associate-group-form-group .nx-sub-label");
    }

    public SelenideElement searchBox() {
      return child(".nx-search-dropdown .nx-text-input__input");
    }

    public ElementsCollection searchResults() {
      return children(".nx-search-dropdown__menu .nx-dropdown-button");
    }

    public ElementsCollection addedItems() {
      return children(".nx-transfer-list__item");
    }

    public SelenideElement deleteRoleButton() {
      return child("#delete-role-button");
    }

    public SelenideElement saveButton() {
      return child(".nx-form__submit-btn");
    }

    public NxDeleteModal getDeleteModal() {
      return new NxDeleteModal("#role-config-delete-modal");
    }

    public SelenideElement disabledGroupSearchWarning() {
      return child("#ldap-servers-alert");
    }

    public String confirmRemovalThroughUpdateText(String roleName, OwnerType ownerType) {
      return "You are about to remove the " + roleName + " role from " + getReadableOwnerType(ownerType) +
          ". Next time, consider using the \"Delete\" button; it will save you some clicks!";
    }

    public String confirmRemovalText(String roleName, OwnerType ownerType) {
      return "You are about to remove the " + roleName + " role from " + getReadableOwnerType(ownerType) + ".";
    }

    private String getReadableOwnerType(OwnerType ownerType) {
      if (OwnerType.REPOSITORY_CONTAINER.equals(ownerType)) {
        return "all repository managers";
      }
      if (OwnerType.REPOSITORY_MANAGER.equals(ownerType)) {
        return "this repository manager";
      }

      return "this " + ownerType;
    }
  }
}
