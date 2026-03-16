/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.MainHeader;
import com.sonatype.clm.testing.functional.elements.RoutingErrorBox;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.Condition;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class RoutingErrorTest
    extends AbstractFunctionalTest
{
  private static String invalidUrl() {
    return BaseUrl.resolvePageUrl("/foo");
  }

  @BeforeClass
  public static void startup() {
    refreshOrOpen(OwnerSummaryPage.url());
    loginAsAdmin();
  }

  @Before
  public void before() {
    refreshOrOpen(OwnerSummaryPage.url());
  }

  @Test
  public void validRoutesDoNotShowError() {
    RoutingErrorBox.errorBox().shouldNotBe(Condition.visible);
  }

  @Test
  public void invalidRoutesShowErrorThenHiddenOnOriginalValidRoute() {
    refreshOrOpen(invalidUrl());
    RoutingErrorBox.errorBox().shouldBe(Condition.visible);
    RoutingErrorBox.errorMessage().shouldHave(RoutingErrorBox.errorText("Unknown Address"));

    refreshOrOpen(OwnerSummaryPage.url());
    RoutingErrorBox.errorBox().shouldNotBe(Condition.visible);
  }

  @Test
  public void invalidRoutesShowErrorThenHiddenOnNewValidRoute() {
    refreshOrOpen(invalidUrl());
    RoutingErrorBox.errorBox().shouldBe(Condition.visible);
    MainHeader.menuBar().shouldBe(Condition.visible);
    RoutingErrorBox.errorMessage().shouldHave(RoutingErrorBox.errorText("Unknown Address"));
    eyesWatcher.eyesCheck();

    refreshOrOpen(ReportListPage.url());
    RoutingErrorBox.errorBox().shouldNotBe(Condition.visible);
  }
}
