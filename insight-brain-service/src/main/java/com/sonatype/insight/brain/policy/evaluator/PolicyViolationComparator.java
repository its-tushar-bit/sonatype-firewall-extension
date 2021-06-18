/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.Comparator;
import java.util.List;

import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.model.policy.PolicyViolationComparable;
import com.sonatype.insight.brain.policy.comparison.ConstraintFactsListComparator;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public class PolicyViolationComparator
    implements Comparator<PolicyViolationComparable>
{
  public static final Comparator<PolicyViolationComparable> COMPARATOR = new PolicyViolationComparator();

  @Override
  public int compare(PolicyViolationComparable v1, PolicyViolationComparable v2) {
    // Policy id
    int result = v1.getPolicyId().compareTo(v2.getPolicyId());
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

    // Component identifier
    result = nullCheck(v1.getComponentIdentifier(), v2.getComponentIdentifier());
    if (result != 0) {
      return result;
    }
    if (v1.getComponentIdentifier() != null) {
      result = v1.getComponentIdentifier().compareTo(v2.getComponentIdentifier());
    }
    if (result != 0) {
      return result;
    }

    return ConstraintFactsListComparator.CONSTRAINT_FACTS_LIST_COMPARATOR.compare(v1.getConstraintFacts(),
        v2.getConstraintFacts());
  }

  // null is greater than not null
  private int compareNullableStrings(String s1, String s2) {
    int result = nullCheck(s1, s2);
    if (result != 0) {
      return result;
    }
    if (s1 == null) {
      return 0;
    }
    return s1.compareTo(s2);
  }

  /**
   * Null objects are treated as infinitely large.
   * 
   * @return 1 if o1 is not null while o2 is, or -1 if o2 is not null and o1 is. 0 if both objects are either null or
   *         not null.
   */
  private int nullCheck(Object o1, Object o2) {
    if (o1 == null && o2 != null) {
      return 1;
    }
    else if (o1 != null && o2 == null) {
      return -1;
    }

    return 0;
  }

  public static String computeUniqueAppPolicyConstraintId(String applicationId,
                                                          String policyId,
                                                          List<ConstraintFact> constraintFacts)
  {
    StringBuilder allConditionsHashIds = new StringBuilder();

    for (ConstraintFact constraintFact: CollectionUtils.emptyIfNull(constraintFacts)) {
      for (ConditionFact conditionFact : CollectionUtils.emptyIfNull(constraintFact.getConditionFacts())) {
        if (StringUtils.isNotBlank(conditionFact.getReason())) {
          allConditionsHashIds.append(conditionFact.getReason().hashCode());
        }
      }
    }
    return applicationId + policyId + allConditionsHashIds;
  }
}
