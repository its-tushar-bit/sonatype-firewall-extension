/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.api.v2.dto.ApiOwnerRepositoryConnectionsDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryConnectionDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryConnectionStatusDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiStatusDTO;
import com.sonatype.insight.brain.model.OwnerType;

/**
 * Resource for configuring repository connections
 */
public interface ApiRepositoryConnectionResourceV2
{
  ApiRepositoryConnectionDTO addRepositoryConnection(
      OwnerType ownerType,
      String internalOwnerId,
      ApiRepositoryConnectionDTO repositoryConnection);

  ApiRepositoryConnectionDTO updateRepositoryConnection(
      OwnerType ownerType,
      String internalOwnerId,
      String repositoryConnectionId,
      ApiRepositoryConnectionDTO repositoryConnection);

  void deleteRepositoryConnection(OwnerType ownerType, String internalOwnerId, String repositoryConnectionId);

  ApiRepositoryConnectionDTO getRepositoryConnection(
      OwnerType ownerType,
      String internalOwnerId,
      String repositoryConnectionId);

  ApiOwnerRepositoryConnectionsDTO getOwnerRepositoryConnections(
      OwnerType ownerType,
      String internalOwnerId,
      boolean inherit);

  void upateOwnerRepositoryConnectionStatus(
      OwnerType ownerType,
      String internalOwnerId,
      ApiRepositoryConnectionStatusDTO repositoryConnectionStatusDTO);

  ApiStatusDTO testRepositoryConnection(
      OwnerType ownerType,
      String internalOwnerId,
      ApiRepositoryConnectionDTO repositoryConnection);

  ApiStatusDTO testRepositoryConnection(
      OwnerType ownerType,
      String internalOwnerId,
      String repositoryConnectionId);
}
