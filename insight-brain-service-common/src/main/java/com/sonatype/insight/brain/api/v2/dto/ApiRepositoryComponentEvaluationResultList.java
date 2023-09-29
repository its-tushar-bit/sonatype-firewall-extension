/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.ArrayList;
import java.util.List;

public class ApiRepositoryComponentEvaluationResultList
{
  public List<ApiRepositoryComponentEvaluationResult> results = new ArrayList<>();

  public static class ApiRepositoryComponentEvaluationResult
  {
    public List<ApiPolicyViolationDTOV2> policyViolations = new ArrayList<>();
  }
}
