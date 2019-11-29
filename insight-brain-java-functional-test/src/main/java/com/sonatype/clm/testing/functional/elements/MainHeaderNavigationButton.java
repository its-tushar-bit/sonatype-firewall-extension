/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.Condition;

import static com.codeborne.selenide.Condition.cssClass;

public class MainHeaderNavigationButton
    extends BasicElement<MainHeaderNavigationButton>
{
  public static final Condition CLASS_ACTIVE = cssClass("active");

  public MainHeaderNavigationButton(String selector) {
    super(".iq-navigation", selector);
  }
}
