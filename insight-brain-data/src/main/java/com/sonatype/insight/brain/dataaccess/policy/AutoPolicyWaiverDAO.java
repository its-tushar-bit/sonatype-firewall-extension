/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverExclusion;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;

@Named
@Singleton
public class AutoPolicyWaiverDAO
    extends AbstractOperationalSqlDAO<AutoPolicyWaiver>
{
  private final AutoPolicyWaiverExclusionDAO autoPolicyWaiverExclusionDAO;

  @Inject
  public AutoPolicyWaiverDAO(
      final OperationalDataStore operationalDataStore,
      final AutoPolicyWaiverExclusionDAO autoPolicyWaiverExclusionDAO)
  {
    super(operationalDataStore);
    this.autoPolicyWaiverExclusionDAO = autoPolicyWaiverExclusionDAO;
  }

  public List<AutoPolicyWaiver> getByOwnerId(String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerId(tx, ownerId);
    }
  }

  public List<AutoPolicyWaiver> getByOwnerId(TransactionContext tx, String ownerId) {
    String sQuery = "SELECT entity FROM AutoPolicyWaiver entity" +
        " WHERE entity.ownerId=?1";
    return getList(tx, sQuery, ownerId);
  }

  public AutoPolicyWaiver getByIdAndOwnerIdNotNull(String autoPolicyWaiverId, String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByIdAndOwnerIdNotNull(tx, autoPolicyWaiverId, ownerId);
    }
  }

  public AutoPolicyWaiver getByIdAndOwnerIdNullable(String autoPolicyWaiverId, String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByIdAndOwnerId(tx, autoPolicyWaiverId, ownerId);
    }
  }

  public AutoPolicyWaiver getByIdAndOwnerIdNotNull(TransactionContext tx, String autoPolicyWaiverId, String ownerId) {
    AutoPolicyWaiver autoPolicyWaiver = getByIdAndOwnerId(tx, autoPolicyWaiverId, ownerId);
    if (autoPolicyWaiver == null) {
      String errorMessage = "Cannot find a waiver with ID " + autoPolicyWaiverId + " for owner " + ownerId + ".";
      throw new NotFoundException(errorMessage);
    }
    return autoPolicyWaiver;
  }

  public AutoPolicyWaiver getByIdAndOwnerId(TransactionContext tx, String autoPolicyWaiverId, String ownerId) {
    String sQuery = "SELECT waiver FROM AutoPolicyWaiver waiver WHERE waiver.id=?1 AND waiver.ownerId=?2";
    return get(tx, sQuery, autoPolicyWaiverId, ownerId);
  }

  @Override
  public void delete(TransactionContext tx, AutoPolicyWaiver autoPolicyWaiver) {
    for (AutoPolicyWaiverExclusion autoPolicyWaiverExclusion : autoPolicyWaiverExclusionDAO
        .getByOwnerIdAndAutoPolicyWaiverId(autoPolicyWaiver.getOwnerId(), autoPolicyWaiver.getId()))
    {
      autoPolicyWaiverExclusionDAO.delete(autoPolicyWaiverExclusion);
    }
    super.delete(tx, autoPolicyWaiver);
  }
}
