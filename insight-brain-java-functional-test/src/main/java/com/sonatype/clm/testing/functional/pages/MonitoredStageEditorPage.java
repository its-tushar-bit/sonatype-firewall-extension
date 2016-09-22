/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.elements.Radio;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class MonitoredStageEditorPage
{
  public static final String HEADER_TEXT = "Continuous Monitoring";

  public static final String ROOT = "#continuous-monitoring-editor";

  public static String url(String ownerType, String ownerId) {
    return BaseUrl.uriBuilder().fragment("/management/edit/{ownerType}/{ownerId}/monitoring").build(ownerType, ownerId)
        .toString();
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

  public static Radio selectedStage() {
    return new Radio($(ROOT + " input:checked").parent());
  }

  public static Radio getStageByName(final String stageName) {
    return new Radio($$(ROOT + " label.radio span p").findBy(text(stageName)).parent().parent());
  }

  public static SelenideElement updateButton() {
    return $(ROOT + " button[type^=submit]");
  }

  public static SelenideElement unsupportedLicenseWarning() {
    return $(ROOT + " .alert");
  }
}
