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
    return new PolicyThreatLevelFilter(".tree-view-group:nth-child(7)");
  }

  public static PolicyViolationStateFilter policyViolationStateFilter() {
    return new PolicyViolationStateFilter();
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

  public static SelenideElement saveFilterNameLabel() {
    return $(".dashboard-filter-name");
  }

  public static SelenideElement saveFilterDirtyAsterisk() {
    return $(".dashboard-filter-name .dashboard-filter-dirty-asterisk");
  }

  public static void toggleTwisties() {
    applicationFilter().twisty().click();
    applicationCategoryFilter().twisty().click();
    stageFilter().twisty().click();
    policyTypeFilter().twisty().click();
    policyViolationStateFilter().twisty().click();
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

    public SelenideElement deleteFilters() {
      return $("#show-delete-filters-dialog");
    }

    public SelenideElement tooltip() {
      return $(".filter-label-tooltip");
    }

    public SelenideElement emptyListMessage() {
      return child(".iq-action-list__item--empty");
    }

    public SaveFilterDialog saveFilterDialog() {
      return new SaveFilterDialog();
    }

    public DeleteFiltersDialog deleteFiltersDialog() {
      return new DeleteFiltersDialog();
    }

    public DeleteDialog deleteDialog() {
      return new DeleteDialog();
    }

    public ElementsCollection filters() {
      return children("#manage-filter-list", ".iq-action-list__item");
    }

    public SelenideElement filter(int i) {
      return child("#manage-filter-list", ".iq-action-list__item", SelectorUtils.nthChild(i + 1));
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

    public SelenideElement confirmation() {
      return $("#save-filter-confirmation");
    }

    public SelenideElement confirmContinue() {
      return $("#save-filter-confirmation .btn-primary");
    }
  }

  public static class DeleteFiltersDialog
      extends BasicElement<DeleteFiltersDialog>
  {
    public DeleteFiltersDialog() {
      super("#delete-filters-modal");
    }

    public SelenideElement deleteButton() {
      return child(".clm-modal-footer", ".btn-primary");
    }

    public ElementsCollection filters() {
      return children(".clm-form iq-checkbox");
    }

    public IqCheckbox checkboxItem(int index) {
      return new IqCheckbox(child(".clm-form iq-checkbox", nthChild(index)));
    }
  }

  public static class DeleteDialog
      extends BasicElement<DeleteDialog>
  {
    public DeleteDialog() {
      super("#delete-modal");
    }

    public SelenideElement body() {
      return child(".clm-modal-body");
    }

    public SelenideElement continueButton() {
      return child(".clm-modal-footer", ".btn-primary");
    }
    
    public SelenideElement cancelButton() {
      return child(".btn:not(.btn-primary)[type='button']");
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
      return children(".clm-form iq-checkbox");
    }

    public IqCheckbox checkboxItem(int index) {
      return new IqCheckbox(child(".clm-form iq-checkbox", nthChild(index)));
    }

    public IqCheckbox allItems() {
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

    public IqCheckbox security() {
      return super.checkboxItem(2);
    }

    public IqCheckbox license() {
      return super.checkboxItem(3);
    }

    public IqCheckbox quality() {
      return super.checkboxItem(4);
    }

    public IqCheckbox other() {
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

    public IqCheckbox build() {
      return checkboxItem(2);
    }

    public IqCheckbox stageRelase() {
      return checkboxItem(3);
    }

    public IqCheckbox release() {
      return checkboxItem(4);
    }

    public IqCheckbox operate() {
      return checkboxItem(5);
    }

  }

  public static class PolicyViolationStateFilter
      extends DashboardFilter
  {
    public PolicyViolationStateFilter() {
      super("#policy-violation-state-filter");
    }

    public IqCheckbox open() {
      return checkboxItem(2);
    }

    public IqCheckbox waived() {
      return checkboxItem(3);
    }
  }
}
