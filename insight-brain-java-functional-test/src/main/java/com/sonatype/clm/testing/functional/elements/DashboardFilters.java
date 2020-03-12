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
      return $("#show-save-filter-modal");
    }

    public SelenideElement deleteFilters() {
      return $("#show-delete-filters-modal");
    }

    public SelenideElement tooltip() {
      return $(".filter-label-tooltip");
    }

    public SelenideElement emptyListMessage() {
      return child(".iq-list__item--empty");
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
      return children("#manage-filter-list", ".iq-list__item");
    }

    public SelenideElement filter(int i) {
      return child("#manage-filter-list", ".iq-list__item", SelectorUtils.nthChild(i + 1));
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

  public static class DeleteFiltersDialog
      extends BasicElement<DeleteFiltersDialog>
  {
    public DeleteFiltersDialog() {
      super("#delete-filters-modal");
    }

    public SelenideElement deleteButton() {
      return child(".iq-modal-footer", ".iq-btn--primary");
    }

    public ElementsCollection filters() {
      return children(".iq-form iq-checkbox");
    }

    public IqCheckbox checkboxItem(int index) {
      return new IqCheckbox(child(".iq-form iq-checkbox", nthChild(index)));
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
      return child(".iq-modal-footer", ".iq-btn--primary");
    }

    public SelenideElement cancelButton() {
      return child(".iq-btn:not(.iq-btn--primary)[type='button']");
    }
  }

  public static class CategoryFilter
      extends NxTreeViewMultiSelect
  {
    public CategoryFilter(final String selector) {
      super(selector);
    }

    public NxCheckbox noCategory() {
      return new NxCheckbox(child(".nx-tree-view__children .nx-tree-view__child", nthChild(2)));
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
