/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import java.util.List;

import com.sonatype.clm.dto.model.policy.ConstraintFact;

/**
 * @since 1.33
 */
public interface PolicyViolationComparable
    extends ComponentIdentifierAndHashComparable
{
  String getPolicyId();

  String getPolicyName();

  int getThreatLevel();

  List<ConstraintFact> getConstraintFacts();

  String getConstraintFactsId();
}
