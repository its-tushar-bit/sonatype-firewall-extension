/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional;

import com.sonatype.clm.testing.functional.utils.ElementUtils;

import com.codeborne.selenide.SelenideElement;

public class NxLoadingSpinner
{
  public static SelenideElement seeAndWaitForDismissal(final SelenideElement parent) {
    SelenideElement loadingSpinner = parent.$(".nx-loading-spinner");
    return ElementUtils.seeElementAndWaitForDismissal(loadingSpinner);
  }
}
