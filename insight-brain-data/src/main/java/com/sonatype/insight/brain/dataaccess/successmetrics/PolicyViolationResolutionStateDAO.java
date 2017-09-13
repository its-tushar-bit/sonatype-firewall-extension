/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.successmetrics;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractAggregationSqlDAO;
import com.sonatype.insight.brain.model.successmetrics.PolicyViolationResolutionState;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * @since 1.33
 */
public class PolicyViolationResolutionStateDAO
    extends AbstractAggregationSqlDAO<PolicyViolationResolutionState>
{
  @Override
  public PolicyViolationResolutionState getById(String id) {
    String sQuery = "SELECT entity FROM PolicyViolationResolutionState entity WHERE entity.id = ?1";
    return get(sQuery, id);
  }

  public List<PolicyViolationResolutionState> getByApplicationId(String applicationId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByApplicationId(tx, applicationId);
    }
  }

  public void deleteByApplicationId(String applicationId) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      deleteByApplicationId(tx, applicationId);
      tx.commit();
    }
  }

  public void deleteByApplicationId(TransactionContext tx, String applicationId) {
    List<PolicyViolationResolutionState> resolutionStates = getByApplicationId(tx, applicationId);
    for (PolicyViolationResolutionState resolutionState : resolutionStates) {
      delete(tx, resolutionState);
    }
  }

  private List<PolicyViolationResolutionState> getByApplicationId(TransactionContext tx, String applicationId) {
    String sQuery = "SELECT entity FROM PolicyViolationResolutionState entity" + //
        " WHERE entity.applicationId = ?1";

    return getList(tx, sQuery, applicationId);
  }
}
