/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.comparison;

import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ComponentH2Test
public class AutoPolicyWaiverViolationConstraintFactComparatorTest
    extends AbstractComponentH2Test
{
  private final AutoPolicyWaiverViolationConstraintFactComparator constraintFactComparator =
      new AutoPolicyWaiverViolationConstraintFactComparator();

  private ConstraintFact constraintFact1;

  private ConstraintFact constraintFact2;

  @BeforeEach
  @Override
  public void setUp() throws Exception {
    super.setUp();

    constraintFact1 = new ConstraintFact();
    constraintFact2 = new ConstraintFact();
  }

  @Test
  public void testCompare_Against_Null() {
    assertThat(constraintFactComparator.compare(null, null)).isEqualTo(0);
    assertThat(constraintFactComparator.compare(null, new ConstraintFact())).isEqualTo(1);
    assertThat(constraintFactComparator.compare(new ConstraintFact(), null)).isEqualTo(-1);
  }

  @Test
  public void testCompare_By_ConstraintId() {
    constraintFact1.setConstraintId("abcd");
    assertThat(constraintFactComparator.compare(constraintFact1, constraintFact2)).isGreaterThanOrEqualTo(-1);

    constraintFact2.setConstraintId("abcd");
    assertThat(constraintFactComparator.compare(constraintFact1, constraintFact2)).isEqualTo(0);

    constraintFact2.setConstraintId("aaaa");
    assertThat(constraintFactComparator.compare(constraintFact1, constraintFact2)).isGreaterThanOrEqualTo(1);

    constraintFact2.setConstraintId("zzzz");
    assertThat(constraintFactComparator.compare(constraintFact1, constraintFact2)).isLessThanOrEqualTo(-1);
  }

  @Test
  public void testCompare_By_ConstraintName() {
    constraintFact1.setConstraintName("abcd");
    assertThat(constraintFactComparator.compare(constraintFact1, constraintFact2)).isGreaterThanOrEqualTo(-1);

    constraintFact2.setConstraintName("abcd");
    assertThat(constraintFactComparator.compare(constraintFact1, constraintFact2)).isEqualTo(0);

    constraintFact2.setConstraintName("aaaa");
    assertThat(constraintFactComparator.compare(constraintFact1, constraintFact2)).isGreaterThanOrEqualTo(1);

    constraintFact2.setConstraintName("zzzz");
    assertThat(constraintFactComparator.compare(constraintFact1, constraintFact2)).isLessThanOrEqualTo(-1);
  }

  @Test
  public void testCompare_By_OperatorName() {
    constraintFact1.setOperatorName("abcd");
    assertThat(constraintFactComparator.compare(constraintFact1, constraintFact2)).isGreaterThanOrEqualTo(-1);

    constraintFact2.setOperatorName("abcd");
    assertThat(constraintFactComparator.compare(constraintFact1, constraintFact2)).isEqualTo(0);

    constraintFact2.setOperatorName("aaaa");
    assertThat(constraintFactComparator.compare(constraintFact1, constraintFact2)).isGreaterThanOrEqualTo(1);

    constraintFact2.setOperatorName("zzzz");
    assertThat(constraintFactComparator.compare(constraintFact1, constraintFact2)).isLessThanOrEqualTo(-1);
  }
}
