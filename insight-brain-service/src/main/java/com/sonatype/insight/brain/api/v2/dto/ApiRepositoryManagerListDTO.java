/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.List;

public class ApiRepositoryManagerListDTO
{
  public List<ApiRepositoryManagerDTO> repositoryManagers;

  public ApiRepositoryManagerListDTO() {
  }

  public ApiRepositoryManagerListDTO(
      final List<ApiRepositoryManagerDTO> repositoryManagers)
  {
    this.repositoryManagers = repositoryManagers;
  }
}
