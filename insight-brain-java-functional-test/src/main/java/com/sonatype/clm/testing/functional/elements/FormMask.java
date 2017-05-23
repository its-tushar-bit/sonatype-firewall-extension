/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.ex.ElementNotFound;
import org.openqa.selenium.StaleElementReferenceException;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

public class FormMask
{
  public static SelenideElement seeAndWaitForDismissal() {
    SelenideElement mask = $(".form-mask");

    try {
      mask.shouldBe(visible);
    }
    catch (ElementNotFound | StaleElementReferenceException e) {
      // ok the mask opened and closed before we got a chance to check
      return mask;
    }

    return mask.shouldNotBe(visible);
  }
}
