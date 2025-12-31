/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.LoginModal;
import com.sonatype.clm.testing.functional.pages.AdministratorsPage;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.LdapServerListPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.ProductLicensePage;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.clm.testing.functional.pages.RoleManagementPage;
import com.sonatype.clm.testing.functional.pages.SystemNoticeConfigurationPage;
import com.sonatype.clm.testing.functional.pages.UserManagementPage;
import com.sonatype.clm.testing.functional.pages.WebhookConfigurationPage;
import com.sonatype.insight.brain.dataaccess.configuration.SystemNoticeDAO;
import com.sonatype.insight.brain.model.configuration.SystemNotice;

import com.codeborne.selenide.WebElementCondition;
import org.junit.After;
import org.junit.Before;

import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.visible;

public class SystemNoticeTest
    extends AbstractFunctionalTest
{
  private static final String FIVE_HUNDRED_CHARACTERS = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
      + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
      + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
      + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
      + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
      + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
      + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
      + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
      + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
      + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

  private static final SystemNotice EMPTY_DISABLED = createSystemNotice("", false);

  private static final SystemNotice FILLED_DISABLED = createSystemNotice(FIVE_HUNDRED_CHARACTERS, false);

  private static final SystemNotice FILLED_ENABLED = createSystemNotice(FIVE_HUNDRED_CHARACTERS, true);

  private static final String[] PAGE_URLS = new String[]{
      DashboardPage.url(),
      ReportListPage.url(),
      OwnerSummaryPage.url(),
      UserManagementPage.url(),
      RoleManagementPage.url(),
      ProductLicensePage.url(),
      AdministratorsPage.url(),
      new LdapServerListPage().url(),
      WebhookConfigurationPage.url(),
      SystemNoticeConfigurationPage.url()
  };

  private final com.sonatype.clm.testing.functional.elements.SystemNotice systemNotice =
      new com.sonatype.clm.testing.functional.elements.SystemNotice();

  private SystemNoticeDAO systemNoticeDAO;

  @Before
  public void setUp() {
    systemNoticeDAO = lookup(SystemNoticeDAO.class);
  }

  @After
  public void after() {
    systemNoticeDAO.update(EMPTY_DISABLED);
  }

  private void disabledSystemNotice_NotShownOnLogin() {
    checkSystemNoticeVisibilityOnLogin(EMPTY_DISABLED, hidden);
    checkSystemNoticeVisibilityOnLogin(FILLED_DISABLED, hidden);
  }

  private void enabledSystemNotice_ShownOnLogin() {
    checkSystemNoticeVisibilityOnLogin(FILLED_ENABLED, visible);
    eyesWatcher.eyesCheck("System Notice on Login");
  }

  private void disabledSystemNotice_NotShownOnPages() {
    checkSystemNoticeVisibilityAfterLogin(EMPTY_DISABLED, hidden);
    checkSystemNoticeVisibilityAfterLogin(FILLED_DISABLED, hidden);
  }

  private void enabledSystemNotice_ShownOnPages() {
    checkSystemNoticeVisibilityAfterLogin(FILLED_ENABLED, visible);
    eyesWatcher.eyesCheck("System Notice on Page");
  }

  private void login(final String url) {
    refreshOrOpen(url);
    loginAsAdmin();
  }

  private void checkSystemNoticeVisibilityOnLogin(
      final SystemNotice systemNotice,
      final WebElementCondition visibility)
  {
    systemNoticeDAO.update(systemNotice);
    refreshOrOpen(DashboardPage.url());
    new LoginModal().systemNotice().shouldBe(visibility);
  }

  private void checkSystemNoticeVisibilityAfterLogin(
      final SystemNotice systemNotice,
      final WebElementCondition visibility)
  {
    systemNoticeDAO.update(systemNotice);
    refresh();
    for (String url : PAGE_URLS) {
      refreshOrOpen(url);
      this.systemNotice.shouldBe(visibility);
    }
  }

  private static SystemNotice createSystemNotice(final String message, final boolean enabled) {
    final SystemNotice systemNotice = new SystemNotice();
    systemNotice.setMessage(message);
    systemNotice.setEnabled(enabled);
    return systemNotice;
  }
}
