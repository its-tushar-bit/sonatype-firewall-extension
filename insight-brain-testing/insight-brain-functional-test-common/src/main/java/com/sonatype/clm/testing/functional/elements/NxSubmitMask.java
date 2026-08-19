/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.utils.ElementUtils;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class NxSubmitMask
{
  public static SelenideElement seeAndWaitForDismissal() {
    SelenideElement mask = $(".nx-submit-mask");
    return ElementUtils.seeElementAndWaitForDismissal(mask);
  }
}
