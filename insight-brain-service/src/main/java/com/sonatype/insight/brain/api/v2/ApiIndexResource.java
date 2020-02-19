/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.io.IOException;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.search.index.IndexService;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.vulnerability.VulnerabilityDetailResource;

import com.codahale.metrics.annotation.Timed;

/**
 * @since GLOBAL_SEARCH
 */
@Named
@Timed
@Path(PublicApiPaths.INDEX_RESOURCE_PATH)
public class ApiIndexResource
{
  private final IndexService indexService;

  private final InsightWork insightWork;

  private final VulnerabilityDetailResource vulnerabilityDetailResource;

  @Inject
  public ApiIndexResource(
      IndexService indexService,
      InsightWork insightWork,
      VulnerabilityDetailResource vulnerabilityDetailResource)
  {
    this.indexService = indexService;
    this.insightWork = insightWork;
    this.vulnerabilityDetailResource = vulnerabilityDetailResource;
  }

  @POST
  public void createSearchIndex() throws IOException {
    indexService
        .createSearchIndex(insightWork.getWorkDir().toPath(), this::getHtml);
  }

  private String getHtml(String refId) {
    String refIdLower = refId.toLowerCase(Locale.ROOT);
    String source;
    if (refIdLower.startsWith("cve")) {
      source = "cve";
    }
    else if (refIdLower.startsWith("sonatype")) {
      source = "sonatype";
    }
    else {
      return refIdLower;
    }
    return vulnerabilityDetailResource.getDetails(source, refId, null, null, null, null, null, null).getHtmlDetails();
  }
}
