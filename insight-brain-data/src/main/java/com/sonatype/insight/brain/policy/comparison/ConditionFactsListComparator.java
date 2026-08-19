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

public class ConditionFactsListComparator
    implements Comparator<List<ConditionFact>>
{
  static final Comparator<ConditionFact> CONDITION_FACT_COMPARATOR = new ConditionFactComparator();

  @Override
  public int compare(List<ConditionFact> conditionFacts1, List<ConditionFact> conditionFacts2) {
    // ConditionFact count
    int result = conditionFacts1.size() - conditionFacts2.size();
    if (result != 0) {
      return conditionFacts1.size() - conditionFacts2.size();
    }

    // Condition facts
    // Sort the two list of condition facts before comparing them one by one.
    conditionFacts1 = new ArrayList<>(conditionFacts1);
    conditionFacts1.sort(CONDITION_FACT_COMPARATOR);
    conditionFacts2 = new ArrayList<>(conditionFacts2);
    conditionFacts2.sort(CONDITION_FACT_COMPARATOR);

    Iterator<ConditionFact> conditionFacts2Iter = conditionFacts2.iterator();
    for (ConditionFact conditionFact1 : conditionFacts1) {
      ConditionFact conditionFact2 = conditionFacts2Iter.next();
      result = CONDITION_FACT_COMPARATOR.compare(conditionFact1, conditionFact2);
      if (result != 0) {
        return result;
      }
    }

    return 0;
  }
}
