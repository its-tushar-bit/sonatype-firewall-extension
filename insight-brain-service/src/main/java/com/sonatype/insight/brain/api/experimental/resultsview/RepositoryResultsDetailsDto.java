/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.resultsview;

import java.util.Date;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryResultsDetails;

public class RepositoryResultsDetailsDto
{
  public Integer threatLevel;

  public String policyName;

  public String repositoryManagerId;

  public String repositoryId;

  public String componentDisplayText;

  public String pathname;

  public ApiComponentIdentifierDTOV2 componentIdentifier;

  public String hash;

  public String matchStateId;

  public Date quarantineTime;

  public Boolean waived;

  public RepositoryResultsDetailsDto() {
  }

  public RepositoryResultsDetailsDto(final RepositoryResultsDetails details) {
    ComponentIdentifier componentIdentifierFromJson = ComponentIdentifierAdapter
        .formatAndJsonToComponentIdentifier(details.componentIdFormat, details.componentIdCoordinatesJson);

    this.threatLevel = details.policyThreatLevel;
    this.policyName = details.policyName;
    this.repositoryManagerId = details.repositoryManagerId;
    this.repositoryId = details.repositoryId;
    this.componentDisplayText = details.componentDisplayName;
    this.pathname = details.pathname;
    this.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifierFromJson);
    this.hash = details.hash;
    this.matchStateId = details.matchStateId;
    this.quarantineTime = details.quarantineTime;
    this.waived = details.waived;
  }
}
