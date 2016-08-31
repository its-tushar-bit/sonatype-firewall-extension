/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.DashboardPage;

import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Selenide.open;

public class DashboardViolationsTest
    extends AbstractFunctionalTest
{
  @BeforeClass
  public static void beforeClass() {
    open(DashboardPage.URL);
    loginAsAdmin();
  }

  @Test
  public void testNewestRiskRedirectsToViolations() {
    refreshOrOpen(DashboardPage.NEWEST_RISK_URL);
    waitUntilUrl(DashboardPage.VIOLATIONS_URL);
  }
}
