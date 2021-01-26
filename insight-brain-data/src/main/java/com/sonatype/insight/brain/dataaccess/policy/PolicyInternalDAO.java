/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.Collection;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.tag.PolicyTagDAO;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.brain.model.SearchIndexChange.ChangeType;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.tag.PolicyTag;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;

/**
 * @since 1.9
 */
public class PolicyInternalDAO
    extends AbstractOperationalSqlDAO<PolicyInternal>
{
  @Override
  protected PolicyInternal getById(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM PolicyInternal entity" + //
        " WHERE entity.id=?1";
    return get(tx, sQuery, id);
  }

  PolicyInternal getByIdNotNull(TransactionContext tx, String id) {
    PolicyInternal policyInternal = getById(tx, id);
    if (policyInternal == null) {
      throw new NotFoundException("Cannot find a policy with ID " + id + ".");
    }
    return policyInternal;
  }

  PolicyInternal getByIdNotNull(String id) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByIdNotNull(tx, id);
    }
  }

  List<PolicyInternal> getByIds(Collection<String> ids) {
    String sQuery = "SELECT entity FROM PolicyInternal entity" + //
        " WHERE entity.id IN (?1)";
    return getList(sQuery, ids);
  }

  List<PolicyInternal> getByOwnerIds(Collection<String> ownerIds) {
    String sQuery = "SELECT entity FROM PolicyInternal entity" + //
        " WHERE entity.ownerId IN (?1)" + //
        " ORDER BY entity.nameLowercaseNoWhitespace";
    return getList(sQuery, ownerIds);
  }

  List<PolicyInternal> getByOwnerId(String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerId(tx, ownerId);
    }
  }

  List<PolicyInternal> getByOwnerId(TransactionContext tx, String ownerId) {
    String sQuery = "SELECT entity FROM PolicyInternal entity" + //
        " WHERE entity.ownerId=?1" + //
        " ORDER BY entity.nameLowercaseNoWhitespace";
    return getList(tx, sQuery, ownerId);
  }

  PolicyInternal getByOwnerIdAndName(TransactionContext tx, String ownerId, String name) {
    name = NameHelper.normalize(name);
    String sQuery = "SELECT entity FROM PolicyInternal entity" + //
        " WHERE entity.ownerId=?1 AND entity.nameLowercaseNoWhitespace=?2";
    return get(tx, sQuery, ownerId, name);
  }

  List<PolicyInternal> getByName(String name) {
    name = NameHelper.normalize(name);
    String sQuery = "SELECT entity FROM PolicyInternal entity" + //
        " WHERE entity.nameLowercaseNoWhitespace=?1";
    return getList(sQuery, name);
  }

  public List<PolicyInternal> getAll(TransactionContext tx) {
    String sQuery = "SELECT entity FROM PolicyInternal entity";
    return getList(tx, sQuery);
  }

  @Override
  public void delete(TransactionContext tx, PolicyInternal policy) {
    // Cascade to policy waivers
    PolicyWaiverDAO policyWaiverDAO = new PolicyWaiverDAO();
    List<PolicyWaiver> policyWaivers = policyWaiverDAO.getByPolicyId(tx, policy.getId());
    for (PolicyWaiver policyWaiver : policyWaivers) {
      policyWaiverDAO.delete(tx, policyWaiver);
    }

    // Cascade to policy tags
    PolicyTagDAO policyTagDAO = new PolicyTagDAO();
    List<PolicyTag> policyTags = policyTagDAO.getByPolicyId(tx, policy.getId());
    for (PolicyTag policyTag : policyTags) {
      policyTagDAO.delete(tx, policyTag);
    }

    super.delete(tx, policy);
  }

  @Override
  protected SearchIndexChange newSearchIndexChange(PolicyInternal entity) {
    return new SearchIndexChange(ChangeType.POLICY, entity.getId());
  }
}
