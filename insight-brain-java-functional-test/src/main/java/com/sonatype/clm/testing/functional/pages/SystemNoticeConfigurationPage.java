/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.CLM;
import com.sonatype.clm.testing.functional.elements.Toggle;
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

  public Toggle displayToggle() {
    return new Toggle(childSelector("#system-notice-display-toggle-checkbox"));
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
    return displayToggle().isChecked();
  }

  public void setDisplay(final boolean display) {
    if (display != isDisplayed()) {
      displayToggle().click();
    }
  }

  public void setDisplayAndUpdate(final boolean display) {
    setDisplay(display);
    save();
  }

  public void displayMatches(final boolean display) {
    if (display) {
      displayToggle().shouldBe(checked);
    }
    else {
      displayToggle().shouldNotBe(checked);
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
    displayToggle().click();

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

  public void tooltipShowing() {
    tooltip().should(exist, exactText("There are no changes to update."));
  }

  public void tooltipNotShowing() {
    tooltip().shouldNot(exist);
  }
}
