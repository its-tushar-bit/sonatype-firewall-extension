/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.sonatype.insight.json.store.ISODateSerializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * @since 1.82.0
 */
public class ApiPolicyViolationDiffDTO
{
  public ApiApplicationDTO application;

  public ApiApplicationEvaluationCommitDTO fromCommit;

  public ApiApplicationEvaluationCommitDTO toCommit;

  @JsonSerialize(using = ISODateSerializer.class)
  public Date diffTime;

  public Set<ApiPolicyViolationForDiffDTO> addedViolations = new HashSet<>();

  public Set<ApiPolicyViolationForDiffDTO> sameViolations = new HashSet<>();

  public Set<ApiPolicyViolationForDiffDTO> removedViolations = new HashSet<>();
}
