/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

import static com.codeborne.selenide.Selenide.$$;

public class EvaluationStatusModal
    extends BasicElement<EvaluationStatusModal>
{
  private static final String ROOT = "#evaluation-status-modal";

  private static final String DESCRIPTION_SELECTOR = ".nx-list__description";

  public EvaluationStatusModal() {
    super(ROOT);
  }

  public SelenideElement bundleFileName() {
    return $$(DESCRIPTION_SELECTOR).get(0);
  }

  public SelenideElement bundleAppName() {
    return $$(DESCRIPTION_SELECTOR).get(1);
  }

  public SelenideElement bundleStageName() {
    return $$(DESCRIPTION_SELECTOR).get(2);
  }

  public SelenideElement evaluateBundleStatus() {
    return $(".nx-progress-bar__label-text");
  }

  public SelenideElement viewReportButton() {
    return $(".evaluation-status .nx-btn--primary");
  }

  public SelenideElement closeButton() {
    return $(".evaluation-status .nx-btn--secondary");
  }
}
