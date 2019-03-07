/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dto.repository;

import java.util.List;

/**
 * @since 1.19.0
 */
public class RepositoriesDTO
{
  public List<RepositoryDTO> repositories;

  // Needed for de-serialization
  public RepositoriesDTO() {
  }

  public RepositoriesDTO(final List<RepositoryDTO> repositories) {
    this.repositories = repositories;
  }
}
