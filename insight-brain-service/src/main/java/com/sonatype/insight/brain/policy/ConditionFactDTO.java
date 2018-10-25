/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import com.sonatype.clm.dto.model.policy.ConditionFact;

public class ConditionFactDTO
{
  public String summary;

  public String reason;

  public ConditionFactDTO(ConditionFact conditionFact) {
    summary = conditionFact.getSummary();
    reason = conditionFact.getReason();
  }
}
