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

import com.sonatype.clm.dto.model.policy.ConstraintFact;

import static com.sonatype.insight.brain.policy.comparison.AutoPolicyWaiverViolationConstraintFactComparator.CONSTRAINT_FACT_COMPARATOR;
import static com.sonatype.insight.brain.utils.CompareUtil.compareObjectsByNull;

/**
 * {@link Comparator} for {@link List}s of {@link ConstraintFact}s. First compares lists to have value and same amount
 * of items, after which we sort the lists and compare each {@link ConstraintFact} through the
 * {@link AutoPolicyWaiverViolationConstraintFactComparator}
 */
public class AutoPolicyWaiverViolationConstraintFactsListComparator
    implements Comparator<List<ConstraintFact>>
{
  public static final Comparator<List<ConstraintFact>> CONSTRAINT_FACTS_LIST_COMPARATOR =
      new AutoPolicyWaiverViolationConstraintFactsListComparator();

  @Override
  public int compare(final List<ConstraintFact> constraintFacts1, final List<ConstraintFact> constraintFacts2) {
    if (constraintFacts1 == null && constraintFacts2 == null) {
      return 0;
    }

    int result = compareObjectsByNull(constraintFacts1, constraintFacts2);
    if (result != 0) {
      return result;
    }

    result = constraintFacts1.size() - constraintFacts2.size();
    if (result != 0) {
      return result;
    }

    // Sort the two list of constraint facts before comparing them one by one.
    List<ConstraintFact> sortedConstraintFacts1 = new ArrayList<>(constraintFacts1);
    sortedConstraintFacts1.sort(CONSTRAINT_FACT_COMPARATOR);

    List<ConstraintFact> sortedConstraintFacts2 = new ArrayList<>(constraintFacts2);
    sortedConstraintFacts2.sort(CONSTRAINT_FACT_COMPARATOR);

    Iterator<ConstraintFact> constraintFacts2Iter = sortedConstraintFacts2.iterator();
    for (ConstraintFact constraintFact1 : sortedConstraintFacts1) {
      ConstraintFact constraintFact2 = constraintFacts2Iter.next();

      result = CONSTRAINT_FACT_COMPARATOR.compare(constraintFact1, constraintFact2);
      if (result != 0) {
        return result;
      }
    }

    return 0;
  }
}
