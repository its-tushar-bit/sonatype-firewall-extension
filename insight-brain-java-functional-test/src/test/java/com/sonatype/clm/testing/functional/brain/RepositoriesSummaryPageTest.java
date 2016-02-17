/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.RepositoriesSummaryPage;
import com.sonatype.clm.testing.functional.pages.RepositoriesSummaryPage.SummaryTile;

import com.codeborne.selenide.SelenideElement;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.open;

public class RepositoriesSummaryPageTest
    extends AbstractFunctionalTest
{
  @BeforeClass
  public static void startup() {
    open(RepositoriesSummaryPage.URL);
    loginAsAdmin();
  }

  @Test
  public void testRepositorySummaryView()
  {
    SelenideElement nameElement = SummaryTile.name();
    nameElement.isDisplayed();
    nameElement.shouldHave(text("Repositories"));
    SummaryTile.configButton().isDisplayed();
    SummaryTile.accessButton().isDisplayed();
  }
}
