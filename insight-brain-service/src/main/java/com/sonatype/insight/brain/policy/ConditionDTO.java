/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.conditions.ConditionTypes;

public class ConditionDTO
{
  public String summary;

  public ConditionDTO(final Condition condition) {
    summary = ConditionTypes.getById(condition.getConditionTypeId()).explainCondition(condition);
  }
}
