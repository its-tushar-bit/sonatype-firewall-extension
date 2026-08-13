/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.comparison;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ComponentH2Test
public class AutoPolicyWaiverViolationConditionFactsListComparatorTest
    extends AbstractComponentH2Test
{
  private final AutoPolicyWaiverViolationConditionFactsListComparator conditionFactsListComparator =
      new AutoPolicyWaiverViolationConditionFactsListComparator();

  @Test
  public void testCompare_Against_Null() {
    assertThat(conditionFactsListComparator.compare(null, null)).isEqualTo(0);
    assertThat(conditionFactsListComparator.compare(null, new ArrayList<>())).isEqualTo(1);
    assertThat(conditionFactsListComparator.compare(new ArrayList<>(), null)).isEqualTo(-1);
  }

  @Test
  public void testCompare_WithConditionFacts() {
    List<ConditionFact> conditionFacts1 = new ArrayList<>();
    List<ConditionFact> conditionFacts2 = new ArrayList<>();

    assertThat(conditionFactsListComparator.compare(conditionFacts1, conditionFacts2)).isEqualTo(0);

    ConditionFact conditionFact1 = new ConditionFact();
    conditionFacts1.add(conditionFact1);
    assertThat(conditionFactsListComparator.compare(conditionFacts1, conditionFacts2)).isEqualTo(1);

    ConditionFact conditionFact2 = new ConditionFact();
    conditionFacts2.add(conditionFact2);
    assertThat(conditionFactsListComparator.compare(conditionFacts1, conditionFacts2)).isEqualTo(0);

    conditionFact1.setConditionIndex(1);
    conditionFact2.setConditionIndex(2);
    assertThat(conditionFactsListComparator.compare(conditionFacts1, conditionFacts2)).isEqualTo(-1);

    conditionFact1.setConditionIndex(2);
    conditionFact2.setConditionIndex(1);
    assertThat(conditionFactsListComparator.compare(conditionFacts1, conditionFacts2)).isEqualTo(1);

    conditionFacts2.add(conditionFact1);
    assertThat(conditionFactsListComparator.compare(conditionFacts1, conditionFacts2)).isEqualTo(-1);

    conditionFacts1.add(conditionFact2);
    assertThat(conditionFactsListComparator.compare(conditionFacts1, conditionFacts2)).isEqualTo(0);
  }
}
