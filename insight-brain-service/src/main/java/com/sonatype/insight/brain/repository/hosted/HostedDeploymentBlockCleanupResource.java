/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted;

import java.time.Duration;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.sonatype.insight.brain.api.v2.HasFeature;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.error.exception.BadRequestException;

import com.codahale.metrics.annotation.Timed;
import org.apache.shiro.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manual trigger for {@link HostedDeploymentBlockCleanupService}. Exists to support
 * (a) administrators who need to purge block records sooner than the 24-hour periodic job, and
 * (b) test environments — the {@code olderThanMinutes=0} form lets QA blow away all block rows
 * before/after a smoke run.
 * <p>
 * <b>Permission:</b> intentionally unrestricted at the resource layer for 1.0 GA. The trade-off
 * is captured in PMQ-HRE-012 (PM questions doc) and a future change can add
 * {@code @Authorize(permission = SYSTEM_ADMIN)} once we settle the permission model for the
 * forthcoming "blocked deployments" UI.
 */
@Named
@Singleton
@Timed
@Path(HostedDeploymentBlockCleanupResource.RESOURCE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@HasFeature(SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION)
public class HostedDeploymentBlockCleanupResource
{
  public static final String RESOURCE_PATH = "api/v2/hostedDeploymentBlocks";

  static final String CLEANUP_PATH = "cleanup";

  private static final Logger log = LoggerFactory.getLogger(HostedDeploymentBlockCleanupResource.class);

  private final HostedDeploymentBlockCleanupService cleanupService;

  private final Configuration configuration;

  @Inject
  public HostedDeploymentBlockCleanupResource(
      final HostedDeploymentBlockCleanupService cleanupService,
      final Configuration configuration)
  {
    this.cleanupService = cleanupService;
    this.configuration = configuration;
  }

  /**
   * Synchronously delete {@code hosted_deployment_block} rows older than the supplied cutoff.
   *
   * @param olderThanMinutes optional. When absent, falls back to
   *          {@link Configuration#getHostedDeploymentBlockRetentionHours()} converted
   *          to minutes (i.e. the same cutoff the periodic job uses). {@code 0}
   *          deletes every row regardless of age. Negative values yield 400.
   * @return 200 with body {@code {"deleted":N,"cutoffTime":"...","olderThanMinutes":N}}
   */
  @POST
  @Path(CLEANUP_PATH)
  public Response runCleanup(@QueryParam("olderThanMinutes") final Integer olderThanMinutes) {
    long effectiveMinutes;
    if (olderThanMinutes == null) {
      Integer retentionHours = configuration.getHostedDeploymentBlockRetentionHours();
      effectiveMinutes = Duration.ofHours(retentionHours == null ? 24 : retentionHours).toMinutes();
    }
    else {
      if (olderThanMinutes < 0) {
        throw new BadRequestException("olderThanMinutes must be >= 0: olderThanMinutes=" + olderThanMinutes);
      }
      effectiveMinutes = olderThanMinutes;
    }

    log.info("Manual hosted deployment block cleanup triggered: olderThanMinutes={} (param={}) requestedBy={}",
        effectiveMinutes, olderThanMinutes, currentPrincipal());

    HostedDeploymentBlockCleanupService.CleanupOutcome outcome =
        cleanupService.runCleanup(Duration.ofMinutes(effectiveMinutes));

    return Response.ok(new CleanupResponse(outcome.deleted(), outcome.cutoffTime().toString(), effectiveMinutes))
        .build();
  }

  /**
   * Returns the authenticated principal name for log attribution. Defensive against missing
   * Shiro context (admin task path, tests). Until PMQ-HRE-012 lands a proper {@code @Audited}
   * gate, this is the only forensic record of who triggered a destructive cleanup.
   */
  private static String currentPrincipal() {
    try {
      Object principal = SecurityUtils.getSubject().getPrincipal();
      return principal != null ? principal.toString() : "anonymous";
    }
    catch (RuntimeException e) {
      return "unknown";
    }
  }

  /**
   * Response body for {@code POST .../cleanup}. {@code cutoffTime} is the absolute instant the
   * service computed and used for the DELETE. {@code olderThanMinutes} echoes back the effective
   * cutoff so callers can confirm what the server interpreted (especially when the caller relies
   * on the configured-retention default).
   */
  static record CleanupResponse(int deleted, String cutoffTime, long olderThanMinutes)
  {
  }
}
