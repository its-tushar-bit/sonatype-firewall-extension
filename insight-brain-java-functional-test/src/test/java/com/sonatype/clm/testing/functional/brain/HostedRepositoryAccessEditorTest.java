/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.pages.AccessEditorPage;
import com.sonatype.clm.testing.functional.pages.RepositoriesSummaryPage;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Role;

import org.junit.After;
import org.junit.Before;

import static com.codeborne.selenide.Condition.text;

public class HostedRepositoryAccessEditorTest
    extends AbstractAccessEditorTest
{
  @Before
  public void init() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newHostedRepository(repositoryManager, "npm-hosted", "npm", true);
    super.init(repository);
  }

  @After
  public void cleanup() {
    for (MembershipMapping mapping : membershipMappingDAO.getByContextId(currentOwner.getId())) {
      membershipMappingDAO.delete(mapping);
    }
  }

  @Override
  protected void shouldBeOnInitialPage() {
    RepositoriesSummaryPage.summaryTile().name().shouldHave(text(currentOwner.getName()));
  }

  @Override
  protected void goFromSummaryToAddRole() {
    RepositoriesSummaryPage.accessTile().addRoleButton().click();
    waitUntilUrl(AccessEditorPage.urlToCreate(currentOwner));
  }

  @Override
  protected void goFromSummaryToEditRole(Role role) {
    RepositoriesSummaryPage.accessTile().localAccessRole(role.getName()).click();
    waitUntilUrl(AccessEditorPage.urlToEdit(currentOwner, role.getId()));
  }
}
