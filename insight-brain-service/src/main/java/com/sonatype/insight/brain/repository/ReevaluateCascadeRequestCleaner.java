/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.repository.ReevaluateCascadeRequestDAO;
import com.sonatype.insight.brain.model.repository.ReevaluateCascadeRequest;
import com.sonatype.insight.brain.model.repository.ReevaluateCascadeRequestStatus;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.InsightJob;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
@DisallowConcurrentExecution
public class ReevaluateCascadeRequestCleaner
    implements InsightJob
{
  private static final Logger log = LoggerFactory.getLogger(ReevaluateCascadeRequestCleaner.class);

  // Visible for testing
  static final String TASK_NAME = "ReevaluateCascadeRequestCleaner";

  // Visible for testing
  static final Duration PERIOD = Duration.ofHours(24);

  // Visible for testing
  static final Duration LIFESPAN = Duration.ofHours(24);

  public static final String CLEANER_ERROR = "Reevaluate cascade request cleaner error";

  private final TaskScheduler taskScheduler;

  private final ReevaluateCascadeRequestDAO reevaluateCascadeRequestDAO;

  public boolean disableForTesting;

  @Inject
  public ReevaluateCascadeRequestCleaner(
      TaskScheduler taskScheduler,
      ReevaluateCascadeRequestDAO reevaluateCascadeRequestDAO)
  {
    this.taskScheduler = taskScheduler;
    this.reevaluateCascadeRequestDAO = reevaluateCascadeRequestDAO;
  }

  @Override
  public void register() {
    if (disableForTesting) {
      return;
    }
    // Note: executing every PERIOD means the oldest a ReevaluateCascadeRequest can be is PERIOD + LIFESPAN
    taskScheduler.schedulePeriodicTask(this, PERIOD);
  }

  @Override
  public void deregister() {
    // noop
  }

  @Override
  public void execute(JobExecutionContext context) {
    execute(this::deleteExpiredReevaluateCascadeRequests, log, CLEANER_ERROR);
  }

  private void deleteExpiredReevaluateCascadeRequests() {
    Date cutoffDate = new Date(System.currentTimeMillis() - LIFESPAN.toMillis());

    try (TransactionContext tx = reevaluateCascadeRequestDAO.createTransactionContext()) {
      tx.begin();

      List<ReevaluateCascadeRequest> expiredRequests =
          reevaluateCascadeRequestDAO.findBeforeOrOn(tx, cutoffDate);

      if (!expiredRequests.isEmpty()) {
        Set<String> requestIds = expiredRequests.stream()
            .peek(request -> {
              if (request.getStatus() == ReevaluateCascadeRequestStatus.PENDING ||
                  request.getStatus() == ReevaluateCascadeRequestStatus.IN_PROGRESS) {
                log.warn("Re-evaluate cascade request with ID {} for component hash {} has not completed in {} hours " +
                    "and is being cleaned up. Status: {}, created at: {}",
                    request.getId(), request.getComponentReferenceHash(), LIFESPAN.toHours(),
                    request.getStatus(), request.getCreatedAt());
              }
            })
            .map(ReevaluateCascadeRequest::getId)
            .collect(Collectors.toSet());

        reevaluateCascadeRequestDAO.deleteByRequestIds(tx, requestIds);

        log.debug("Cleaned up {} expired re-evaluate cascade requests and their associated progress entries",
            expiredRequests.size());
      }

      tx.commit();
    }
  }

  @Override
  public String getJobName() {
    return TASK_NAME;
  }
}
