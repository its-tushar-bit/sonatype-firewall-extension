/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.mtiq.user;

import com.sonatype.clm.testing.functional.mtiq.AbstractMtiqFunctionalTest;
import com.sonatype.clm.testing.functional.mtiq.pages.MtiqUserManagementPage;

import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.Condition.visible;

public class MtiqUserManagementTest
    extends AbstractMtiqFunctionalTest
{
  @Before
  public void initialLogin() {
    refreshOrOpen(MtiqUserManagementPage.url());
    loginAsAdmin();
  }

  @Test
  public void testPageLoad() {
    new MtiqUserManagementPage().shouldBe(visible);
  }

  // TODO: flesh out with more tests in a separate PR
}
