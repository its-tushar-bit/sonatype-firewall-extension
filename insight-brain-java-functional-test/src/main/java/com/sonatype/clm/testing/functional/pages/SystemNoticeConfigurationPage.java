/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.exactValue;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Selenide.$;

public class SystemNoticeConfigurationPage
    extends BasicElement<SystemNoticeConfigurationPage>
{
  public static final String URL = BaseUrl.resolvePageUrl("/systemNotice");

  private static final String ROOT_SELECTOR = "#system-notice-configuration";

  private static final Condition OFF = cssClass("off");

  public SystemNoticeConfigurationPage() {
    super(ROOT_SELECTOR);
  }

  public SelenideElement root() {
    return $(ROOT_SELECTOR);
  }

  public SelenideElement explanation() {
    return child("#system-notice-explanation");
  }

  public SelenideElement text() {
    return child("#system-notice-text");
  }

  public SelenideElement display() {
    return child("#system-notice-display");
  }

  public SelenideElement displayToggle() {
    return display().parent();
  }

  public SelenideElement update() {
    return child("#system-notice-update");
  }

  public SelenideElement cancel() {
    return child("#system-notice-cancel");
  }

  public SelenideElement tooltip() {
    return root().find(".tooltip");
  }

  public String getText() {
    return text().getValue();
  }

  public void setText(final String text) {
    text().setValue(text);
  }

  public void setTextAndUpdate(final String text) {
    setText(text);
    update().click();
  }

  public void textMatches(final String text) {
    text().shouldHave(exactValue(text));
  }

  public boolean isDisplayed() {
    return !displayToggle().getAttribute("class").contains("off");
  }

  public void setDisplay(final boolean display) {
    if (display != isDisplayed()) {
      displayToggle().click();
    }
  }

  public void setDisplayAndUpdate(final boolean display) {
    setDisplay(display);
    update().click();
  }

  public void displayMatches(final boolean display) {
    if (display) {
      displayToggle().shouldNotHave(OFF);
    }
    else {
      displayToggle().shouldHave(OFF);
    }
  }

  public void toggleDisplay() {
    displayToggle().click();
  }

  public void toggleDisplayAndUpdate() {
    toggleDisplay();
    update().click();
  }

  public void setTextAndDisplay(final String text, final boolean display) {
    setText(text);
    setDisplay(display);
  }

  public void setTextAndDisplayAndUpdate(final String text, final boolean display) {
    setTextAndDisplay(text, display);
    update().click();
  }

  public void setTextAndToggleDisplay(final String text) {
    setText(text);
    toggleDisplay();
  }

  public void tooltipShowing() {
    tooltip().should(exist, exactText("There are no changes to update."));
  }

  public void tooltipNotShowing() {
    tooltip().shouldNot(exist);
  }
}
