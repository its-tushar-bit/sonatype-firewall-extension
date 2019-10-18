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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.17
 */
public class RepositoryPolicyViolationDAO
    extends AbstractOperationalSqlDAO<RepositoryPolicyViolation>
{
  private static final Logger log = LoggerFactory.getLogger(RepositoryPolicyViolationDAO.class);

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

  public List<RepositoryPolicyViolation> getByRepositoryId(String repositoryId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByRepositoryId(tx, repositoryId);
    }
  }

  private List<RepositoryPolicyViolation> getByRepositoryId(TransactionContext tx, String repositoryId) {
    String sQuery = "SELECT entity FROM RepositoryPolicyViolation entity" + //
        " WHERE entity.repositoryId=?1";
    return getList(tx, sQuery, repositoryId);
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

  public List<RepositoryPolicyViolation> getActiveByRepositoryId(String repositoryId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getActiveByRepositoryId(tx, repositoryId);
    }
  }

  public List<RepositoryPolicyViolation> getActiveByRepositoryId(TransactionContext tx, String repositoryId) {
    String sQuery = "SELECT entity FROM RepositoryPolicyViolation entity" + //
        " WHERE entity.repositoryId=?1" + //
        " AND entity.active=true";
    return getList(tx, sQuery, repositoryId);
  }

  @Override
  public void delete(RepositoryPolicyViolation entity) {
    long start = System.currentTimeMillis();

    super.delete(entity);

    long duration = System.currentTimeMillis() - start;
    if (duration > 1000) {
      log.debug("Deleted repository policy violation with id {} in {} ms.", entity.getId(), duration);
    }
  }
}
