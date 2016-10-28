/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;


import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.utils.SelectorUtils;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class DashboardFilters extends BasicElement<DashboardFilters>
{

  public static final Condition INACTIVE = cssClass("inactive");

  public static final Condition NO_CHANGES_MESSAGE = text("There are no changes to update.");

  public static DashboardFilter applicationFilter() {
    return new DashboardFilter(".tree-view-group:nth-child(2)");
  }

  public static DashboardFilter applicationCategoryFilter() {
    return new DashboardFilter(".tree-view-group:nth-child(3)");
  }

  public static StageFilter stageFilter() {
    return new StageFilter(".tree-view-group:nth-child(4)");
  }

  public static PolicyTypeFilter policyTypeFilter() {
    return new PolicyTypeFilter(".tree-view-group:nth-child(5)");
  }

  public static PolicyThreatLevelFilter policyThreatLevelFilter() {
    return new PolicyThreatLevelFilter(".tree-view-group:nth-child(6)");
  }

  public static Button applyButton() {
    return new Button("#dashboard-filter-apply");
  }

  public static SelenideElement revertButton() {
    return $("#dashboard-filter-revert");
  }

  public static SelenideElement clearButton() {
    return $("#dashboard-filter-clear");
  }

  public static void toggleTwisties() {
    applicationFilter().twisty().click();
    applicationCategoryFilter().twisty().click();
    stageFilter().twisty().click();
    policyTypeFilter().twisty().click();
    policyThreatLevelFilter().twisty().click();
  }

  public static ManageFilters manage() {
    return new ManageFilters();
  }

  public static class ManageFilters
      extends BasicElement<ManageFilters>
  {
    public ManageFilters() {
      super("#manage-filters-dropdown");
    }

    public SelenideElement openMenuButton() {
      return $("#manage-filters-button");
    }

    public SelenideElement dropdownMenu() {
      return child(".dropdown-menu");
    }

    public SelenideElement saveFilter() {
      return $("#show-save-filter-dialog");
    }

    public SelenideElement tooltip() {
      return $(".filter-label-tooltip");
    }

    public SelenideElement emptyListMessage() {
      return child(".empty-list");
    }

    public SaveFilterDialog saveFilterDialog() {
      return new SaveFilterDialog();
    }

    public ElementsCollection filters() {
      return children(".dropdown-contents", ".filter-item");
    }

    public SelenideElement filter(int i) {
      return child(".dropdown-contents", ".filter-item", SelectorUtils.nthChild(i + 1));
    }
  }

  public static class SaveFilterDialog
      extends BasicElement<SaveFilterDialog>
  {
    public SaveFilterDialog() {
      super("#save-filter-modal");
    }

    public SelenideElement saveButton() {
      return child(".clm-modal-footer", ".btn-primary");
    }

    public SelenideElement nameInput() {
      return $("#filter-name-input");
    }
  }

  public static class DashboardFilter extends BasicElement<DashboardFilter>
  {

    public DashboardFilter(final String selector) {
      super(selector);
    }

    public SelenideElement twisty() {
      return child(".tree-view-item");
    }

    public ElementsCollection multiSelectList() {
      return children(".clm-form .tree-view-item.checkbox");
    }

    public Checkbox checkboxItem(int index) {
      return new Checkbox(child(".clm-form .tree-view-item.checkbox", nthChild(index)));
    }

    public Checkbox allItems() {
      return checkboxItem(1);
    }

    public SelenideElement counter() {
      return child(".dashboard-filter-counter");
    }

    public SelenideElement anchor() {
      return child("a");
    }

    public SelenideElement tooltip() {
      return $(".tooltip-inner");
    }
  }

  public static class PolicyTypeFilter extends DashboardFilter {

    public PolicyTypeFilter(final String selector) {
      super(selector);
    }

    public Checkbox security() {
      return super.checkboxItem(2);
    }

    public Checkbox license() {
      return super.checkboxItem(3);
    }

    public Checkbox quality() {
      return super.checkboxItem(4);
    }

    public Checkbox other() {
      return super.checkboxItem(5);
    }
  }

  public static class PolicyThreatLevelFilter extends DashboardFilter {

    public PolicyThreatLevelFilter(final String selector) {
      super(selector);
    }

    public ThreatLevelSlider slider() {
      return new ThreatLevelSlider(childSelector(".policy-threat-level-slider"));
    }
  }

  public static class StageFilter extends DashboardFilter {

    public StageFilter(final String selector) {
      super(selector);
    }

    public Checkbox build() {
      return checkboxItem(2);
    }

    public Checkbox stageRelase() {
      return checkboxItem(3);
    }

    public Checkbox release() {
      return checkboxItem(4);
    }

    public Checkbox operate() {
      return checkboxItem(5);
    }

  }
}

