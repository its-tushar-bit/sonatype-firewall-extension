/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.event.orchestrate.rule.processing;

import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.common.exception.ExceptionHelper;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepositoryUrlErrorRule
    implements EventProcessedListener
{
  private static final Logger log = LoggerFactory.getLogger(RepositoryUrlErrorRule.class);

  @VisibleForTesting
  static final List<String> URL_ERROR_MESSAGES = ImmutableList.of(
      "reason: Not Found",
      "Could not resolve to a Repository");

  @VisibleForTesting
  static final int REPO_URL_ERROR_THRESHOLD = 3;

  private final Map<String, AppRepositoryUrlError> appRepositoryUrlErrorMap = new HashMap<>();

  private final SourceControlUtils sourceControlUtils;

  public RepositoryUrlErrorRule(SourceControlUtils sourceControlUtils) {
    this.sourceControlUtils = sourceControlUtils;
  }

  public boolean canPushEvent(SourceControlEvent event) {
    AppRepositoryUrlError appRepositoryUrlError = appRepositoryUrlErrorMap.get(event.getApplicationId());
    if (null == appRepositoryUrlError || appRepositoryUrlError.errorCount < REPO_URL_ERROR_THRESHOLD) {
      return true;
    }
    else {
      // does the event have an updated repo url?
      GitRepositoryInfo gitRepositoryInfo =
          sourceControlUtils.getGitRepositoryInfoForApplication(event.getApplicationId());
      boolean urlsMatch = gitRepositoryInfo.normalizedRepositoryUrl.equals(appRepositoryUrlError.repositoryUrl);
      if (!urlsMatch) {
        appRepositoryUrlErrorMap.remove(event.getApplicationId());
      }
      else {
        log.debug("Event processing for application {} repository {} suspended due to multiple errors.",
            event.getApplicationId(), gitRepositoryInfo.getRepositoryUrl());
      }
      return !urlsMatch;
    }
  }

  public void onEventProcessingError(SourceControlEvent event, Exception e) {
    if (isUrlRelatedError(e)) {
      GitRepositoryInfo gitRepositoryInfo =
          sourceControlUtils.getGitRepositoryInfoForApplication(event.getApplicationId());
      if (null != gitRepositoryInfo) {
        appRepositoryUrlErrorMap.computeIfAbsent(event.getApplicationId(),
            key -> new AppRepositoryUrlError(gitRepositoryInfo.normalizedRepositoryUrl, 0))
            .increment();
      }
    }
  }

  @Override
  public void onEventProcessed(SourceControlEvent event) {
    appRepositoryUrlErrorMap.remove(event.getApplicationId());
  }

  private static class AppRepositoryUrlError
  {
    String repositoryUrl;

    int errorCount;

    AppRepositoryUrlError(String repositoryUrl, int errorCount) {
      this.repositoryUrl = repositoryUrl;
      this.errorCount = errorCount;
    }

    void increment() {
      errorCount++;
    }
  }

  private boolean isUrlRelatedError(Exception e) {
    if (ExceptionHelper.hasCauseOrSuppressedOfType(e, UnknownHostException.class)) {
      return true;
    }
    return URL_ERROR_MESSAGES.stream().anyMatch(msg -> e.getMessage().contains(msg));
  }
}
