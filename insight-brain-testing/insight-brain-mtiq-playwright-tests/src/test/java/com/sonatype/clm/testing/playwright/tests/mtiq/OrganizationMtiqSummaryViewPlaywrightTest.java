/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests.mtiq;

import com.sonatype.clm.testing.playwright.categories.MtiqTest;

import org.junit.Before;
import org.junit.experimental.categories.Category;

@Category(MtiqTest.class)
public class OrganizationMtiqSummaryViewPlaywrightTest
    extends AbstractMtiqSummaryViewPlaywrightTest
{
  @Before
  public void init() {
    super.init(tempEntity.newOrganization(YE_OLE_ORGANIZATION));
  }
}
