/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v1.dto;

import java.util.List;

import com.sonatype.insight.brain.api.v2.dto.ApiPolicyViolationDTOV2;

/**
 * @deprecated since 1.13.0, use {@link ApiPolicyViolationDTOV2}
 *
 * @since 1.12.0
 */
@Deprecated
public class ApiPolicyViolationDTO
{
  public String policyId;

  public String policyName;

  public String stageId;

  public String reportUrl;

  public List<ApiConstraintViolationDTO> constraintViolations;

  public ApiMavenComponentDTO mavenComponent;
}
