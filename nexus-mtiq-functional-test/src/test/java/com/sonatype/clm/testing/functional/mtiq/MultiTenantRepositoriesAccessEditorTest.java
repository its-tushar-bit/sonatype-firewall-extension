/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.mtiq;

import com.sonatype.clm.testing.functional.pages.AccessEditorPage;
import com.sonatype.clm.testing.functional.pages.RepositoriesSummaryPage;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.security.MembershipMapping;

import org.junit.After;
import org.junit.Before;

import static com.codeborne.selenide.Condition.text;

public class MultiTenantRepositoriesAccessEditorTest
    extends AbstractMtiqAccessEditorTest
{
  @Before
  public void init() {
    super.init(RepositoryContainer.SINGLETON);
  }

  @Override
  protected void shouldBeOnInitialPage() {
    RepositoriesSummaryPage.summaryTile().name().shouldHave(text("Repository Managers"));
  }

  @After
  public void cleanup() {
    MembershipMappingDAO membershipMappingDAO = new MembershipMappingDAO();
    for (MembershipMapping mapping : membershipMappingDAO.getByContextId(RepositoryContainer.REPOSITORY_CONTAINER_ID)) {
      membershipMappingDAO.delete(mapping);
    }
  }

  @Override
  protected void goFromSummaryToAddRole() {
    RepositoriesSummaryPage.accessTile().addRoleButton().click();
    waitUntilUrl(AccessEditorPage.urlToCreate(currentOwner));
  }
}
