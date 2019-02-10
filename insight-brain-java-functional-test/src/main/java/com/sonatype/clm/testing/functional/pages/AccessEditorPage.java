/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.elements.DoubleColumnPicker;
import com.sonatype.clm.testing.functional.elements.Dropdown;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;

public class AccessEditorPage
{
  public static final Condition NEW_TITLE_TEXT = text("Add a Role");

  public static final Condition DROPDOWN_DEFAULT_TEXT = text("-- Select Role --");

  public static final Condition CONFIRM_REMOVAL_HEADER_TEXT = text("Remove Role");

  public static final String ACCESS_EDITOR_ID = "#access-editor";

  public static final String DISABLED_GROUP_SEARCH_WARNING =
      "One or more LDAP servers have group search disabled, which will affect your results";

  public static String urlToEdit(Owner owner, String accessRoleId) {
    return urlToEdit(owner.getType(), owner.getPublicId(), accessRoleId);
  }

  public static String urlToEdit(OwnerType ownerType, String ownerId, String accessRoleId) {
    return urlToCreate(ownerType, ownerId) + "/" + accessRoleId;
  }

  public static String urlToCreate(Owner owner) {
    return urlToCreate(owner.getType(), owner.getPublicId());
  }

  public static String urlToCreate(OwnerType ownerType, String ownerId) {
    if (OwnerType.REPOSITORY_CONTAINER.equals(ownerType)) {
      return BaseUrl.resolvePageUrl("/management/edit/repositories/access");
    }
    return BaseUrl.resolvePageUrl("/management/edit/{ownerType}/{ownerId}/access", ownerType, ownerId);
  }

  public static SelenideElement title() {
    return $(ACCESS_EDITOR_ID + " h2");
  }

  public static Dropdown roleDropdown() {
    return new Dropdown(ACCESS_EDITOR_ID, "dropdown-selector");
  }

  public static DoubleColumnPicker picker() {
    return new DoubleColumnPicker();
  }

  public static SelenideElement saveButton() {
    return $("#save-access-role-button");
  }

  public static SelenideElement removeRoleButton() {
    return $("#remove-role-button");
  }

  public static SelenideElement addGroupBox() {
    return $("#access-add-group-input");
  }

  public static SelenideElement addGroupButton() {
    return $("#add-group-button");
  }

  public static SelenideElement searchBox() {
    return $("#access-user-search-input");
  }

  public static SelenideElement searchButton() {
    return $("#user-search-button");
  }
  
  public static SelenideElement disabledGroupSearchWarning() {
    return $("#disabled-group-search-warning");
  }

  public static Condition confirmRemovalThroughUpdateText(String roleName, OwnerType ownerType) {
    return text("You are about to remove the " + roleName + " role from " +
        (OwnerType.REPOSITORY_CONTAINER.equals(ownerType) ? "all repositories" :
            "this " + ownerType) +
        ". Next time, consider using the \"Remove Role\" button; it will save you some clicks!");
  }

  public static Condition confirmRemovalText(String roleName, OwnerType ownerType) {
    return text("You are about to remove the " + roleName + " role from " +
        (OwnerType.REPOSITORY_CONTAINER.equals(ownerType) ? "all repositories" :
            "this " + ownerType) + ".");
  }
}
