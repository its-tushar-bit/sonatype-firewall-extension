/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * DTO describing the policy violations data in an application composition report.
 *
 * @since 1.64
 */
public class ApiReportPolicyDataDTOV2
{
  public Date reportTime;

  public String reportTitle;

  /**
   * @since 1.92
   */
  public String commitHash;

  /**
   * @since 1.98
   */
  public String initiator;

  public ApiApplicationBaseDTO application;

  @JsonInclude(content = Include.NON_NULL, value = Include.NON_EMPTY)
  public Map<String, Integer> counts;

  // components in app, in no particular order
  public List<ApiReportComponentPolicyViolationsDTOV2> components = new ArrayList<>();
}
