/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional;

import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.ex.ElementNotFound;
import org.openqa.selenium.StaleElementReferenceException;

import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.visible;

public class NxLoadingSpinner
{
  public static SelenideElement seeAndWaitForDismissal(final SelenideElement parent) {
    SelenideElement loadingSpinner = parent.$(".nx-loading-spinner");

    try {
      loadingSpinner.shouldBe(visible);
    }
    catch (AssertionError e) {
      if (e instanceof ElementNotFound || e.getCause() instanceof StaleElementReferenceException) {
        // ok the spinner appeared and disappeared before we got a chance to check
        return loadingSpinner;
      }
      throw e;
    }

    return loadingSpinner.shouldBe(hidden);
  }
}
