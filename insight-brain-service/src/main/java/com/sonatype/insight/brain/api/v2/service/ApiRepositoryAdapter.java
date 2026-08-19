/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryDTO;
import com.sonatype.insight.brain.model.repository.Repository;

public class ApiRepositoryAdapter
{
  public static ApiRepositoryDTO convert(Repository repository) {
    ApiRepositoryDTO repositoryDTO = new ApiRepositoryDTO();
    repositoryDTO.repositoryId = repository.getId();
    repositoryDTO.publicId = repository.getPublicId();
    repositoryDTO.format = repository.getFormat();
    return repositoryDTO;
  }
}
