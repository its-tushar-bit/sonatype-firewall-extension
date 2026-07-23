/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests.mtiq;

import com.sonatype.clm.testing.playwright.categories.MtiqTest;
import com.sonatype.clm.testing.playwright.mtiq.AbstractMtiqUiTest;
import com.sonatype.clm.testing.playwright.pages.OwnerSummaryPage;
import com.sonatype.insight.brain.model.Owner;

import org.junit.Before;
import org.junit.experimental.categories.Category;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@Category(MtiqTest.class)
public abstract class AbstractMtiqSummaryViewPlaywrightTest
    extends AbstractMtiqUiTest
{
  protected static final String YE_OLE_ORGANIZATION = "Ye Ole Organization";

  protected Owner currentOwner;

  @Before
  public void boot() {
    playwrightLoginAdminAt("/");
  }

  protected void init(Owner owner) {
    this.currentOwner = owner;
    playwrightRefreshOrOpen(OwnerSummaryPage.url(owner));
    assertThat(new OwnerSummaryPage().ownerName()).containsText(owner.getName());
  }
}
