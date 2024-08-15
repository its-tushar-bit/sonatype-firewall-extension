/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.codeborne.selenide.CheckResult;
import com.codeborne.selenide.Driver;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebElementCondition;

import org.openqa.selenium.WebElement;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class ReportPolicyPage
    extends ReportPage
{
  public static class PolicyReportRow
  {
    private final SelenideElement element;

    public PolicyReportRow(SelenideElement element) {
      this.element = element;
    }

    public SelenideElement coordinates() {
      return element.find(".l1");
    }

    public boolean isWaived() {
      return !element.find("img[src='flag_white.png']").isDisplayed();
    }

    public void openCip() {
      element.find(".slick-cell.scoreCol").click();
    }
  }

  public static class PolicyReportRows
  {
    private final ElementsCollection elements;

    public PolicyReportRows(ElementsCollection elements) {
      this.elements = elements;
    }

    public PolicyReportRow row(int num) {
      return new PolicyReportRow(elements.get(num));
    }

    public void waitForRows(int count) {
      elements.shouldHave(size(count));
    }
  }

  public static SelenideElement allView() {
    return $("#policy-violation-filter li:nth-child(2) a");
  }

  public static ElementsCollection resultsWithNoScore() {
    return rows().filterBy(new WebElementCondition("resultsWithNoScore")
    {

      @Override
      public CheckResult check(Driver driver, WebElement element) {
        return $(element).find(".noScore").exists() ? CheckResult.accepted() : CheckResult.rejected("", null);
      }
    });
  }

  public static PolicyReportRow row(int num) {
    return new PolicyReportRow(rows().get(num));
  }

  public static ElementsCollection rows() {
    return $$("#componentcontainer .slick-row");
  }

  public static SelenideElement summaryView() {
    return $("#policy-violation-filter li:first-child a");
  }

  public static SelenideElement waivedView() {
    return $("#policy-violation-filter li:last-child a");
  }
}
