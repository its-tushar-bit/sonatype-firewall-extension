/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class MainView
{
  public static SelenideElement mainView() {
    return $(".nx-page");
  }

  /**
   * The element witin which all of the ui-router managed content appears. Essentially, everything besides
   * modals and the MainHeader is within this element
   */
  public static SelenideElement uiView() {
    return $(".nx-page-content > ui-view");
  }
}
