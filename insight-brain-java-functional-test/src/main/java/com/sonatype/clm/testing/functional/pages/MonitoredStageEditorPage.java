/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.elements.IqRadio;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

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

  public static Condition unsupportedLicenseText() {
    return text("Policy monitoring is not supported by your license");
  }

  public static SelenideElement title() {
    return $(ROOT + " h2");
  }

  public static IqRadio selectedStage() {
    return new IqRadio($(ROOT + " iq-radio input:checked").parent().parent());
  }

  public static IqRadio getStageByName(final String stageName) {
    return new IqRadio($$(ROOT + " iq-radio .description").findBy(text(stageName)).parent().parent().parent());
  }

  public static SelenideElement updateButton() {
    return $(ROOT + " button[type^=submit]");
  }

  public static SelenideElement unsupportedLicenseWarning() {
    return $(ROOT + " .iq-alert");
  }
}
