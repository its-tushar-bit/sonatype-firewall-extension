/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import java.time.Duration;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.utils.SelectorUtils;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebElementCondition;

import static com.codeborne.selenide.Condition.clickable;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.sonatype.clm.testing.functional.elements.CLM.DISABLED;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class DashboardFilters
    extends BasicElement<DashboardFilters>
{
  public static final WebElementCondition ACTIVE = cssClass("nx-counter--active");

  public static final WebElementCondition NO_CHANGES_MESSAGE = text("There are no changes to update.");

  public static final WebElementCondition SELECTED_SAVED_FILTER_OPTION =
      cssClass("iq-manage-filters-dropdown__option--selected");

  public static SelenideElement filterContainer() {
    return $("#dashboard-filter-container");
  }

  public static SelenideElement modalBackdrop() {
    return $(".nx-modal-backdrop");
  }

  public static SelenideElement closeButton() {
    return $(".nx-drawer-header__close-button");
  }

  public static void closeFilter() {
    // make sure the close button is clickable, sometimes it is briefly not
    DashboardFilters.closeButton().shouldBe(clickable);
    DashboardFilters.closeButton().click();

    // Make sure it's closed before continuing as the filter can obscure other elements
    // Increasing timeout because I have seen failures here running on local due to simply not waiting quite long enough
    DashboardFilters.filterContainer().shouldNotBe(visible, Duration.ofSeconds(10));
  }

  public static Tooltip closeButtonTooltip() {
    return new Tooltip("#dashboard-filter-close-btn-tooltip");
  }

  public static NxTreeViewMultiSelect organizationFilter() {
    return new NxTreeViewMultiSelect("#org-app-filters > div:nth-child(1)");
  }

  public static NxTreeViewMultiSelect applicationFilter() {
    return new NxTreeViewMultiSelect("#org-app-filters > div:nth-child(2)");
  }

  public static NxTreeViewMultiSelect repositoryFilter() {
    return new NxTreeViewMultiSelect("#repositories-filter");
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

  public static ExpirationDateFilter expirationDateFilter() {
    return new ExpirationDateFilter("#expiration-date-filter");
  }

  public static NxPolicyThreatLevelFilter policyThreatLevelFilter() {
    return new NxPolicyThreatLevelFilter("#threat-level-filter");
  }

  public static PolicyThreatLevelFilter iqPolicyThreatLevelFilter() {
    return new PolicyThreatLevelFilter("#threat-level-filter");
  }

  public static DashboardReasonsFilter iqPolicyWaiverReasonFilter() {
    return new DashboardReasonsFilter("#policy-waiver-reason-filter");
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
      super(".nx-drawer-header .iq-manage-filters-dropdown");
    }

    public SelenideElement selectedFilterLabel() {
      return child(".iq-manage-filters-dropdown__label");
    }

    public SelenideElement selectedFilterDirtyAsterisk() {
      return child(".iq-manage-filters-dropdown__dirty-asterisk");
    }

    public SelenideElement openMenuButton() {
      return child(".nx-dropdown__toggle");
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
      return child(".nx-footer", ".nx-btn--primary");
    }

    public SelenideElement cancelButton() {
      return child(".nx-form__cancel-btn");
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
      return child(".nx-form__submit-btn");
    }

    public SelenideElement cancelButton() {
      return child(".nx-form__cancel-btn");
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

    public NxCheckbox getFilterCheckboxAt(int i) {
      return new NxCheckbox(child(".nx-collapsible-items__children .nx-collapsible-items__child", nthChild(i + 1)));
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

  public static class ExpirationDateFilter
      extends NxTreeViewMultiSelect
  {
    public ExpirationDateFilter(final String selector) {
      super(selector);
    }

    public NxRadio all() {
      return super.radioItem(1);
    }

    public NxRadio in24hours() {
      return super.radioItem(2);
    }

    public NxRadio in7days() {
      return super.radioItem(3);
    }

    public NxRadio in30days() {
      return super.radioItem(4);
    }

    public NxRadio in90days() {
      return super.radioItem(5);
    }

    public NxRadio inOver90days() {
      return super.radioItem(6);
    }

    public NxRadio never() {
      return super.radioItem(7);
    }
  }

  public static class StageFilter
      extends NxTreeViewMultiSelect
  {
    public StageFilter(final String selector) {
      super(selector);
    }

    public NxCheckbox source() {
      return checkboxItem(2);
    }

    public NxCheckbox build() {
      return checkboxItem(3);
    }

    public NxCheckbox stageRelase() {
      return checkboxItem(4);
    }

    public NxCheckbox release() {
      return checkboxItem(5);
    }

    public NxCheckbox operate() {
      return checkboxItem(6);
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

    public NxCheckbox legacyViolation() {
      return checkboxItem(4);
    }
  }
}
