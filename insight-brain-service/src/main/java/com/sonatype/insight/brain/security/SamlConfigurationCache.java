/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.configuration.saml.SamlConfigurationService;
import com.sonatype.insight.brain.model.configuration.saml.SamlConfiguration;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.InsightJob;
import com.sonatype.insight.brain.tenancy.TenantReference;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Caches the current tenant's {@link SamlConfiguration} in memory so the SAML servlet filter (and the
 * request resolver it drives) can read it without a request-scoped persistence/feature context — the
 * SAML filter runs early in the servlet chain, before the JAX-RS layer where {@link SamlConfigurationService}
 * can be read directly.
 *
 * <p>
 * The cache is populated at startup ({@link #register()}) and refreshed whenever the configuration
 * changes, propagating the refresh across cluster nodes.
 */
@Named
@Singleton
@DisallowConcurrentExecution
public class SamlConfigurationCache
    implements InsightJob
{
  private static final Logger log = LoggerFactory.getLogger(SamlConfigurationCache.class);

  // Visible for testing
  static final String TASK_NAME = "SamlConfigurationCache";

  private static final String REFRESH_ERROR = "SAML configuration cache refresh error";

  private final SamlConfigurationService samlConfigurationService;

  private final TaskScheduler taskScheduler;

  private final TenantReference<SamlConfiguration> cachedConfiguration = new TenantReference<>();

  @Inject
  public SamlConfigurationCache(SamlConfigurationService samlConfigurationService, TaskScheduler taskScheduler) {
    this.samlConfigurationService = samlConfigurationService;
    this.taskScheduler = taskScheduler;
  }

  @Override
  public void register() {
    try {
      refresh();
    }
    catch (RuntimeException e) {
      log.error("The SAML configuration is invalid and needs to be fixed by a system administrator", e);
    }
  }

  @Override
  public void deregister() {
    cachedConfiguration.remove();
  }

  /**
   * @return the cached SAML configuration for the current tenant, or {@code null} if SAML is not configured.
   */
  public SamlConfiguration get() {
    return cachedConfiguration.get();
  }

  public void refresh() {
    SamlConfiguration samlConfiguration = samlConfigurationService.get();
    if (samlConfiguration != null) {
      cachedConfiguration.set(samlConfiguration);
    }
    else {
      cachedConfiguration.remove();
    }
    log.info("SAML integration {}", cachedConfiguration.get() != null ? "enabled" : "disabled");
  }

  public void refreshAllClusterNodes() {
    refresh();
    taskScheduler.scheduleOneTimeTaskForAllOtherNodes(this);
  }

  @Override
  public void execute(JobExecutionContext context) {
    execute(this::refresh, log, REFRESH_ERROR);
  }

  @Override
  public String getJobName() {
    return TASK_NAME;
  }
}
