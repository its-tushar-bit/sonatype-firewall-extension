/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * @since 1.168.0
 */
public class ApiSecurityIssueAnalysisDTO
{
  @JsonInclude(Include.NON_NULL)
  public String state;

  @JsonInclude(Include.NON_NULL)
  public String justification;

  @JsonInclude(Include.NON_NULL)
  public String response;

  @JsonInclude(Include.NON_NULL)
  public String detail;
}
