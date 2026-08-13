/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.utils;

import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.Keys;

public class InputUtils
{
  public static void clearInput(SelenideElement element) {

    while (!element.getAttribute("value").equals("")) {
      element.sendKeys(Keys.BACK_SPACE);
    }
  }
}
