/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import java.util.List;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @since 1.17
 */
public class RepositoryServiceTest
    extends AbstractComponentTest
{
  private static final String MANUAL_REPO_MAN_INSTANCE_ID = "manualDeleteRepoManagerInstanceId";

  private static final String REPO_MAN_INSTANCE_ID = "repoManagerInstanceId";

  private static final String REPO_PUBLIC_ID = "repoId";

  @Inject
  private RepositoryService repositoryService;

  private RepositoryManagerDAO repositoryManagerDAO = new RepositoryManagerDAO();

  private RepositoryDAO repositoryDAO = new RepositoryDAO();

  @After
  public void cleanup() {
    RepositoryManager repositoryManager = repositoryManagerDAO.getByInstanceId(MANUAL_REPO_MAN_INSTANCE_ID);
    if (repositoryManager != null) {
      repositoryManagerDAO.delete(repositoryManager);
    }
  }

  @Test
  public void testEnableRepository_noRepositoryManager() throws Exception {
    repositoryService.enableRepository(MANUAL_REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    RepositoryManager repositoryManager = repositoryManagerDAO.getByInstanceId(MANUAL_REPO_MAN_INSTANCE_ID);

    assertNotNull(repositoryManager);

    List<Repository> repositories = repositoryDAO.getByRepositoryManagerId(repositoryManager.getId());

    assertEquals(1, repositories.size());
    assertEquals(REPO_PUBLIC_ID, repositories.get(0).getPublicId());
    assertTrue(repositories.get(0).isEnabled());
  }

  @Test
  public void testEnableRepository_existingRepositoryManager() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);

    repositoryService.enableRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    List<Repository> repositories = repositoryDAO.getByRepositoryManagerId(repositoryManager.getId());

    assertEquals(1, repositories.size());
    assertEquals(REPO_PUBLIC_ID, repositories.get(0).getPublicId());
    assertTrue(repositories.get(0).isEnabled());
  }

  @Test
  public void testEnableRepository_existingRepository() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, false);

    repositoryService.enableRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    List<Repository> repositories = repositoryDAO.getByRepositoryManagerId(repositoryManager.getId());

    assertEquals(1, repositories.size());
    assertEquals(REPO_PUBLIC_ID, repositories.get(0).getPublicId());
    assertTrue(repositories.get(0).isEnabled());
  }
}
