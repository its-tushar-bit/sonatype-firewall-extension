/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.tenancy.TenantManaged;
import java.io.PrintWriter;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;

public interface InsightJob
    extends Job, TenantManaged
{
  default void execute(Runnable apply, Logger log, String errorDescription) {
    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forSystem()) {
      apply.run();
    }
    catch (Exception e) {
      log.error("{}: {}", errorDescription, e.getMessage(), e);
    }
    catch (Throwable t) {
      // Try to log to stderr before trying the standard logging because the standard logging may not be operational
      // at this point.
      t.printStackTrace();
      log.error(t.getMessage(), t);
      System.exit(1);
    }
  }

  /**
   * Executes the job directly for testing purposes.
   *
   * @param context the job execution context (can be null for tests)
   * @param output the output writer (can be null for tests)
   * @deprecated Use the Quartz-based execution via {@link #execute(JobExecutionContext)} instead.
   *             This method is kept for backward compatibility with tests.
   */
  @Deprecated
  default void executeForTest(JobExecutionContext context, PrintWriter output) throws Exception {
    execute(context);
  }

  /**
   * @return the job name to store in the qrtz_job_details table, this should not change without a migrator
   */
  String getJobName();
}
