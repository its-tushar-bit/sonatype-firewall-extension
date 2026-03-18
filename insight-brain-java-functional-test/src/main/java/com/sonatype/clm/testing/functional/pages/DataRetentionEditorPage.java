/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.NxFormSelect;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.clm.testing.functional.utils.ScrollUtil;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.exist;

public class DataRetentionEditorPage
    extends BasicElement<DataRetentionEditorPage>
{
  private static final String RETENTION_EDITOR_ID = "#retention-editor";

  public static String url(String organizationId) {
    return BaseUrl.resolvePageUrl("/management/edit/organization/{organizationId}/data-retention", organizationId);
  }

  public DataRetentionEditorPage() {
    super(RETENTION_EDITOR_ID);
  }

  public ApplicationReportRetentionEditor editor(String contextId) {
    return new ApplicationReportRetentionEditor(contextId);
  }

  public static class RetentionEditor
      extends BasicElement<RetentionEditor>
  {
    protected String contextId;

    public RetentionEditor(String contextId) {
      super("#retention-editor-" + contextId);
      this.contextId = contextId;
    }

    public SelenideElement radioButtonGroup() {
      return child("");
    }

    public SelenideElement inheritRadioButton() {
      return child("#retention-editor-inherit-" + contextId);
    }

    public SelenideElement disableRadioButton() {
      return child("#retention-editor-disable-" + contextId);
    }

    public SelenideElement customRadioButton() {
      return child("#retention-editor-custom-" + contextId);
    }

    public SelenideElement customRow() {
      return child(".custom-purge-row");
    }

    public SelenideElement ageErrorMessage() {
      return child(".custom-purge-row .nx-form-group:nth-child(2) .nx-field-validation-message");
    }

    public SelenideElement countErrorMessage() {
      return child(".custom-purge-row .nx-form-group:nth-child(5) .nx-field-validation-message");
    }

    public SelenideElement maxAgeInput() {
      return child("input[name='" + contextId + "-age-input']");
    }

    public void scrollIntoView() {
      ScrollUtil.awaitEndOfScrolling(getElement().should(exist).scrollIntoView(true));
    }
  }

  public static class ApplicationReportRetentionEditor
      extends RetentionEditor
  {
    public ApplicationReportRetentionEditor(String contextId) {
      super(contextId);
    }

    public NxFormSelect maxAgeDropdown() {
      return new NxFormSelect("[name='" + contextId + "-age-modifier']");
    }

    public SelenideElement maxCountInput() {
      return child("input[name='" + contextId + "-count-input']");
    }
  }

  public static class SuccessMetricsRetentionEditor
      extends RetentionEditor
  {
    public SuccessMetricsRetentionEditor() {
      super("success-metrics");
    }
  }

  public SelenideElement updateButton() {
    return child(".nx-form__submit-btn");
  }
}
