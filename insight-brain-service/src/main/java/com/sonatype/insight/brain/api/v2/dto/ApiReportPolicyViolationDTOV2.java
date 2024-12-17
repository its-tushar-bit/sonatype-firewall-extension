/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * @since 1.64
 */
public class ApiReportPolicyViolationDTOV2
{
  public String policyId;

  public String policyName;

  @JsonInclude(Include.NON_NULL)
  public String policyThreatCategory;

  public int policyThreatLevel;

  public String policyViolationId;

  public boolean waived;

  @JsonInclude(Include.NON_NULL)
  public boolean waivedWithAutoWaiver;

  /**
   * @deprecated Use legacyViolation
   */
  @Deprecated
  public boolean grandfathered;

  public boolean legacyViolation;

  public List<ApiReportConstraintViolationDTOV2> constraints = new ArrayList<>();
}
