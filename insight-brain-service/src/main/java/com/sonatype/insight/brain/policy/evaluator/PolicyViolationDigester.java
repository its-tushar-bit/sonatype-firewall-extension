/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import com.sonatype.insight.brain.model.policy.PolicyViolationComparable;

public class PolicyViolationDigester
{
  private static <T extends PolicyViolationComparable> List<T> sort(
      Collection<? extends T> policyViolations,
      Comparator<PolicyViolationComparable> comparator)
  {
    List<T> result = new ArrayList<>();
    result.addAll(policyViolations);
    result.sort(comparator);
    return result;
  }

  public static <T extends PolicyViolationComparable> PolicyViolationDiff<T> digestPolicyViolations(
      Collection<? extends T> oldViolations,
      Collection<? extends T> newViolations)
  {
    return digestPolicyViolations(oldViolations, newViolations, PolicyViolationComparator.COMPARATOR);
  }

  public static <T extends PolicyViolationComparable> PolicyViolationDiff<T> digestPolicyViolations(
      Collection<? extends T> oldViolations,
      Collection<? extends T> newViolations,
      Comparator<PolicyViolationComparable> comparator)
  {
    PolicyViolationDiff<T> diff = new PolicyViolationDiff<>();

    if (oldViolations == null) {
      diff.addAppeared(newViolations);
      return diff;
    }
    List<T> newViolationsSorted = sort(newViolations, comparator);
    List<T> oldViolationsSorted = sort(oldViolations, comparator);

    int i = 0;
    int j = 0;
    while (true) {
      if (j >= oldViolationsSorted.size()) {
        if (i >= newViolationsSorted.size()) {
          break; // nothing left
        }
        diff.addAppeared(newViolationsSorted.get(i++));
      }
      else if (i >= newViolationsSorted.size()) {
        diff.addCleared(oldViolationsSorted.get(j++));
      }
      else {
        final T newViolation = newViolationsSorted.get(i);
        final T oldViolation = oldViolationsSorted.get(j);

        final int comparison = comparator.compare(newViolation, oldViolation);
        if (comparison < 0) {
          diff.addAppeared(newViolation);
          i++;
        }
        else if (comparison > 0) {
          diff.addCleared(oldViolation);
          j++;
        }
        else {
          diff.addSame(newViolation, oldViolation);
          i++;
          j++;
        }
      }
    }

    return diff;
  }
}
