/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scm.event;

import java.util.Date;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.tenancy.TenantUtil;

import org.slf4j.Logger;

import static com.sonatype.insight.brain.scm.event.AbstractSourceControlEventLogger.SCM_EVENT_LOGGER_NAME;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Factory for creating source control event loggers
 */
@Named
@Singleton
public class SourceControlEventLoggerFactory
{
  private static final Logger log = getLogger(SourceControlEventLoggerFactory.class);

  private static final TenantUtil TENANT_UTIL = new TenantUtil();

  private final CurrentUser currentUser;

  @Inject
  public SourceControlEventLoggerFactory(final CurrentUser currentUser) {
    this.currentUser = currentUser;
    logPotentialMisconfiguration();
  }

  public PullRequestCommentingLogger newLogger(
      final Date logTimestamp,
      final Application application,
      final Organization organization,
      final GitRepositoryInfo gitRepositoryInfo)
  {
    return new PullRequestCommentingLogger(logTimestamp, application, organization, gitRepositoryInfo, currentUser);
  }

  private void logPotentialMisconfiguration() {
    if (TENANT_UTIL.isMultiTenant()) {
      if (!getLogger(SCM_EVENT_LOGGER_NAME).isInfoEnabled()) {
        log.warn("Disabling source control logging for logger {}. Instance is MTIQ but logger is disabled.",
            SCM_EVENT_LOGGER_NAME);
      }
    }
    else {
      log.debug("Disabling source control logging for logger {}. Instance is not MTIQ.", SCM_EVENT_LOGGER_NAME);
    }
  }
}
