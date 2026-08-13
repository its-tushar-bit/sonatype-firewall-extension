/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.comparison;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ComponentH2Test
public class AutoPolicyWaiverViolationConstraintFactsListComparatorTest
    extends AbstractComponentH2Test
{
  private final AutoPolicyWaiverViolationConstraintFactsListComparator constraintFactsListComparator =
      new AutoPolicyWaiverViolationConstraintFactsListComparator();

  @Test
  public void testCompare_Against_Null() {
    assertThat(constraintFactsListComparator.compare(null, null)).isEqualTo(0);
    assertThat(constraintFactsListComparator.compare(null, new ArrayList<>())).isEqualTo(1);
    assertThat(constraintFactsListComparator.compare(new ArrayList<>(), null)).isEqualTo(-1);
  }

  @Test
  public void testCompare_WithConstraintFacts() {
    List<ConstraintFact> constraintFacts1 = new ArrayList<>();
    List<ConstraintFact> constraintFacts2 = new ArrayList<>();

    assertThat(constraintFactsListComparator.compare(constraintFacts1, constraintFacts2)).isEqualTo(0);

    ConstraintFact constraintFact1 = new ConstraintFact();
    constraintFacts1.add(constraintFact1);
    assertThat(constraintFactsListComparator.compare(constraintFacts1, constraintFacts2)).isEqualTo(1);

    ConstraintFact constraintFact2 = new ConstraintFact();
    constraintFacts2.add(constraintFact2);
    assertThat(constraintFactsListComparator.compare(constraintFacts1, constraintFacts2)).isEqualTo(0);

    constraintFact1.setConstraintName("test1");
    constraintFact2.setConstraintName("test2");
    assertThat(constraintFactsListComparator.compare(constraintFacts1, constraintFacts2)).isEqualTo(-1);

    constraintFact1.setConstraintName("test2");
    constraintFact2.setConstraintName("test1");
    assertThat(constraintFactsListComparator.compare(constraintFacts1, constraintFacts2)).isEqualTo(1);

    constraintFacts2.add(constraintFact1);
    assertThat(constraintFactsListComparator.compare(constraintFacts1, constraintFacts2)).isEqualTo(-1);

    constraintFacts1.add(constraintFact2);
    assertThat(constraintFactsListComparator.compare(constraintFacts1, constraintFacts2)).isEqualTo(0);
  }
}
