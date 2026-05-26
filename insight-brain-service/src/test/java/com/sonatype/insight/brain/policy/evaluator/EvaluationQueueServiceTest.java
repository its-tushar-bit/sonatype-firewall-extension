/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.when;

import com.sonatype.insight.brain.dataaccess.evaluation.EvaluationQueueDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.evaluation.EvaluationQueue;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.policy.evaluator.queue.EvaluationQueueService;
import com.sonatype.insight.brain.scheduler.QuartzJobStoreTX;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.quartz.impl.jdbcjobstore.SchedulerStateRecord;

public class EvaluationQueueServiceTest
    extends AbstractComponentTest
{
  private EvaluationQueueService evaluationQueueService;

  @Mock
  private QuartzJobStoreTX mockQuartzJobStoreTX;

  @Inject
  private EvaluationQueueDAO evaluationQueueDAO;

  @Before
  public void setUp() {
    evaluationQueueService = new EvaluationQueueService(mockQuartzJobStoreTX, evaluationQueueDAO);
  }

  @Test
  public void testAcquireRows() {
    when(mockQuartzJobStoreTX.getInstanceId()).thenReturn("worker1");
    Application app = tempEntity.newApplicationWithParent();
    EvaluationQueue item1 =
        tempEntity.newEvaluationQueue(3, app.getId(), BuildStageType.ID, "1.0.0", new Date(), new Date(), null);
    EvaluationQueue item2 =
        tempEntity.newEvaluationQueue(2, app.getId(), BuildStageType.ID, "2.0.0", new Date(), new Date(), null);
    EvaluationQueue item3 =
        tempEntity.newEvaluationQueue(1, app.getId(), BuildStageType.ID, "3.0.0", new Date(), new Date(), null);

    evaluationQueueService.acquireRows(2);

    assertThat(evaluationQueueDAO.getById(item1.getId()).getWorkerId()).isNull();
    assertThat(evaluationQueueDAO.getById(item2.getId()).getWorkerId()).isEqualTo("worker1");
    assertThat(evaluationQueueDAO.getById(item3.getId()).getWorkerId()).isEqualTo("worker1");
  }

  @Test
  public void testUnacquireRows() {
    Application app = tempEntity.newApplicationWithParent();
    EvaluationQueue item1 =
        tempEntity.newEvaluationQueue(3, app.getId(), BuildStageType.ID, "1.0.0", new Date(), new Date(), "worker2");
    EvaluationQueue item2 =
        tempEntity.newEvaluationQueue(2, app.getId(), BuildStageType.ID, "2.0.0", new Date(), new Date(), "worker1");
    EvaluationQueue item3 =
        tempEntity.newEvaluationQueue(1, app.getId(), BuildStageType.ID, "3.0.0", new Date(), new Date(), "worker1");

    evaluationQueueService.unacquireRows(Set.of(item2.getId(), item3.getId()));

    assertThat(evaluationQueueDAO.getById(item1.getId()).getWorkerId()).isEqualTo("worker2");
    assertThat(evaluationQueueDAO.getById(item2.getId()).getWorkerId()).isNull();
    assertThat(evaluationQueueDAO.getById(item3.getId()).getWorkerId()).isNull();
  }

  @Test
  public void testClearInactiveWorkerIds_unknownWorkerIds() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "1.0.0", new Date(), new Date(), null);
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "2.0.0", new Date(), new Date(),
        "worker1");
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "3.0.0", new Date(), new Date(),
        "worker2");
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "4.0.0", new Date(), new Date(),
        "worker3");
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "5.0.0", new Date(), new Date(),
        "worker4");
    when(mockQuartzJobStoreTX.getSchedulerStateRecords()).thenReturn(List.of(
        createSchedulerStateRecord("worker1", System.currentTimeMillis()),
        createSchedulerStateRecord("worker2", System.currentTimeMillis())));

    evaluationQueueService.clearInactiveWorkerIds(Duration.ofSeconds(1).toMillis());

    assertThat(evaluationQueueDAO.getAll()).map(EvaluationQueue::getWorkerId).containsOnly(null, "worker1", "worker2");
  }

  @Test
  public void testClearInactiveWorkerIds_failedWorkerIds() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "1.0.0", new Date(), new Date(), null);
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "2.0.0", new Date(), new Date(),
        "worker1");
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "3.0.0", new Date(), new Date(),
        "worker2");
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "4.0.0", new Date(), new Date(),
        "worker3");
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "5.0.0", new Date(), new Date(),
        "worker4");
    long expiredCheckinTimestamp = System.currentTimeMillis() - QuartzJobStoreTX.FAILED_CLUSTER_CHECKIN_INTERVAL_MILLIS;
    doCallRealMethod().when(mockQuartzJobStoreTX).isFailed(any());
    when(mockQuartzJobStoreTX.getSchedulerStateRecords()).thenReturn(List.of(
        createSchedulerStateRecord("worker1", System.currentTimeMillis()),
        createSchedulerStateRecord("worker2", System.currentTimeMillis()),
        createSchedulerStateRecord("worker3", expiredCheckinTimestamp),
        createSchedulerStateRecord("worker4", expiredCheckinTimestamp)));

    evaluationQueueService.clearInactiveWorkerIds(Duration.ofSeconds(1).toMillis());

    assertThat(evaluationQueueDAO.getAll()).map(EvaluationQueue::getWorkerId).containsOnly(null, "worker1", "worker2");
  }

  @Test
  public void testClearInactiveWorkerIds_expiredWorkerIds() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "1.0.0", new Date(), new Date(), null);
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "2.0.0", new Date(), new Date(),
        "worker1");
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "3.0.0", new Date(), new Date(),
        "worker2");
    // Keep a wide margin from the expiration threshold to avoid boundary timing flakes.
    Date expiredDate = DateUtils.addSeconds(new Date(), -10);
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "4.0.0", expiredDate, expiredDate,
        "worker3");
    tempEntity.newEvaluationQueue(1, application.getId(), BuildStageType.ID, "5.0.0", expiredDate, expiredDate,
        "worker4");
    when(mockQuartzJobStoreTX.getSchedulerStateRecords()).thenReturn(List.of(
        createSchedulerStateRecord("worker1", System.currentTimeMillis()),
        createSchedulerStateRecord("worker2", System.currentTimeMillis()),
        createSchedulerStateRecord("worker3", System.currentTimeMillis()),
        createSchedulerStateRecord("worker4", System.currentTimeMillis())));

    evaluationQueueService.clearInactiveWorkerIds(Duration.ofSeconds(1).toMillis());

    assertThat(evaluationQueueDAO.getAll()).map(EvaluationQueue::getWorkerId).containsOnly(null, "worker1", "worker2");
  }

  private SchedulerStateRecord createSchedulerStateRecord(
      final String schedulerInstanceId,
      final long checkinTimestamp)
  {
    SchedulerStateRecord schedulerStateRecord = new SchedulerStateRecord();
    schedulerStateRecord.setSchedulerInstanceId(schedulerInstanceId);
    schedulerStateRecord.setCheckinTimestamp(checkinTimestamp);
    return schedulerStateRecord;
  }
}
