/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.pages.RepositoriesSummaryPage.SummaryTile;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Role;

import org.junit.Before;
import org.junit.BeforeClass;

public class RepositoriesAccessEditorTest
    extends AbstractAccessEditorTest
{
  @Before
  public void init() {
    super.init(RepositoryContainer.SINGLETON);
  }

  protected void goFromSummaryToAddRole() {
    SummaryTile.addRoleButton().click();
  }

  protected void goFromSummaryToEditRole(Role role) {
    refresh();
    SummaryTile.localAccessRole(role.getName()).click();
  }
}
