/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryComponentEvaluationRequestList.ApiRepositoryComponentEvaluationRequest;
import com.sonatype.insight.json.store.ApiDateFormat;

public class ApiRepositoryComponentEvaluationResultList
{
  public String repositoryManagerId;

  public String repositoryId;

  public String repositoryPublicId;

  public String repositoryType;

  public List<ApiRepositoryComponentEvaluationResult> results = new ArrayList<>();

  public static class ApiRepositoryComponentEvaluationResult
  {
    public boolean quarantined;

    @ApiDateFormat
    public Date quarantineDate;

    public ApiRepositoryComponentEvaluationRequest component;

    @ApiDateFormat
    public Date catalogDate;

    public List<ApiPolicyViolationDTOV2> policyViolations = new ArrayList<>();
  }
}
