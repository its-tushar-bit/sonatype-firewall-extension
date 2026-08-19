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
public class ApiReportComponentPolicyViolationsDTOV2
    extends ApiComponentDTOV2
{
  public String matchState;

  public List<String> pathnames = new ArrayList<>();

  @JsonInclude(Include.NON_NULL)
  public ApiDependencyDataDTO dependencyData;

  // occurrences of violations, in no particular order
  public List<ApiReportPolicyViolationDTOV2> violations = new ArrayList<>();
}
