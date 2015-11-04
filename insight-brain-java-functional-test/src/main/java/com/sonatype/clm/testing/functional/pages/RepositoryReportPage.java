/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class RepositoryReportPage
{
  private static final String BASE_URL = "audit-report/index.html";

  public static String url(String repositoryId)
  {
    return BASE_URL + "?repositoryId=" + repositoryId;
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

    public static SelenideElement allMatchState() {
      return $("#all-matches");
    }

    public static SelenideElement exactMatchState() {
      return $("#exact-matches");
    }

    public static SelenideElement unknownMatchState() {
      return $("#unknown-matches");
    }

    public static SelenideElement summaryViolations() {
      return $("#summary-violations");
    }

    public static SelenideElement allViolations() {
      return $("#all-violations");
    }

    public static SelenideElement waivedViolations() {
      return $("#waived-violations");
    }

    public static SelenideElement quarantinedViolations() {
      return $("#quarantined-violations");
    }

    public static SelenideElement allMatchStateButton() {
      return allMatchState().find("a");
    }

    public static SelenideElement exactMatchStateButton() {
      return exactMatchState().find("a");
    }

    public static SelenideElement unknownMatchStateButton() {
      return unknownMatchState().find("a");
    }

    public static SelenideElement summaryViolationsButton() {
      return summaryViolations().find("a");
    }

    public static SelenideElement allViolationsButton() {
      return allViolations().find("a");
    }

    public static SelenideElement waivedViolationsButton() {
      return waivedViolations().find("a");
    }

    public static SelenideElement quarantinedViolationsButton() {
      return quarantinedViolations().find("a");
    }
  }

  public static class Table
  {
    public static final Condition criticalThreat = cssClass("criticalScore");

    public static final Condition moderateThreat = cssClass("moderateScore");

    public static final Condition severeThreat = cssClass("severeScore");

    public static final Condition noThreat = cssClass("noScore");

    public static final Condition ignoredScore = cssClass("ignoredScore");

    public static Row row(int num) {
      return new Row(rows().get(num));
    }

    public static ElementsCollection rows() {
      // Very specific selector to avoid catching the CIP SV table
      return $$("#componentTable > .slick-viewport > .grid-canvas > .slick-row");
    }

    public static SelenideElement cip() {
      return $("#informationPanel");
    }

    public static SelenideElement cipTab(String name) {
      return cip().$$(".nav > li a").find(text(name));
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
