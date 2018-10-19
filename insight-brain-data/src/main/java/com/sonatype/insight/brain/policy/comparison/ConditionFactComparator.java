/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.comparison;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Comparator;

import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.insight.brain.model.policy.facts.ConditionTrigger;
import com.sonatype.insight.json.store.JsonUtils;

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
        try {
          // This deserializes json into ConditionTrigger instances where ConditionTrigger.trigger is a map of the
          // property names/values of the original object that triggered the policy condition.
          // So we compare two maps for equality below...
          ConditionTrigger conditionTrigger1 = JsonUtils.parse(conditionFact1.getTriggerJson(), ConditionTrigger.class);
          ConditionTrigger conditionTrigger2 = JsonUtils.parse(conditionFact2.getTriggerJson(), ConditionTrigger.class);
          if (conditionTrigger1.getTrigger().equals(conditionTrigger2.getTrigger())) {
            return 0;
          }
        }
        catch (IOException e) {
          throw new UncheckedIOException(e);
        }
        // If the triggers are not equal, then the order is not important as long as it is consistent.
        // The comparison of the triggers as json should be good enough.
        result = conditionFact1.getTriggerJson().compareTo(conditionFact2.getTriggerJson());
      }

      return result;
    }

    return 0;
  }
}
