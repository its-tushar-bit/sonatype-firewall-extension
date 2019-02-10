/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

import static com.sonatype.clm.testing.functional.utils.SelectorUtils.createSelector;

public class EvaluateApplicationModal
    extends BasicElement<EvaluateApplicationModal>
{
  private static final String ROOT = "#evaluate-application-modal";

  public static final String SELECT_STAGE_TEXT = "-- Select Stage --";

  public EvaluateApplicationModal() {
    super(ROOT);
  }

  public SelenideElement fileInput() {
    return $("#bundle-file");
  }

  public Dropdown stageDropdown() {
    return new Dropdown(ROOT, "dropdown-selector");
  }

  public NotifyRadioButtons notifyRadioButtons() {
    return new NotifyRadioButtons(selector);
  }

  public SelenideElement uploadButton() {
    return $("#evaluate-bundle-upload");
  }

  public SelenideElement cancelButton() {
    return $("#evaluate-bundle-cancel");
  }

  public SelenideElement bundleFileName() {
    return $("#bundle-file-name");
  }

  public SelenideElement bundleAppName() {
    return $("#bundle-app-name");
  }

  public SelenideElement bundleStageName() {
    return $("#bundle-stage-name");
  }

  public SelenideElement evaluateBundleStatus() {
    return $("#evaluate-bundle-status");
  }

  public SelenideElement viewReportButton() {
    return $("#evaluate-bundle-view");
  }

  public SelenideElement closeButton() {
    return $("#evaluate-bundle-close");
  }

  public static SelenideElement disabledNotificationsMessage() {
    return $("#eval-notifications-disabled-message");
  }

  public static class NotifyRadioButtons
  {
    private final String root;

    public NotifyRadioButtons(String root) {
      this.root = root;
    }

    public IqRadio yes() {
      return new IqRadio($(createSelector(root, "iq-radio", "[value=\"'true'\"]")));
    }

    public IqRadio no() {
      return new IqRadio($(createSelector(root, "iq-radio", "[value=\"'false'\"]")));
    }
  }
}
