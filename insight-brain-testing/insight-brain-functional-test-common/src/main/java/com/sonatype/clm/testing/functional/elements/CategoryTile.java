/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.utils.SelectorUtils;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebElementCondition;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;

public class CategoryTile
    extends OwnerTile
{
  public static WebElementCondition inheritedText(String parent) {
    return Condition.text("inherited from " + parent);
  }

  public static WebElementCondition noneDefinedText() {
    return Condition.text("No application categories defined");
  }

  public static WebElementCondition noneAssignedText() {
    return Condition.text("No application categories assigned");
  }

  public static WebElementCondition subHeaderText(Application application) {
    return Condition.text("assigned to " + application.getName());
  }

  public static WebElementCondition subHeaderText(Organization organization) {
    return Condition.text("available to apps in " + organization.getName());
  }

  public static WebElementCondition buttonText(@SuppressWarnings("unused") Application application) {
    return Condition.text("assign a category");
  }

  public static WebElementCondition buttonText(@SuppressWarnings("unused") Organization organization) {
    return Condition.text("add a category");
  }

  public CategoryTile() {
    super("#owner-pill-app-categories");
  }

  public SelenideElement addCategoryButton() {
    return $("#add-category-button");
  }

  public ElementsCollection categoryLists() {
    return children(".nx-list");
  }

  public ElementsCollection categoryListsSubheaders() {
    return children(".nx-h3");
  }

  public NxList categoryList(int num) {
    return new NxList(categoryLists().get(num));
  }

  public SelenideElement categoryListSubheader(int num) {
    return this.categoryListsSubheaders().get(num);
  }

  public SelenideElement localCategory(String categoryName) {
    return children("ul .nx-list__item").findBy(text(categoryName));
  }

  public SelenideElement localCategoryLink(String categoryName) {
    return children("ul .nx-list__link").findBy(text(categoryName));
  }

  public ElementsCollection inheritedCategoriesLists() {
    return children(".nx-collapsible-items__children dl");
  }

  public InheritedCategoriesList inheritedCategoriesList(String ownerId) {
    return new InheritedCategoriesList("#application-categories-for-" + ownerId);
  }

  public static class InheritedCategoriesList
      extends BasicElement<InheritedCategoriesList>
  {
    public InheritedCategoriesList(String... selectors) {
      super(selectors);
    }

    public ElementsCollection elements() {
      return children(".categories-element");
    }

    public InheritedCategory element(int num) {
      return new InheritedCategory(selector, ".categories-element", SelectorUtils.nthChild(num + 1));
    }
  }

  public static class InheritedCategory
      extends BasicElement<InheritedCategory>
  {
    public InheritedCategory(String... selectors) {
      super(selectors);
    }

    public SelenideElement icon() {
      return child(".nx-icon");
    }

    public SelenideElement label() {
      return child(".categories-label");
    }

    public SelenideElement description() {
      return child(".categories-description");
    }
  }
}
