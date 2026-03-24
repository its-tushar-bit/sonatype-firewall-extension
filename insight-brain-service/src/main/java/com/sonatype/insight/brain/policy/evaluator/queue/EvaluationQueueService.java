/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator.queue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.dataaccess.evaluation.EvaluationQueueDAO;
import com.sonatype.insight.brain.model.evaluation.EvaluationQueue;
import com.sonatype.insight.brain.scheduler.QuartzJobStoreTX;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.quartz.JobPersistenceException;
import org.quartz.impl.jdbcjobstore.SchedulerStateRecord;

@Named
@Singleton
public class EvaluationQueueService
{
  private final QuartzJobStoreTX quartzJobStoreTX;

  private final EvaluationQueueDAO evaluationQueueDAO;

  @Inject
  public EvaluationQueueService(final QuartzJobStoreTX quartzJobStoreTX, final EvaluationQueueDAO evaluationQueueDAO) {
    this.quartzJobStoreTX = quartzJobStoreTX;
    this.evaluationQueueDAO = evaluationQueueDAO;
  }

  public List<EvaluationQueue> acquireRows(final int limit) {
    return evaluationQueueDAO.acquireRows(quartzJobStoreTX.getInstanceId(), limit);
  }

  public void unacquireRows(final Set<String> ids) {
    evaluationQueueDAO.unacquireRows(ids);
  }

  public void clearInactiveWorkerIds(final long expirationMillis) throws JobPersistenceException {
    List<SchedulerStateRecord> schedulerStateRecords = quartzJobStoreTX.getSchedulerStateRecords();
    Set<String> activeWorkerIds = schedulerStateRecords.stream()
        .filter(schedulerStateRecord -> !quartzJobStoreTX.isFailed(schedulerStateRecord))
        .map(SchedulerStateRecord::getSchedulerInstanceId)
        .collect(Collectors.toSet());
    evaluationQueueDAO.clearInactiveWorkerIds(activeWorkerIds);
    evaluationQueueDAO.clearExpiredWorkerIds(expirationMillis);
  }
}
