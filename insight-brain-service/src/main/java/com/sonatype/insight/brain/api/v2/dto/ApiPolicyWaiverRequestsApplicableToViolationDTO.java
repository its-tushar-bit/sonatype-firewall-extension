/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.ArrayList;
import java.util.List;

public class ApiPolicyWaiverRequestsApplicableToViolationDTO
{
  public List<ApiPolicyWaiverRequestDTO> activeWaiverRequests = new ArrayList<>();

  public List<ApiPolicyWaiverRequestDTO> expiredWaiverRequests = new ArrayList<>();
}
