/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.insight.brain.model.Organization;

import org.junit.Before;

public class OrganizationSummaryViewTest
    extends AbstractSummaryViewTest
{

  private static final String YE_OLE_ORGANIZATION = "Ye Ole Organization";

  private Organization organization;

  @Before
  public void init() {
    organization = tempEntity.newOrganization(YE_OLE_ORGANIZATION);
    super.init();
  }

  @Override
  protected String getName() {
    return YE_OLE_ORGANIZATION;
  }

  @Override
  protected String getOwnerType() {
    return "organization";
  }

  @Override
  protected String getId() {
    return organization.getId();
  }
}
