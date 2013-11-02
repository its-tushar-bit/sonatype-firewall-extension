/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.trending;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.trending.TrendingReport;
import com.sonatype.insight.brain.security.AuthorizationChecker;
import com.sonatype.insight.brain.security.AuthzContext;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Trending report generation and caching.
 * 
 * @since 1.7
 */
@Named
@Path(TrendingReportService.SERVICE_PATH)
public class TrendingReportService
{
  public static final String SERVICE_PATH = "rest/trending";

  public static final long CACHE_MAX_AGE_MS = TimeUnit.DAYS.toMillis(1); // one day

  private final Logger log = LoggerFactory.getLogger(getClass());

  private final TrendingReportAsyncProcessor processor;

  private final TrendingReportCache cache;

  private final AuthorizationChecker authChecker = new AuthorizationChecker();

  @Inject
  public TrendingReportService(TrendingReportAsyncProcessor processor, TrendingReportCache cache) {
    this.processor = processor;
    this.cache = cache;
  }

  /**
   * Returns trending report data. Returns cached version, if available. Trending report data is automatically
   * regenerated if cached copy is older than {@link #CACHE_MAX_AGE_MS} milliseconds. If cached trending report data is
   * not available, initiates trending report data calculation in a background thread.
   * 
   * @param force is set to {@code true}, expire cached and generate new trending report data.
   * @return returns trending report data. returns {@code null} if trending report data has not been calculated yet.
   * @since 1.7
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public TrendingReport get(@QueryParam("force") boolean force) throws IOException {
    final String username = SecurityUtils.getSubject().getPrincipal().toString();

    final boolean isAdmin = authChecker.isPermitted(username, Permission.ADMIN,
        Collections.<AuthzContext.Key, Object> emptyMap());

    if (!isAdmin && force) {
      throw new UnauthorizedException("Not authorized to force trending report regeneration");
    }

    final TrendingReport cached = cache.readCached();

    final long now = System.currentTimeMillis();

    if (force || cached == null || (now - cached.getMeta().getGeneratedOn()) > CACHE_MAX_AGE_MS) {
      if (log.isDebugEnabled()) {
        if (force) {
          log.debug("Regenerating trendong report: forced.");
        }
        else if (cached == null) {
          log.debug("Regenerating trendong report: no cached trending report data.");
        }
        else {
          log.debug("Regenerating trendong report: cached trending report data is too old ({}ms).", now
              - cached.getMeta().getGeneratedOn());
        }
      }

      processor.calculate();
    }

    if (cached != null) {
      final boolean regenerationRunning = processor.isRunning();

      cached.getMeta().setCanRegenerate(isAdmin);
      cached.getMeta().setRegenerating(regenerationRunning);

      log.debug("Cached trending report data age={}ms, regenerationRunning={}",
          now - cached.getMeta().getGeneratedOn(), regenerationRunning);
    } else {
      log.debug("No cached trending report data");
    }

    return cached;
  }
}
