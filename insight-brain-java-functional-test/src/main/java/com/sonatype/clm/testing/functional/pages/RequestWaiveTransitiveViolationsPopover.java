/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.Button;
import com.sonatype.clm.testing.functional.elements.NxCodeSnippet;
import com.sonatype.clm.testing.functional.elements.NxThreatCounter;

import com.codeborne.selenide.SelenideElement;

import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class RequestWaiveTransitiveViolationsPopover
    extends BasicElement<RequestWaiveTransitiveViolationsPopover>
{
  private static final String ROOT = "#request-waive-transitive-violations-popover";

  public RequestWaiveTransitiveViolationsPopover() {
    super(ROOT);
  }

  public Button toggle() {
    return new Button("#request-waive-transitive-violations-popover-toggle");
  }

  public SelenideElement countsTitle() {
    return child(".transitive-violations-counts-group .nx-read-only__data");
  }

  public NxThreatCounter counts() {
    return new NxThreatCounter(".nx-threat-counter-container");
  }

  public RequestWaiveTransitiveViolationsPopoverCount count(int index) {
    return new RequestWaiveTransitiveViolationsPopoverCount(
        childSelector("#request-waive-transitive-violations-counts div", nthChild(index + 1)));
  }

  public static class RequestWaiveTransitiveViolationsPopoverCount
      extends BasicElement<RequestWaiveTransitiveViolationsPopoverCount>
  {
    public RequestWaiveTransitiveViolationsPopoverCount(String selector) {
      super(selector);
    }

    public SelenideElement text() {
      return child("dt");
    }

    public SelenideElement count() {
      return child("dd");
    }
  }

  public NxCodeSnippet applicationPublicIdContainer() {
    return new NxCodeSnippet("#request-waive-transitive-violations-application-public-id");
  }

  public NxCodeSnippet reportIdContainer() {
    return new NxCodeSnippet("#request-waive-transitive-violations-report-id");
  }

  public NxCodeSnippet componentHashContainer() {
    return new NxCodeSnippet("#request-waive-transitive-violations-component-hash");
  }

  public NxCodeSnippet curlExampleContainer() {
    return new NxCodeSnippet("#request-waive-transitive-violations-curl-example");
  }
}
