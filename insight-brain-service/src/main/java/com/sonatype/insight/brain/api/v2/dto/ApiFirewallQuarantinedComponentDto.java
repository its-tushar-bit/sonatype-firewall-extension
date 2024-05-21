/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.Date;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.dataaccess.repository.FirewallQuarantinedComponentDetails;
import com.sonatype.insight.json.store.ApiDateFormat;

public class ApiFirewallQuarantinedComponentDto
{
  public Integer threatLevel;

  public String policyName;

  public boolean quarantined;

  @ApiDateFormat
  public Date quarantineDate;

  public ComponentIdentifier componentIdentifier;

  public String pathname;

  public String displayName;

  public String repositoryId;

  public String repositoryName;

  public String hash;

  public String matchState;

  public ApiFirewallQuarantinedComponentDto() {
  }

  public ApiFirewallQuarantinedComponentDto(final FirewallQuarantinedComponentDetails details) {
    ComponentIdentifier componentIdentifierFromJson = ComponentIdentifierAdapter
        .formatAndJsonToComponentIdentifier(details.componentIdFormat, details.componentIdCoordinatesJson);

    this.threatLevel = details.threatLevel;
    this.policyName = details.policyName;
    this.quarantined = details.quarantined;
    this.quarantineDate = details.quarantineDate;
    this.componentIdentifier = componentIdentifierFromJson;
    this.pathname = details.pathname;
    this.displayName = details.displayName;
    this.repositoryId = details.repositoryId;
    this.repositoryName = details.repositoryName;
    this.hash = details.hash;
    this.matchState = details.matchState;
  }
}
