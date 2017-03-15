/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.SelenideElement;

/**
 * CLM iq-checkbox Widget. Uses a pseudo element for the checkbox therefore clicks cannot be processed by the
 * Chrome Webdriver on {@link #input()}
 */
public class IqCheckbox extends Checkbox
{
  public IqCheckbox(SelenideElement iqCheckboxElement) {
    super(iqCheckboxElement.$("label"));
  }

  public SelenideElement label() {
    return super.element;
  }
}
