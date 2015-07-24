/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

public class PolicyWaiverDAO
    extends AbstractOperationalSqlDAO<PolicyWaiver>
{
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

    ApplicationDAO applicationDAO = new ApplicationDAO();
    Application application = applicationDAO.getById(ownerId);
    if (application != null) {
      policyWaivers.addAll(getByOwnerId(application.getOrganizationId()));
    }
    policyWaivers.addAll(getByOwnerId(ownerId));
    return policyWaivers;
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

  private PolicyWaiver getByHashAndPolicyIdAndConstraintIdAndOwnerId(TransactionContext tx, String hash, String policyId,
      String constraintId, String ownerId)
  {
    String sQuery = "SELECT entity FROM PolicyWaiver entity" + //
        " WHERE entity.hash=?1 AND entity.policyId=?2 AND entity.constraintId=?3 AND entity.ownerId=?4";
    return get(tx, sQuery, hash, policyId, constraintId, ownerId);
  }

  @Override
  public void insert(TransactionContext tx, PolicyWaiver entity) {
    PolicyWaiver other = getByHashAndPolicyIdAndConstraintIdAndOwnerId(tx, entity.getHash(), entity.getPolicyId(),
        entity.getConstraintId(), entity.getOwnerId());
    if (other != null) {
      throw new BadRequestException("This policy waiver already exists.");
    }
    if (entity.getComment() != null && entity.getComment().length() > 1000) {
      throw new BadRequestException("Comment length must not exceed 1000 characters.");
    }

    entity.setCreateTime(new Date());

    super.insert(tx, entity);
  }

  @Override
  public void update(TransactionContext tx, PolicyWaiver entity) {
    throw new UnsupportedOperationException();
  }
}
