/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.evaluation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.evaluation.EvaluationQueue;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ComplianceStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;

import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.ACTIVE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.tuple;

public class EvaluationQueueDAOTest
    extends AbstractDbDAOTest
{
  protected EvaluationQueueDAO dao;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createEvaluationQueueDAO();
  }

  @Test
  public void testCRUD() {
    Application application1 = tempEntity.newApplicationWithParent();
    Application application2 = tempEntity.newApplicationWithParent();

    EvaluationQueue evaluationQueue = new EvaluationQueue();
    evaluationQueue.setPriority(1);
    evaluationQueue.setApplicationId(application1.getId());
    evaluationQueue.setStageTypeId(BuildStageType.ID);
    evaluationQueue.setVersion("1.0.0");
    evaluationQueue.setCreateTime(new Date(0));
    evaluationQueue.setUpdateTime(new Date(1));
    evaluationQueue.setWorkerId("worker1");

    dao.insert(evaluationQueue);

    assertThat(evaluationQueue.getId()).isNotNull();

    EvaluationQueue stored = dao.getById(evaluationQueue.getId());

    assertThat(stored).usingRecursiveComparison().ignoringFields(JPA.IGNORE_FIELDS).isEqualTo(evaluationQueue);

    evaluationQueue.setPriority(2);
    evaluationQueue.setApplicationId(application2.getId());
    evaluationQueue.setStageTypeId(ReleaseStageType.ID);
    evaluationQueue.setVersion("2.0.0");
    evaluationQueue.setCreateTime(new Date(3));
    evaluationQueue.setUpdateTime(new Date(4));
    evaluationQueue.setWorkerId("worker2");

    dao.update(evaluationQueue);

    stored = dao.getById(evaluationQueue.getId());

    assertThat(stored).usingRecursiveComparison().ignoringFields(JPA.IGNORE_FIELDS).isEqualTo(evaluationQueue);

    dao.delete(evaluationQueue);

    assertThat(dao.getById(evaluationQueue.getId())).isNull();
  }

  @Test
  public void testGetAll_ordersByPriority() {
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newEvaluationQueue(3, application.getId(), BuildStageType.ID, "1.0.0", new Date(0), new Date(1), null);
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "2.0.0", new Date(0), new Date(1), null);
    tempEntity.newEvaluationQueue(2, application.getId(), BuildStageType.ID, "3.0.0", new Date(0), new Date(1), null);

    assertThat(dao.getAll()).map(EvaluationQueue::getVersion).containsExactly("2.0.0", "3.0.0", "1.0.0");
  }

  @Test
  public void testUniqueConstraint() {
    Application application1 = tempEntity.newApplicationWithParent();
    Application application2 = tempEntity.newApplicationWithParent();

    tempEntity.newEvaluationQueue(1, application1.getId(), BuildStageType.ID, "1.0.0", new Date(0), new Date(1), null);

    // Different app is ok
    tempEntity.newEvaluationQueue(1, application2.getId(), BuildStageType.ID, "1.0.0", new Date(0), new Date(1), null);
    // Different stage is ok
    tempEntity.newEvaluationQueue(1, application1.getId(), ReleaseStageType.ID, "1.0.0", new Date(0), new Date(1),
        null);
    // Different version is ok
    tempEntity.newEvaluationQueue(1, application1.getId(), BuildStageType.ID, "2.0.0", new Date(0), new Date(1), null);

    // Same app, stage, and version is not ok
    assertThatExceptionOfType(org.jooq.exception.IntegrityConstraintViolationException.class).isThrownBy(
        () -> tempEntity.newEvaluationQueue(1, application1.getId(), BuildStageType.ID, "1.0.0", new Date(2),
            new Date(3),
            "worker"));
  }

  @Test
  public void testAcquireRows_NullWorkerId() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> dao.acquireRows(null, null))
        .withMessageContaining("workerId cannot be null.");
  }

  @Test
  public void testAcquireRows_NegativeLimit() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> dao.acquireRows("worker", -1))
        .withMessageContaining("limit must be positive.");
  }

  @Test
  public void testAcquireRows_ZeroLimit() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> dao.acquireRows("worker", 0))
        .withMessageContaining("limit must be positive.");
  }

  @Test
  public void testAcquireRows_NoRows() {
    assertThat(dao.acquireRows("worker1", null)).isEmpty();
  }

  @Test
  public void testAcquireRows_AllAvailable() {
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "1.0.0", new Date(0), new Date(0), null);
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "2.0.0", new Date(1), new Date(1), null);

    List<EvaluationQueue> acquired = dao.acquireRows("worker1", null);

    assertThat(acquired).hasSize(2)
        .extracting(EvaluationQueue::getVersion)
        .containsExactlyInAnyOrder("1.0.0", "2.0.0");
    assertThat(acquired).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(acquired.stream().map(EvaluationQueue::getId).map(dao::getById).toList());
  }

  @Test
  public void testAcquireRows_Limit() {
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "1.0.0", new Date(0), new Date(0), null);
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "2.0.0", new Date(1), new Date(1), null);

    List<EvaluationQueue> acquired = dao.acquireRows("worker1", 1);

    assertThat(acquired).hasSize(1)
        .extracting(EvaluationQueue::getVersion)
        .containsExactly("1.0.0");
    assertThat(acquired).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(acquired.stream().map(EvaluationQueue::getId).map(dao::getById).toList());
  }

  @Test
  public void testAcquireRows_SomeAlreadyTaken() {
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "1.0.0", new Date(0), new Date(0),
        "worker2");
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "2.0.0", new Date(1), new Date(1), null);

    List<EvaluationQueue> acquired = dao.acquireRows("worker1", null);

    assertThat(acquired).hasSize(1)
        .extracting(EvaluationQueue::getVersion)
        .containsExactly("2.0.0");
    assertThat(acquired).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(acquired.stream().map(EvaluationQueue::getId).map(dao::getById).toList());
  }

  @Test
  public void testAcquireRows_SomeAlreadyTakenBySelf() {
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "1.0.0", new Date(0), new Date(0),
        "worker1");
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "2.0.0", new Date(1), new Date(1), null);

    List<EvaluationQueue> acquired = dao.acquireRows("worker1", null);

    assertThat(acquired).hasSize(1)
        .extracting(EvaluationQueue::getVersion)
        .containsExactly("2.0.0");
    assertThat(acquired).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(acquired.stream().map(EvaluationQueue::getId).map(dao::getById).toList());
  }

  @Test
  public void testAcquireRows_AllAlreadyTaken() {
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "1.0.0", new Date(0), new Date(0),
        "worker2");
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "2.0.0", new Date(1), new Date(1),
        "worker3");

    assertThat(dao.acquireRows("worker1", null)).isEmpty();
  }

  @Test
  public void testAcquireRows_OrdersByPriorityAscending() {
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newEvaluationQueue(3, application.getId(), BuildStageType.ID, "1.0.0", new Date(300), new Date(300),
        null);
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "2.0.0", new Date(100), new Date(100),
        null);
    tempEntity.newEvaluationQueue(2, application.getId(), BuildStageType.ID, "3.0.0", new Date(200), new Date(200),
        null);

    List<EvaluationQueue> acquired = dao.acquireRows("worker1", 3);

    assertThat(acquired).extracting(EvaluationQueue::getVersion).containsExactly("2.0.0", "3.0.0", "1.0.0");
    assertThat(acquired).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(acquired.stream().map(EvaluationQueue::getId).map(dao::getById).toList());
  }

  @Test
  public void testAcquireRows_Contention() throws Exception {
    // H2 1.4.196 uses table-level locks, not row-level locks
    // When one connection holds a table lock, other connections must wait

    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "1.0.0", new Date(100), new Date(100),
        null);
    tempEntity.newEvaluationQueue(2, application.getId(), BuildStageType.ID, "2.0.0", new Date(200), new Date(200),
        null);
    tempEntity.newEvaluationQueue(3, application.getId(), BuildStageType.ID, "3.0.0", new Date(300), new Date(300),
        null);

    CountDownLatch tableLockAcquired = new CountDownLatch(1);
    CountDownLatch workerStarted = new CountDownLatch(1);

    // Thread that holds a table lock on evaluation_queue
    Thread lockingThread = new Thread(() -> {
      try (Connection conn = databaseRule.getOperationalDataStore().getDataSource().getConnection()) {
        conn.setAutoCommit(false);
        // H2 acquires table lock with FOR UPDATE
        try (PreparedStatement ps = conn.prepareStatement(
            "SELECT * FROM " + OperationalDataStore.ID + ".evaluation_queue" +
                " FOR UPDATE"))
        {
          ps.executeQuery();
        }
        tableLockAcquired.countDown();
        // Wait for worker to start, then hold lock briefly
        workerStarted.await(2, TimeUnit.SECONDS);
        Thread.sleep(500); // Hold lock for 500ms
        conn.rollback();
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    });

    // Thread that tries to acquire rows while lock is held
    List<EvaluationQueue>[] acquiredHolder = new List[1];
    Thread workerThread = new Thread(() -> {
      try {
        workerStarted.countDown();
        // This should block until lockingThread releases the table lock
        acquiredHolder[0] = dao.acquireRows("worker", 3);
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    });

    lockingThread.start();
    tableLockAcquired.await();

    // Start worker thread - it will block on the table lock
    workerThread.start();

    // Wait for both threads to complete
    lockingThread.join(5000);
    workerThread.join(5000);

    // Worker should have successfully acquired all rows after waiting for the lock
    assertThat(acquiredHolder[0]).hasSize(3)
        .extracting(EvaluationQueue::getVersion)
        .containsExactly("1.0.0", "2.0.0", "3.0.0");
  }

  @Test
  public void testAcquireRows_Updates_UpdateTime() {
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "1.0.0", new Date(0), new Date(0), null);
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "2.0.0", new Date(1), new Date(1), null);
    Date now = new Date();

    List<EvaluationQueue> acquired = dao.acquireRows("worker1", null);

    assertThat(acquired)
        .map(EvaluationQueue::getUpdateTime)
        .allSatisfy(updateTime -> assertThat(updateTime).isAfterOrEqualTo(now));
    assertThat(acquired).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(acquired.stream().map(EvaluationQueue::getId).map(dao::getById).toList());
  }

  @Test
  public void testClearExpiredWorkerIds_ZeroExpiration() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> dao.clearExpiredWorkerIds(0))
        .withMessageContaining("expirationMillis must be positive.");
  }

  @Test
  public void testClearExpiredWorkerIds_NegativeExpiration() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> dao.clearExpiredWorkerIds(-1))
        .withMessageContaining("expirationMillis must be positive.");
  }

  @Test
  public void testClearExpiredWorkerIds_NoExpiredWorkers() {
    Application application = tempEntity.newApplicationWithParent();
    Date now = new Date();
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "1.0.0", now, now, "worker1");
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "2.0.0", now, now, "worker2");

    dao.clearExpiredWorkerIds(5000);

    assertThat(dao.getAll()).map(EvaluationQueue::getWorkerId).containsOnly("worker1", "worker2");
  }

  @Test
  public void testClearExpiredWorkerIds_ClearsExpiredWorkers() {
    Application application = tempEntity.newApplicationWithParent();
    Date oldTime = new Date(System.currentTimeMillis() - 10000);
    Date recentTime = new Date();
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "1.0.0", oldTime, oldTime, "worker1");
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "2.0.0", oldTime, oldTime, "worker2");
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "3.0.0", recentTime, recentTime,
        "worker3");

    dao.clearExpiredWorkerIds(5000);

    assertThat(dao.getAll()).map(EvaluationQueue::getWorkerId).containsOnly(null, "worker3");
  }

  @Test
  public void testClearExpiredWorkerIds_WorkerWithRecentActivity() {
    Application application = tempEntity.newApplicationWithParent();
    Date oldTime = new Date(System.currentTimeMillis() - 10000);
    Date recentTime = new Date();
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "1.0.0", oldTime, oldTime, "worker1");
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "2.0.0", recentTime, recentTime,
        "worker1");

    dao.clearExpiredWorkerIds(5000);

    assertThat(dao.getAll()).map(EvaluationQueue::getWorkerId).containsOnly("worker1");
  }

  @Test
  public void testClearExpiredWorkerIds_Updates_UpdateTime() {
    Application application = tempEntity.newApplicationWithParent();
    Date oldTime = new Date(System.currentTimeMillis() - 10000);
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "1.0.0", oldTime, oldTime, "worker1");
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "2.0.0", oldTime, oldTime, "worker2");
    Date now = new Date();

    dao.clearExpiredWorkerIds(5000);

    assertThat(dao.getAll())
        .map(EvaluationQueue::getUpdateTime)
        .allSatisfy(updateTime -> assertThat(updateTime).isAfterOrEqualTo(now));
  }

  @Test
  public void testGetMaxPriority_noRows() {
    Integer maxPriority = dao.getMaxPriority();
    assertThat(maxPriority).isNull();
  }

  @Test
  public void testGetMaxPriority_multipleRows() {
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newEvaluationQueue(100, application.getId(), BuildStageType.ID, "1.0.0", new Date(), new Date(), null);
    tempEntity.newEvaluationQueue(500, application.getId(), BuildStageType.ID, "2.0.0", new Date(), new Date(), null);
    tempEntity.newEvaluationQueue(250, application.getId(), BuildStageType.ID, "3.0.0", new Date(), new Date(), null);

    long maxPriority = dao.getMaxPriority();
    assertThat(maxPriority).isEqualTo(500);
  }

  @Test
  public void testGetMaxPriority_singleRow() {
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newEvaluationQueue(123, application.getId(), BuildStageType.ID, "1.0.0", new Date(), new Date(), null);

    long maxPriority = dao.getMaxPriority();
    assertThat(maxPriority).isEqualTo(123);
  }

  @Test
  public void testUnacquireRows_EmptySet() {
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "1.0.0", new Date(0), new Date(1),
        "worker1");
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "2.0.0", new Date(0), new Date(1),
        "worker2");

    dao.unacquireRows(new HashSet<>());

    assertThat(dao.getAll()).map(EvaluationQueue::getWorkerId).containsOnly("worker1", "worker2");
  }

  @Test
  public void testUnacquireRows_NullSet() {
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "1.0.0", new Date(0), new Date(1),
        "worker1");
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "2.0.0", new Date(0), new Date(1),
        "worker2");

    dao.unacquireRows(null);

    assertThat(dao.getAll()).map(EvaluationQueue::getWorkerId).containsOnly("worker1", "worker2");
  }

  @Test
  public void testUnacquireRows_SingleId() {
    Application application = tempEntity.newApplicationWithParent();
    EvaluationQueue item1 =
        tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "1.0.0", new Date(0), new Date(1),
            "worker1");
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "2.0.0", new Date(0), new Date(1),
        "worker2");

    dao.unacquireRows(Set.of(item1.getId()));

    assertThat(dao.getAll())
        .extracting(EvaluationQueue::getVersion, EvaluationQueue::getWorkerId)
        .containsExactlyInAnyOrder(
            tuple("1.0.0", null),
            tuple("2.0.0", "worker2"));
  }

  @Test
  public void testUnacquireRows_MultipleIds() {
    Application application = tempEntity.newApplicationWithParent();
    EvaluationQueue item1 =
        tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "1.0.0", new Date(0), new Date(1),
            "worker1");
    EvaluationQueue item2 =
        tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "2.0.0", new Date(0), new Date(1),
            "worker2");
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "3.0.0", new Date(0), new Date(1),
        "worker3");

    dao.unacquireRows(Set.of(item1.getId(), item2.getId()));

    assertThat(dao.getAll())
        .extracting(EvaluationQueue::getVersion, EvaluationQueue::getWorkerId)
        .containsExactlyInAnyOrder(
            tuple("1.0.0", null),
            tuple("2.0.0", null),
            tuple("3.0.0", "worker3"));
  }

  @Test
  public void testUnacquireRows_AlreadyUnacquired() {
    Application application = tempEntity.newApplicationWithParent();
    EvaluationQueue item1 =
        tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "1.0.0", new Date(0), new Date(1),
            null);
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "2.0.0", new Date(0), new Date(1),
        "worker2");

    dao.unacquireRows(Set.of(item1.getId()));

    assertThat(dao.getAll())
        .extracting(EvaluationQueue::getVersion, EvaluationQueue::getWorkerId)
        .containsExactlyInAnyOrder(
            tuple("1.0.0", null),
            tuple("2.0.0", "worker2"));
  }

  @Test
  public void testUnacquireRows_NonExistentId() {
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "1.0.0", new Date(0), new Date(1),
        "worker1");
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "2.0.0", new Date(0), new Date(1),
        "worker2");

    dao.unacquireRows(Set.of("non-existent-id"));

    assertThat(dao.getAll()).map(EvaluationQueue::getWorkerId).containsOnly("worker1", "worker2");
  }

  @Test
  public void testUnacquireRows_Updates_UpdateTime() {
    Application application = tempEntity.newApplicationWithParent();
    EvaluationQueue item1 =
        tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "1.0.0", new Date(0), new Date(1),
            "worker1");
    EvaluationQueue item2 =
        tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "2.0.0", new Date(0), new Date(1),
            "worker2");
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "3.0.0", new Date(0), new Date(1),
        "worker3");
    Date now = new Date();

    dao.unacquireRows(Set.of(item1.getId(), item2.getId()));

    assertThat(dao.getAll())
        .filteredOn(queue -> queue.getVersion().equals("1.0.0") || queue.getVersion().equals("2.0.0"))
        .map(EvaluationQueue::getUpdateTime)
        .allSatisfy(updateTime -> assertThat(updateTime).isAfterOrEqualTo(now));

    assertThat(dao.getById(item1.getId()).getWorkerId()).isNull();
    assertThat(dao.getById(item2.getId()).getWorkerId()).isNull();
  }

  @Test
  public void testClearInactiveWorkerIds_Null() {
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "1.0.0", new Date(0), new Date(1), null);
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "2.0.0", new Date(0), new Date(1),
        "worker1");
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "3.0.0", new Date(0), new Date(1),
        "worker1");
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "4.0.0", new Date(0), new Date(1),
        "worker2");

    dao.clearInactiveWorkerIds(null);

    assertThat(dao.getAll()).map(EvaluationQueue::getWorkerId).containsOnly((String) null);
  }

  @Test
  public void testClearInactiveWorkerIds_Empty() {
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "1.0.0", new Date(0), new Date(1), null);
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "2.0.0", new Date(0), new Date(1),
        "worker1");
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "3.0.0", new Date(0), new Date(1),
        "worker1");
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "4.0.0", new Date(0), new Date(1),
        "worker2");

    dao.clearInactiveWorkerIds(Set.of());

    assertThat(dao.getAll()).map(EvaluationQueue::getWorkerId).containsOnly((String) null);
  }

  @Test
  public void testClearInactiveWorkerIds_SingleId() {
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "1.0.0", new Date(0), new Date(1), null);
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "2.0.0", new Date(0), new Date(1),
        "worker1");
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "3.0.0", new Date(0), new Date(1),
        "worker1");
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "4.0.0", new Date(0), new Date(1),
        "worker2");

    dao.clearInactiveWorkerIds(Set.of("worker1"));

    assertThat(dao.getAll()).map(EvaluationQueue::getWorkerId).containsOnly(null, "worker1");
  }

  @Test
  public void testClearInactiveWorkerIds_MultipleIds() {
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "1.0.0", new Date(0), new Date(1), null);
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "2.0.0", new Date(0), new Date(1),
        "worker1");
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "3.0.0", new Date(0), new Date(1),
        "worker1");
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "4.0.0", new Date(0), new Date(1),
        "worker2");

    dao.clearInactiveWorkerIds(Set.of("worker1", "worker2"));

    assertThat(dao.getAll()).map(EvaluationQueue::getWorkerId).containsOnly(null, "worker1", "worker2");
  }

  @Test
  public void testFindExisting_NullSboms() {
    Set<ThirdPartySbomMetadata> result = dao.findExisting(null);

    assertThat(result).isEmpty();
  }

  @Test
  public void testFindExisting_EmptySboms() {
    Set<ThirdPartySbomMetadata> result = dao.findExisting(List.of());

    assertThat(result).isEmpty();
  }

  @Test
  public void testFindExisting_MultipleSbomsWithSomeExisting() {
    Application application = tempEntity.newApplicationWithParent();
    ThirdPartySbomMetadata sbom1 = tempEntity.newThirdPartySbomMetadata(application.getId(), ACTIVE, "file1.xml");
    ThirdPartySbomMetadata sbom2 = tempEntity.newThirdPartySbomMetadata(application.getId(), ACTIVE, "file2.xml");
    ThirdPartySbomMetadata sbom3 = tempEntity.newThirdPartySbomMetadata(application.getId(), ACTIVE, "file3.xml");
    tempEntity.newEvaluationQueue(1, sbom1.getApplicationId(), ComplianceStageType.ID, sbom1.getSbomVersion(),
        new Date(), new Date(), null);
    tempEntity.newEvaluationQueue(2, sbom2.getApplicationId(), ComplianceStageType.ID, sbom2.getSbomVersion(),
        new Date(), new Date(), null);

    Set<ThirdPartySbomMetadata> result = dao.findExisting(List.of(sbom1, sbom2, sbom3));

    assertThat(result).containsExactlyInAnyOrder(sbom1, sbom2);
  }

  @Test
  public void testInsert_IgnoreDuplicateKey_False_ThrowsOnDuplicate() {
    Application application = tempEntity.newApplicationWithParent();
    EvaluationQueue entity = new EvaluationQueue();
    entity.setPriority(1);
    entity.setApplicationId(application.getId());
    entity.setStageTypeId(BuildStageType.ID);
    entity.setVersion("1.0.0");
    entity.setCreateTime(new Date());
    entity.setUpdateTime(new Date());
    dao.insert(entity, false);

    EvaluationQueue duplicate = new EvaluationQueue();
    duplicate.setPriority(2);
    duplicate.setApplicationId(application.getId());
    duplicate.setStageTypeId(BuildStageType.ID);
    duplicate.setVersion("1.0.0");
    duplicate.setCreateTime(new Date());
    duplicate.setUpdateTime(new Date());

    assertThatExceptionOfType(Exception.class).isThrownBy(() -> dao.insert(duplicate, false));
  }

  @Test
  public void testInsert_IgnoreDuplicateKey_True_IgnoresDuplicate() {
    Application application = tempEntity.newApplicationWithParent();
    EvaluationQueue entity = new EvaluationQueue();
    entity.setPriority(1);
    entity.setApplicationId(application.getId());
    entity.setStageTypeId(BuildStageType.ID);
    entity.setVersion("1.0.0");
    entity.setCreateTime(new Date());
    entity.setUpdateTime(new Date());
    dao.insert(entity, false);

    EvaluationQueue duplicate = new EvaluationQueue();
    duplicate.setPriority(2);
    duplicate.setApplicationId(application.getId());
    duplicate.setStageTypeId(BuildStageType.ID);
    duplicate.setVersion("1.0.0");
    duplicate.setCreateTime(new Date());
    duplicate.setUpdateTime(new Date());
    dao.insert(duplicate, true);

    assertThat(dao.getAll()).hasSize(1)
        .extracting(EvaluationQueue::getPriority)
        .containsExactly(1);
  }

  @Test
  public void testInsertBatch_IgnoreDuplicateKey_False_ThrowsOnDuplicate() {
    Application application = tempEntity.newApplicationWithParent();
    EvaluationQueue existing = new EvaluationQueue();
    existing.setPriority(1);
    existing.setApplicationId(application.getId());
    existing.setStageTypeId(BuildStageType.ID);
    existing.setVersion("1.0.0");
    existing.setCreateTime(new Date());
    existing.setUpdateTime(new Date());
    dao.insert(existing, false);

    EvaluationQueue duplicate = new EvaluationQueue();
    duplicate.setPriority(2);
    duplicate.setApplicationId(application.getId());
    duplicate.setStageTypeId(BuildStageType.ID);
    duplicate.setVersion("1.0.0");
    duplicate.setCreateTime(new Date());
    duplicate.setUpdateTime(new Date());

    assertThatExceptionOfType(Exception.class).isThrownBy(() -> dao.insertBatch(List.of(duplicate), false));
  }

  @Test
  public void testInsertBatch_IgnoreDuplicateKey_True_IgnoresDuplicates() {
    Application application = tempEntity.newApplicationWithParent();
    EvaluationQueue existing = new EvaluationQueue();
    existing.setPriority(1);
    existing.setApplicationId(application.getId());
    existing.setStageTypeId(BuildStageType.ID);
    existing.setVersion("1.0.0");
    existing.setCreateTime(new Date());
    existing.setUpdateTime(new Date());
    dao.insert(existing, false);

    EvaluationQueue duplicate = new EvaluationQueue();
    duplicate.setPriority(2);
    duplicate.setApplicationId(application.getId());
    duplicate.setStageTypeId(BuildStageType.ID);
    duplicate.setVersion("1.0.0");
    duplicate.setCreateTime(new Date());
    duplicate.setUpdateTime(new Date());

    EvaluationQueue newEntry = new EvaluationQueue();
    newEntry.setPriority(3);
    newEntry.setApplicationId(application.getId());
    newEntry.setStageTypeId(BuildStageType.ID);
    newEntry.setVersion("2.0.0");
    newEntry.setCreateTime(new Date());
    newEntry.setUpdateTime(new Date());

    dao.insertBatch(List.of(duplicate, newEntry), true);

    assertThat(dao.getAll()).hasSize(2)
        .extracting(EvaluationQueue::getVersion)
        .containsExactlyInAnyOrder("1.0.0", "2.0.0");
    assertThat(dao.getAll())
        .filteredOn(q -> q.getVersion().equals("1.0.0"))
        .extracting(EvaluationQueue::getPriority)
        .containsExactly(1);
  }

  @Test
  public void testClearInactiveWorkerIds_Updates_UpdateTime() {
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "1.0.0", new Date(0), new Date(1), null);
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "2.0.0", new Date(0), new Date(1),
        "worker1");
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "3.0.0", new Date(0), new Date(1),
        "worker1");
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "4.0.0", new Date(0), new Date(1),
        "worker2");
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "5.0.0", new Date(0), new Date(1),
        "worker3");
    Date now = new Date();

    dao.clearInactiveWorkerIds(Set.of("worker1", "worker2"));

    assertThat(dao.getAll()).map(EvaluationQueue::getWorkerId).containsOnly(null, "worker1", "worker2");
    assertThat(dao.getAll())
        .filteredOn(evaluationQueue -> evaluationQueue.getUpdateTime().equals(now) ||
            evaluationQueue.getUpdateTime().after(now))
        .extracting(EvaluationQueue::getVersion)
        .containsExactly("5.0.0");
  }
}
