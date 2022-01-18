/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements.componentdetails;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

public class InnerSourceRepositorySourceAlert
    extends BasicElement<InnerSourceRepositorySourceAlert>
{
  public static final String ROOT = "#inner-source-repository-source-alert";

  public InnerSourceRepositorySourceAlert() {
    super(ROOT);
  }

  public SelenideElement content() {
    return child(".nx-alert__content");
  }
}
