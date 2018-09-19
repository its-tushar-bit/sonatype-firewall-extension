/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.Comparator;

import com.sonatype.clm.dto.model.policy.ConditionFact;

class ConditionFactComparator implements Comparator<ConditionFact>
{
  @Override
  public int compare(ConditionFact conditionFact1, ConditionFact conditionFact2) {
    // Condition type
    int result = conditionFact1.getConditionTypeId().compareTo(conditionFact2.getConditionTypeId());
    if (result != 0) {
      return result;
    }

    // If the condition index is null, then this policy violation was created before we added condition trigger data
    // to policy violations.
    // In this case we ignore the condition index and trigger data in the newer policy violation.

    // Condition index
    if (conditionFact1.getConditionIndex() != null && conditionFact2.getConditionIndex() != null) {
      result = conditionFact1.getConditionIndex() - conditionFact2.getConditionIndex();
      if (result != 0) {
        return result;
      }

      // Condition trigger
      // Not all condition types store trigger data.
      if (conditionFact1.getTriggerJson() != null && conditionFact2.getTriggerJson() != null) {
        result = conditionFact1.getTriggerJson().compareTo(conditionFact2.getTriggerJson());
      }
      if (result != 0) {
        return result;
      }
    }

    return 0;
  }
}