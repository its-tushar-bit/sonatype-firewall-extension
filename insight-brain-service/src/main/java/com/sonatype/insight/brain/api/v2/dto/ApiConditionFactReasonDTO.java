/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import com.sonatype.clm.dto.model.policy.ConditionFact;

/**
 * @since 1.81
 */
public class ApiConditionFactReasonDTO
{
  public String reason;

  public ApiConditionFactReasonDTO(ConditionFact conditionFact) {
    reason = conditionFact.getReason();
  }
}
