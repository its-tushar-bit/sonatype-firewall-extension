/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.comparison;

import java.util.Comparator;

import com.sonatype.clm.dto.model.policy.ConstraintFact;

import static com.sonatype.insight.brain.policy.comparison.AutoPolicyWaiverViolationConditionFactsListComparator.CONDITION_FACTS_LIST_COMPARATOR;
import static com.sonatype.insight.brain.utils.CompareUtil.compareObjectsByNull;
import static com.sonatype.insight.brain.utils.CompareUtil.compareTo;

/**
 * {@link Comparator} for {@link ConstraintFact} objects that compares them based on the following fields:
 * <ul>
 * <li>constraintId</li>
 * <li>constraintName</li>
 * <li>operatorName</li>
 * <li>conditionFacts</li>
 * </ul>
 *
 * <p>
 * The constraintId field is compared by its strings.
 * <br>
 * The constraintName field is compared by its strings.
 * <br>
 * The operatorName field is compared by its strings.
 * <br>
 * The conditionFacts field is compared by using the {@link AutoPolicyWaiverViolationConditionFactsListComparator}.
 * </p>
 *
 * <p>
 * This comparison is done differently than in {@link ConstraintFactComparator} because the constraint facts
 * here are checked for more than just conditionFacts and constraintId.
 * </p>
 */
public class AutoPolicyWaiverViolationConstraintFactComparator
    implements Comparator<ConstraintFact>
{
  static final Comparator<ConstraintFact> CONSTRAINT_FACT_COMPARATOR =
      new AutoPolicyWaiverViolationConstraintFactComparator();

  @Override
  public int compare(final ConstraintFact constraintFact1, final ConstraintFact constraintFact2) {
    if (constraintFact1 == null && constraintFact2 == null) {
      return 0;
    }

    int result = compareObjectsByNull(constraintFact1, constraintFact2);
    if (result != 0) {
      return result;
    }

    result = compareTo(constraintFact1.getConstraintId(), constraintFact2.getConstraintId());
    if (result != 0) {
      return result;
    }

    result = compareTo(constraintFact1.getConstraintName(), constraintFact2.getConstraintName());
    if (result != 0) {
      return result;
    }

    result = compareTo(constraintFact1.getOperatorName(), constraintFact2.getOperatorName());
    if (result != 0) {
      return result;
    }

    return CONDITION_FACTS_LIST_COMPARATOR.compare(
        constraintFact1.getConditionFacts(),
        constraintFact2.getConditionFacts());
  }
}
