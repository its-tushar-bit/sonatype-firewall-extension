/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.LoginPage;
import com.sonatype.clm.testing.playwright.pages.SystemNoticePage;
import com.sonatype.clm.testing.playwright.pages.SystemNoticePageAssertions;
import com.sonatype.insight.brain.configuration.SystemNoticeService;
import com.sonatype.insight.brain.model.configuration.SystemNotice;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

public class SystemNoticePlaywrightTest
    extends AbstractIqUiTest
{
  private static final String NOTICE_TEXT = "Maintenance window 22:00-23:00 UTC.";

  private SystemNoticePage noticePage;

  private SystemNoticePageAssertions noticeAssertions;

  @BeforeEach
  public void setUp() {
    playwrightRefreshOrOpen(SystemNoticePage.url());
    playwrightLogin();
    noticePage = new SystemNoticePage();
    noticeAssertions = new SystemNoticePageAssertions(noticePage);
  }

  @AfterEach
  public void cleanup() {
    playwrightLogout();
    SystemNotice empty = new SystemNotice();
    empty.setMessage("");
    empty.setEnabled(false);
    lookup(SystemNoticeService.class).updateSystemNotice(empty);
  }

  @Test
  @Tag("regression")
  public void testSystemNoticeConfigurationPageRenders() {
    noticeAssertions.shouldRenderPageLayout();
  }

  @Test
  @Tag("regression")
  public void testSystemNotice_savePersistsAcrossReload() {
    noticePage.noticeText().fill(NOTICE_TEXT);
    noticePage.enabledToggle().click();
    noticePage.updateButton().click();
    waitForSubmitMaskSuccess();

    playwrightRefreshOrOpen(SystemNoticePage.url());
    noticeAssertions.shouldHaveNoticeText(NOTICE_TEXT);
    noticeAssertions.shouldHaveEnabledToggleChecked();
  }

  @Test
  @Tag("regression")
  public void testSystemNotice_appearsOnLoginScreen() {
    noticePage.noticeText().fill(NOTICE_TEXT);
    noticePage.enabledToggle().click();
    noticePage.updateButton().click();
    waitForSubmitMaskSuccess();

    try {
      playwrightLogout();

      LoginPage loginPage = new LoginPage();
      assertThat(loginPage.systemNotice()).isVisible();
      assertThat(loginPage.systemNotice()).containsText(NOTICE_TEXT);
    }
    finally {
      // Always re-establish session so @After cleanup's playwrightLogout() can find the user menu.
      playwrightLogin();
    }
  }
}
