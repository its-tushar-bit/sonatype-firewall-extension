/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class ConstraintFactDTOTest
{
  @Test
  public void testConstraintFactDTO() {
    ConstraintFactDTO constraintFactDTO = new ConstraintFactDTO(
        constraintFact("constraintName", conditionFact("summary1", "reason1"), conditionFact("summary2", "reason1"),
            conditionFact("summary3", "reason2")));

    assertThat(constraintFactDTO, is(notNullValue()));
    assertThat(constraintFactDTO.constraintName, is("constraintName"));
    assertThat(constraintFactDTO.satisfiedConditions, hasSize(3));
    assertThat(constraintFactDTO.satisfiedConditions.get(0).summary, is("summary1"));
    assertThat(constraintFactDTO.satisfiedConditions.get(0).reason, is("reason1"));
    assertThat(constraintFactDTO.satisfiedConditions.get(1).summary, is("summary2"));
    assertThat(constraintFactDTO.satisfiedConditions.get(1).reason, is("reason1"));
    assertThat(constraintFactDTO.satisfiedConditions.get(2).summary, is("summary3"));
    assertThat(constraintFactDTO.satisfiedConditions.get(2).reason, is("reason2"));
  }

  private ConstraintFact constraintFact(String constraintName, ConditionFact... conditionFacts) {
    return new ConstraintFact("constraintId", constraintName, "operatorName").with(conditionFacts);
  }

  private ConditionFact conditionFact(String summary, String reason) {
    return new ConditionFact("conditionTypeId", 0, summary, reason);
  }
}
