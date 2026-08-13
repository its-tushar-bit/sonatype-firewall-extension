/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.policy.ProxyRepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.model.MigrationTracker;
import com.sonatype.insight.brain.model.policy.ProxyRepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.repository.InactiveRepositoryViolationCleaner.InactiveRepositoryViolationCleanerWorker;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

@ComponentH2Test
public class InactiveRepositoryViolationCleanerTest
    extends AbstractComponentH2Test
{
  @Inject
  private InactiveRepositoryViolationCleaner inactiveRepositoryViolationCleaner;

  @Inject
  private MigrationTrackerDAO migrationTrackerDAO;

  @Inject
  private ProxyRepositoryPolicyViolationDAO proxyRepositoryPolicyViolationDAO;

  @Test
  public void testStart() throws Exception {
    migrationTrackerDAO.deleteById(InactiveRepositoryViolationCleaner.MIGRATION_ID);
    Repository repository = tempEntity.newRepository();
    for (int i = 0; i < InactiveRepositoryViolationCleaner.BATCH_SIZE + 1; i++) {
      newInactiveViolation(repository);
    }
    ProxyRepositoryPolicyViolation activeViolation = tempEntity.newRepositoryPolicyViolation(repository.getId());

    inactiveRepositoryViolationCleaner.start();

    assertThat(inactiveRepositoryViolationCleaner.workerThread).isNotNull();
    inactiveRepositoryViolationCleaner.workerThread.join(5000);
    assertThat(proxyRepositoryPolicyViolationDAO.getById(activeViolation.getId())).isNotNull();
    assertThat(getInactiveViolationCount(repository)).isEqualTo(0);
    assertThat(migrationTrackerDAO.isTrackerPresent(InactiveRepositoryViolationCleaner.MIGRATION_ID)).isTrue();
  }

  @Test
  public void testStart_AlreadyMigrated() throws Exception {
    if (!migrationTrackerDAO.isTrackerPresent(InactiveRepositoryViolationCleaner.MIGRATION_ID)) {
      migrationTrackerDAO.insert(new MigrationTracker(InactiveRepositoryViolationCleaner.MIGRATION_ID));
    }
    Repository repository = tempEntity.newRepository();
    ProxyRepositoryPolicyViolation inactiveViolation = newInactiveViolation(repository);

    inactiveRepositoryViolationCleaner.start();

    assertThat(inactiveRepositoryViolationCleaner.workerThread).isNull();
    assertThat(proxyRepositoryPolicyViolationDAO.getById(inactiveViolation.getId())).isNotNull();
  }

  @Test
  public void testRun_DisallowConcurrentExecution() throws Exception {
    InactiveRepositoryViolationCleanerWorker spyInactiveRepositoryViolationCleanerWorker =
        spy(inactiveRepositoryViolationCleaner.new InactiveRepositoryViolationCleanerWorker());
    Callable<Void> callable = () -> {
      spyInactiveRepositoryViolationCleanerWorker.run();
      return null;
    };
    Consumer<Answer<Void>> answerConsumer = answer -> {
      try {
        doAnswer(answer).when(spyInactiveRepositoryViolationCleanerWorker).doDeleteInactiveRepositoryPolicyViolations();
      }
      catch (Exception e) {
        throw new RuntimeException(e.getMessage(), e);
      }
    };
    testCallable_DisallowConcurrentExecution(callable, answerConsumer);
  }

  private ProxyRepositoryPolicyViolation newInactiveViolation(Repository repository) throws SQLException {
    ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId());
    String databaseSchema = databaseContainerRule.getOperationalDataStore().getDatabaseSchema();
    try (Connection connection = databaseContainerRule.getOperationalDataStore().getDataSource().getConnection();
        PreparedStatement preparedStatement = connection.prepareStatement("UPDATE " + databaseSchema +
            ".proxy_repository_policy_violation SET active=false WHERE proxy_repository_policy_violation_id=?"))
    {
      connection.setAutoCommit(true);
      preparedStatement.setString(1, proxyRepositoryPolicyViolation.getId());
      int updated = preparedStatement.executeUpdate();
      assertThat(updated).isEqualTo(1);
    }
    return proxyRepositoryPolicyViolation;
  }

  private int getInactiveViolationCount(Repository repository) throws SQLException {
    String databaseSchema = databaseContainerRule.getOperationalDataStore().getDatabaseSchema();
    try (Connection connection = databaseContainerRule.getOperationalDataStore().getDataSource().getConnection();
        PreparedStatement preparedStatement = connection.prepareStatement(
            "SELECT COUNT(*) FROM " + databaseSchema +
                ".proxy_repository_policy_violation WHERE repository_id=? AND active=false"))
    {
      preparedStatement.setString(1, repository.getId());
      try (ResultSet resultSet = preparedStatement.executeQuery()) {
        resultSet.next();
        return resultSet.getInt(1);
      }
    }
  }
}
