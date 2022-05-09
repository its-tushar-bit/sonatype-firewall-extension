/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.api.v2.dto.ApiOwnerArtifactoryConnectionDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiArtifactoryConnectionDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiArtifactoryConnectionStatusDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiStatusDTO;
import com.sonatype.insight.brain.model.OwnerType;

/**
 * Resource for configuring artifactory connections
 */
public interface ApiArtifactoryConnectionResourceV2
{
  ApiArtifactoryConnectionDTO addArtifactoryConnection(
      OwnerType ownerType,
      String internalOwnerId,
      ApiArtifactoryConnectionDTO artifactoryConnection);

  ApiArtifactoryConnectionDTO updateArtifactoryConnection(
      OwnerType ownerType,
      String internalOwnerId,
      String artifactoryConnectionId,
      ApiArtifactoryConnectionDTO artifactoryConnection);

  void deleteArtifactoryConnection(OwnerType ownerType, String internalOwnerId, String artifactoryConnectionId);

  ApiArtifactoryConnectionDTO getArtifactoryConnection(
      OwnerType ownerType,
      String internalOwnerId,
      String artifactoryConnectionId);

  ApiOwnerArtifactoryConnectionDTO getOwnerArtifactoryConnection(
      OwnerType ownerType,
      String internalOwnerId,
      boolean inherit);

  void updateOwnerArtifactoryConnectionStatus(
      OwnerType ownerType,
      String internalOwnerId,
      ApiArtifactoryConnectionStatusDTO artifactoryConnectionStatusDTO);

  ApiStatusDTO testArtifactoryConnection(
      OwnerType ownerType,
      String internalOwnerId,
      ApiArtifactoryConnectionDTO artifactoryConnection);

  ApiStatusDTO testArtifactoryConnection(
      OwnerType ownerType,
      String internalOwnerId,
      String artifactoryConnectionId);
}
