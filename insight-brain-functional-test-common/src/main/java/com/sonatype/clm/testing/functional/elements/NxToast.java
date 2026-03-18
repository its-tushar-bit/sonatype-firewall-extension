/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

public class NxToast
    extends BasicElement<NxToast>
{
  private static final String ROOT = ".nx-toast-container";

  public NxToast() {
    super(ROOT + " .nx-toast > .nx-alert");
  }

  public NxToast(String toastType) {
    super(ROOT + " .nx-toast > .nx-alert.nx-alert--" + toastType);
  }

  public SelenideElement closeButton() {
    return child(".nx-btn--close");
  }
}
