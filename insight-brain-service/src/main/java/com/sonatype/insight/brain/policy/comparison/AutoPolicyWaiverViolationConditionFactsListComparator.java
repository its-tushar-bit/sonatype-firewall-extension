/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.comparison;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.sonatype.clm.dto.model.policy.ConditionFact;

import static com.sonatype.insight.brain.policy.comparison.AutoPolicyWaiverViolationConditionFactComparator.CONDITION_FACT_COMPARATOR;
import static com.sonatype.insight.brain.utils.CompareUtil.compareObjectsByNull;

/**
 * {@link Comparator} for {@link List}s of {@link ConditionFact}s. First compares lists to have value and same amount of
 * items, after which we sort the lists and compare each {@link ConditionFact} through the
 * {@link AutoPolicyWaiverViolationConditionFactComparator}
 */
public class AutoPolicyWaiverViolationConditionFactsListComparator
    implements Comparator<List<ConditionFact>>
{
  static final Comparator<List<ConditionFact>> CONDITION_FACTS_LIST_COMPARATOR =
      new AutoPolicyWaiverViolationConditionFactsListComparator();

  @Override
  public int compare(final List<ConditionFact> conditionFacts1, final List<ConditionFact> conditionFacts2) {
    if (conditionFacts1 == null && conditionFacts2 == null) {
      return 0;
    }

    int result = compareObjectsByNull(conditionFacts1, conditionFacts2);
    if (result != 0) {
      return result;
    }

    result = conditionFacts1.size() - conditionFacts2.size();
    if (result != 0) {
      return result;
    }

    // Condition facts
    // Sort the two list of condition facts before comparing them one by one.
    List<ConditionFact> sortedConditionFacts1 = new ArrayList<>(conditionFacts1);
    sortedConditionFacts1.sort(CONDITION_FACT_COMPARATOR);

    List<ConditionFact> sortedConditionFacts2 = new ArrayList<>(conditionFacts2);
    sortedConditionFacts2.sort(CONDITION_FACT_COMPARATOR);

    Iterator<ConditionFact> conditionFacts2Iter = sortedConditionFacts2.iterator();
    for (ConditionFact conditionFact1 : sortedConditionFacts1) {
      ConditionFact conditionFact2 = conditionFacts2Iter.next();
      result = CONDITION_FACT_COMPARATOR.compare(conditionFact1, conditionFact2);
      if (result != 0) {
        return result;
      }
    }

    return 0;
  }
}
