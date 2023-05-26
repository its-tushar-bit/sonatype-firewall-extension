/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.dto;

import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.api.v2.dto.ApiOwnerDTO;
import com.sonatype.nexus.scm.SourceControlProvider;

public class ApiUserRateLimitsDTO
{
  public String user;

  public SourceControlProvider provider;

  public Set<ApiOwnerDTO> definingOwners;

  public Set<ApiOwnerDTO> associatedApplications;

  public List<ApiRateLimitDTO> rateLimits;
}
