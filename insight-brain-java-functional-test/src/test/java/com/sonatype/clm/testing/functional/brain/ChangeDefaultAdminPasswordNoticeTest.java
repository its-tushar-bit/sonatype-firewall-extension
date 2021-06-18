/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.ChangeDefaultAdminPasswordNotice;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.clm.testing.functional.pages.RoleManagementPage;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.User;

import com.codeborne.selenide.Condition;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.insight.brain.model.security.MembershipMapping.GLOBAL_CONTEXT_ID;

public class ChangeDefaultAdminPasswordNoticeTest
    extends AbstractFunctionalTest
{
  private static final String[] URLs = new String[] {
      DashboardPage.url(),
      ReportListPage.url(),
      RoleManagementPage.url()
  };

  private UserDAO userDAO = new UserDAO();

  @BeforeClass
  public static void beoreClass() {
    testCLMServer.getCLMServer().getConfiguration().setEnableDefaultPasswordWarning(true);
  }

  @AfterClass
  public static void afterClass() {
    testCLMServer.getCLMServer().getConfiguration().setEnableDefaultPasswordWarning(false);
  }

  @Before
  public void before() {
    refreshOrOpen(DashboardPage.url());
  }

  @After
  public void after() {
    logout();
    testCLMServer.getCLMServer().getConfiguration().setEnableDefaultPasswordWarning(true);
  }

  @Test
  public void testChangeDefaultAdminPasswordNotice_defaultUser() {
    loginAsAdmin();

    DashboardPage.filterToggle().shouldBe(visible).click();
    eyesWatcher.eyesCheck("DefaultAdminPasswordNotice with dashboard filter");

    assertNotice(visible, text("change your password"));

    User admin = userDAO.getByUsername("admin");
    String originalPassword = admin.getPassword();
    try {
      admin.setPassword("foo");
      userDAO.update(admin);
      refresh();
      assertNotice(hidden);
    }
    finally {
      admin.setPassword(originalPassword);
      userDAO.update(admin);
    }

    testCLMServer.getCLMServer().getConfiguration().setEnableDefaultPasswordWarning(false);
    refresh();
    assertNotice(hidden);
  }

  @Test
  public void testChangeDefaultAdminPasswordNotice_nonAdminUser() {
    createUser();
    login();

    assertNotice(hidden);
  }

  @Test
  public void testChangeDefaultAdminPasswordNotice_nonDefaultAdminUser() {
    createUser();
    grantPermissions(getUsername(), GLOBAL_CONTEXT_ID, Permission.CONFIGURE_SYSTEM);
    login();

    assertNotice(visible, text("The \"admin\" user has the default password set"));
    eyesWatcher.eyesCheck();
    testCLMServer.getCLMServer().getConfiguration().setEnableDefaultPasswordWarning(false);
    refresh();
    assertNotice(hidden);
  }

  private void assertNotice(Condition... conditions) {
    ChangeDefaultAdminPasswordNotice notice = new ChangeDefaultAdminPasswordNotice();
    for (String url : URLs) {
      refreshOrOpen(url);
      notice.shouldHave(conditions);
    }
  }
}
