/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.ErrorBox;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.ReportListPage;

import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.open;

public abstract class AbstractSummaryViewTest
    extends AbstractFunctionalTest
{
  @BeforeClass
  public static void boot() {
    open(ReportListPage.URL);
    loginAsAdmin();
  }

  public void init() {
    open(OwnerSummaryPage.url(getOwnerType(), getId()));
    OwnerSummaryPage.SummaryTile.name().shouldHave(text(getName()));
  }

  @Test
  public void testSummaryTile() {
    OwnerSummaryPage.SummaryTile.name().shouldBe(visible).shouldHave(text(getName()));
    OwnerSummaryPage.SummaryTile.icon().shouldBe(visible);
  }

  @Test
  public void testSummaryTile_missing() {
    open(OwnerSummaryPage.url(getOwnerType(), "fakeid"));

    ErrorBox error = OwnerSummaryPage.SummaryTile.error();
    error.root().shouldBe(visible);
    error.message().shouldHave(text("unable to locate"));
    error.retryButton().shouldBe(visible, enabled);
  }

  protected abstract String getOwnerType();

  protected abstract String getId();

  protected abstract String getName();

}
