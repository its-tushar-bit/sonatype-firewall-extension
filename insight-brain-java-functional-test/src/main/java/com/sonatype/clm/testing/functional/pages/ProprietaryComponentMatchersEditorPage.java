/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.codeborne.selenide.Condition;

import static com.codeborne.selenide.Condition.text;

public class ProprietaryComponentMatchersEditorPage
{
  public static Condition summaryText(int localCount, int inheritedCount) {
    return text(localCount + " local, " + inheritedCount + " inherited");
  }
}
