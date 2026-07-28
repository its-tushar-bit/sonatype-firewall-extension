/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests.mtiq;

import com.sonatype.clm.testing.playwright.categories.MtiqTest;

import com.sonatype.clm.testing.playwright.pages.AccessEditorPage;
import com.sonatype.clm.testing.playwright.pages.OwnerSummaryPage;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.security.MembershipMapping;

import org.junit.After;
import org.junit.Before;
import org.junit.experimental.categories.Category;

/**
 * MTIQ migration of the Selenide {@code MultiTenantRepositoriesAccessEditorTest}: exercises the
 * "Add an External Group" flow against the repository-manager container Access editor.
 *
 * <p>
 * The repository container is a shared singleton (not a {@code TemporaryEntity}), so any membership
 * mappings created here must be cleaned up explicitly.
 */
@Category(MtiqTest.class)
public class MultiTenantRepositoriesAccessEditorPlaywrightTest
    extends AbstractMtiqAccessEditorPlaywrightTest
{
  @Before
  public void init() {
    super.init(RepositoryContainer.SINGLETON);
  }

  @After
  public void cleanup() {
    if (membershipMappingDAO == null) {
      return;
    }
    for (MembershipMapping mapping : membershipMappingDAO.getByContextId(RepositoryContainer.REPOSITORY_CONTAINER_ID)) {
      membershipMappingDAO.delete(mapping);
    }
  }

  @Override
  protected String newRoleEditorUrl() {
    return OwnerSummaryPage.editRepositoryContainerUrl(AccessEditorPage.ADD_ACCESS_URL_FRAGMENT);
  }
}
