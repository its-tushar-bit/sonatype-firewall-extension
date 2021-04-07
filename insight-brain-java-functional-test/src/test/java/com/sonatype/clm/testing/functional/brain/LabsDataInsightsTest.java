/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.LabsDataInsightsPage;

import org.junit.Test;

import static com.codeborne.selenide.Condition.visible;

public class LabsDataInsightsTest
    extends AbstractFunctionalTest
{
  @Test
  public void directUrlAccessWithNoPermissions() {
    refreshOrOpen(LabsDataInsightsPage.url());
    loginAsAdmin();

    LabsDataInsightsPage.noPermissions().shouldBe(visible);
  }
}
