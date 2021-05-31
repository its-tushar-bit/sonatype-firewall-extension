/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.CLM;
import com.sonatype.clm.testing.functional.elements.NxToggle;
import com.sonatype.clm.testing.functional.elements.Tooltip;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.checked;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.exactValue;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Selenide.$;

public class SystemNoticeConfigurationPage
    extends BasicElement<SystemNoticeConfigurationPage>
{
  public static final String NOT_DIRTY_FORM = "There are no changes to update";

  public static final String EMPTY_NOTICE_MESSAGE = "Notice Text cannot be blank";

  public static String url() {
    return BaseUrl.resolvePageUrl("/systemNoticeConfiguration");
  }

  private static final String ROOT_SELECTOR = "#system-notice-configuration";

  private static final int DOUBLE_CLICK_TIME = 500;

  private long lastClicked;

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

  public NxToggle toggle() {
    return new NxToggle(childSelector("#system-notice-display-toggle-checkbox"));
  }

  public SelenideElement update() {
    return child(".nx-form__submit-btn");
  }

  public SelenideElement cancel() {
    return child("#system-notice-cancel");
  }

  public Tooltip tooltip() {
    return Tooltip.get();
  }

  private void save() {
    update().click();
    update().shouldBe(CLM.DISABLED); // wait until REST request completed (no form mask used...)
  }

  public String getText() {
    return text().getValue();
  }

  public void setText(final String text) {
    text().setValue(text);
  }

  public void setTextAndUpdate(final String text) {
    setText(text);
    save();
  }

  public void textMatches(final String text) {
    text().shouldHave(exactValue(text));
  }

  public boolean isDisplayed() {
    return toggle().input().isSelected();
  }

  public void setDisplay(final boolean display) {
    if (display != isDisplayed()) {
      toggle().click();
    }
  }

  public void setDisplayAndUpdate(final boolean display) {
    setDisplay(display);
    save();
  }

  public void displayMatches(final boolean display) {
    if (display) {
      toggle().input().shouldBe(checked);
    }
    else {
      toggle().input().shouldNotBe(checked);
    }
  }

  public void toggleDisplay() {
    // Prevent subsequent clicks from happening too fast, otherwise the browser may interpret it as a double-click.
    long timeBetweenClicks = System.currentTimeMillis() - lastClicked;
    if (timeBetweenClicks < DOUBLE_CLICK_TIME) {
      try {
        Thread.sleep(DOUBLE_CLICK_TIME - timeBetweenClicks);
      }
      catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
    toggle().click();

    lastClicked = System.currentTimeMillis();
  }

  public void toggleDisplayAndUpdate() {
    toggleDisplay();
    save();
  }

  public void setTextAndDisplay(final String text, final boolean display) {
    setText(text);
    setDisplay(display);
  }

  public void setTextAndDisplayAndUpdate(final String text, final boolean display) {
    setTextAndDisplay(text, display);
    save();
  }

  public void setTextAndToggleDisplay(final String text) {
    setText(text);
    toggleDisplay();
  }

  public void tooltipShowing(final String message) {
    tooltip().should(exist, exactText(message));
  }

  public void tooltipNotShowing() {
    tooltip().shouldNot(exist);
  }
}
