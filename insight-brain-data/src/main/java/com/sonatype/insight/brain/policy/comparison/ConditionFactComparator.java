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

    // Condition trigger - prioritize comparison by actual trigger content over index
    // Not all condition types store trigger data.
    if (conditionFact1.getConditionIndex() != null && conditionFact2.getConditionIndex() != null &&
        conditionFact1.getTriggerJson() != null && conditionFact2.getTriggerJson() != null) {
      try {
        // De-serialize and then re-serialize the triggers in order to ensure consistent formatting and key ordering
        // for the string-based comparison below
        ConditionTrigger conditionTrigger1 = JsonUtils.parse(conditionFact1.getTriggerJson(), ConditionTrigger.class);
        ConditionTrigger conditionTrigger2 = JsonUtils.parse(conditionFact2.getTriggerJson(), ConditionTrigger.class);

        // Compare only the trigger content, not the conditionIndex within ConditionTrigger
        // This allows violations with the same trigger (e.g., same coordinate pattern) but different indexes
        // to be considered equal, fixing CLM-38434
        String triggerString1 = objectMapper.writeValueAsString(conditionTrigger1.getTrigger());
        String triggerString2 = objectMapper.writeValueAsString(conditionTrigger2.getTrigger());

        result = triggerString1.compareTo(triggerString2);
        // When both conditions have trigger data, the trigger content is the definitive comparison
        // Return immediately regardless of whether triggers are equal or different
        // This prevents index from affecting comparison when trigger data is available
        return result;
      }
      catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    // Condition index
    // Only compare by index if:
    // 1. Both have non-null indexes, AND
    // 2. One or both lack trigger data
    if (conditionFact1.getConditionIndex() != null && conditionFact2.getConditionIndex() != null) {
      result = conditionFact1.getConditionIndex() - conditionFact2.getConditionIndex();
      if (result != 0) {
        return result;
      }
    }

    return 0;
  }
}
