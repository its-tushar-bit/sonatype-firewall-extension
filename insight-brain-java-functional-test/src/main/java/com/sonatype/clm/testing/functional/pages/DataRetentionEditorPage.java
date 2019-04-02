/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.Dropdown;
import com.sonatype.clm.testing.functional.elements.IqRadio;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.SelenideElement;

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

  public static class ApplicationReportRetentionEditor
      extends BasicElement<ApplicationReportRetentionEditor>
  {
    private String contextId;

    public ApplicationReportRetentionEditor(String contextId) {
      super("#retention-editor-" + contextId);
      this.contextId = contextId;
    }

    public SelenideElement radioButtonGroup() {
      return child(".iq-radio-group");
    }

    public IqRadio inheritRadioButton() {
      return new IqRadio(child("#retention-editor-inherit-" + contextId));
    }

    public IqRadio disableRadioButton() {
      return new IqRadio(child("#retention-editor-disable-" + contextId));
    }

    public IqRadio customRadioButton() {
      return new IqRadio(child("#retention-editor-custom-" + contextId));
    }

    public SelenideElement customRow() {
      return child(".iq-form-row");
    }

    public SelenideElement maxAgeInput() {
      return child("input[name='" + contextId + "-age-input']");
    }

    public Dropdown maxAgeDropdown() {
      return new Dropdown("dropdown-selector[name='" + contextId + "-age-modifier']");
    }

    public SelenideElement maxCountInput() {
      return child("input[name='" + contextId + "-count-input']");
    }
  }

  public SelenideElement updateButton() {
    return child("#save-retention-button");
  }
}
