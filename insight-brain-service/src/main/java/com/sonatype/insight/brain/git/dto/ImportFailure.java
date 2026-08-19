/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.dto;

import com.sonatype.nexus.scm.api.model.SCMRepository;

public class ImportFailure
{
  private String errorMessage;

  private SCMRepository repository;

  // for jackson
  public ImportFailure() {
  }

  public ImportFailure(
      final SCMRepository repository,
      final String errorMessage)
  {
    this.repository = repository;
    this.errorMessage = errorMessage;
  }

  public SCMRepository getRepository() {
    return repository;
  }

  public String getErrorMessage() {
    return errorMessage;
  }
}
