/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.WebElementCondition;

import static com.codeborne.selenide.Condition.text;

public class RemoveModal
    extends DeleteModal
{
  public static WebElementCondition headerText(String resourceType) {
    return text("Clear " + resourceType);
  }

  public static WebElementCondition bodyText(String resourceName) {
    return text("You are about to remove " + resourceName + ".");
  }
}
