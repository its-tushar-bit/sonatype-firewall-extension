/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebElementCondition;

import static com.codeborne.selenide.Selenide.$;

public class RoutingErrorBox
{
  public static WebElementCondition errorText(String error) {
    return Condition.text("Please try to reload the page, if the problem persists contact your server administrator. ("
        + error + ")");
  }

  public static SelenideElement errorBox() {
    return $(".iq-alert--error");
  }

  public static SelenideElement errorMessage() {
    return errorBox().$("p:last-child");
  }
}
