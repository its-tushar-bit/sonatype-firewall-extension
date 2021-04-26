/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * @since 1.17
 */
public class RepositoryPolicyViolationDAO
    extends AbstractOperationalSqlDAO<RepositoryPolicyViolation>
{
  @Override
  protected RepositoryPolicyViolation getById(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM RepositoryPolicyViolation entity" + //
        " WHERE entity.id=?1";
    return get(tx, sQuery, id);
  }

  public List<RepositoryPolicyViolation> getActiveByRepositoryIdAndPathname(String repositoryId, String pathname) {
    try (TransactionContext tx = createTransactionContext()) {
      return getActiveByRepositoryIdAndPathname(tx, repositoryId, pathname);
    }
  }

  public List<RepositoryPolicyViolation> getActiveByRepositoryIdAndPathname(TransactionContext tx,
                                                                            String repositoryId,
                                                                            String pathname)
  {
    String sQuery = "SELECT entity FROM RepositoryPolicyViolation entity" + //
        " WHERE entity.repositoryId=?1" + //
        " AND entity.pathname=?2" + //
        " AND entity.active=true" + //
        " ORDER BY entity.threatLevel DESC, entity.policyId";
    return getList(tx, sQuery, repositoryId, pathname);
  }

  public List<RepositoryPolicyViolation> getActiveByRepositoryIdAndPathnameAndWaived(String repositoryId,
                                                                                     String pathname,
                                                                                     boolean isWaived)
  {
    String sQuery = "SELECT entity FROM RepositoryPolicyViolation entity" + //
        " WHERE entity.repositoryId=?1" + //
        " AND entity.pathname=?2" + //
        " AND entity.isWaived=?3" + //
        " AND entity.active=true" + //
        " ORDER BY entity.threatLevel DESC, entity.policyId";
    return getList(sQuery, repositoryId, pathname, isWaived);
  }

  /**
   * @since 1.78
   */
  public List<RepositoryPolicyViolation> getByRepositoryIdAndPathnameAndActionAndNotWaived(
      String repositoryId,
      String pathname,
      String actionTypeId)
  {
    String sQuery = "SELECT entity FROM RepositoryPolicyViolation entity" + //
        " WHERE entity.repositoryId=?1" + //
        " AND entity.pathname=?2" + //
        " AND entity.actionTypeId=?3" + //
        " AND entity.isWaived=false" + //
        " AND entity.active=true";
    return getList(sQuery, repositoryId, pathname, actionTypeId);
  }

  /**
   * @since 1.113
   */
  public List<RepositoryPolicyViolation> getByRepositoryIdAndPathnameAndAction(
      String repositoryId,
      String pathname,
      String actionTypeId)
  {
    String sQuery = "SELECT entity FROM RepositoryPolicyViolation entity" + //
        " WHERE entity.repositoryId=?1" + //
        " AND entity.pathname=?2" + //
        " AND entity.actionTypeId=?3" + //
        " AND entity.active=true";
    return getList(sQuery, repositoryId, pathname, actionTypeId);
  }

  public List<RepositoryPolicyViolation> getByRepositoryId(String repositoryId) {
    String sQuery = "SELECT entity FROM RepositoryPolicyViolation entity" + //
        " WHERE entity.repositoryId=?1";
    return getList(sQuery, repositoryId);
  }

  public List<RepositoryPolicyViolation> getActiveByRepositoryIdAndNotWaived(final String repositoryId) {
    String sQuery = "SELECT entity FROM RepositoryPolicyViolation entity" + //
        " WHERE entity.repositoryId=?1" + //
        " AND entity.active=true" + //
        " AND entity.isWaived=false";
    return getList(sQuery, repositoryId);
  }

  public List<RepositoryPolicyViolation> getActiveWaivedRepositoryPolicyViolations(
      final Collection<String> repositoryIds)
  {
    String sQuery = "SELECT entity FROM RepositoryPolicyViolation entity" + //
        " WHERE entity.repositoryId=?1" + //
        " AND entity.active=true" + //
        " AND entity.isWaived=true";
    return getRepositoryPolicyViolations(sQuery, repositoryIds);
  }

  private List<RepositoryPolicyViolation> getRepositoryPolicyViolations(
      String sQuery,
      Collection<String> repositoryIds)
  {
    List<RepositoryPolicyViolation> repositoryPolicyViolations = new ArrayList<>();
    for (String repositoryId : repositoryIds) {
      Object[] parameters = {repositoryId};
      repositoryPolicyViolations.addAll(getList(sQuery, parameters));
    }

    return repositoryPolicyViolations;
  }

  public List<RepositoryPolicyViolation> getActiveByRepositoryId(TransactionContext tx, String repositoryId) {
    String sQuery = "SELECT entity FROM RepositoryPolicyViolation entity" + //
        " WHERE entity.repositoryId=?1" + //
        " AND entity.active=true";
    return getList(tx, sQuery, repositoryId);
  }

  @Override
  public final void delete(RepositoryPolicyViolation entity) {
    // WARNING: Don't add any business logic to this method because, for performance reasons,
    // we bypass this method when deleting all policy violations for a repository.
    // See https://issues.sonatype.org/browse/CLM-15648 for details
    super.delete(entity);
  }

  @Override
  public final void delete(TransactionContext tx, RepositoryPolicyViolation entity) {
    // WARNING: Don't add any business logic to this method because, for performance reasons,
    // we bypass this method when deleting all policy violations for a repository.
    // See https://issues.sonatype.org/browse/CLM-15648 for details
    super.delete(tx, entity);
  }

  public void deleteByRepositoryId(TransactionContext tx, String repositoryId) {
    if (isDatabaseEmbedded()) {
      // We do not enroll the deletions in the transaction on purpose.
      // This improves performance and keeps db operations (including commits) reasonably short, which means other
      // concurrent db operations are blocked for shorter periods of time (H2 is single threaded).
      // See https://issues.sonatype.org/browse/CLM-15648 for details
      getByRepositoryId(repositoryId).forEach(this::delete);
    }
    else {
      // For performance reasons, we bypass the standard delete (per entity) method here.
      // We cannot do this for H2 until we upgrade to a multi-threaded H2 version.
      // See https://issues.sonatype.org/browse/CLM-15648 for details
      String sQuery = "DELETE FROM RepositoryPolicyViolation entity WHERE entity.repositoryId=?1";
      createQuery(sQuery, repositoryId).executeUpdate(tx);
    }
  }

  public List<RepositoryPolicyViolation> getByRepositoryIdAndPathname(String repositoryId, String pathname) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByRepositoryIdAndPathname(tx, repositoryId, pathname);
    }
  }

  public List<RepositoryPolicyViolation> getByRepositoryIdAndPathname(
      TransactionContext tx,
      String repositoryId,
      String pathname)
  {
    String sQuery = "SELECT entity FROM RepositoryPolicyViolation entity" + //
        " WHERE entity.repositoryId=?1" + //
        " AND entity.pathname=?2";
    return getList(tx, sQuery, repositoryId, pathname);
  }

  public long getCount() {
    String sQuery = "SELECT COUNT(entity) FROM RepositoryPolicyViolation entity";
    return getSingle(Long.class, sQuery);
  }
}
