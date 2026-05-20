/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.githubapp;

import java.util.List;
import java.util.Optional;

import com.sonatype.insight.brain.dataaccess.githubapp.GitHubAppDAO;
import com.sonatype.insight.brain.model.githubapp.GitHubApp;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class GitHubAppSelectionService
{
  private static final Logger log = LoggerFactory.getLogger(GitHubAppSelectionService.class);

  private final GitHubAppDAO gitHubAppDAO;

  private final GitHubAppSelectionCache selectionCache;

  @Inject
  public GitHubAppSelectionService(GitHubAppDAO gitHubAppDAO, GitHubAppSelectionCache selectionCache) {
    this.gitHubAppDAO = gitHubAppDAO;
    this.selectionCache = selectionCache;
  }

  /**
   * Selects a GitHub App for the given owner using deterministic hash-based routing: the same owner always maps to the
   * same app. When apps are added/removed, the cache is invalidated and assignments may shift.
   */
  public GitHubApp select(String requestingOwnerId) {
    if (requestingOwnerId == null) {
      return null;
    }
    Optional<GitHubApp> cached = selectionCache.get(requestingOwnerId);
    if (cached != null) {
      return cached.orElse(null);
    }

    List<GitHubApp> apps = gitHubAppDAO.getNearestGitHubApps(requestingOwnerId);
    if (apps.isEmpty()) {
      selectionCache.put(requestingOwnerId, Optional.empty());
      return null;
    }

    GitHubApp selected;
    if (apps.size() == 1) {
      selected = apps.get(0);
    }
    else {
      int index = Math.floorMod(requestingOwnerId.hashCode(), apps.size());
      selected = apps.get(index);
    }

    selectionCache.put(requestingOwnerId, Optional.of(selected));
    log.debug("Selected GitHub App {} for IQ application {}", selected.getId(), requestingOwnerId);
    return selected;
  }
}
