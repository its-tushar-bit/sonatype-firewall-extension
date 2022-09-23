/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.Condition;

import static com.codeborne.selenide.Condition.and;
import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.cssClass;

public class CLM
{
  public static final Condition PRISTINE = cssClass("ng-pristine");

  public static final Condition RSC_PRISTINE = cssClass("pristine");

  public static final Condition DISABLED = cssClass("disabled");

  public static final Condition EXPANDED = cssClass("expand");

  public static final Condition COLLAPSED = cssClass("collapse");

  public static final Condition SELECTED = cssClass("selected");

  public static final Condition IQ_DISABLED = cssClass("iq-disabled");

  public static final Condition RSC_DISABLED = and(
      "is disabled",
      cssClass("disabled"),
      attribute("aria-disabled", "true" )
  );

  public static final Condition CSS_SIDEBAR_OPEN = cssClass("open");

  public static final Condition CSS_SIDEBAR_CLOSED = cssClass("closed");

  public static final Condition NX_RADIO_SELECTED = cssClass("tm-checked");

  public static final Condition NX_RADIO_CHECKBOX_DISABLED = cssClass("nx-radio-checkbox--disabled");
}
