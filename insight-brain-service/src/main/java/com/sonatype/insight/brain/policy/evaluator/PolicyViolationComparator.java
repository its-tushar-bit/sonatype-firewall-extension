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

    // Hash and component identifier
    result = ComponentIdentifierAndHashComparator.COMPARATOR.compare(v1, v2);
    if (result != 0) {
      return result;
    }

    // Shortcut the constraints comparison if the IDs are the same.
    if (v1.getConstraintFactsId() != null && v1.getConstraintFactsId().equals(v2.getConstraintFactsId())) {
      return 0;
    }

    // If the IDs are not the same we need to compare the constraint facts because they might be functionally equivalent
    return ConstraintFactsListComparator.CONSTRAINT_FACTS_LIST_COMPARATOR.compare(v1.getConstraintFacts(),
        v2.getConstraintFacts());
  }

  public static String computeUniqueAppPolicyConstraintId(
      String applicationId,
      String policyId,
      List<ConstraintFact> constraintFacts)
  {
    StringBuilder allConditionsHashIds = new StringBuilder();

    for (ConstraintFact constraintFact : CollectionUtils.emptyIfNull(constraintFacts)) {
      for (ConditionFact conditionFact : CollectionUtils.emptyIfNull(constraintFact.getConditionFacts())) {
        if (StringUtils.isNotBlank(conditionFact.getReason())) {
          allConditionsHashIds.append(conditionFact.getReason().hashCode());
        }
      }
    }
    return applicationId + policyId + allConditionsHashIds;
  }
}
