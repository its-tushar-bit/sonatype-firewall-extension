/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.elements.ErrorBox;
import com.sonatype.clm.testing.functional.elements.PillButton;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.clm.testing.functional.utils.SelectorUtils;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class OwnerSummaryPage
{
  public static String url(Owner owner) {
    return url(owner.getType(), owner.getPublicId());
  }

  public static String url(OwnerType ownerType, String id) {
    if (OwnerType.REPOSITORY_CONTAINER.equals(ownerType)) {
      return BaseUrl.uriBuilder().fragment("/management/view/repositories").build().toString();
    }

    return BaseUrl.uriBuilder().fragment("/management/view/{ownerType}/{ownerId}").build(ownerType, id).toString();
  }

  static SelenideElement scrollContainer() {
    return $(".tile-scroll-container");
  }

  public static class SummaryTile
  {
    private static final String ROOT_ID = "#owner-summary";

    private static SelenideElement root() {
      return $(ROOT_ID);
    }

    public static SelenideElement name() {
      return $(SelectorUtils.createSelector(ROOT_ID, ".iq-tile-header"));
    }

    public static SelenideElement publicId() {
      return $(SelectorUtils.createSelector(ROOT_ID, ".iq-tile-header__description"));
    }

    public static SelenideElement headerIcon() {
      return $(SelectorUtils.createSelector(ROOT_ID, ".iq-tile-header__icon", "img"));
    }

    public static SelenideElement contact() {
      return root().find(".iq-tile-header__subtitle");
    }

    public static SelenideElement icon() {
      return $("img");
    }

    public static SelenideElement addLabelButton() {
      return $("#add-label-button");
    }

    public static SelenideElement addLTGButton() {
      return $("#add-ltg-button");
    }

    public static SelenideElement addRoleButton() {
      return $("#add-role-button");
    }

    public static SelenideElement addCategoryButton() {
      return $("#add-category-button");
    }

    public static SelenideElement addPolicyButton() {
      return $("#add-policy-button");
    }

    public static SelenideElement localLabel(String labelName) {
      return $$("#owner-pill-comp-labels  ul div.title").findBy(text(labelName));
    }

    public static SelenideElement localCategory(String categoryName) {
      return $$("#owner-pill-app-categories ul div.title").findBy(text(categoryName));
    }

    public static SelenideElement localLTG(String ltgName) {
      return localLTGs().findBy(text(ltgName));
    }

    public static ElementsCollection localLTGs() {
      return $$("#owner-pill-ltgs .simple-list:first-child ul div.threat-group-title");
    }

    public static SelenideElement localPolicy(String policyName) {
      return $$("#owner-pill-policy table tr").findBy(text(policyName));
    }

    public static SelenideElement monitoredStage() {
      return $("#continuous-monitoring div");
    }

    public static SelenideElement proprietaryComponentMatchers() {
      return $("#proprietary-component-matchers div.title");
    }
    
    public static SelenideElement localAccessRole(String roleName) {
      return $$("#owner-pill-access table td.role").findBy(text(roleName));
    }

    public static ErrorBox error() {
      return new ErrorBox(ROOT_ID, ".iq-alert.iq-alert--error");
    }

    public static PillButton appCategoriesButton() {
      return new PillButton(scrollContainer(), "#owner-app-categories-button");
    }

    public static PillButton policyButton() {
      return new PillButton(scrollContainer(), "#owner-policy-button");
    }

    public static PillButton labelsButton() {
      return new PillButton(scrollContainer(), "#owner-comp-labels-button");
    }

    public static PillButton ltgsButton() {
      return new PillButton(scrollContainer(), "#owner-ltgs-button");
    }

    public static PillButton accessButton() {
      return new PillButton(scrollContainer(), "#owner-access-button");
    }
  }
}
