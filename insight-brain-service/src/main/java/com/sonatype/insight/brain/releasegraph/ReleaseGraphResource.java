/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.releasegraph;

import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.report.ReportDownloader;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.HttpStatusCode;

import com.google.common.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Path("rest/report/{applicationId}/{scanId}/releaseGraph")
public class ReleaseGraphResource
{
  private static final Logger log = LoggerFactory.getLogger(ReleaseGraphResource.class);

  private final InsightWork work;

  private final LoadingCache<ReleaseGraphKey, byte[]> cache;

  private static final long YEAR = 365 * 24 * 60 * 60 * 1000;

  private final ReportDownloader reportDownloader;

  private final CLMLicenseManager licenseManager;

  @Inject
  public ReleaseGraphResource(LoadingCache<ReleaseGraphKey, byte[]> cache, InsightWork work,
      ReportDownloader reportDownloader, CLMLicenseManager licenseManager)
  {
    this.cache = cache;
    this.work = work;
    this.reportDownloader = reportDownloader;
    this.licenseManager = licenseManager;
  }

  @GET
  public Response getImage(@PathParam("applicationId") final String applicationPublicId,
      @PathParam("scanId") final String scanId, @QueryParam("groupId") String groupId,
      @QueryParam("artifactId") String artifactId, @QueryParam("version") String version)
  {
    log.debug("Creating popularity graph for {}:{}:{} for scan {}", groupId, artifactId, version, scanId);
    try {
      return Response
          .ok(cache.get(new ReleaseGraphKey(groupId, artifactId, version, new ReportItemKey(reportDownloader,
              licenseManager.getLicenseFingerprint(), applicationPublicId, scanId, work))), "image/png")
          .expires(new Date(System.currentTimeMillis() + YEAR)).build();
    }
    catch (Exception e) {
      // undo any wrapping of resource exceptions introduced by Guava caches
      for (Throwable t = e; t instanceof RuntimeException; t = t.getCause()) {
        if (t.getClass().isAnnotationPresent(HttpStatusCode.class) || t instanceof WebApplicationException) {
          // Log the original exception so we don't lose error details
          log.error(e.getMessage(), e);
          throw (RuntimeException) t;
        }
      }

      throw new RuntimeException("Error creating popularity graph for " + groupId + ":" + artifactId + ":" + version
          + " for report " + scanId, e);
    }
  }
}
