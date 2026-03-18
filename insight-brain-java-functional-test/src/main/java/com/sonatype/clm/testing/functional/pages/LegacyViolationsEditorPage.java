/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;

import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.WebElementCondition;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class LegacyViolationsEditorPage
{
  public static final String ROOT = "#legacy-violation-editor";

  public static String url(Owner owner) {
    return url(owner.getType(), owner.getPublicId());
  }

  public static String url(OwnerType ownerType, String ownerId) {
    return BaseUrl.resolvePageUrl("/management/edit/{ownerType}/{ownerId}/legacyViolations", ownerType, ownerId);
  }

  public static String statusMessageText(String inheritedFromName, Boolean legacyViolations) {
    StringBuilder sb = new StringBuilder();
    if (inheritedFromName != null) {
      sb.append("Legacy violations are ");
      sb.append(Boolean.TRUE.equals(legacyViolations) ? "enabled" : "disabled");
      sb.append(" (Inheriting from ");
      sb.append(inheritedFromName);
      sb.append(")");
    }
    if (inheritedFromName == null) {
      sb.append("Legacy violations are ");
      sb.append(Boolean.TRUE.equals(legacyViolations) ? "enabled" : "disabled");
    }
    return sb.toString();
  }

  public static SelenideElement title() {
    return $("h1");
  }

  public static SelenideElement legacyViolationInherited(Boolean legacyViolations) {
    String statusMessage = "Inherit from parent (" +
        (Boolean.TRUE.equals(legacyViolations) ? "Enabled" : "Disabled") +
        ")";
    return policyRadioButton(statusMessage);
  }

  public static SelenideElement legacyViolationEnabled() {
    return policyRadioButton("Enable");
  }

  public static SelenideElement legacyViolationDisabled() {
    return policyRadioButton("Disable");
  }

  public static ElementsCollection policyRadioButttons() {
    return $$(".nx-radio-checkbox.nx-radio");
  }

  private static SelenideElement policyRadioButton(String name) {
    return $$(".nx-radio__content").findBy(text(name)).parent();
  }

  public static SelenideElement overridesCheckbox() {
    return $(".nx-radio-checkbox.nx-checkbox");
  }

  public static SelenideElement form() {
    return $(".nx-form");
  }

  public static SelenideElement updateButton() {
    return $(".nx-form__submit-btn");
  }

  public static SelenideElement disabledMessage() {
    return $("#legacy-violations-disabled-message");
  }

  public static SelenideElement unsupportedLicenseWarning() {
    return $(".nx-alert.nx-alert--error");
  }

  public static WebElementCondition unsupportedLicenseText() {
    return text("Legacy Violations are not supported by your license");
  }
}
