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
import static com.sonatype.clm.testing.functional.elements.CLM.DISABLED;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class DashboardFilters extends BasicElement<DashboardFilters>
{

  public static final Condition ACTIVE = cssClass("iq-counter--active");

  public static final Condition NO_CHANGES_MESSAGE = text("There are no changes to update.");

  public static DashboardFilter organizationFilter() {
    return new DashboardFilter("#org-app-filters iq-tree-view-multi-select:nth-child(1)");
  }

  public static DashboardFilter applicationFilter() {
    return new DashboardFilter("#org-app-filters iq-tree-view-multi-select:nth-child(2)");
  }

  public static CategoryFilter applicationCategoryFilter() {
    return new CategoryFilter("#category-filter");
  }

  public static StageFilter stageFilter() {
    return new StageFilter("#stage-filter");
  }

  public static PolicyTypeFilter policyTypeFilter() {
    return new PolicyTypeFilter("#policy-type-filter");
  }

  public static AgeFilter ageFilter() {
    return new AgeFilter("#age-filter");
  }

  public static PolicyThreatLevelFilter policyThreatLevelFilter() {
    return new PolicyThreatLevelFilter("#threat-level-filter");
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

  public static void apply() {
    applyButton().shouldNotBe(DISABLED).click();
    // wait for changes to be processed
    applyButton().shouldBe(DISABLED);
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

    public SelenideElement header() {
      return child(".iq-modal-header");
    }

    public SelenideElement saveButton() {
      return child(".iq-modal-footer", ".btn-primary");
    }

    public SelenideElement cancelButton() {
      return child("#dismissBtn");
    }

    public SelenideElement nameInput() {
      return $("#filter-name-input");
    }

    public SelenideElement confirmation() {
      return $("#save-filter-confirmation");
    }

    public IqRadio saveAsRadio() {
      return new IqRadio($("#dashboard-filter-save-as"));
    }

    public IqRadio overwriteRadio() {
      return new IqRadio($("#dashboard-filter-overwrite"));
    }
  }

  public static class DeleteFiltersDialog
      extends BasicElement<DeleteFiltersDialog>
  {
    public DeleteFiltersDialog() {
      super("#delete-filters-modal");
    }

    public SelenideElement deleteButton() {
      return child(".iq-modal-footer", ".btn-primary");
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
      return child(".iq-modal-content");
    }

    public SelenideElement continueButton() {
      return child(".iq-modal-footer", ".btn-primary");
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
      return child(".iq-tree-view__trigger");
    }

    public ElementsCollection multiSelectList() {
      return children(".iq-tree-view__child iq-checkbox");
    }

    public ElementsCollection singleSelectList() {
      return children("iq-radio.iq-tree-view__child");
    }

    public IqCheckbox checkboxItem(int index) {
      return new IqCheckbox(child(".iq-tree-view__children .iq-tree-view__child", nthChild(index), "iq-checkbox"));
    }

    public IqRadio radioItem(int index) {
      return new IqRadio(child(".iq-tree-view__children iq-radio.iq-tree-view__child", nthChild(index)));
    }

    public IqCheckbox allItems() {
      return checkboxItem(1);
    }

    public SelenideElement counter() {
      return child(".iq-counter");
    }

    public SelenideElement anchor() {
      return child("a");
    }

    public SelenideElement tooltip() {
      return $(".tooltip-inner");
    }
  }

  public static class CategoryFilter
      extends DashboardFilter
  {
    public CategoryFilter(final String selector) {
      super(selector);
    }

    public IqCheckbox noCategory() {
      return new IqCheckbox(child(".iq-tree-view__children .iq-tree-view__child:last-child iq-checkbox"));
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

  public static class AgeFilter
      extends DashboardFilter
  {

    public AgeFilter(final String selector) {
      super(selector);
    }

    public IqRadio past30days() {
      return super.radioItem(3);
    }

    public IqRadio past90days() {
      return super.radioItem(4);
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
