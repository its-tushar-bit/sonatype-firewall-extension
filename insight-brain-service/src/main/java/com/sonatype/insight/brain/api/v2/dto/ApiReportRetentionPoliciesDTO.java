/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @since 1.63
 */
public class ApiReportRetentionPoliciesDTO
{
  public Map<String, ApiReportRetentionPolicyDTO> stages = new LinkedHashMap<>();
}
