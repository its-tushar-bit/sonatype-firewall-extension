/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage.SummaryTile;
import com.sonatype.insight.brain.model.security.Role;

import org.junit.Before;

public class ApplicationAccessEditorTest
    extends AbstractAccessEditorTest
{

  @Before
  public void init() {
    //note the ȧ being used to force a character to be encoded
    super.init(tempEntity.newApplicationWithParent("test_ȧpp_id"));
  }

  @Override
  protected void goFromSummaryToAddRole() {
    refresh(); // pills often fail to load CLM-5827
    SummaryTile.accessButton().click();
    SummaryTile.addRoleButton().click();
  }

  @Override
  protected void goFromSummaryToEditRole(Role role) {
    refresh(); // pills often fail to load CLM-5827
    SummaryTile.localAccessRole(role.getName()).click();
  }
}
