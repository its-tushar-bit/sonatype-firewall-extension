/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.service.InsightConfig;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.yammer.dropwizard.lifecycle.Managed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.21
 */
@Named
@Singleton
public class RepositoryPolicyAlertNotificationTask
    implements Managed
{
  private int interval;

  private final RepositoryPolicyAlertEmailer emailer;

  private final PendingRepositoryPolicyNotifications notifications;

  private ScheduledExecutorService executor;

  @Inject
  public RepositoryPolicyAlertNotificationTask(final InsightConfig insightConfig,
                                               final RepositoryPolicyAlertEmailer emailer,
                                               final PendingRepositoryPolicyNotifications notifications)
  {
    this.interval = insightConfig.getRepositoryPolicyViolationNotificationInterval();
    this.emailer = emailer;
    this.notifications = notifications;
  }

  @Override
  public void start() {
    if (executor != null) {
      return;
    }

    executor = Executors.newSingleThreadScheduledExecutor(
        new ThreadFactoryBuilder().setNameFormat("RepositoryPolicyAlertNotificationTask").build());

    executor.scheduleWithFixedDelay(new ProcessAlertRunnable(notifications, emailer), 0, interval, TimeUnit.SECONDS);
  }

  @Override
  public void stop() {
    if (executor != null) {
      executor.shutdown();
      executor = null;
    }
  }

  static class ProcessAlertRunnable
      implements Runnable
  {
    private final PendingRepositoryPolicyNotifications queue;

    private final RepositoryPolicyAlertEmailer emailer;

    private final Logger log = LoggerFactory.getLogger(ProcessAlertRunnable.class);

    private final RepositoryDAO repositoryDAO = new RepositoryDAO();

    ProcessAlertRunnable(final PendingRepositoryPolicyNotifications queue, final RepositoryPolicyAlertEmailer emailer) {
      this.queue = queue;
      this.emailer = emailer;
    }

    @Override
    public void run() {
      try {
        Map<String, List<PolicyNotification>> repoNotificationMap = queue.remove();

        for (String repositoryId : repoNotificationMap.keySet()) {
          List<PolicyNotification> notifications = repoNotificationMap.get(repositoryId);
          log.debug("Found {} repository policy notifications for repository {}.", notifications.size(), repositoryId);

          Repository repository = repositoryDAO.getById(repositoryId);

          if (repository != null) {
            emailer.sendNotifications(repository, notifications);
          }
        }
      }
      catch (Exception e) {
        log.error("Encountered an error while processing repository policy violation notifications.", e);
      }
    }
  }
}
