/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.ElementsCollection;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class PolicyViolationGrandfatheringEditorPage
{
  public static final String ROOT = "#violation-grandfathering-editor";

  public static String url(Owner owner) {
    return url(owner.getType(), owner.getPublicId());
  }

  public static String url(OwnerType ownerType, String ownerId) {
    return BaseUrl.resolvePageUrl("/management/edit/{ownerType}/{ownerId}/grandfathering", ownerType, ownerId);
  }

  public static String statusMessageText(String inheritedFromName, Boolean grandfathering) {
    StringBuilder sb = new StringBuilder();
    if (inheritedFromName != null) {
      sb.append("Inherit from ");
      sb.append(inheritedFromName);
      sb.append(" (");
    }
    sb.append("Grandfathering is ");
    sb.append(Boolean.TRUE.equals(grandfathering) ? "enabled" : "disabled");
    if (inheritedFromName != null) {
      sb.append(")");
    }
    return sb.toString();
  }

  public static SelenideElement title() {
    return $("h1");
  }

  public static SelenideElement grandfatheringInherited() {
    return policyRadioButton("Inherit from parent organization");
  }

  public static SelenideElement grandfatheringEnabled() {
    return policyRadioButton("Enable");
  }

  public static SelenideElement grandfatheringDisabled() {
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

  public static SelenideElement statusMessage() {
    return $(".nx-read-only");
  }

  public static SelenideElement disabledMessage() {
    return $("#violation-grandfathering-disabled-message");
  }

  public static SelenideElement unsupportedLicenseWarning() {
    return $(".nx-alert.nx-alert--error");
  }

  public static Condition unsupportedLicenseText() {
    return text("Policy violation grandfathering is not supported by your license");
  }
}
