/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.WebElementCondition;

import static com.codeborne.selenide.Condition.cssClass;

public class SidebarNavigationButton
    extends BasicElement<SidebarNavigationButton>
{
  public static final WebElementCondition CLASS_ACTIVE = cssClass("selected");

  public SidebarNavigationButton(String selector) {
    super(".nx-global-sidebar-2__nav", selector);
  }
}
