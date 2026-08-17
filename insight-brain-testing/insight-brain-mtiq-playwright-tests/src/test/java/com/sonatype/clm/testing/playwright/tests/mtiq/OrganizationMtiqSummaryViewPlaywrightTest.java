/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests.mtiq;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;

@Tag("mtiq")
public class OrganizationMtiqSummaryViewPlaywrightTest
    extends AbstractMtiqSummaryViewPlaywrightTest
{
  @BeforeEach
  public void init() {
    super.init(tempEntity.newOrganization(YE_OLE_ORGANIZATION));
  }
}
