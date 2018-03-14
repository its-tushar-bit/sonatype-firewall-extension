/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.ex.UIAssertionError;

import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class RepositoryReportPage
{
  public static String url(String repositoryId) {
    return BaseUrl.rootUriBuilder().path("assets/audit-report/index.html").queryParam("repositoryId", repositoryId)
        .build().toString();
  }

  public static void waitForComponentUpdater() {
    final SelenideElement updaterModal = componentUpdater().getElement();
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
    updaterModal.shouldBe(hidden);
    // We probably missed it. Probably...
  }

  public static ComponentUpdater componentUpdater() {
    return new ComponentUpdater();
  }

  public static Summary summary() {
    return new Summary();
  }

  public static Filter filter() {
    return new Filter();
  }

  public static Table table() {
    return new Table();
  }

  public static class ComponentUpdater
      extends BasicElement<ComponentUpdater>
  {
    public ComponentUpdater() {
      super("#component-updater");
    }

    public SelenideElement dismissButton() {
      return child(".modal-footer .btn");
    }
  }

  public static class Summary
      extends BasicElement<Summary>
  {
    public Summary() {
      super("#summary");
    }

    public SelenideElement criticalCount() {
      return child(".pv-red");
    }

    public SelenideElement severeCount() {
      return child(".pv-orange");
    }

    public SelenideElement moderateCount() {
      return child(".pv-yellow");
    }

    public SelenideElement violatingComponentsCount() {
      return child("#policyViolationContainer .value_sml");
    }

    public SelenideElement noPolicyViolations() {
      return $(".pval");
    }

    public SelenideElement identifiedCount() {
      return child(".header-container .value_lrg");
    }

    public SelenideElement identifiedPercent() {
      return child(".header-container .value_sml");
    }

    public SelenideElement quarantinedCount() {
      return child("#quarantineContainer .value_lrg .bdg");
    }
  }

  public static class Filter
      extends BasicElement<Filter>
  {
    public static class Button
        extends BasicElement<Button>
    {
      public Button(String selector) {
        super(selector);
      }

      @Override
      public Button click() {
        child("a").click();
        return me();
      }
    }

    public static final Condition ACTIVE = cssClass("active");

    public Filter() {
      super("div[repository-violation-table-filter]");
    }

    public Button allMatchState() {
      return new Button("#all-matches");
    }

    public Button exactMatchState() {
      return new Button("#exact-matches");
    }

    public Button unknownMatchState() {
      return new Button("#unknown-matches");
    }

    public Button summaryViolations() {
      return new Button("#summary-violations");
    }

    public Button allViolations() {
      return new Button("#all-violations");
    }

    public Button waivedViolations() {
      return new Button("#waived-violations");
    }

    public Button quarantinedViolations() {
      return new Button("#quarantined-violations");
    }
  }

  public static class Table
      extends BasicElement<Table>
  {
    public static final Condition CRITICAL_THREAT = cssClass("criticalScore");

    // Very specific selector to avoid catching the CIP SV table
    private static final String TABLE_ROW_SELECTOR = "> .slick-viewport > .grid-canvas > .slick-row";

    public static final Condition MODERATE_THREAT = cssClass("moderateScore");

    public static final Condition SEVERE_THREAT = cssClass("severeScore");

    public static final Condition NO_THREAT = cssClass("noScore");

    public static final Condition IGNORED_SCORE = cssClass("ignoredScore");

    public Table() {
      super("#componentTable");
    }

    public Row row(int num) {
      return new Row(child(TABLE_ROW_SELECTOR, "[row='" + num + "']"));
    }

    public ElementsCollection rows() {
      return children(TABLE_ROW_SELECTOR);
    }

    public Row rowByName(String name) {
      return new Row(rows().findBy(text(name)));
    }

    public SelenideElement cip() {
      return $("#informationPanel");
    }

    public SelenideElement cipCloseButton() {
      return $("#informationPanel .close");
    }

    public SelenideElement cipTab(String name) {
      return $$("#informationPanel .nav > li a").find(text(name));
    }

    public SelenideElement closeCipButton() {
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
        table().cip().shouldBe(visible);
        // A visible table row can apparently be clicked before the CIP is fully ready, causing a JS error from
        // ComponentInformationPanelPlugin.position() which in turn leaves the CIP improperly positioned & sized.
        // To avoid having later interactions stumble over this broken CIP, we try harder to open it in good state.
        if (!table().cip().getAttribute("style").isEmpty()) {
          break;
        }
        if (System.currentTimeMillis() - start > Configuration.timeout) {
          throw UIAssertionError.wrapThrowable(new IllegalStateException("CIP not properly positioned"),
              Configuration.timeout);
        }
        table().cipCloseButton().click();
      }
    }
  }
}
