/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.Button;
import com.sonatype.clm.testing.functional.elements.NxThreatCounter;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class WaiveTransitiveViolationsPopover
    extends BasicElement<WaiveTransitiveViolationsPopover>
{
  private static final String ROOT = "#waive-transitive-violations-popover";

  public WaiveTransitiveViolationsPopover() {
    super(ROOT);
  }

  public Button toggle() {
    return new Button("#waive-transitive-violations-popover-toggle");
  }

  public SelenideElement countsTitle() {
    return child(".transitive-violations-counts-group .nx-read-only__data");
  }

  public NxThreatCounter counts() {
    return new NxThreatCounter(".nx-threat-counter-container");
  }

  public WaiveTransitiveViolationsPopoverCount count(int index) {
    return new WaiveTransitiveViolationsPopoverCount(
        childSelector("#waive-transitive-violations-counts div", nthChild(index + 1)));
  }

  public static class WaiveTransitiveViolationsPopoverCount
      extends BasicElement<WaiveTransitiveViolationsPopoverCount>
  {
    public WaiveTransitiveViolationsPopoverCount(String selector) {
      super(selector);
    }

    public SelenideElement text() {
      return child("dt");
    }

    public SelenideElement count() {
      return child("dd");
    }
  }

  public SelenideElement scope() {
    return child("#waive-transitive-violations-scopes");
  }

  public SelenideElement expiryTimesSelect() {
    return child("#waive-transitive-violations-expirations");
  }

  public ElementsCollection expiryTimesOptions() {
    return children("#waive-transitive-violations-expirations option");
  }

  public SelenideElement comments() {
    return child("#waive-transitive-violations-comments");
  }

  public Button cancelButton() {
    return new Button("#waive-transitive-violations-popover-cancel");
  }

  public Button saveButton() {
    return new Button("#waive-transitive-violations-popover-save");
  }

  public Button retryButton() {
    return new Button("#waive-transitive-violations-popover-save-error .nx-btn");
  }

  public SelenideElement submitError() {
    return child("#waive-transitive-violations-popover-save-error");
  }
}
