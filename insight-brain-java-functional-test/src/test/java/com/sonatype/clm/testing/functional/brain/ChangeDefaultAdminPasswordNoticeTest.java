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

import com.codeborne.selenide.WebElementCondition;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.insight.brain.model.security.MembershipMapping.GLOBAL_CONTEXT_ID;

public class ChangeDefaultAdminPasswordNoticeTest
    extends AbstractFunctionalTest
{
  private static final String[] URLs = new String[]{
    DashboardPage.url(),
    ReportListPage.url(),
    RoleManagementPage.url()
  };

  private UserDAO userDAO;

  @Before
  public void before() {
    userDAO = lookup(UserDAO.class);
    setEnableDefaultPasswordWarning(true);
    refreshOrOpen(DashboardPage.url());
  }

  @After
  public void after() {
    logout();
    setEnableDefaultPasswordWarning(false);
  }

  @Test
  public void testChangeDefaultAdminPasswordNotice_defaultUser() {
    loginAsAdmin();

    DashboardPage.filterToggle().shouldBe(visible).click();

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

    setEnableDefaultPasswordWarning(false);
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
    setEnableDefaultPasswordWarning(false);
    refresh();
    assertNotice(hidden);
  }

  private void assertNotice(WebElementCondition... conditions) {
    ChangeDefaultAdminPasswordNotice notice = new ChangeDefaultAdminPasswordNotice();
    for (String url : URLs) {
      refreshOrOpen(url);
      notice.shouldHave(conditions);
    }
  }
}
