/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.resultsview;

import java.util.Date;

import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryResultsDetails;

public class RepositoryResultsDetailsResponseDto
{
  public Integer threatLevel;

  public String policyName;

  public String componentDisplayText;

  public Date quarantineTime;

  public Boolean waived;

  public RepositoryResultsDetailsResponseDto() {
  }

  public RepositoryResultsDetailsResponseDto(final RepositoryResultsDetails details) {
    this.threatLevel = details.policyThreatLevel;
    this.policyName = details.policyName;
    this.componentDisplayText = buildComponentDisplayText(details);
    this.quarantineTime = details.quarantineTime;
    this.waived = details.waived;
  }

  private static String buildComponentDisplayText(final RepositoryResultsDetails details) {
    final ComponentIdentifier componentIdentifier =
        ComponentIdentifierAdapter.formatAndJsonToComponentIdentifier(details.componentIdFormat,
            details.componentIdCoordinatesJson);
    if (componentIdentifier != null) {
      return ComponentDisplayNameUtil.fromIdentifier(componentIdentifier).toString();
    }
    String pathname = details.pathname;
    if (pathname == null) {
      return null;
    }

    return pathname.substring(pathname.lastIndexOf('/') + 1) + " (" + pathname + ")";
  }
}
