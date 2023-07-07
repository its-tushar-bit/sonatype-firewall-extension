/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.mtiq;

import com.sonatype.clm.testing.functional.elements.SidebarNavigation;
import com.sonatype.clm.testing.functional.pages.AccessEditorPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;

import org.junit.Before;

public class MultiTenantApplicationAccessEditorTest
    extends AbstractMtiqAccessEditorTest
{
  @Before
  public void init() {
    // note the ȧ being used to force a character to be encoded
    super.init(tempEntity.newApplicationWithParent("test_ȧpp_id", "ApplicationAccessEditorTest app"));
  }

  @Override
  protected void goFromSummaryToAddRole() {
    SidebarNavigation.closeNavigationSidebar();
    OwnerSummaryPage.summaryTile().accessButton().click();
    OwnerSummaryPage.accessTile().addRoleButton().click();
    waitUntilUrl(AccessEditorPage.urlToCreate(currentOwner));
  }
}
