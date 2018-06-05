/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.policy.PolicyViolationComparable;

public class PolicyViolationComparator
    implements Comparator<PolicyViolationComparable>
{
  public static final Comparator<PolicyViolationComparable> COMPARATOR = new PolicyViolationComparator();

  private static final Comparator<List<ConstraintFact>> CONSTRAINT_FACTS_LIST_COMPARATOR = new ConstraintFactsListComparator();

  private static final Comparator<ConstraintFact> CONSTRAINT_FACT_COMPARATOR = new ConstraintFactComparator();

  private static final Comparator<List<ConditionFact>> CONDITION_FACTS_LIST_COMPARATOR = new ConditionFactsListComparator();

  private static final Comparator<ConditionFact> CONDITION_FACT_COMPARATOR = new ConditionFactComparator();

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

    return CONSTRAINT_FACTS_LIST_COMPARATOR.compare(v1.getConstraintFacts(), v2.getConstraintFacts());
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

  private static class ConstraintFactsListComparator implements Comparator<List<ConstraintFact>>
  {
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

  private static class ConstraintFactComparator implements Comparator<ConstraintFact>
  {
    @Override
    public int compare(ConstraintFact constraintFact1, ConstraintFact constraintFact2) {
      // Constraint id
      int result = constraintFact1.getConstraintId().compareTo(constraintFact2.getConstraintId());
      if (result != 0) {
        return result;
      }

      // Constraint name
      String constraintFact1Name = NameHelper.normalize(constraintFact1.getConstraintName());
      String constraintFact2Name = NameHelper.normalize(constraintFact2.getConstraintName());
      result = constraintFact1Name.compareTo(constraintFact2Name);
      if (result != 0) {
        return result;
      }

      // Condition facts
      result = CONDITION_FACTS_LIST_COMPARATOR.compare(constraintFact1.getConditionFacts(),
          constraintFact2.getConditionFacts());
      if (result != 0) {
        return result;
      }

      return 0;
    }
  }

  private static class ConditionFactsListComparator implements Comparator<List<ConditionFact>>
  {
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

  private static class ConditionFactComparator implements Comparator<ConditionFact>
  {
    @Override
    public int compare(ConditionFact conditionFact1, ConditionFact conditionFact2) {
      // Condition type
      int result = conditionFact1.getConditionTypeId().compareTo(conditionFact2.getConditionTypeId());
      if (result != 0) {
        return result;
      }

      // If the condition index is null, then this policy violation was created before we added condition trigger data
      // to policy violations.
      // In this case we ignore the condition index and trigger data in the newer policy violation.

      // Condition index
      if (conditionFact1.getConditionIndex() != null && conditionFact2.getConditionIndex() != null) {
        result = conditionFact1.getConditionIndex() - conditionFact2.getConditionIndex();
        if (result != 0) {
          return result;
        }

        // Condition trigger
        // Not all condition types store trigger data.
        if (conditionFact1.getTriggerJson() != null && conditionFact2.getTriggerJson() != null) {
          result = conditionFact1.getTriggerJson().compareTo(conditionFact2.getTriggerJson());
        }
        if (result != 0) {
          return result;
        }
      }

      // Condition type
      result = conditionFact1.getConditionTypeId().compareTo(conditionFact2.getConditionTypeId());
      if (result != 0) {
        return result;
      }

      // Condition trigger
      // If the condition type is supposed to store trigger data, but the trigger data is missing, then this policy
      // violation was created before we added trigger data to policy violations.
      // In this case we ignore the trigger data in the newer policy violation.
      if (conditionFact1.getTriggerJson() != null && conditionFact2.getTriggerJson() != null) {
        result = conditionFact1.getTriggerJson().compareTo(conditionFact2.getTriggerJson());
      }
      if (result != 0) {
        return result;
      }

      return 0;
    }
  }
}
