/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import com.sonatype.insight.brain.dataaccess.DAOFactory;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationConstraintFactsDAO;
import com.sonatype.insight.brain.model.policy.PolicyViolationConstraintFactsDAOProvider;

public class PolicyViolationConstraintFactsDaoTestHelper
{
  public static void inject(final DAOFactory daoFactory) {
    PolicyViolationConstraintFactsDAO constraintsDAO = daoFactory.createPolicyViolationConstraintFactsDAO();
    PolicyViolationConstraintFactsDAOProvider.inject(constraintsDAO);
  }
}
