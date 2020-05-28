/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.successmetrics;

import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.DataRetentionPolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.configuration.DataRetentionPolicy;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.MDCUsernameScope;

import io.dropwizard.lifecycle.Managed;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
@DisallowConcurrentExecution
public class SuccessMetricsPurger
    implements Managed, Job
{
  public static final String NAME = "SuccessMetricsPurger";

  private static final Logger log = LoggerFactory.getLogger(SuccessMetricsPurger.class);

  private final DataRetentionPolicyDAO dataRetentionPolicyDAO;

  private final ApplicationDAO applicationDAO;

  private final OwnerDAO ownerDAO;

  private final PolicyViolationDAO policyViolationDAO;

  private final TaskScheduler taskScheduler;

  public boolean disableForTesting;

  @Inject
  public SuccessMetricsPurger(
      DataRetentionPolicyDAO dataRetentionPolicyDAO,
      ApplicationDAO applicationDAO,
      OwnerDAO ownerDAO,
      PolicyViolationDAO policyViolationDAO,
      TaskScheduler taskScheduler)
  {
    this.dataRetentionPolicyDAO = dataRetentionPolicyDAO;
    this.applicationDAO = applicationDAO;
    this.ownerDAO = ownerDAO;
    this.policyViolationDAO = policyViolationDAO;
    this.taskScheduler = taskScheduler;
  }

  @Override
  public void start() {
    if (disableForTesting) {
      return;
    }
    taskScheduler.scheduleDailyTask(SuccessMetricsPurger.class, NAME, LocalTime.of(1, 30));
    Date nextExecutionTime = taskScheduler.getNextExecutionTime(NAME);
    log.debug("Scheduled periodic purging of obsolete success metrics for {}", nextExecutionTime);
  }

  @Override
  public void stop() {
    // noop
  }

  @Override
  public void execute(JobExecutionContext context) {
    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forSystem()) {
      purgeSuccessMetrics();
    }
    catch (Exception e) {
      log.error("Success Metrics Purging error: {}", e.getMessage(), e);
    }
    catch (Throwable t) {
      // Try to log to stderr before trying the standard logging because the standard logging may not be operational
      // at this point.
      t.printStackTrace();
      log.error(t.getMessage(), t);
      System.exit(1);
    }
  }

  void purgeSuccessMetrics() {
    List<Application> applications = applicationDAO.getAll();
    log.debug("Purging obsolete success metrics from {} applications", applications.size());
    int purgedApplications = 0;
    for (Application application : applications) {
      if (purgeSuccessMetrics(application)) {
        purgedApplications++;
      }
    }
    if (purgedApplications > 0) {
      log.info("Purged obsolete success metrics from {} applications", purgedApplications);
    }
  }

  private boolean purgeSuccessMetrics(Application application) {
    Date cutoffDate = getCutoffDate(getDataRetentionPolicy(application));
    if (cutoffDate == null) {
      return false;
    }
    int deletedRows = policyViolationDAO.deleteFixedByApplicationIdAndDate(application.getId(), cutoffDate);
    if (deletedRows > 0) {
      log.info("Purged {} obsolete records older than {} from violation history of application {}", deletedRows,
          cutoffDate, application.getName());
    }
    return deletedRows > 0;
  }

  private DataRetentionPolicy getDataRetentionPolicy(Application application) {
    for (Owner owner : ownerDAO.walkHierarchy(application)) {
      DataRetentionPolicy dataRetentionPolicy = dataRetentionPolicyDAO.getByOwnerIdAndContextId(owner.getId(),
          DataRetentionPolicy.CONTEXT_ID_SUCCESS_METRICS);
      if (dataRetentionPolicy != null) {
        return dataRetentionPolicy;
      }
    }
    return null;
  }

  private Date getCutoffDate(DataRetentionPolicy dataRetentionPolicy) {
    if (dataRetentionPolicy == null || !dataRetentionPolicy.isPurgingEnabled()) {
      return null;
    }
    int maxAgeInYears = Math.max(1, dataRetentionPolicy.getMaxAgeInDays() / 365);
    return Date
        .from(ZonedDateTime.now().withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS).minusYears(maxAgeInYears).toInstant());
  }
}
