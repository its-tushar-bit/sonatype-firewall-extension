/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.clm.dto.model.sourcecontrol.ApiSourceControlRepositoryUserDTO;
import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiSourceControlDTO;
import com.sonatype.insight.brain.model.OwnerType;

/**
 * Resource for API Source Control
 */
public interface ApiSourceControlResource
{
  ApiSourceControlDTO getSourceControl(OwnerType ownerType, String internalOwnerId);

  ApiSourceControlDTO addSourceControl(OwnerType ownerType, String internalOwnerId, ApiSourceControlDTO sourceControl);

  ApiSourceControlDTO updateSourceControl(OwnerType ownerType,
                                          String internalOwnerId,
                                          ApiSourceControlDTO sourceControl);

  void deleteSourceControl(OwnerType ownerType, String internalOwnerId);

  ApiSourceControlDTO addOrUpdateSourceControl(String publicId,
                                               String repositoryUrl,
                                               ApiSourceControlRepositoryUserDTO apiSourceControlRepoUserDTO);
}
