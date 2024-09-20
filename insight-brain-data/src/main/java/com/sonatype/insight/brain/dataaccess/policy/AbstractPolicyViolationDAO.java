/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import com.sonatype.insight.brain.model.policy.AbstractPolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyViolationConstraintFacts;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * This is an interface but called "Abstract" to mirror the class hierarchy of AbstractPolicyViolation which is the
 * parent class to PolicyViolation and RepositoryPolicyViolation.
 */
public interface AbstractPolicyViolationDAO
{
  long getCountWhereConstraintFactsJsonNotNull();

  default void storeConstraints(
      final PolicyViolationConstraintFactsDAO policyViolationConstraintFactsDAO,
      final AbstractPolicyViolation entity)
  {
    String json = entity.getConstraintFactsJsonWithoutLoading();
    if (!isBlank(json)) {
      PolicyViolationConstraintFacts constraints = policyViolationConstraintFactsDAO.createIfNotExists(json);
      entity.setConstraintFactsId(constraints.getId());
      entity.clearConstraintFactsJson();
    }
  }
}
