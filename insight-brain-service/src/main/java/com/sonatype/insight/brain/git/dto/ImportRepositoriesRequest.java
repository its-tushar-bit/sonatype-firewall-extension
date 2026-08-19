/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.dto;

import java.util.List;
import java.util.StringJoiner;

import com.sonatype.nexus.scm.api.model.SCMRepository;

/**
 * Container class to hold the list of repositories to be imported during SCM Onboarding, plus
 * some additional data for telemetry
 */
public class ImportRepositoriesRequest
{
  /**
   * the total number of repositories found, including those that were already imported
   */
  public Integer totalRepoCount;

  /**
   * the number of repositories which have already been imported into IQ
   */
  public Integer prevImportedCount;

  /**
   * the list of repositories to be imported
   */
  public List<SCMRepository> scmRepositories;

  /**
   * empty constructor for JSON serialization
   */
  public ImportRepositoriesRequest() {
  }

  public ImportRepositoriesRequest(
      final List<SCMRepository> scmRepositories,
      final Integer totalRepoCount,
      final Integer prevImportedCount)
  {
    this.scmRepositories = scmRepositories;
    this.totalRepoCount = totalRepoCount;
    this.prevImportedCount = prevImportedCount;
  }

  public Integer getTotalRepoCount() {
    return totalRepoCount;
  }

  public ImportRepositoriesRequest setTotalRepoCount(final Integer totalRepoCount) {
    this.totalRepoCount = totalRepoCount;
    return this;
  }

  public Integer getPrevImportedCount() {
    return prevImportedCount;
  }

  public ImportRepositoriesRequest setPrevImportedCount(final Integer prevImportedCount) {
    this.prevImportedCount = prevImportedCount;
    return this;
  }

  public List<SCMRepository> getScmRepositories() {
    return scmRepositories;
  }

  public ImportRepositoriesRequest setScmRepositories(final List<SCMRepository> scmRepositories) {
    this.scmRepositories = scmRepositories;
    return this;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", ImportRepositoriesRequest.class.getSimpleName() + "[", "]")
        .add("totalRepoCount=" + totalRepoCount)
        .add("prevImportedCount=" + prevImportedCount)
        .add("scmRepositories=" + scmRepositories)
        .toString();
  }
}
