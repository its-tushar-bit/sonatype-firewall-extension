/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage.SummaryTile;
import com.sonatype.insight.brain.model.security.Role;

import org.junit.Before;

public class OrganizationAccessEditorTest
    extends AbstractAccessEditorTest
{

  @Before
  public void init() {
    super.init(tempEntity.newOrganization());
  }

  @Override
  protected void goFromSummaryToAddRole() {
    SummaryTile.accessButton().click();
    SummaryTile.addRoleButton().click();
  }

  @Override
  protected void goFromSummaryToEditRole(Role role) {
    SummaryTile.accessButton().click();
    SummaryTile.localAccessRole(role.getName()).click();
  }
}
