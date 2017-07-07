/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.sonatype.clm.dto.model.repository.migration.MigrationState;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.service.AbstractBrainServiceTest;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.SimpleAuthentication;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.awaitility.core.ThrowingRunnable;
import org.junit.Before;
import org.junit.Test;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@org.junit.Ignore("To be re-enabled by CLM-8310")
public class FirewallMigrationClientTest
    extends AbstractBrainServiceTest
{
  private static final String TARGET_REPOSITORY_PUBLIC_ID = "central";

  private RepositoryManager targetRepositoryManager;

  private FirewallMigrationClient client;

  @Before
  public void start() {
    targetRepositoryManager = tempEntity.newRepositoryManager();
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
    tempEntity.newRepository(targetRepositoryManager, TARGET_REPOSITORY_PUBLIC_ID);
    RepositoryManager sourceRepositoryManager = tempEntity.newRepositoryManager();
    Repository sourceRepository = tempEntity.newRepository(sourceRepositoryManager, "sourceRepository");

    client.migrateRepositoryHistory(sourceRepositoryManager.getInstanceId(), sourceRepository.getPublicId(),
        targetRepositoryManager.getInstanceId(), TARGET_REPOSITORY_PUBLIC_ID);
  }

  @Test
  public void testMigrateRepositoryHistory_SourceError() throws Exception {
    tempEntity.newRepository(targetRepositoryManager, TARGET_REPOSITORY_PUBLIC_ID);
    String sourceManager = "sourceManager";
    String sourceRepository = "sourceRepository";

    try {
      client.migrateRepositoryHistory(sourceManager, sourceRepository, targetRepositoryManager.getInstanceId(),
          TARGET_REPOSITORY_PUBLIC_ID);
      fail("Expected exception");
    }
    catch (HttpResponseException e) {
      assertEquals(HttpStatus.SC_NOT_FOUND, e.getStatusCode());
      assertEquals(RepositoryDAO.getErrMsgMissingRepo(sourceManager, sourceRepository), e.getMessage());
    }
  }

  @Test
  public void testGetRepositoryMigrationState() throws Exception {
    tempEntity.newRepository(targetRepositoryManager, TARGET_REPOSITORY_PUBLIC_ID);
    RepositoryManager sourceRepositoryManager = tempEntity.newRepositoryManager();
    Repository sourceRepository = tempEntity.newRepository(sourceRepositoryManager, "sourceRepository");

    client.migrateRepositoryHistory(sourceRepositoryManager.getInstanceId(), sourceRepository.getPublicId(),
        targetRepositoryManager.getInstanceId(), TARGET_REPOSITORY_PUBLIC_ID);

    await().atMost(10, TimeUnit.SECONDS).untilAsserted(new ThrowingRunnable()
    {
      @Override
      public void run() throws IOException {
        assertEquals(
            client.getRepositoryMigrationState(targetRepositoryManager.getInstanceId(), TARGET_REPOSITORY_PUBLIC_ID)
                .getState(),
            MigrationState.COMPLETED);
      }
    });
  }

  @Test
  public void testGetRepositoryMigrationState_Error() throws Exception {
    try {
      client.getRepositoryMigrationState(targetRepositoryManager.getInstanceId(), TARGET_REPOSITORY_PUBLIC_ID);
      fail("Expected exception");
    }
    catch (HttpResponseException e) {
      assertEquals(HttpStatus.SC_NOT_FOUND, e.getStatusCode());
      assertEquals(
          RepositoryDAO.getErrMsgMissingRepo(targetRepositoryManager.getInstanceId(), TARGET_REPOSITORY_PUBLIC_ID),
          e.getMessage());
    }
  }
}
