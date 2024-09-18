/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationConstraintFactsDAO;

public final class PolicyViolationConstraintFactsDAOProvider
{
  private static PolicyViolationConstraintFactsDAO policyViolationConstraintFactsDAO;

  @Inject
  public static void inject(final PolicyViolationConstraintFactsDAO policyViolationConstraintFactsDAO) {
    PolicyViolationConstraintFactsDAOProvider.policyViolationConstraintFactsDAO = policyViolationConstraintFactsDAO;
  }

  public static String getConstraintFactsJson(final String constraintFactsId) {
    PolicyViolationConstraintFacts constraints = policyViolationConstraintFactsDAO.getById(constraintFactsId);
    return constraints.getConstraintFactsJson();
  }
}
