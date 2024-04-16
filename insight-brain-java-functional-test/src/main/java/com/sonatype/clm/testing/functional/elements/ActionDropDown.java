/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.createSelector;

public class ActionDropDown
{
  private static final String ROOT = "#iq-owner-actions-dropdown.nx-dropdown";

  public static SelenideElement root() {
    return $(ROOT);
  }

  public static SelenideElement menu() {
    return $(createSelector(ROOT, ".nx-dropdown-menu"));
  }

  public static SelenideElement selectContact() {
    return $("#select-contact-link");
  }

  public static SelenideElement editOwner() {
    return $("#app-org-link");
  }

  public static SelenideElement viewRepositoryResults() {
    return $(".nx-dropdown-menu .nx-text-link");
  }

  public static SelenideElement moveOwner() {
    return $("#owner-move-link");
  }

  public static SelenideElement changeApplicationId() {
    return $("#change-app-id-link");
  }

  public static SelenideElement deleteOwnerButton() {
    return $("#delete-owner-link");
  }

  public static SelenideElement legacyViolation() {
    return $("#legacy-violation-link");
  }

  public static SelenideElement revokeLegacyViolation() {
    return $("#revoke-legacy-violation-link");
  }

  public static SelenideElement evaluateFile() {
    return $("#eval-file-link");
  }

  public static SelenideElement importPoliciesButton() {
    return $("#import-policies-link");
  }

  public static SelenideElement copyAppIdButton() {
    return $("#copy-app-id-link");
  }

  public static SelenideElement copyOrgIdButton() {
    return $("#copy-org-id-link");
  }

  public static SelenideElement actionButton() {
    return $(createSelector(ROOT, "button"));
  }

  public static Condition reportLinkText(String stageName) {
    stageName = stageName.equals("Stage Release") ? "stage" : stageName;
    return text("View " + stageName + " report");
  }

  public static ElementsCollection reportLinks() {
    return root().findAll("#app-report-link");
  }

  public static SelenideElement reportLink(int num) {
    return reportLinks().get(num);
  }

  public static ElementsCollection actions() {
    return menu().findAll("button.nx-dropdown-button");
  }
}
