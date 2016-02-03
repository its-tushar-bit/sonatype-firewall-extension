/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class EvaluateApplicationModal
{
  public static SelenideElement root() {
    return $("#evaluate-application-modal");
  }

  public static Condition defaultStageText() {
    return Condition.text("-- Select CLM Stage --");
  }

  public static SelenideElement fileInput() {
    return root().$("#bundleFile");
  }

  public static DropdownSelector stageDropdown() {
    return new DropdownSelector(root().$("dropdown-selector"));
  }

  public static NotifyRadioButtons notifyRadioButtons() {
    return new NotifyRadioButtons(root());
  }

  public static SelenideElement uploadButton() {
    return root().$("#evaluate-bundle-upload");
  }

  public static SelenideElement cancelButton() {
    return root().$("#evaluate-bundle-cancel");
  }

  public static SelenideElement bundleFileName() {
    return root().$("#bundle-file-name");
  }

  public static SelenideElement bundleAppName() {
    return root().$("#bundle-app-name");
  }

  public static SelenideElement bundleStageName() {
    return root().$("#bundle-stage-name");
  }

  public static SelenideElement evaluateBundleStatus() {
    return root().$("#evaluate-bundle-status");
  }

  public static SelenideElement viewReportButton() {
    return root().$("#evaluate-bundle-view");
  }

  public static SelenideElement closeButton() {
    return root().$("#evaluate-bundle-close");
  }

  public static class NotifyRadioButtons
  {
    private SelenideElement root;

    public NotifyRadioButtons(SelenideElement root) {
      this.root = root;
    }

    public SelenideElement yes() {
      return root.$$("input[type='radio']").get(0);
    }

    public SelenideElement no() {
      return root.$$("input[type='radio']").get(1);
    }
  }
}
