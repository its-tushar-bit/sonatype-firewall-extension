/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Selenide.$;

public class RepositoryReportPage
{
  private static final String BASE_URL = "audit-report/index.html";

  public static String url(String repositoryManagerInstanceId, String repositoryPublicId)
      throws UnsupportedEncodingException
  {
    repositoryManagerInstanceId = URLEncoder.encode(repositoryManagerInstanceId, "UTF-8");
    repositoryPublicId = URLEncoder.encode(repositoryPublicId, "UTF-8");

    return BASE_URL + "?repositoryManagerInstanceId=" + repositoryManagerInstanceId + "&repositoryPublicId="
        + repositoryPublicId;
  }

  public static class Summary
  {
    public static SelenideElement root() {
      return $("#summary");
    }

    public static SelenideElement criticalCount() {
      return root().find(".pv-red");
    }

    public static SelenideElement severeCount() {
      return root().find(".pv-orange");
    }

    public static SelenideElement moderateCount() {
      return root().find(".pv-yellow");
    }

    public static SelenideElement violatingComponentsCount() {
      return root().find("#policyViolationContainer .value_sml");
    }

    public static SelenideElement noPolicyViolations() {
      return $(".pval");
    }

    public static SelenideElement identifiedCount() {
      return root().find(".header-container .value_lrg");
    }

    public static SelenideElement identifiedPercent() {
      return root().find(".header-container .value_sml");
    }

    public static SelenideElement quarantinedCount() {
      return root().find("#quarantineContainer .value_lrg .bdg");
    }
  }

  public static class Filter
  {
    public static final Condition active = cssClass("active");
  }

  public static class Table
  {
    public static final Condition criticalThreat = cssClass("criticalScore");

    public static final Condition moderateThreat = cssClass("moderateScore");

    public static final Condition severeThreat = cssClass("severeScore");

    public static final Condition noThreat = cssClass("noScore");

    public static final Condition ignoredScore = cssClass("ignoredScore");

    public static SelenideElement root() {
      // Very specific selector to avoid catching the CIP SV table
      return $("#componentTable > .slick-viewport > .grid-canvas");
    }

    public static Row row(int num) {
      return new Row(rows().get(num));
    }

    public static ElementsCollection rows() {
      return root().$$(":scope > .slick-row");
    }
  }

  public static class Row
  {
    private SelenideElement element;

    Row(SelenideElement element) {
      this.element = element;
    }

    public SelenideElement policy() {
      return element.find(".slick-cell.l0.r0 > div");
    }

    public SelenideElement component() {
      return element.find(".slick-cell.l1.r1");
    }

    public SelenideElement waived() {
      return element.find(".waived");
    }

    public SelenideElement quarantined() {
      return element.find(".icon-ban-circle");
    }
  }
}
