/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.sonatype.insight.brain.model.policy.PolicyViolation;

public class PolicyViolationDigester
{
  private static List<PolicyViolation> sort(Collection<PolicyViolation> policyViolations) {
    List<PolicyViolation> result = new ArrayList<>();
    result.addAll(policyViolations);
    Collections.sort(result, PolicyViolationComparator.COMPARATOR);
    return result;
  }

  public static PolicyViolationDiff digestPolicyViolations(Collection<PolicyViolation> oldViolations,
      Collection<PolicyViolation> newViolations)
  {
    PolicyViolationDiff diff = new PolicyViolationDiff();

    if (oldViolations == null) {
      diff.addAppeared(newViolations);
      return diff;
    }
    List<PolicyViolation> newViolationsSorted = sort(newViolations);
    List<PolicyViolation> oldViolationsSorted = sort(oldViolations);

    int i = 0, j = 0;
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
        final PolicyViolation newViolation = newViolationsSorted.get(i);
        final PolicyViolation oldViolation = oldViolationsSorted.get(j);

        final int comparison = PolicyViolationComparator.COMPARATOR.compare(newViolation, oldViolation);
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
