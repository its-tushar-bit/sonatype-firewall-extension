/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.sonatype.insight.brain.model.policy.PolicyViolation;

public class PolicyViolationDigester
{
  private static final Comparator<PolicyViolation> POLICY_VIOLATION_COMPARATOR = new Comparator<PolicyViolation>()
  {
    @Override
    public int compare(PolicyViolation v1, PolicyViolation v2) {
      // Policy id
      int result = v1.getPolicyId().compareTo(v2.getPolicyId());
      if (result != 0) {
        return result;
      }

      // Policy name
      result = v1.getPolicyName().compareTo(v2.getPolicyName());
      if (result != 0) {
        return result;
      }

      // Threat level
      result = v1.getThreatLevel() - v2.getThreatLevel();
      if (result != 0) {
        return result;
      }

      // Hash
      result = compareNullableStrings(v1.getHash(), v2.getHash());
      if (result != 0) {
        return result;
      }

      // Group id
      result = compareNullableStrings(v1.getGroupId(), v2.getGroupId());
      if (result != 0) {
        return result;
      }

      // Artifact id
      result = compareNullableStrings(v1.getArtifactId(), v2.getArtifactId());
      if (result != 0) {
        return result;
      }

      // Version
      result = compareNullableStrings(v1.getVersion(), v2.getVersion());
      if (result != 0) {
        return result;
      }

      // Constraint facts
      result = v1.getConstraintFactsJson().compareTo(v2.getConstraintFactsJson());
      if (result != 0) {
        return result;
      }

      return 0;
    }

    // null is greater than not null
    private int compareNullableStrings(String s1, String s2) {
      if (s1 == null) {
        if (s2 == null) {
          return 0;
        }
        return 1;
      }
      if (s2 == null) {
        return -1;
      }
      return s1.compareTo(s2);
    }
  };
  
  private static List<PolicyViolation> sort(List<PolicyViolation> policyViolations) {
    List<PolicyViolation> result = new ArrayList<>();
    result.addAll(policyViolations);
    Collections.sort(result, POLICY_VIOLATION_COMPARATOR);
    return result;
  }

  public static PolicyViolationDiff digestPolicyViolations(List<PolicyViolation> newViolations,
      List<PolicyViolation> oldViolations)
  {
    PolicyViolationDiff diff = new PolicyViolationDiff();

    if (oldViolations == null) {
      diff.addAppeared(newViolations);
      return diff;
    }
    newViolations = sort(newViolations);
    oldViolations = sort(oldViolations);

    int i = 0, j = 0;
    while (true) {
      if (j >= oldViolations.size()) {
        if (i >= newViolations.size()) {
          break; // nothing left
        }
        diff.addAppeared(newViolations.get(i++));
      }
      else if (i >= newViolations.size()) {
        diff.addCleared(oldViolations.get(j++));
      }
      else {
        final PolicyViolation newViolation = newViolations.get(i);
        final PolicyViolation oldViolation = oldViolations.get(j);

        final int comparison = POLICY_VIOLATION_COMPARATOR.compare(newViolation, oldViolation);
        if (comparison < 0) {
          diff.addAppeared(newViolation);
          i++;
        }
        else if (comparison > 0) {
          diff.addCleared(oldViolation);
          j++;
        }
        else {
          i++;
          j++;
        }
      }
    }

    return diff;
  }
}
