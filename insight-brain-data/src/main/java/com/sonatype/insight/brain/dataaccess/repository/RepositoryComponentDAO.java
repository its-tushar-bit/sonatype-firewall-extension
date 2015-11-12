/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * @since 1.17
 */
public class RepositoryComponentDAO
    extends AbstractOperationalSqlDAO<RepositoryComponent>
{
  @Override
  public RepositoryComponent getById(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM RepositoryComponent entity" + //
        " WHERE entity.id=?1";
    return get(tx, sQuery, id);
  }

  public List<RepositoryComponent> getByRepositoryId(String repositoryId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByRepositoryId(tx, repositoryId);
    }
  }

  public List<RepositoryComponent> getByRepositoryId(TransactionContext tx, String repositoryId) {
    String sQuery = "SELECT entity FROM RepositoryComponent entity" + //
        " WHERE entity.repositoryId=?1";
    return getList(tx, sQuery, repositoryId);
  }

  public RepositoryComponent getByRepositoryIdAndPathname(String repositoryId, String pathname)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return getByRepositoryIdAndPathname(tx, repositoryId, pathname);
    }
  }

  public RepositoryComponent getByRepositoryIdAndPathname(TransactionContext tx, String repositoryId, String pathname)
  {
    String sQuery = "SELECT entity FROM RepositoryComponent entity" + //
        " WHERE entity.repositoryId=?1" + //
        " AND entity.pathname=?2";
    return get(tx, sQuery, repositoryId, pathname);
  }

  public int getComponentCountByRepositoryId(String repositoryId) {
    String sQuery = "SELECT COUNT(component.id) FROM RepositoryComponent component" + //
        " WHERE component.repositoryId=?1";

    return getSingle(Number.class, sQuery, repositoryId).intValue();
  }

  public int getKnownComponentCountByRepositoryId(String repositoryId) {
    String sQuery = "SELECT COUNT(component.id) FROM RepositoryComponent component" + //
        " WHERE component.repositoryId=?1 AND component.matchStateId <> ?2";

    return getSingle(Number.class, sQuery, repositoryId, MatchState.UNKNOWN.getId()).intValue();
  }

  public int getQuarantinedComponentCountByRepositoryId(String repositoryId) {
    String sQuery = "SELECT COUNT(component.id) FROM RepositoryComponent component" + //
        " WHERE component.repositoryId=?1 AND component.quarantineTime IS NOT NULL AND component.unquarantineTime IS NULL";

    return getSingle(Number.class, sQuery, repositoryId).intValue();
  }

  public List<RepositoryComponent> getQuarantinedByRepositoryId(TransactionContext tx, String repositoryId) {
    String sQuery = "SELECT entity FROM RepositoryComponent entity" + //
        " WHERE entity.repositoryId=?1 AND entity.quarantineTime IS NOT NULL AND entity.unquarantineTime IS NULL";

    return getList(tx, sQuery, repositoryId);
  }

  @Override
  public void delete(TransactionContext tx, RepositoryComponent repositoryComponent) {
    // Mark all violations for this component as inactive.
    RepositoryPolicyViolationDAO policyViolationDAO = new RepositoryPolicyViolationDAO();
    for (RepositoryPolicyViolation policyViolation : policyViolationDAO.getActiveByRepositoryIdAndPathname(tx,
        repositoryComponent.getRepositoryId(), repositoryComponent.getPathname())) {
      policyViolation.setActive(false);
      policyViolationDAO.update(tx, policyViolation);
    }

    super.delete(tx, repositoryComponent);
  }

  /**
   * Deletes all components in a repository.
   * 
   * WARNING: This method bypasses the standard DAO delete(tx) method, so it has to match its implementation/behavior.
   */
  public int deleteByRepositoryId(TransactionContext tx, String repositoryId) {
    // Mark all violations for this repository as inactive.
    RepositoryPolicyViolationDAO repositoryPolicyViolationDAO = new RepositoryPolicyViolationDAO();
    for (RepositoryPolicyViolation policyViolation : repositoryPolicyViolationDAO.getActiveByRepositoryId(tx,
        repositoryId)) {
      policyViolation.setActive(false);
      repositoryPolicyViolationDAO.update(tx, policyViolation);
    }

    // Delete all components.
    String sQuery = "DELETE FROM RepositoryComponent entity" + //
        " WHERE entity.repositoryId=?1";
    Query query = createQuery(sQuery, repositoryId);
    return query.executeUpdate(tx);
  }
}
