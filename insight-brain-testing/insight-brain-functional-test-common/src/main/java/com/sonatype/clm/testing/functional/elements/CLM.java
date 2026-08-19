/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.WebElementCondition;

import static com.codeborne.selenide.Condition.and;
import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.cssClass;

public class CLM
{
  public static final WebElementCondition PRISTINE = cssClass("ng-pristine");

  public static final WebElementCondition RSC_PRISTINE = cssClass("pristine");

  public static final WebElementCondition DISABLED = cssClass("disabled");

  public static final WebElementCondition EXPANDED = cssClass("expand");

  public static final WebElementCondition COLLAPSED = cssClass("collapse");

  public static final WebElementCondition SELECTED = cssClass("selected");

  public static final WebElementCondition IQ_DISABLED = cssClass("iq-disabled");

  public static final WebElementCondition RSC_DISABLED = and(
      "is disabled",
      cssClass("disabled"),
      attribute("aria-disabled", "true"));

  public static final WebElementCondition RSC_TERTIARY_DISABLED = and(
      "is disabled",
      attribute("disabled"),
      attribute("aria-disabled", "true"));

  public static final WebElementCondition CSS_SIDEBAR_OPEN = cssClass("open");

  public static final WebElementCondition CSS_SIDEBAR_CLOSED = cssClass("closed");

  public static final WebElementCondition NX_RADIO_SELECTED = cssClass("tm-checked");

  public static final WebElementCondition NX_RADIO_CHECKBOX_DISABLED = cssClass("nx-radio-checkbox--disabled");
}
