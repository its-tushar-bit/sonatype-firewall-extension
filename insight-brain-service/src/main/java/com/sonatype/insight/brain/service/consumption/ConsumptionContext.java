/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.consumption;

import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.product.license.ProductLicense;

import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread-local context carrying consumption metadata per request or background job.
 *
 * @since 1.204
 */
public final class ConsumptionContext
{
  private static final Logger log = LoggerFactory.getLogger(ConsumptionContext.class);

  /**
   * Plain {@link ThreadLocal} (not {@code InheritableThreadLocal}): inheritance without a
   * {@code TenantAwareOneTimeRunnable}-equivalent submit-time wrapper would let a pool worker
   * capture a stale context at thread creation and replay it across unrelated tasks. Callers
   * that cross thread boundaries must propagate context explicitly via {@link #snapshot()}
   * and {@link #scopeRestored}.
   */
  private static final ThreadLocal<ConsumptionContext> HOLDER = new ThreadLocal<>();

  private String orgId;

  private String tier;

  private String source;

  private boolean directApiRequest;

  private String userId;

  private String appId;

  private String scanId;

  private ConsumptionContext() {
  }

  static void set(String orgId, String tier, String source) {
    set(orgId, tier, source, false);
  }

  static void set(String orgId, String tier, String source, boolean directApiRequest) {
    ConsumptionContext ctx = new ConsumptionContext();
    ctx.orgId = orgId;
    ctx.tier = tier;
    ctx.source = source;
    ctx.directApiRequest = directApiRequest;
    HOLDER.set(ctx);
  }

  @Nullable
  public static ConsumptionContext get() {
    return HOLDER.get();
  }

  public static void clear() {
    HOLDER.remove();
  }

  public String getOrgId() {
    return orgId;
  }

  public String getTier() {
    return tier;
  }

  public String getSource() {
    return source;
  }

  public boolean isDirectApiRequest() {
    return directApiRequest;
  }

  @Nullable
  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  @Nullable
  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  @Nullable
  public String getScanId() {
    return scanId;
  }

  public void setScanId(String scanId) {
    this.scanId = scanId;
  }

  public static void initBackgroundJob(ProductLicense productLicense) {
    initBackgroundJob(productLicense, null);
  }

  public static void initBackgroundJob(ProductLicense productLicense, @Nullable String appId) {
    try {
      if (!SystemConfigurationPropertyFeature.CONSUMPTION_REPORTING.isEnabled()) {
        return;
      }
      String orgId = ConsumptionOrgIdResolver.resolveForBackgroundJob();
      if (orgId == null) {
        return;
      }
      set(orgId, ConsumptionTierResolver.resolveTier(productLicense),
          ConsumptionSourceClassifier.Source.CONTINUOUS_MONITOR.token());
      if (appId != null) {
        ConsumptionContext ctx = get();
        if (ctx != null) {
          ctx.setAppId(appId);
        }
      }
    }
    catch (Exception e) {
      log.debug("Failed to init ConsumptionContext for background job", e);
    }
  }

  /** Immutable snapshot for propagating context across threads. */
  public record Snapshot(String orgId, String tier, String source, boolean directApiRequest)
  {
  }

  @Nullable
  public static Snapshot snapshot() {
    ConsumptionContext ctx = get();
    return ctx == null ? null : new Snapshot(ctx.orgId, ctx.tier, ctx.source, ctx.directApiRequest);
  }

  /** Restores {@code snapshot} onto the current thread; no-op when null. */
  public static void restore(@Nullable Snapshot snapshot) {
    if (snapshot != null) {
      set(snapshot.orgId(), snapshot.tier(), snapshot.source(), snapshot.directApiRequest());
    }
  }

  /** Scopes the current context to {@code ownerId}/{@code scanId}; only fires when ownerType is APPLICATION. */
  public static void scopeToApp(OwnerType ownerType, String ownerId, @Nullable String scanId) {
    ConsumptionContext ctx = get();
    if (ctx != null && ownerType == OwnerType.APPLICATION) {
      ctx.setAppId(ownerId);
      if (scanId != null) {
        ctx.setScanId(scanId);
      }
    }
  }

  /** Restores {@code snapshot} and applies non-null {@code appId}/{@code scanId} overrides. */
  public static void restoreAndScope(@Nullable Snapshot snapshot, @Nullable String appId, @Nullable String scanId) {
    if (snapshot == null) {
      return;
    }
    restore(snapshot);
    ConsumptionContext ctx = get();
    if (ctx == null) {
      return;
    }
    if (appId != null) {
      ctx.setAppId(appId);
    }
    if (scanId != null) {
      ctx.setScanId(scanId);
    }
  }

  /**
   * AutoCloseable handle returned by {@link #scopeBackgroundJob} and
   * {@link #scopeRestored}. Captures the previous {@link ConsumptionContext}
   * at construction and restores it on {@link #close()}, so nested scopes on
   * the same thread behave as a stack (mirrors {@code AuditSession} and
   * {@code TenantThreadLocal}). Intended for try-with-resources only;
   * {@link #close()} must be called exactly once per Scope instance.
   */
  public static final class Scope
      implements AutoCloseable
  {
    @Nullable
    private final ConsumptionContext previous;

    private Scope() {
      this.previous = HOLDER.get();
    }

    @Override
    public void close() {
      if (previous != null) {
        HOLDER.set(previous);
      }
      else {
        HOLDER.remove();
      }
    }
  }

  public static Scope scopeBackgroundJob(ProductLicense productLicense) {
    Scope scope = new Scope();
    initBackgroundJob(productLicense);
    return scope;
  }

  public static Scope scopeBackgroundJob(ProductLicense productLicense, @Nullable String appId) {
    Scope scope = new Scope();
    initBackgroundJob(productLicense, appId);
    return scope;
  }

  /**
   * Restores {@code snapshot} and applies non-null {@code appId}/{@code scanId}
   * overrides (see {@link #restoreAndScope(Snapshot, String, String)}) and
   * returns a {@link Scope} that restores the previous context on close.
   */
  public static Scope scopeRestored(
      @Nullable Snapshot snapshot,
      @Nullable String appId,
      @Nullable String scanId)
  {
    Scope scope = new Scope();
    restoreAndScope(snapshot, appId, scanId);
    return scope;
  }
}
