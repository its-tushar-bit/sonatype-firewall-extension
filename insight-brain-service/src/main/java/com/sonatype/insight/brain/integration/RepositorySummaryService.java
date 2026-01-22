/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;

import java.util.List;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.AuthzFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class RepositorySummaryService
{
  private static final Logger log = LoggerFactory.getLogger(RepositorySummaryService.class);

  private final RepositoryDAO repositoryDAO;

  @Inject
  public RepositorySummaryService(final RepositoryDAO repositoryDAO) {
    this.repositoryDAO = repositoryDAO;
  }

  List<RepositorySummary> getRepositories() {
    log.debug("Received request to get repositories");
    return toRepositorySummaryList(getRepositoriesForEvaluateComponent());
  }

  @AuthzFilter(permission = Permission.EVALUATE_COMPONENT, context = AuthzFilter.Context.REPOSITORY)
  List<Repository> getRepositoriesForEvaluateComponent() {
    return repositoryDAO.getAll();
  }

  private List<RepositorySummary> toRepositorySummaryList(List<Repository> repositories) {
    List<RepositorySummary> result = repositories.stream() ///
        .map(repo -> new RepositorySummary(repo.getId(), repo.getName())) //
        .sorted((repo1, repo2) -> repo1.name.compareToIgnoreCase(repo2.name)) //
        .collect(Collectors.toList());

    return result;
  }
}
