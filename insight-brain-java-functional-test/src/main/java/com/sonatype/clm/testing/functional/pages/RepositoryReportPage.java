/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.ex.UIAssertionError;

import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.createSelector;

public class RepositoryReportPage
{
  private static final String BASE_URL = "assets/audit-report/index.html";

  public static String url(String repositoryId) {
    return BASE_URL + "?repositoryId=" + repositoryId;
  }

  public static void waitForComponentUpdater() {
    final SelenideElement updaterModal = ComponentUpdater.root();
    final long start = System.currentTimeMillis();

    boolean hasAppeared = false;

    while (System.currentTimeMillis() - start < Configuration.timeout) {
      boolean visible = updaterModal.isDisplayed();
      if (hasAppeared && !visible) {
        return;
      }
      hasAppeared = visible;

      try {
        Thread.sleep(Configuration.pollingInterval);
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      }
    }
    updaterModal.shouldNotBe(visible);
    // We probably missed it. Probably...
  }

  public static class ComponentUpdater
  {
    private static final String ROOT_SELECTOR = "#component-updater";

    public static SelenideElement root() {
      return $(ROOT_SELECTOR);
    }

    public static SelenideElement dismiss() {
      return $(ROOT_SELECTOR + " .modal-footer .btn");
    }
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
    public static final Condition ACTIVE = cssClass("active");

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
    public static final Condition CRITICAL_THREAT = cssClass("criticalScore");

    private static final String TABLE_ROW_SELECTOR = "#componentTable > .slick-viewport > .grid-canvas > .slick-row";

    public static final Condition MODERATE_THREAT = cssClass("moderateScore");

    public static final Condition SEVERE_THREAT = cssClass("severeScore");

    public static final Condition NO_THREAT = cssClass("noScore");

    public static final Condition IGNORED_SCORE = cssClass("ignoredScore");

    public static Row row(int num) {
      return new Row($(createSelector(TABLE_ROW_SELECTOR, "[row='" + num + "']")));
    }

    public static ElementsCollection rows() {
      // Very specific selector to avoid catching the CIP SV table
      return $$(TABLE_ROW_SELECTOR);
    }

    public static Row rowByName(String name) {
      return new Row(rows().findBy(text(name)));
    }

    public static SelenideElement cip() {
      return $("#informationPanel");
    }

    public static SelenideElement cipCloseButton() {
      return $("#informationPanel .close");
    }

    public static SelenideElement cipTab(String name) {
      return cip().$$(".nav > li a").find(text(name));
    }

    public static SelenideElement closeCipButton() {
      return $("#informationPanel .close");
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

    public void openCip() {
      for (long start = System.currentTimeMillis();;) {
        component().shouldBe(visible).click();
        Table.cip().shouldBe(visible);
        // A visible table row can apparently be clicked before the CIP is fully ready, causing a JS error from
        // ComponentInformationPanelPlugin.position() which in turn leaves the CIP improperly positioned & sized.
        // To avoid having later interactions stumble over this broken CIP, we try harder to open it in good state.
        if (!Table.cip().getAttribute("style").isEmpty()) {
          break;
        }
        if (System.currentTimeMillis() - start > Configuration.timeout) {
          throw UIAssertionError.wrapThrowable(new IllegalStateException("CIP not properly positioned"),
              Configuration.timeout);
        }
        Table.cipCloseButton().click();
      }
    }
  }
}
