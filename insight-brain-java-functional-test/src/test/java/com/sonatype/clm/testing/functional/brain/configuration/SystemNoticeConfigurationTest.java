/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.configuration;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.CLM;
import com.sonatype.clm.testing.functional.elements.SystemNotice;
import com.sonatype.clm.testing.functional.pages.SystemNoticeConfigurationPage;
import com.sonatype.insight.brain.dataaccess.configuration.SystemNoticeDAO;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.appear;
import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.visible;

public class SystemNoticeConfigurationTest
    extends AbstractFunctionalTest
{
  private static final com.sonatype.insight.brain.model.configuration.SystemNotice DEFAULT_SYSTEM_NOTICE =
      new com.sonatype.insight.brain.model.configuration.SystemNotice();

  static {
    DEFAULT_SYSTEM_NOTICE.setMessage("text");
    DEFAULT_SYSTEM_NOTICE.setEnabled(false);
  }

  private SystemNoticeConfigurationPage systemNoticeConfigurationPage = new SystemNoticeConfigurationPage();

  private SystemNotice systemNotice = new SystemNotice();

  private SystemNoticeDAO systemNoticeDAO = new SystemNoticeDAO();

  private String text;

  private boolean display;

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(SystemNoticeConfigurationPage.url());
    loginAsAdmin();
  }

  @Test
  public void systemNoticeConfigurationTest() throws Exception {
    explanationIsVisible();

    cannotUpdateWithoutChanges();
    cannotCancelWithoutChanges();

    noChangesToUpdateTooltipShowsWithoutChanges();
    noticeTextEmptyMessageTooltipShows();

    clickCancel_RevertsText();
    clickCancel_RevertsDisplay();
    clickCancel_RevertsTextAndDisplay();

    systemNotice_InitiallyMatchesConfiguration();
    clickUpdate_UpdatesSystemNoticeDisplay();
    clickUpdate_UpdatesSystemNoticeText();
    clickUpdate_UpdatesSystemNoticeTextAndDisplay();

    cannotEnterMoreThan500Characters();
  }

  @After
  public void after() {
    systemNoticeDAO.update(DEFAULT_SYSTEM_NOTICE);
  }

  private void explanationIsVisible() {
    init();
    systemNoticeConfigurationPage.explanation().shouldBe(visible);
    systemNoticeConfigurationPage.explanation().shouldNotBe(empty);
  }

  private void cannotUpdateWithoutChanges() throws Exception {
    init();
    systemNoticeConfigurationPage.update().shouldHave(CLM.DISABLED);
    elementDisabled_WhenTextReverted(systemNoticeConfigurationPage.update(), CLM.DISABLED);
    elementDisabled_WhenDisplayReverted(systemNoticeConfigurationPage.update(), CLM.DISABLED);
    elementDisabled_WhenTextAndDisplayReverted(systemNoticeConfigurationPage.update(), CLM.DISABLED);
  }

  private void cannotCancelWithoutChanges() throws Exception {
    init();
    systemNoticeConfigurationPage.cancel().shouldHave(attribute("disabled"));
    elementDisabled_WhenTextReverted(systemNoticeConfigurationPage.cancel(), attribute("disabled"));
    elementDisabled_WhenDisplayReverted(systemNoticeConfigurationPage.cancel(), attribute("disabled"));
    elementDisabled_WhenTextAndDisplayReverted(systemNoticeConfigurationPage.cancel(), attribute("disabled"));
  }

  private void noChangesToUpdateTooltipShowsWithoutChanges() throws Exception {
    init();
    systemNoticeConfigurationPage.update().hover();
    systemNoticeConfigurationPage.tooltipShowing(SystemNoticeConfigurationPage.NOT_DIRTY_FORM);

    systemNoticeConfigurationPage.toggle().hover();
    systemNoticeConfigurationPage.toggleDisplay();
    systemNoticeConfigurationPage.update().hover();
    systemNoticeConfigurationPage.tooltipNotShowing();

    systemNoticeConfigurationPage.toggle().hover();
    systemNoticeConfigurationPage.toggleDisplay();
    systemNoticeConfigurationPage.update().hover();
    systemNoticeConfigurationPage.tooltipShowing(SystemNoticeConfigurationPage.NOT_DIRTY_FORM);
  }

  private void noticeTextEmptyMessageTooltipShows() throws Exception {
    init();
    systemNoticeConfigurationPage.setDisplay(true);
    systemNoticeConfigurationPage.setText(" ");

    systemNoticeConfigurationPage.update().hover();
    systemNoticeConfigurationPage.tooltipShowing(SystemNoticeConfigurationPage.EMPTY_NOTICE_MESSAGE);

    systemNoticeConfigurationPage.toggle().hover();
    systemNoticeConfigurationPage.toggleDisplay();

    systemNoticeConfigurationPage.update().hover();
    systemNoticeConfigurationPage.tooltipNotShowing();
    systemNoticeConfigurationPage.cancel().click();
  }

  private void clickCancel_RevertsText() throws Exception {
    init();
    systemNoticeConfigurationPage.setText(text + " updated");
    systemNoticeConfigurationPage.cancel().click();
    systemNoticeConfigurationPage.textMatches(text);
  }

  private void clickCancel_RevertsDisplay() throws Exception {
    init();
    systemNoticeConfigurationPage.toggleDisplay();
    systemNoticeConfigurationPage.cancel().click();
    systemNoticeConfigurationPage.displayMatches(display);
  }

  private void clickCancel_RevertsTextAndDisplay() throws Exception {
    init();
    systemNoticeConfigurationPage.setTextAndToggleDisplay(text + " updated");
    systemNoticeConfigurationPage.cancel().click();
    systemNoticeConfigurationPage.textMatches(text);
    systemNoticeConfigurationPage.displayMatches(display);
  }

  private void systemNotice_InitiallyMatchesConfiguration() throws Exception {
    init();
    systemNoticeMatchesConfiguration();

    systemNoticeConfigurationPage.setTextAndDisplayAndUpdate(text, !display);
    refresh();
    systemNoticeMatchesConfiguration();

    systemNoticeConfigurationPage.setTextAndDisplayAndUpdate(text + " updated", !display);
    refresh();
    systemNoticeMatchesConfiguration();
  }

  private void clickUpdate_UpdatesSystemNoticeDisplay() throws Exception {
    init();
    systemNoticeConfigurationPage.toggleDisplayAndUpdate();
    systemNoticeMatchesConfiguration();

    systemNoticeConfigurationPage.toggleDisplayAndUpdate();
    systemNoticeMatchesConfiguration();
  }

  private void clickUpdate_UpdatesSystemNoticeText() throws Exception {
    init();
    systemNoticeConfigurationPage.setDisplayAndUpdate(true);
    systemNoticeConfigurationPage.setTextAndUpdate(text + " updated");
    systemNoticeMatchesConfiguration();

    systemNoticeConfigurationPage.setDisplayAndUpdate(false);
    systemNoticeConfigurationPage.setTextAndUpdate(text + " updated again");
    systemNoticeConfigurationPage.setDisplayAndUpdate(true);
    systemNoticeMatchesConfiguration();
  }

  private void clickUpdate_UpdatesSystemNoticeTextAndDisplay() throws Exception {
    init();
    systemNoticeConfigurationPage.setDisplayAndUpdate(true);
    systemNoticeConfigurationPage.setTextAndDisplayAndUpdate(text + " updated", false);
    systemNoticeMatchesConfiguration();

    systemNoticeConfigurationPage.setTextAndDisplayAndUpdate(text + " updated again", true);
    systemNoticeMatchesConfiguration();
  }

  private void cannotEnterMoreThan500Characters() throws Exception {
    init();
    final String fiveHundredCharacters = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    systemNoticeConfigurationPage.setText("");
    systemNoticeConfigurationPage.text().sendKeys(fiveHundredCharacters);
    systemNoticeConfigurationPage.textMatches(fiveHundredCharacters);
    systemNoticeConfigurationPage.text().sendKeys("b");
    systemNoticeConfigurationPage.textMatches(fiveHundredCharacters);
  }

  private void init() {
    systemNoticeDAO.update(DEFAULT_SYSTEM_NOTICE);
    refreshOrOpen(SystemNoticeConfigurationPage.url());
    systemNoticeConfigurationPage.should(appear);
    text = DEFAULT_SYSTEM_NOTICE.getMessage();
    display = DEFAULT_SYSTEM_NOTICE.isEnabled();
    systemNoticeConfigurationPage.textMatches(text);
    systemNoticeConfigurationPage.displayMatches(display);
  }

  private void elementDisabled_WhenTextReverted(SelenideElement element, Condition disabledCondition) {
    systemNoticeConfigurationPage.setText(text + " updated");
    element.shouldNotHave(disabledCondition);
    systemNoticeConfigurationPage.setText(text);
    element.shouldHave(disabledCondition);
  }

  private void elementDisabled_WhenDisplayReverted(SelenideElement element, Condition disabledCondition) {
    systemNoticeConfigurationPage.toggleDisplay();
    element.shouldNotHave(disabledCondition);
    systemNoticeConfigurationPage.toggleDisplay();
    element.shouldHave(disabledCondition);
  }

  private void elementDisabled_WhenTextAndDisplayReverted(SelenideElement element, Condition disabledCondition) {
    systemNoticeConfigurationPage.setTextAndToggleDisplay(text + " updated");
    element.shouldNotHave(disabledCondition);
    systemNoticeConfigurationPage.setTextAndToggleDisplay(text);
    element.shouldHave(disabledCondition);
  }

  private void systemNoticeMatchesConfiguration() {
    systemNoticeConfigurationPage.should(appear);
    if (systemNoticeConfigurationPage.isDisplayed()) {
      systemNotice.shouldBe(visible).shouldHave(exactText(systemNoticeConfigurationPage.getText()));
    }
    else {
      systemNotice.shouldBe(hidden);
    }
  }
}
