/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.Condition;

import static com.codeborne.selenide.Condition.cssClass;

public class CLM
{
  public static final Condition INITIAL_VALUE_CLASS = cssClass("initial-value");

  public static final Condition DISABLED_CLASS = cssClass("disabled");
}
