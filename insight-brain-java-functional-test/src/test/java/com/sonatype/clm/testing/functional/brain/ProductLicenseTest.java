/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.ProductLicensePage;

import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.matchText;
import static com.codeborne.selenide.Condition.visible;

public class ProductLicenseTest
    extends AbstractFunctionalTest
{
  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(ProductLicensePage.url());
    loginAsAdmin();
  }

  @Test
  public void testLicenseInformation() {
    ProductLicensePage.expiry().shouldBe(visible).should(matchText("[a-zA-Z]+ [0-9]+, 2[0-9]{3}"));
    ProductLicensePage.fingerprint().shouldBe(visible).should(matchText("[0-9a-fA-F]+"));
  }
}
