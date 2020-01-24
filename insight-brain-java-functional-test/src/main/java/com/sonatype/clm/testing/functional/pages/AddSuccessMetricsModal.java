/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.IqCheckbox;
import com.sonatype.clm.testing.functional.elements.IqRadio;
import com.sonatype.clm.testing.functional.utils.SelectorUtils;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class AddSuccessMetricsModal
    extends BasicElement<AddSuccessMetricsModal>
{
  public static final String FOOTER_ERROR_CLASS = "iq-error";

  public static final String SUBMIT_BUTTON_ERROR_CLASS = "iq-btn--error";

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

  public IqRadio includingMostRecentEvaluations() {
    return new IqRadio(child("#add-success-metrics-latest"));
  }

  public IqRadio onlyForFullCalendarWeeksAndMonths() {
    return new IqRadio(child("#add-success-metrics-monthly"));
  }

  public IqRadio allApplicationsRadioBtn() {
    return new IqRadio(child("#add-success-metrics-report-all-applications"));
  }

  public IqRadio customRadioBtn() {
    return new IqRadio(child("#add-success-metrics-report-custom"));
  }

  public SelenideElement createBtn() {
    return child("#add-success-metrics-report-submit-btn");
  }

  public SelenideElement cancelBtn() {
    return child("#add-success-metrics-report-cancel-btn");
  }

  public SelenideElement orgPicker() {
    return child("iq-org-app-picker-angular > [name=organizations]");
  }

  public SelenideElement orgPickerTrigger() {
    return orgPicker().$(".iq-tree-view__trigger");
  }

  public SelenideElement orgPickerCounter() {
    return orgPickerTrigger().$(".iq-counter");
  }

  public SelenideElement appPicker() {
    return child("iq-org-app-picker-angular > [name=applications]");
  }

  public SelenideElement appPickerCounter() {
    return appPickerTrigger().$(".iq-counter");
  }

  public SelenideElement appPickerTrigger() {
    return appPicker().$(".iq-tree-view__trigger");
  }

  public IqCheckbox nthOrg(int index) {
    return new IqCheckbox(orgPicker().$(SelectorUtils.createSelector(".iq-tree-view__child", nthChild(index))));
  }

  public IqCheckbox nthApp(int index) {
    return new IqCheckbox(appPicker().$(SelectorUtils.createSelector(".iq-tree-view__child", nthChild(index))));
  }

  public SelenideElement footer() {
    return child("footer");
  }
}
