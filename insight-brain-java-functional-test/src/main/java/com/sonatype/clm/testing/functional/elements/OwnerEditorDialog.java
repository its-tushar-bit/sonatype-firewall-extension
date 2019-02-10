/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.utils.SelectorUtils;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class OwnerEditorDialog
{
  public static SelenideElement root() {
    return $("#owner-editor");
  }

  public static SelenideElement title() {
    return root().find(".iq-modal-header h2");
  }

  public static SelenideElement name() {
    return root().find(".iq-modal-content input[name=name]");
  }

  public static SelenideElement publicId() {
    return root().find(".iq-modal-content input[name=publicId]");
  }

  public static SelenideElement saveButton() {
    return root().find(".iq-modal-footer .iq-btn--primary");
  }

  public static SelenideElement cancelButton() {
    return root().find(".iq-modal-footer button[type=button]");
  }

  public static IqRadio defaultIcon() {
    return new IqRadio($("#owner-editor-icon-default"));
  }

  public static IqRadio customIcon() {
    return new IqRadio($("#owner-editor-icon-custom"));
  }

  public static IqRadio robotIcon() {
    return new IqRadio($("#owner-editor-icon-robot"));
  }

  public static class RobotIconSelector
  {
    public static final String ROOT = "#robot-icon-selector";

    public static SelenideElement button() {
      return $(SelectorUtils.createSelector(ROOT, "button"));
    }

    public static SelenideElement icon() {
      return $(SelectorUtils.createSelector(ROOT, ".iq-owner-icon"));
    }
  }
}
