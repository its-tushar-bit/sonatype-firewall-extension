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
import com.codeborne.selenide.WebElementCondition;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class MonitoredStageEditorPage
{
  public static final String HEADER_TEXT = "Continuous Monitoring";

  public static final String ROOT = "#continuous-monitoring-editor";

  public static String url(Owner owner) {
    return url(owner.getType(), owner.getPublicId());
  }

  public static String url(OwnerType ownerType, String ownerId) {
    return BaseUrl.resolvePageUrl("/management/edit/{ownerType}/{ownerId}/monitoring", ownerType, ownerId);
  }

  public static String inheritFromParentDoNotMonitorText(String parentsName) {
    return "Inherit from " + parentsName + " (Do not monitor)";
  }

  public static WebElementCondition unsupportedLicenseText() {
    return text("Your IQ Server license does not enable this feature.");
  }

  public static SelenideElement title() {
    return $("h1");
  }

  public static SelenideElement selectedStage() {
    return $(".nx-radio input:checked").parent();
  }

  public static SelenideElement getStageByName(final String stageName) {
    return $$(".nx-radio__content").findBy(text(stageName)).parent();
  }

  public static SelenideElement updateButton() {
    return $(".nx-form__submit-btn");
  }

  public static SelenideElement unsupportedLicenseWarning() {
    return $(".nx-alert.nx-alert--error");
  }
}
