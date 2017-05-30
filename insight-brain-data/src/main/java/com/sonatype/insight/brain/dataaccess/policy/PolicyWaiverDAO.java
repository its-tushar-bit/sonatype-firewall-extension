/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

public class PolicyWaiverDAO
    extends AbstractOperationalSqlDAO<PolicyWaiver>
{
  private static final OwnerDAO ownerDAO = new OwnerDAO();

  @Override
  protected PolicyWaiver getById(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM PolicyWaiver entity" + //
        " WHERE entity.id=?1";
    return get(tx, sQuery, id);
  }

  private PolicyWaiver getByIdNotNull(TransactionContext tx, String id) {
    PolicyWaiver policyWaiver = getById(tx, id);
    if (policyWaiver == null) {
      throw new NotFoundException("Cannot find a policy waiver with ID " + id + ".");
    }
    return policyWaiver;
  }

  public PolicyWaiver getByIdNotNull(String id) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByIdNotNull(tx, id);
    }
  }

  public List<PolicyWaiver> getByOwnerId(String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerId(tx, ownerId);
    }
  }

  /**
   * Gets all policy waivers that target the specified component hash in the context of the given app/org. Note that a
   * component can be subject to a waiver that refers to its specific hash or to a waiver that applies to the entire
   * app/org.
   */
  public List<PolicyWaiver> getByOwnerIdAndHash(String ownerId, String hash) {
    try (TransactionContext tx = createTransactionContext()) {
      List<PolicyWaiver> waivers = new ArrayList<>();
      waivers.addAll(getByOwnerIdAndHash(tx, ownerId, hash));
      waivers.addAll(getByOwnerIdAndHash(tx, ownerId, null));
      return waivers;
    }
  }

  public List<PolicyWaiver> getApplicableByOwnerId(String ownerId) {
    List<PolicyWaiver> policyWaivers = new ArrayList<>();

    loadByOwnerId(policyWaivers, ownerId);

    return policyWaivers;
  }

  private void loadByOwnerId(List<PolicyWaiver> policyWaivers, String ownerId) {
    if (ownerId == null) {
      return;
    }

    Owner owner = ownerDAO.getById(ownerId);
    loadByOwnerId(policyWaivers, owner.getParentOwnerId());
    policyWaivers.addAll(getByOwnerId(ownerId));
  }

  private List<PolicyWaiver> getByOwnerIdAndHash(TransactionContext tx, String ownerId, String hash) {
    String sQuery = "SELECT entity FROM PolicyWaiver entity" + //
        " WHERE entity.ownerId=?1 AND entity.hash=?2";
    return getList(tx, sQuery, ownerId, hash);
  }

  public List<PolicyWaiver> getByOwnerId(TransactionContext tx, String ownerId) {
    String sQuery = "SELECT entity FROM PolicyWaiver entity" + //
        " WHERE entity.ownerId=?1";
    return getList(tx, sQuery, ownerId);
  }

  public List<PolicyWaiver> getByPolicyId(String policyId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByPolicyId(tx, policyId);
    }
  }

  public List<PolicyWaiver> getByPolicyId(TransactionContext tx, String policyId) {
    String sQuery = "SELECT entity FROM PolicyWaiver entity" + //
        " WHERE entity.policyId=?1";
    return getList(tx, sQuery, policyId);
  }

  public List<PolicyWaiver> getByPolicyIdAndOwnerIds(TransactionContext tx, String policyId, Set<String> ownerIds) {
    String sQuery = "SELECT entity FROM PolicyWaiver entity" + //
        " WHERE entity.policyId=?1 AND entity.ownerId IN (?2)";
    return getList(tx, sQuery, policyId, ownerIds);
  }

  private PolicyWaiver getByHashAndPolicyIdAndOwnerId(TransactionContext tx,
                                                      String hash,
                                                      String policyId,
                                                      String ownerId)
  {
    String sQuery = "SELECT entity FROM PolicyWaiver entity" + //
        " WHERE entity.hash=?1 AND entity.policyId=?2 AND entity.ownerId=?3";
    return get(tx, sQuery, hash, policyId, ownerId);
  }

  @Override
  public void insert(TransactionContext tx, PolicyWaiver entity) {
    PolicyWaiver other = getByHashAndPolicyIdAndOwnerId(tx, entity.getHash(), entity.getPolicyId(), entity.getOwnerId());
    if (other != null) {
      throw new BadRequestException("This policy waiver already exists.");
    }
    if (entity.getComment() != null && entity.getComment().length() > 1000) {
      throw new BadRequestException("Comment length must not exceed 1000 characters.");
    }

    if (entity.getCreateTime() == null) {
      entity.setCreateTime(new Date());
    }

    super.insert(tx, entity);
  }

  @Override
  public void update(TransactionContext tx, PolicyWaiver entity) {
    PolicyWaiver other = getByHashAndPolicyIdAndOwnerId(tx, entity.getHash(), entity.getPolicyId(), entity.getOwnerId());
    if (other != null && !other.getId().equals(entity.getId())) {
      throw new BadRequestException("A policy waiver for the same hash, policy and owner already exists.");
    }
    if (entity.getComment() != null && entity.getComment().length() > 1000) {
      throw new BadRequestException("Comment length must not exceed 1000 characters.");
    }

    super.update(tx, entity);
  }
}
