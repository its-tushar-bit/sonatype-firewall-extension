/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.remediation;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiComponentOverrideOptionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiPolicyWaiverOptionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionDTO;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @since 1.64
 */
public class ApiComponentRemediationValueDTO
{
  public List<ApiVersionChangeOptionDTO> versionChanges = new ArrayList<>();

  // ignoring the other fields until we release those remediation types
  @JsonIgnore
  public List<ApiPolicyWaiverOptionDTO> policyWaivers = new ArrayList<>();

  @JsonIgnore
  public List<ApiComponentOverrideOptionDTO> componentOverrides = new ArrayList<>();
}
