/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.evaluation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.evaluation.EvaluationQueue;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@PostgresTest
public class EvaluationQueueDAOPostgresTest
    extends EvaluationQueueDAOTest
{
  @Override
  @Test
  public void testAcquireRows_Contention() throws Exception {
    Application application1 = tempEntity.newApplicationWithParent();
    tempEntity.newEvaluationQueue(3, application1.getId(), BuildStageType.ID, "1.0.0", new Date(100), new Date(100),
        null);
    tempEntity.newEvaluationQueue(1, application1.getId(), BuildStageType.ID, "2.0.0", new Date(200), new Date(200),
        null);
    tempEntity.newEvaluationQueue(2, application1.getId(), BuildStageType.ID, "3.0.0", new Date(300), new Date(300),
        null);

    CountDownLatch locksAcquiredByContender = new CountDownLatch(1);
    CountDownLatch locksAcquiredByWorker = new CountDownLatch(1);

    Thread lockingThread = new Thread(() -> {
      try (Connection conn = databaseRule.getOperationalDataStore().getDataSource().getConnection()) {
        conn.setAutoCommit(false);
        try (PreparedStatement ps = conn.prepareStatement(
            "SELECT * FROM " + OperationalDataStore.ID + ".evaluation_queue" +
                " WHERE version IN ('1.0.0', '2.0.0')" +
                " FOR UPDATE"))
        {
          ps.executeQuery();
        }
        locksAcquiredByContender.countDown();
        locksAcquiredByWorker.await(2, TimeUnit.SECONDS);
        conn.rollback();
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    });

    lockingThread.start();
    locksAcquiredByContender.await();

    List<EvaluationQueue> acquired = dao.acquireRows("worker", 3);

    locksAcquiredByWorker.countDown();
    lockingThread.join();
    assertThat(acquired).hasSize(1)
        .extracting(EvaluationQueue::getVersion)
        .containsExactly("3.0.0");
  }
}
