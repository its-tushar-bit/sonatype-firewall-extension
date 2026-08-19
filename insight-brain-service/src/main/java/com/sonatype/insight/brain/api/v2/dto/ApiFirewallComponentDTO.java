/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.json.store.ApiDateFormat;

public class ApiFirewallComponentDTO
{
  public String displayName;

  public String repository;

  @ApiDateFormat
  public Date quarantineDate;

  @ApiDateFormat
  public Date dateCleared;

  public List<ApiPolicyViolationDTOV2> quarantinePolicyViolations = new ArrayList<>();

  public ComponentIdentifier componentIdentifier;

  public String pathname;

  public String hash;

  public String matchState;

  public String repositoryId;

  public boolean quarantined;
}
