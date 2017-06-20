/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class DashboardTab
{

  private final SelenideElement link;
  private final SelenideElement tab;

  public DashboardTab(String linkId) {
    this.link = $(linkId);
    this.tab = link.parent();
  }

  public DashboardTab shouldBe(Condition condition) {
    tab.shouldBe(condition);
    return this;
  }

  public DashboardTab shouldNotBe(Condition condition) {
    tab.shouldNotBe(condition);
    return this;
  }

  public SelenideElement counter() {
    return link.$(".iq-counter");
  }

  public void click() {
    link.click();
  }
}
