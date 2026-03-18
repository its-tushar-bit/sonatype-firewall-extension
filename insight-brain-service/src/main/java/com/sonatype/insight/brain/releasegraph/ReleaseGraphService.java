/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.releasegraph;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.error.HttpStatusCode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates the release graph images for the report.
 *
 * @since 1.10
 */
@Named
@Singleton
public class ReleaseGraphService
{
  public static final String CONTENT_TYPE = "image/png";

  private static final Logger log = LoggerFactory.getLogger(ReleaseGraphService.class);

  private final ReleaseGraphCacheProvider releaseGraphCacheProvider;

  @Inject
  public ReleaseGraphService(ReleaseGraphCacheProvider releaseGraphCacheProvider) {
    this.releaseGraphCacheProvider = releaseGraphCacheProvider;
  }

  public byte[] getImage(
      final String applicationPublicId,
      final String scanId,
      ComponentIdentifier componentIdentifier)
  {
    log.debug("Creating popularity graph for {} for scan {}", componentIdentifier, scanId);
    try {
      return releaseGraphCacheProvider.get()
          .get(new ReleaseGraphKey(componentIdentifier, new ReportItemKey(applicationPublicId, scanId)));
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

      throw new RuntimeException(
          "Error creating popularity graph for " + componentIdentifier + " for report " + scanId, e);
    }
  }
}
