/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import java.util.List;

import com.sonatype.nexus.scm.api.model.SCMRepository;

/**
 * Results from an import. Extracted into a separate class in order
 * to handle errors as well as successful imports
 */
public class ImportResults
{
  private List<SCMRepository> importedRepositories;

  private int failedImportCount;

  // for Jackson
  public ImportResults() {
  }

  public ImportResults(final List<SCMRepository> importedRepositories, final int failedImportCount) {
    this.importedRepositories = importedRepositories;
    this.failedImportCount = failedImportCount;
  }

  public List<SCMRepository> getImportedRepositories() {
    return importedRepositories;
  }

  public int getFailedImportCount() {
    return failedImportCount;
  }
}
