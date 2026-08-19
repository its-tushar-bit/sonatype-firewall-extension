/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.List;

/**
 * List of Lifecycle Repository Managers
 *
 * @since 1.198
 */
public class ApiLifecycleRepositoryManagerListDTO
{
  public List<ApiLifecycleRepositoryManagerDTO> repositoryManagers;

  public ApiLifecycleRepositoryManagerListDTO() {
  }

  public ApiLifecycleRepositoryManagerListDTO(
      final List<ApiLifecycleRepositoryManagerDTO> repositoryManagers)
  {
    this.repositoryManagers = repositoryManagers;
  }
}
