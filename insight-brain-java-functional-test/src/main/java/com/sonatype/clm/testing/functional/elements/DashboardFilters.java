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

public class DashboardFilters
    extends BasicElement<DashboardFilters>
{
  public static final Condition ACTIVE = cssClass("nx-counter--active");

  public static final Condition NO_CHANGES_MESSAGE = text("There are no changes to update.");

  public static final Condition SELECTED_SAVED_FILTER_OPTION = cssClass("iq-manage-filters-dropdown__option--selected");

  public static NxTreeViewMultiSelect organizationFilter() {
    return new NxTreeViewMultiSelect("#org-app-filters > div:nth-child(1)");
  }

  public static NxTreeViewMultiSelect applicationFilter() {
    return new NxTreeViewMultiSelect("#org-app-filters > div:nth-child(2)");
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

  public static NxPolicyThreatLevelFilter policyThreatLevelFilter() {
    return new NxPolicyThreatLevelFilter("#threat-level-filter");
  }

  public static PolicyThreatLevelFilter iqPolicyThreatLevelFilter() {
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

  public static SelenideElement saveButton() {
    return $("#dashboard-filter-save");
  }

  public static Tooltip saveButtonTooltip() {
    return new Tooltip("#dashboard-filter-save-tooltip");
  }

  public static ManageFiltersDropdown manageFiltersDropdown() {
    return new ManageFiltersDropdown();
  }

  public static void apply() {
    applyButton().shouldNotBe(DISABLED).click();
    // wait for changes to be processed
    applyButton().shouldBe(DISABLED);
  }

  public static SaveFilterDialog saveFilterDialog() {
    return new SaveFilterDialog();
  }

  public static DeleteFilterDialog deleteFilterDialog() {
    return new DeleteFilterDialog();
  }

  public static class ManageFiltersDropdown
      extends BasicElement<ManageFiltersDropdown>
  {
    public ManageFiltersDropdown() {
      super("#dashboard-filter-header .iq-manage-filters-dropdown");
    }

    public SelenideElement selectedFilterLabel() {
      return child(".iq-manage-filters-dropdown__label");
    }

    public SelenideElement selectedFilterDirtyAsterisk() {
      return child(".iq-manage-filters-dropdown__dirty-asterisk");
    }

    public SelenideElement openMenuButton() {
      return $(".nx-dropdown__toggle");
    }

    public ManageFiltersDropdownMenu dropdownMenu() {
      return new ManageFiltersDropdownMenu(selector);
    }
  }

  public static class ManageFiltersDropdownMenu
      extends BasicElement<ManageFiltersDropdownMenu>
  {
    public ManageFiltersDropdownMenu(String selector) {
      super(selector, ".nx-dropdown-menu");
    }

    public SelenideElement emptyListMessage() {
      return child(".nx-list__item--empty");
    }

    public ElementsCollection options() {
      return children(".iq-manage-filters-dropdown__option");
    }

    public ManageFiltersDropdownOption defaultFilterOption() {
      return option(0);
    }

    public ManageFiltersDropdownOption option(int i) {
      return new ManageFiltersDropdownOption(".iq-manage-filters-dropdown__option", SelectorUtils.nthChild(i + 1));
    }
  }

  public static class ManageFiltersDropdownOption
      extends BasicElement<ManageFiltersDropdownOption>
  {
    public ManageFiltersDropdownOption(String... selectors) {
      super(selectors);
    }

    public SelenideElement selectFilterButton() {
      return child(".nx-dropdown-button--select-filter");
    }

    public SelenideElement deleteFilterButton() {
      return child(".nx-btn--delete-filter");
    }
  }

  public static class SaveFilterDialog
      extends BasicElement<SaveFilterDialog>
  {
    public SaveFilterDialog() {
      super("#save-filter-modal");
    }

    public SelenideElement header() {
      return child(".nx-modal-header");
    }

    public SelenideElement saveButton() {
      return child(".nx-modal-footer", ".nx-btn--primary");
    }

    public SelenideElement cancelButton() {
      return child("#save-filter-modal-cancel-button");
    }

    public SelenideElement nameInput() {
      return child("#filter-name-section", "input");
    }

    public SelenideElement confirmation() {
      return $("#save-filter-confirmation");
    }

    public NxRadio saveAsRadio() {
      return new NxRadio($("#dashboard-filter-save-as"));
    }

    public NxRadio overwriteRadio() {
      return new NxRadio($("#dashboard-filter-overwrite"));
    }
  }

  public static class DeleteFilterDialog
      extends BasicElement<DeleteFilterDialog>
  {
    public DeleteFilterDialog() {
      super("#delete-filter-modal");
    }

    public SelenideElement continueButton() {
      return child("#delete-filter-modal-continue-button");
    }

    public SelenideElement cancelButton() {
      return child("#delete-filter-modal-cancel-button");
    }

    public SelenideElement confirmation() {
      return child("#delete-filter-confirmation");
    }
  }

  public static class CategoryFilter
      extends NxTreeViewMultiSelect
  {
    public CategoryFilter(final String selector) {
      super(selector);
    }

    public NxCheckbox noCategory() {
      return getFilterCheckboxAt(1);
    }

    public NxCheckbox getFilterCheckboxAt(int i ) {
      return new NxCheckbox(child(".nx-tree-view__children .nx-tree-view__child", nthChild(i + 1)));
    }
  }

  public static class PolicyTypeFilter
      extends NxTreeViewMultiSelect
  {
    public PolicyTypeFilter(final String selector) {
      super(selector);
    }

    public NxCheckbox security() {
      return super.checkboxItem(2);
    }

    public NxCheckbox license() {
      return super.checkboxItem(3);
    }

    public NxCheckbox quality() {
      return super.checkboxItem(4);
    }

    public NxCheckbox other() {
      return super.checkboxItem(5);
    }
  }

  public static class AgeFilter
      extends NxTreeViewMultiSelect
  {
    public AgeFilter(final String selector) {
      super(selector);
    }

    public NxRadio past30days() {
      return super.radioItem(3);
    }

    public NxRadio past90days() {
      return super.radioItem(4);
    }
  }

  public static class StageFilter
      extends NxTreeViewMultiSelect
  {
    public StageFilter(final String selector) {
      super(selector);
    }

    public NxCheckbox build() {
      return checkboxItem(2);
    }

    public NxCheckbox stageRelase() {
      return checkboxItem(3);
    }

    public NxCheckbox release() {
      return checkboxItem(4);
    }

    public NxCheckbox operate() {
      return checkboxItem(5);
    }
  }

  public static class PolicyViolationStateFilter
      extends NxTreeViewMultiSelect
  {
    public PolicyViolationStateFilter() {
      super("#policy-violation-state-filter");
    }

    public NxCheckbox open() {
      return checkboxItem(2);
    }

    public NxCheckbox waived() {
      return checkboxItem(3);
    }

    public NxCheckbox grandfathered() {
      return checkboxItem(4);
    }
  }
}
