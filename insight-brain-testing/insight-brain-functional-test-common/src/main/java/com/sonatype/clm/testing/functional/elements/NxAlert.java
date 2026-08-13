/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

public class NxAlert
    extends
    BasicElement<NxAlert>
{
  public NxAlert(String... selector) {
    super(selector);
  }

  public static NxAlert getSuccessAlert() {
    return new NxAlert(".nx-alert.nx-alert--success");
  }

  public static NxAlert getErrorAlert() {
    return new NxAlert(".nx-alert.nx-alert--error");
  }

  public static NxAlert getWarningAlert() {
    return new NxAlert(".nx-alert.nx-alert--warning");
  }

  public static NxAlert getInfoAlert() {
    return new NxAlert(".nx-alert.nx-alert--info");
  }

  public SelenideElement getContentWrapper() {
    return child(".nx-alert__content-wrap");
  }

  public SelenideElement getContent() {
    return getContentWrapper().find(".nx-alert__content");
  }

  public SelenideElement button() {
    return child(".nx-btn");
  }
}
