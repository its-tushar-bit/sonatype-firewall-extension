/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.NxCheckbox;
import com.sonatype.clm.testing.functional.elements.NxRadio;
import com.sonatype.clm.testing.functional.utils.SelectorUtils;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class AddSuccessMetricsModal
    extends BasicElement<AddSuccessMetricsModal>
{
  public static final String SUBMIT_BUTTON_DISABLED_CLASS = "disabled";

  private static final String ROOT_SELECTOR = "#add-success-metrics-report";

  public static final Condition ON_LOAD_WARNING_TEXT = Condition
      .text("Data for incomplete months and weeks will skew averages. May be slow for large data sets.");

  public AddSuccessMetricsModal() {
    super(ROOT_SELECTOR);
  }

  public SelenideElement name() {
    return child("#add-success-metrics-report-name");
  }

  public SelenideElement byMostRecentWarning() {
    return child("#add-success-metrics-perf-warning");
  }

  public NxRadio includingMostRecentEvaluations() {
    return new NxRadio(child("#add-success-metrics-latest"));
  }

  public NxRadio onlyForFullCalendarWeeksAndMonths() {
    return new NxRadio(child("#add-success-metrics-monthly"));
  }

  public NxRadio allApplicationsRadioBtn() {
    return new NxRadio(child("#add-success-metrics-report-all-applications"));
  }

  public NxRadio customRadioBtn() {
    return new NxRadio(child("#add-success-metrics-report-custom"));
  }

  public SelenideElement createBtn() {
    return child(".nx-form__submit-btn");
  }

  public SelenideElement cancelBtn() {
    return child(".nx-form__cancel-btn");
  }

  public SelenideElement orgPicker() {
    return child("#add-success-metrics-report-orgs-apps-filter > div:nth-child(1)");
  }

  public SelenideElement orgPickerTrigger() {
    return orgPicker().$(".nx-tree-view__trigger");
  }

  public SelenideElement orgPickerCounter() {
    return orgPickerTrigger().$(".nx-counter");
  }

  public SelenideElement appPicker() {
    return child("#add-success-metrics-report-orgs-apps-filter > div:nth-child(2)");
  }

  public SelenideElement appPickerCounter() {
    return appPickerTrigger().$(".nx-counter");
  }

  public SelenideElement appPickerTrigger() {
    return appPicker().$(".nx-tree-view__trigger");
  }

  public NxCheckbox nthOrg(int index) {
    return new NxCheckbox(orgPicker().$(SelectorUtils.createSelector(".nx-tree-view__child", nthChild(index))));
  }

  public NxCheckbox nthApp(int index) {
    return new NxCheckbox(appPicker().$(SelectorUtils.createSelector(".nx-tree-view__child", nthChild(index))));
  }

  public SelenideElement footer() {
    return child("footer");
  }
}
