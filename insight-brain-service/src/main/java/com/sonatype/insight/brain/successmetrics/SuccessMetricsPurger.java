/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.successmetrics;

import com.google.common.annotations.VisibleForTesting;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.DataRetentionPolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.HostedRepositoryComponentDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.configuration.DataRetentionPolicy;
import com.sonatype.insight.brain.model.repository.HostedRepositoryComponent;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.AdminTask;
import com.sonatype.insight.brain.service.InsightJob;
import com.sonatype.insight.dataaccess.TransactionContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.io.PrintWriter;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.jooq.exception.DataAccessException;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
@DisallowConcurrentExecution
public class SuccessMetricsPurger
    extends AdminTask
    implements InsightJob
{
  public static final String PATH = "purgeObsoleteSuccessMetrics";

  public static final String NAME = "SuccessMetricsPurger";

  private static final int MAX_RETRIES = 10;

  @VisibleForTesting
  int hrcPageSize = 500;

  private static final Logger log = LoggerFactory.getLogger(SuccessMetricsPurger.class);

  private static final String PURGE_ERROR = "Success Metrics Purging error";

  private final DataRetentionPolicyDAO dataRetentionPolicyDAO;

  private final ApplicationDAO applicationDAO;

  private final HostedRepositoryComponentDAO hostedRepositoryComponentDAO;

  private final PolicyViolationDAO policyViolationDAO;

  private final TaskScheduler taskScheduler;

  public boolean disableForTesting;

  @Inject
  public SuccessMetricsPurger(
      DataRetentionPolicyDAO dataRetentionPolicyDAO,
      ApplicationDAO applicationDAO,
      HostedRepositoryComponentDAO hostedRepositoryComponentDAO,
      PolicyViolationDAO policyViolationDAO,
      TaskScheduler taskScheduler)
  {
    super(PATH);
    this.dataRetentionPolicyDAO = dataRetentionPolicyDAO;
    this.applicationDAO = applicationDAO;
    this.hostedRepositoryComponentDAO = hostedRepositoryComponentDAO;
    this.policyViolationDAO = policyViolationDAO;
    this.taskScheduler = taskScheduler;
  }

  @Override
  public void register() {
    if (disableForTesting) {
      return;
    }
    taskScheduler.scheduleDailyTask(this, LocalTime.of(1, 30));
  }

  @Override
  public void deregister() {
    // noop
  }

  @Override
  public void execute(final Map<String, List<String>> parameters, final PrintWriter output) throws Exception {
    taskScheduler.triggerTaskNow(this, null);
  }

  @Override
  public void execute(JobExecutionContext context) {
    execute(this::purgeSuccessMetrics, log, PURGE_ERROR);
  }

  void purgeSuccessMetrics() {
    List<Application> applications = applicationDAO.getAll();
    log.debug("Purging obsolete success metrics from {} applications", applications.size());
    int purgedApplications = 0;
    for (Application application : applications) {
      if (purgeSuccessMetricsWithRetry(application)) {
        purgedApplications++;
      }
    }
    log.info("Purged obsolete success metrics from {} applications", purgedApplications);

    purgeSuccessMetricsForHostedRepositoryComponents();
  }

  private void purgeSuccessMetricsForHostedRepositoryComponents() {
    int purgedOwners = 0;
    String lastProcessedId = "";
    while (true) {
      List<HostedRepositoryComponent> page;
      try (TransactionContext tx = hostedRepositoryComponentDAO.createTransactionContext()) {
        page = hostedRepositoryComponentDAO.getPage(tx, lastProcessedId, hrcPageSize);
      }
      if (page.isEmpty()) {
        break;
      }
      for (HostedRepositoryComponent hrc : page) {
        if (purgeSuccessMetricsWithRetry(hrc)) {
          purgedOwners++;
        }
        lastProcessedId = hrc.getId();
      }
    }
    log.info("Purged obsolete success metrics from {} hosted repository components", purgedOwners);
  }

  private boolean purgeSuccessMetricsWithRetry(Owner owner) {
    for (int retry = 0; retry <= MAX_RETRIES; retry++) {
      try {
        return purgeSuccessMetrics(owner);
      }
      catch (DataAccessException e) {
        // This exception occurs usually when the embedded database is under too much load from concurrent queries.
        // To avoid having to start over the entire purging task, we retry to get this purging run completed.
        if (retry >= MAX_RETRIES) {
          throw e;
        }
        Duration delay = getDelayForRetry(retry);
        log.debug("Failed to purge obsolete success metrics for owner {}, retrying in {}",
            owner.getName(), delay, retry == 0 ? e : null);
        try {
          Thread.sleep(delay.toMillis());
        }
        catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
          e.addSuppressed(ex);
          throw e;
        }
      }
    }
    return false;
  }

  private boolean purgeSuccessMetrics(Owner owner) {
    Date cutoffDate = getCutoffDate(getDataRetentionPolicy(owner));
    if (cutoffDate == null) {
      return false;
    }
    int deletedRows = policyViolationDAO.deleteFixedByOwnerIdAndDate(owner.getId(), cutoffDate);
    if (deletedRows > 0) {
      log.info("Purged {} obsolete records older than {} from violation history of owner {}", deletedRows,
          cutoffDate, owner.getName());
    }
    return deletedRows > 0;
  }

  private DataRetentionPolicy getDataRetentionPolicy(Owner owner) {
    return dataRetentionPolicyDAO.getByOwnerIdAndContextIdWithHierarchy(
        owner.getId(), DataRetentionPolicy.CONTEXT_ID_SUCCESS_METRICS);
  }

  private Date getCutoffDate(DataRetentionPolicy dataRetentionPolicy) {
    if (dataRetentionPolicy == null || !dataRetentionPolicy.isPurgingEnabled()) {
      return null;
    }
    int maxAgeInYears = Math.max(1, dataRetentionPolicy.getMaxAgeInDays() / 365);
    return Date
        .from(ZonedDateTime.now().withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS).minusYears(maxAgeInYears).toInstant());
  }

  Duration getDelayForRetry(int retry) {
    return Duration.ofSeconds(1 << retry);
  }

  @Override
  public String getJobName() {
    return NAME;
  }
}
