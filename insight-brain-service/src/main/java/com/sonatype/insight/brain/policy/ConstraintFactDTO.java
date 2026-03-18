/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.List;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.policy.ConstraintFact;

public class ConstraintFactDTO
{
  public String constraintName;

  public List<ConditionFactDTO> satisfiedConditions;

  public ConstraintFactDTO(ConstraintFact constraintFact) {
    constraintName = constraintFact.getConstraintName();
    satisfiedConditions = constraintFact.getConditionFacts()
        .stream()
        .map(ConditionFactDTO::new)
        .collect(Collectors.toList());
  }
}
