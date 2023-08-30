/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.mtiq;

import org.junit.Before;

public class OrganizationMtiqSummaryViewTest
    extends AbstractMtiqSummaryViewTest
{
  @Before
  public void init() {
    super.init(tempEntity.newOrganization(YE_OLE_ORGANIZATION));
  }
}
