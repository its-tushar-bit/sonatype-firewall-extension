/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.configuration;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.MainHeader;
import com.sonatype.clm.testing.functional.elements.SystemConfigMenu;
import com.sonatype.clm.testing.functional.pages.DashboardPage;

import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.visible;

public class SystemConfigurationMenuTest
    extends AbstractFunctionalTest
{
  @BeforeClass
  public static void startup() {
    refreshOrOpen(DashboardPage.URL);
    loginAsAdmin();
  }

  @Test
  public void menuEntriesAppear() {
    SystemConfigMenu systemConfigMenu = MainHeader.systemConfigMenu();

    systemConfigMenu.shouldBe(visible);

    systemConfigMenu.dropdownToggle().click();

    systemConfigMenu.users().shouldBe(visible);

    systemConfigMenu.webhooks().shouldBe(visible);

    systemConfigMenu.successMetrics().shouldBe(visible);
  }
}
