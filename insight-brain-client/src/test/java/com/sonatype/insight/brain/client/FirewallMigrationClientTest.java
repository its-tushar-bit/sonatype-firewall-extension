/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import java.util.concurrent.TimeUnit;

import com.sonatype.clm.dto.model.repository.migration.MigrationState;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.testing.AbstractBrainServiceIntegrationTest;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.SimpleAuthentication;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.awaitility.Awaitility.await;

public class FirewallMigrationClientTest
    extends AbstractBrainServiceIntegrationTest
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
  public void testMigrateRepositoryHistory_SourceError() {
    tempEntity.newRepository(targetRepositoryManager, TARGET_REPOSITORY_PUBLIC_ID);
    String sourceManager = "sourceManager";
    String sourceRepository = "sourceRepository";

    assertThatExceptionOfType(HttpResponseException.class)
        .isThrownBy(() -> client.migrateRepositoryHistory(sourceManager, sourceRepository,
            targetRepositoryManager.getInstanceId(), TARGET_REPOSITORY_PUBLIC_ID))
        .withMessage(RepositoryDAO.getErrMsgMissingRepo(sourceManager, sourceRepository))
        .satisfies(e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.SC_NOT_FOUND));
  }

  @Test
  public void testGetRepositoryMigrationState() throws Exception {
    tempEntity.newRepository(targetRepositoryManager, TARGET_REPOSITORY_PUBLIC_ID);
    RepositoryManager sourceRepositoryManager = tempEntity.newRepositoryManager();
    Repository sourceRepository = tempEntity.newRepository(sourceRepositoryManager, "sourceRepository");

    client.migrateRepositoryHistory(sourceRepositoryManager.getInstanceId(), sourceRepository.getPublicId(),
        targetRepositoryManager.getInstanceId(), TARGET_REPOSITORY_PUBLIC_ID);

    await().atMost(10, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(
            client.getRepositoryMigrationState(targetRepositoryManager.getInstanceId(), TARGET_REPOSITORY_PUBLIC_ID)
                .getState()).isEqualTo(MigrationState.COMPLETED));
  }

  @Test
  public void testGetRepositoryMigrationState_Error() {
    assertThatExceptionOfType(HttpResponseException.class)
        .isThrownBy(() -> client.getRepositoryMigrationState(targetRepositoryManager.getInstanceId(),
            TARGET_REPOSITORY_PUBLIC_ID))
        .withMessage(
            RepositoryDAO.getErrMsgMissingRepo(targetRepositoryManager.getInstanceId(), TARGET_REPOSITORY_PUBLIC_ID))
        .satisfies(e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.SC_NOT_FOUND));
  }
}
