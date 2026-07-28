/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests.mtiq;

import com.sonatype.clm.testing.playwright.categories.MtiqTest;

import com.sonatype.insight.brain.model.Application;

import org.junit.Before;
import org.junit.experimental.categories.Category;

@Category(MtiqTest.class)
public class ApplicationMtiqSummaryViewPlaywrightTest
    extends AbstractMtiqSummaryViewPlaywrightTest
{
  private static final String YE_OLE_APPLICATION = "Ye Ole Application";

  @Before
  public void init() {
    // The "ȧpp" suffix intentionally forces a non-ASCII character into the public id.
    Application application = tempEntity.newApplicationWithParent(getClass().getSimpleName() + "ȧpp",
        YE_OLE_APPLICATION, YE_OLE_ORGANIZATION);
    super.init(application);
  }
}
