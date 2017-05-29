/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import com.sonatype.clm.dto.model.repository.migration.MigrationState;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.service.AbstractBrainServiceTest;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.SimpleAuthentication;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class FirewallMigrationClientTest
    extends AbstractBrainServiceTest
{
  private static final String REPOSITORY_PUBLIC_ID = "central";

  private RepositoryManager repositoryManager;

  private FirewallMigrationClient client;

  @Before
  public void start() {
    repositoryManager = tempEntity.newRepositoryManager();
    Configuration configuration = getCLMServer().getClientConfiguration();
    SimpleAuthentication auth = new SimpleAuthentication();
    auth.setPassword("admin123");
    auth.setUsername("admin");
    configuration.setServerAuth(auth);
    client = new FirewallMigrationClient(configuration);
  }

  @Test
  public void testVerifyMigrationSupport() throws Exception {
    client.verifyMigrationSupport("v1");
  }

  @Test
  public void testMigrateRepositoryHistory() throws Exception {
    tempEntity.newRepository(repositoryManager, REPOSITORY_PUBLIC_ID);
    RepositoryManager sourceRepositoryManager = tempEntity.newRepositoryManager();
    Repository sourceRepository = tempEntity.newRepository(sourceRepositoryManager, "sourceRepository");

    client.migrateRepositoryHistory(repositoryManager.getInstanceId(), REPOSITORY_PUBLIC_ID,
        sourceRepositoryManager.getInstanceId(), sourceRepository.getPublicId(), "pathname");
  }

  @Test
  public void testMigrateRepositoryHistory_Error() throws Exception {
    try {
      client.migrateRepositoryHistory(repositoryManager.getInstanceId(), REPOSITORY_PUBLIC_ID, "sourceManager",
          "sourceRepository", "pathname");
      fail("Expected exception");
    }
    catch (HttpResponseException e) {
      assertEquals(HttpStatus.SC_NOT_FOUND, e.getStatusCode());
      assertEquals(RepositoryDAO.getErrMsgMissingRepo(repositoryManager.getInstanceId(), REPOSITORY_PUBLIC_ID),
          e.getMessage());
    }
  }

  @Test
  public void testMigrateRepositoryHistory_SourceError() throws Exception {
    tempEntity.newRepository(repositoryManager, REPOSITORY_PUBLIC_ID);
    String sourceManager = "sourceManager";
    String sourceRepository = "sourceRepository";

    try {
      client.migrateRepositoryHistory(repositoryManager.getInstanceId(), REPOSITORY_PUBLIC_ID, sourceManager,
          sourceRepository, "pathname");
      fail("Expected exception");
    }
    catch (HttpResponseException e) {
      assertEquals(HttpStatus.SC_NOT_FOUND, e.getStatusCode());
      assertEquals(RepositoryDAO.getErrMsgMissingRepo(sourceManager, sourceRepository), e.getMessage());
    }
  }

  @Test
  public void testGetRepositoryMigrationState() throws Exception {
    tempEntity.newRepository(repositoryManager, REPOSITORY_PUBLIC_ID);

    MigrationState migrationState = client
        .getRepositoryMigrationState(repositoryManager.getInstanceId(), REPOSITORY_PUBLIC_ID);
    assertThat(migrationState, is(MigrationState.COMPLETED));
  }

  @Test
  public void testGetRepositoryMigrationState_Error() throws Exception {
    try {
      client.getRepositoryMigrationState(repositoryManager.getInstanceId(), REPOSITORY_PUBLIC_ID);
      fail("Expected exception");
    }
    catch (HttpResponseException e) {
      assertEquals(HttpStatus.SC_NOT_FOUND, e.getStatusCode());
      assertEquals(RepositoryDAO.getErrMsgMissingRepo(repositoryManager.getInstanceId(), REPOSITORY_PUBLIC_ID),
          e.getMessage());
    }
  }
}
