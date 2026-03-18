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
import com.sonatype.clm.dto.model.policy.TriggerReference;
import com.sonatype.clm.dto.model.policy.TriggerReference.Type;
import com.sonatype.insight.brain.model.policy.facts.ConditionTrigger;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import static com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS;
import static com.sonatype.insight.brain.utils.CompareUtil.compareObjectsByNull;
import static com.sonatype.insight.brain.utils.CompareUtil.compareTo;

/**
 * {@link Comparator} for {@link ConditionFact} objects that compares them based on the following fields:
 * <ul>
 * <li>conditionTypeId</li>
 * <li>conditionIndex</li>
 * <li>triggerJson</li>
 * <li>reference</li>
 * <li>reason</li>
 * <li>summary</li>
 * </ul>
 * <p>
 * The conditionTypeId field is compared by its strings.
 * <br>
 * The conditionIndex field is compared by its integers.
 * <br>
 * The triggerJson field is compared by first comparing the conditionIndex field, and then comparing the
 * triggerJson field. If the conditionIndex field is null, the triggerJson field is not compared, then the
 * triggerJson field is first deserialized into a {@link ConditionTrigger} object,
 * then re-serializing the object into a string, and finally comparing the strings.
 * <br>
 * The reference field is compared by first comparing the value field, and then comparing the type field.
 * <br>
 * The type field is compared by first comparing the value field, and then comparing the type field.
 * <br>
 * The reason and summary fields are compared by its strings.
 * </p>
 *
 * <p>
 * This comparison is done differently than in {@link ConditionFactComparator} because the condition facts
 * here are checked for more than just triggerJson, in short if no triggerJson is found we continue to the next
 * fields for comparison.
 * </p>
 */
public class AutoPolicyWaiverViolationConditionFactComparator
    implements Comparator<ConditionFact>
{
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().configure(ORDER_MAP_ENTRIES_BY_KEYS, true);

  public static final Comparator<ConditionFact> CONDITION_FACT_COMPARATOR =
      new AutoPolicyWaiverViolationConditionFactComparator();

  @Override
  public int compare(final ConditionFact conditionFact1, final ConditionFact conditionFact2) {
    if (conditionFact1 == null && conditionFact2 == null) {
      return 0;
    }

    int result = compareObjectsByNull(conditionFact1, conditionFact2);
    if (result != 0) {
      return result;
    }

    result = compareTo(conditionFact1.getConditionTypeId(), conditionFact2.getConditionTypeId());
    if (result != 0) {
      return result;
    }

    result = compareTo(conditionFact1.getConditionIndex(), conditionFact2.getConditionIndex());
    if (result != 0) {
      return result;
    }

    result = compareTriggerJson(conditionFact1, conditionFact2);
    if (result != 0) {
      return result;
    }

    result = compareReferences(conditionFact1.getReference(), conditionFact2.getReference());
    if (result != 0) {
      return result;
    }

    result = compareTo(conditionFact1.getReason(), conditionFact2.getReason());
    if (result != 0) {
      return result;
    }

    return compareTo(conditionFact1.getSummary(), conditionFact2.getSummary());
  }

  private int compareTriggerJson(final ConditionFact conditionFact1, final ConditionFact conditionFact2) {
    // If the condition index is null we don't test further as it's created from the value of triggerJson
    if (conditionFact1.getConditionIndex() == null && conditionFact2.getConditionIndex() == null) {
      return 0;
    }

    // If the condition index don't match we don't test further for trigger json
    int result = compareObjectsByNull(conditionFact1.getConditionIndex(), conditionFact2.getConditionIndex());
    if (result != 0) {
      return result;
    }

    result = conditionFact1.getConditionIndex().compareTo(conditionFact2.getConditionIndex());

    if (result != 0) {
      return result;
    }

    if (conditionFact1.getTriggerJson() == null && conditionFact2.getTriggerJson() == null) {
      return 0;
    }

    result = compareObjectsByNull(conditionFact1.getTriggerJson(), conditionFact2.getTriggerJson());
    if (result != 0) {
      return result;
    }

    try {
      // De-serialize and then re-serialize the triggers in order to ensure consistent formatting and key ordering
      // for the string-based comparison below
      ConditionTrigger conditionTrigger1 = JsonUtils.parse(conditionFact1.getTriggerJson(), ConditionTrigger.class);
      ConditionTrigger conditionTrigger2 = JsonUtils.parse(conditionFact2.getTriggerJson(), ConditionTrigger.class);

      String triggerString1 = OBJECT_MAPPER.writeValueAsString(conditionTrigger1);
      String triggerString2 = OBJECT_MAPPER.writeValueAsString(conditionTrigger2);

      return triggerString1.compareTo(triggerString2);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private int compareReferences(final TriggerReference reference1, final TriggerReference reference2) {
    if (reference1 == null && reference2 == null) {
      return 0;
    }

    int result = compareObjectsByNull(reference1, reference2);
    if (result != 0) {
      return result;
    }

    result = compareTo(reference1.getValue(), reference2.getValue());
    if (result != 0) {
      return result;
    }

    Type type1 = reference1.getType();
    Type type2 = reference2.getType();
    if (type1 == null && type2 == null) {
      return 0;
    }

    result = compareObjectsByNull(type1, type2);
    if (result != 0) {
      return result;
    }

    return reference1.getType().compareTo(reference2.getType());
  }
}
