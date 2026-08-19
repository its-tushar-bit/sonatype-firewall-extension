/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import com.sonatype.insight.brain.dataaccess.component.RepositoryIdentifiedComponentDAO;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.AdminTask;
import com.sonatype.insight.brain.service.InsightJob;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.io.PrintWriter;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
@DisallowConcurrentExecution
public class RepositoryIdentifiedComponentPurger
    extends AdminTask
    implements InsightJob
{
  public static final String PATH = "purgeRepositoryIdentifiedComponents";

  private static final Logger log = LoggerFactory.getLogger(RepositoryIdentifiedComponentPurger.class);

  // Visible for testing
  static final String NAME = "RepositoryIdentifiedComponentPurger";

  private static final String DESCRIPTION = "purging of infrequently accessed repository identified components";

  // Visible for testing
  static final Duration MAX_LAST_ACCESSED = Duration.ofDays(30);

  // Visible for testing
  static final LocalTime EXECUTION_TIME = LocalTime.of(1, 0);

  private final TaskScheduler taskScheduler;

  private final RepositoryIdentifiedComponentDAO repositoryIdentifiedComponentDAO;

  public boolean disableForTesting;

  @Inject
  public RepositoryIdentifiedComponentPurger(
      TaskScheduler taskScheduler,
      RepositoryIdentifiedComponentDAO repositoryIdentifiedComponentDAO)
  {
    super(PATH);
    this.taskScheduler = taskScheduler;
    this.repositoryIdentifiedComponentDAO = repositoryIdentifiedComponentDAO;
  }

  @Override
  public void register() {
    if (disableForTesting) {
      return;
    }

    taskScheduler.scheduleDailyTask(this, EXECUTION_TIME);
  }

  @Override
  public void deregister() {
    // no-op
  }

  @Override
  public void execute(final Map<String, List<String>> parameters, final PrintWriter output) throws Exception {
    taskScheduler.triggerTaskNow(this, null);
  }

  /**
   * @since 1.137
   */
  @Override
  public void execute(JobExecutionContext context) {
    execute(this::purgeRepositoryIdentifiedComponents, log, String.format("Error in %s", DESCRIPTION));
  }

  void purgeRepositoryIdentifiedComponents() {
    log.debug("Starting {}.", DESCRIPTION);
    repositoryIdentifiedComponentDAO.deleteInfrequentlyAccessed(MAX_LAST_ACCESSED);
    log.info("Finished {}.", DESCRIPTION);
  }

  @Override
  public String getJobName() {
    return NAME;
  }
}
