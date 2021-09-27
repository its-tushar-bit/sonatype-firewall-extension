/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements.componentdetails;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

public class InnerSourceProducerAlert
    extends BasicElement<InnerSourceProducerAlert>
{
  public static final String ROOT = "#inner-source-producer-alert";

  public InnerSourceProducerAlert() {
    super(ROOT);
  }

  public SelenideElement latestReportLink() {
    return child(".nx-text-link");
  }

  public SelenideElement content() {
    return child(".nx-alert__content");
  }
}
