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

public class ConstraintFactsListComparator
    implements Comparator<List<ConstraintFact>>
{
  public static final Comparator<List<ConstraintFact>> CONSTRAINT_FACTS_LIST_COMPARATOR =
      new ConstraintFactsListComparator();

  static final Comparator<ConstraintFact> CONSTRAINT_FACT_COMPARATOR = new ConstraintFactComparator();

  @Override
  public int compare(List<ConstraintFact> constraintFacts1, List<ConstraintFact> constraintFacts2) {
    // ConstraintFact count
    int result = constraintFacts1.size() - constraintFacts2.size();
    if (result != 0) {
      return result;
    }

    // Sort the two list of constraint facts before comparing them one by one.
    constraintFacts1 = new ArrayList<>(constraintFacts1);
    constraintFacts1.sort(CONSTRAINT_FACT_COMPARATOR);
    constraintFacts2 = new ArrayList<>(constraintFacts2);
    constraintFacts2.sort(CONSTRAINT_FACT_COMPARATOR);

    Iterator<ConstraintFact> constraintFacts2Iter = constraintFacts2.iterator();
    for (ConstraintFact constraintFact1 : constraintFacts1) {
      ConstraintFact constraintFact2 = constraintFacts2Iter.next();

      result = CONSTRAINT_FACT_COMPARATOR.compare(constraintFact1, constraintFact2);
      if (result != 0) {
        return result;
      }
    }

    return 0;
  }
}
