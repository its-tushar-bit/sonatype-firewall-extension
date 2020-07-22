/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiPullRequestResult;
import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiPullRequestResults;
import com.sonatype.insight.brain.git.EnhancedPullRequestResult;

/**
 * @since 1.97
 */
@Named
@Singleton
public class ApiSourceControlMetricsAdapter
{
  public ApiPullRequestResults convertToDTO(final List<EnhancedPullRequestResult> results) {
    ApiPullRequestResults apiPullRequestResults = new ApiPullRequestResults();
    if (null == results || results.isEmpty()) {
      apiPullRequestResults.results = Collections.emptyList();
    }
    else {
      apiPullRequestResults.results = results.stream().map(result ->
      {
        ApiPullRequestResult apiPullRequestResult = new ApiPullRequestResult();
        apiPullRequestResult.startTime = result.getStartTime();
        apiPullRequestResult.title = result.getTitle();
        apiPullRequestResult.reasoning = result.getReasoning();
        apiPullRequestResult.exceptionThrown = result.isExceptionThrown();
        apiPullRequestResult.successful = result.getTiming().isSuccessful();
        apiPullRequestResult.totalTime = result.getTiming().getTotalTime();
        return apiPullRequestResult;
      }).collect(Collectors.toList());
    }
    return apiPullRequestResults;
  }
}
