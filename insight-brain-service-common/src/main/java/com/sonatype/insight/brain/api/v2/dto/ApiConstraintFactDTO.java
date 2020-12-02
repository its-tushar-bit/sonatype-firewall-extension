/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.List;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.policy.ConstraintFact;

/**
 * @since 1.81
 */
public class ApiConstraintFactDTO
{
  public String constraintName;

  public String constraintId;

  public List<ApiConditionFactReasonDTO> reasons;

  public ApiConstraintFactDTO(ConstraintFact constraintFact) {
    constraintId = constraintFact.getConstraintId();
    constraintName = constraintFact.getConstraintName();
    reasons = constraintFact.getConditionFacts()
        .stream()
        .map(ApiConditionFactReasonDTO::new)
        .collect(Collectors.toList());
  }
}
