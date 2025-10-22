/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import java.time.Duration;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.Button;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

public class BulkWaivePage
    extends BasicElement<BulkWaivePage>
{
  public static final String ROOT = "#bulk-waive-page-container";

  /**
   * URL for application-level bulk waive (from report page)
   */
  public static String url(String publicId, String scanId) {
    return BaseUrl.resolvePageUrl("/applicationReport/{publicId}/{scanId}/bulkWaive",
        publicId, scanId);
  }

  /**
   * URL for CDP-level bulk waive (component details page level)
   */
  public static String url(String publicId, String scanId, String hash) {
    return BaseUrl.resolvePageUrl("/applicationReport/{publicId}/{scanId}/{hash}/bulkWaive",
        publicId, scanId, hash);
  }

  public BulkWaivePage() {
    super(ROOT);
  }

  public BulkWaiveTitle title() {
    return new BulkWaiveTitle();
  }

  public SelenideElement tileHeaderTitle() {
    return child(".nx-tile-header__title h2");
  }

  public Button filterToggleButton() {
    return new Button("#filters-toggle-button");
  }

  public BulkWaiveTable table() {
    return new BulkWaiveTable(childSelector("#bulk-waive-table"));
  }

  public SelenideElement selectedCountMessage() {
    return child(".iq-bulk-waive__selected-count");
  }

  public Button cancelButton() {
    return new Button("#bulk-waive-selection-cancel-button");
  }

  public Button nextButton() {
    return new Button("#bulk-waive-selection-next-button");
  }

  public static SelenideElement pageLoadSpinner() {
    return $(".nx-loading-spinner");
  }

  public static void waitUntilSpinnersGone() {
    pageLoadSpinner().shouldNotBe(visible, Duration.ofSeconds(10));
  }

  /**
   * Inner class representing the bulk waive violations table
   */
  public static class BulkWaiveTable
      extends BasicElement<BulkWaiveTable>
  {
    private static final String BODY_ROW =
        "tbody .nx-table-row:not(.nx-table-row--filter-header):not(.iq-bulk-waive__no-results-row)";

    public BulkWaiveTable(String selector) {
      super(selector);
    }

    public SelenideElement selectAllCheckbox() {
      return child(".iq-bulk-waive__select-all-cell .nx-checkbox");
    }

    public SelenideElement threatHeaderCell() {
      return child(".iq-bulk-waive__threat-cell");
    }

    public SelenideElement policyHeaderCell() {
      return child(".iq-bulk-waive__policy-name-cell");
    }

    public SelenideElement componentHeaderCell() {
      return child(".iq-bulk-waive__component-name-cell");
    }

    public SelenideElement constraintHeaderCell() {
      return child(".iq-bulk-waive__constraint-name-cell");
    }

    public SelenideElement conditionHeaderCell() {
      return child(".iq-bulk-waive__condition-name-cell");
    }

    public SelenideElement policyNameFilter() {
      return child("#report-policy-name-filter");
    }

    public SelenideElement componentNameFilter() {
      return child("#report-component-name-filter");
    }

    public ElementsCollection rows() {
      return children(BODY_ROW);
    }

    public BulkWaiveTableRow row(int index) {
      return new BulkWaiveTableRow(rows().get(index));
    }
  }

  /**
   * Inner class representing a single row in the bulk waive table
   */
  public static class BulkWaiveTableRow
  {
    private final SelenideElement element;

    public BulkWaiveTableRow(SelenideElement element) {
      this.element = element;
    }

    public SelenideElement checkbox() {
      return element.$(".nx-checkbox");
    }

    public SelenideElement threatLevel() {
      return element.$(".iq-bulk-waive__threat-cell");
    }

    public SelenideElement policyName() {
      return element.$(".iq-bulk-waive__policy-name-cell");
    }

    public SelenideElement componentName() {
      return element.$(".iq-bulk-waive__component-name-cell");
    }

    public SelenideElement constraintName() {
      return element.$(".iq-bulk-waive__constraint-name-cell");
    }

    public SelenideElement condition() {
      return element.$(".iq-bulk-waive__condition-name-cell");
    }

    public BulkWaiveTableRow clickRow() {
      element.click();
      return this;
    }

    public BulkWaiveTableRow clickCheckbox() {
      checkbox().click();
      return this;
    }

    public boolean isChecked() {
      return checkbox().isSelected();
    }
  }
}
