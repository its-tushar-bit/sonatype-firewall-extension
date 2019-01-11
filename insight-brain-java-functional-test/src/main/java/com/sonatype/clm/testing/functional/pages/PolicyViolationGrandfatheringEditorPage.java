/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.elements.IqCheckbox;
import com.sonatype.clm.testing.functional.elements.IqRadio;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class PolicyViolationGrandfatheringEditorPage
{
  public static final String HEADER_TEXT = "Policy Violation Grandfathering";

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
    return $(ROOT + " h2");
  }

  public static IqRadio grandfatheringChecked() {
    return new IqRadio($(ROOT + " iq-radio input:checked").parent().parent());
  }

  public static IqRadio grandfatheringInherited() {
    return policyRadioButton("Inherit from parent organization");
  }

  public static IqRadio grandfatheringEnabled() {
    return policyRadioButton("Enable");
  }

  public static IqRadio grandfatheringDisabled() {
    return policyRadioButton("Disable");
  }

  private static IqRadio policyRadioButton(String name) {
    return new IqRadio($$(ROOT + " iq-radio").findBy(text(name)));
  }

  public static IqCheckbox overridesCheckbox() {
    return new IqCheckbox($(ROOT + " iq-checkbox"));
  }

  public static SelenideElement updateButton() {
    return $(ROOT + " button[type^=submit]");
  }

  public static SelenideElement statusMessage() {
    return $(ROOT + " #violation-grandfathering-status-message");
  }

  public static SelenideElement disabledMessage() {
    return $(ROOT + " #violation-grandfathering-disabled-message");
  }

  public static SelenideElement unsupportedLicenseWarning() {
    return $(ROOT + " .iq-alert");
  }

  public static Condition unsupportedLicenseText() {
    return text("Policy violation grandfathering is not supported by your license");
  }
}
