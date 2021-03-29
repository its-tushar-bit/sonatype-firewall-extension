/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.dto;

import java.util.List;

import com.sonatype.nexus.scm.api.model.SCMRepository;

/**
 * Results from an import. Extracted into a separate class in order
 * to handle errors as well as successful imports
 */
public class ImportResults
{
  private List<SCMRepository> importedRepositories;

  private List<ImportFailure> failedRepositories;

  private int failedImportCount;

  // for Jackson
  public ImportResults() {
  }

  public ImportResults(final List<SCMRepository> importedRepositories, final List<ImportFailure> failedRepositories) {
    this.importedRepositories = importedRepositories;
    this.failedRepositories = failedRepositories;
    this.failedImportCount = failedRepositories.size();
  }

  public List<SCMRepository> getImportedRepositories() {
    return importedRepositories;
  }

  public List<ImportFailure> getFailedRepositories() {
    return failedRepositories;
  }

  public int getFailedImportCount() {
    return failedImportCount;
  }
}
