/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import java.util.Map;

import com.sonatype.insight.brain.dataaccess.security.RolePermissionDAO;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.search.session.ReadableContextAuthzCache;
import com.sonatype.insight.brain.service.InsightJob;
import com.sonatype.insight.brain.tenancy.TenantContexts;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;

import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class ClearRolePermissionCache
    implements InsightJob
{
  private static final Logger log = LoggerFactory.getLogger(ClearRolePermissionCache.class);

  // Visible for testing
  static final String TASK_NAME = "ClearRolePermissionCache";

  static final String TENANT_SLUG_PARAMETER = "tenantSlug";

  private final TaskScheduler taskScheduler;

  private final ReadableContextAuthzCache readableContextAuthzCache;

  @Inject
  public ClearRolePermissionCache(
      TaskScheduler taskScheduler,
      ReadableContextAuthzCache readableContextAuthzCache)
  {
    this.taskScheduler = taskScheduler;
    this.readableContextAuthzCache = readableContextAuthzCache;
  }

  public void scheduleClearRolePermissionCacheForAllOtherNodes() {
    taskScheduler.scheduleOneTimeTaskForAllOtherNodes(this,
        Map.of(TENANT_SLUG_PARAMETER, TenantThreadLocal.getTenant().tenantSlug));
  }

  /**
   * Bumps the local authz epoch immediately, then fans out to peer nodes. Peer invalidation is
   * best-effort: if scheduling fails, peers remain stale until the next membership mutation or
   * until {@link ReadableContextAuthzCache}'s write TTL expires (accepted upper bound on
   * cross-node staleness).
   */
  public void invalidateAuthorizationCachesForAllNodes() {
    readableContextAuthzCache.bumpEpoch();
    try {
      scheduleClearRolePermissionCacheForAllOtherNodes();
    }
    catch (RuntimeException e) {
      log.warn(
          "Failed to schedule ClearRolePermissionCache for other nodes; peers may serve stale RBAC until TTL or next bump",
          e);
    }
  }

  @Override
  public void register() {
    RolePermissionDAO.setClearRolePermissionCacheForAllOtherNodes(
        this::invalidateAuthorizationCachesForAllNodes);
  }

  // Visible for testing
  void clearRolePermissionCache() {
    RolePermissionDAO.clearRolePermissionCache();
    readableContextAuthzCache.bumpEpoch();
  }

  @Override
  public void execute(JobExecutionContext context) {
    String tenantSlug = null;
    if (context != null && context.getMergedJobDataMap() != null) {
      tenantSlug = context.getMergedJobDataMap().getString(TENANT_SLUG_PARAMETER);
    }
    if (tenantSlug != null && !tenantSlug.isBlank()) {
      // Jobs scheduled before tenantSlug was added (rolling upgrade) never reach here.
      TenantContexts.runAs(tenantSlug,
          () -> execute(this::clearRolePermissionCache, log, "Failed to clear role permission cache"));
      return;
    }

    // Missing slug: do not clear/bump against whatever tenant is bound on this cluster thread.
    // The originating node already bumped its own epoch at mutation time; peers without a slug
    // wait for TTL or a subsequent slug-bearing fan-out.
    log.warn(
        "ClearRolePermissionCache job missing tenantSlug; skipping RolePermissionDAO/authz epoch clear to avoid wrong-tenant invalidation (rolling-upgrade window)");
  }

  @Override
  public String getJobName() {
    return TASK_NAME;
  }
}
