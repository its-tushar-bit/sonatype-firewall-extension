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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

class ConditionFactComparator implements Comparator<ConditionFact>
{
  // uses its own object mapper since it needs specific configuration options that can impact performance
  private final ObjectMapper objectMapper;

  public ConditionFactComparator() {
    objectMapper = new ObjectMapper();
    objectMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
  }

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
          // De-serialize and then re-serialize the triggers in order to ensure consistent formatting and key ordering
          // for the string-based comparison below
          ConditionTrigger conditionTrigger1 = JsonUtils.parse(conditionFact1.getTriggerJson(), ConditionTrigger.class);
          ConditionTrigger conditionTrigger2 = JsonUtils.parse(conditionFact2.getTriggerJson(), ConditionTrigger.class);

          String triggerString1 = objectMapper.writeValueAsString(conditionTrigger1);
          String triggerString2 = objectMapper.writeValueAsString(conditionTrigger2);

          return triggerString1.compareTo(triggerString2);
        }
        catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      }

      return result;
    }

    return 0;
  }
}
