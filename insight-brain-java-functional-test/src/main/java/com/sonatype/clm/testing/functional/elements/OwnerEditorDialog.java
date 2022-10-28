/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.utils.SelectorUtils;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class OwnerEditorDialog
{
  public static String INVALID_NAME_MESSAGE = "Use valid characters: alphanumeric, \"_\", \".\", \"-\", or spaces";

  public static String INVALID_PUBLICID_MESSAGE = "Use valid characters: alphanumeric, \"_\", \".\" or \"-\"\n";

  public static SelenideElement root() {
    return $("#owner-editor");
  }

  public static SelenideElement title() {
    return root().find("h2");
  }

  public static SelenideElement nameDiv() {
    return root().find("#editor-owner-name .nx-text-input");
  }

  public static SelenideElement name() {
    return root().find("#editor-owner-name .nx-text-input .nx-text-input__input");
  }

  public static SelenideElement nameInvalidMessage() {
    return root().find("#editor-owner-name > .nx-text-input > .nx-text-input__invalid-message");
  }

  public static SelenideElement publicIdDiv() {
    return root().find("#editor-new-id .nx-text-input");
  }

  public static SelenideElement publicId() {
    return root().find("#editor-new-id .nx-text-input .nx-text-input__input");
  }

  public static SelenideElement publicIdInvalidMessage() {
    return root().find("#editor-new-id > .nx-text-input > .nx-text-input__invalid-message");
  }

  public static SelenideElement saveButton() {
    return root().find(".nx-form__submit-btn");
  }

  public static SelenideElement cancelButton() {
    return root().find(".nx-form__cancel-btn");
  }

  public static SelenideElement defaultIcon() {
    return iconRadioButton("Use a default icon");
  }

  public static SelenideElement customIcon() {
    return iconRadioButton("Upload a custom icon");
  }

  public static SelenideElement robotIcon() {
    return iconRadioButton("Get a robot");
  }

  private static SelenideElement iconRadioButton(String name) {
    return $$(".nx-radio__content").findBy(text(name)).parent();
  }

  public static class RobotIconSelector
  {
    public static final String ROOT = "#robot-icon-selector";

    public static SelenideElement button() {
      return $(SelectorUtils.createSelector(ROOT, ".nx-btn"));
    }

    public static SelenideElement icon() {
      return $(SelectorUtils.createSelector(ROOT, ".iq-owner-icon"));
    }
  }
}
