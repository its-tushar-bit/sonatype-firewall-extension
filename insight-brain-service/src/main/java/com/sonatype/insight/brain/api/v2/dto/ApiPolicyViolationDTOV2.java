/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.api.v1.dto.ApiConstraintViolationDTO;

/**
 * @since 1.13.0
 */
public class ApiPolicyViolationDTOV2
{
  public String policyId;

  public String policyName;

  public int threatLevel;

  public List<ApiConstraintViolationDTO> constraintViolations = new ArrayList<>();
}
