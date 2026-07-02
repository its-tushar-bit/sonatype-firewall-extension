/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.ValidationResult;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.InvalidPolicyException;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyValidator;
import com.sonatype.insight.brain.policy.DroolsGenerator;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.apache.commons.lang3.StringUtils;

@Named
@Singleton
public class PolicyDAO
{
  private final PolicyInternalDAO policyInternalDAO;

  private final OwnerDAO ownerDAO;

  private final PolicyValidator policyValidator;

  @Inject
  public PolicyDAO(
      final PolicyInternalDAO policyInternalDAO,
      final OwnerDAO ownerDAO,
      final PolicyValidator policyValidator)
  {
    this.policyInternalDAO = policyInternalDAO;
    this.ownerDAO = ownerDAO;
    this.policyValidator = policyValidator;
  }

  public Policy getById(String id) {
    return PolicyInternal.toPolicy(policyInternalDAO.getById(id));
  }

  public Policy getById(TransactionContext tx, String id) {
    return PolicyInternal.toPolicy(policyInternalDAO.getById(tx, id));
  }

  public Policy getByIdNotNull(String id) {
    return PolicyInternal.toPolicy(policyInternalDAO.getByIdNotNull(id));
  }

  public List<Policy> getByIds(Collection<String> ids) {
    return PolicyInternal.toPolicies(policyInternalDAO.getByIds(ids));
  }

  public List<Policy> getByOwnerId(final String ownerId) {
    return PolicyInternal.toPolicies(policyInternalDAO.getByOwnerId(ownerId));
  }

  public List<Policy> getByOwnerId(TransactionContext tx, String ownerId) {
    return PolicyInternal.toPolicies(policyInternalDAO.getByOwnerId(tx, ownerId));
  }

  public Policy getByOwnerIdAndName(String ownerId, String policyName) {
    try (TransactionContext tx = policyInternalDAO.createTransactionContext()) {
      return getByOwnerIdAndName(tx, ownerId, policyName);
    }
  }

  public Policy getByOwnerIdAndName(TransactionContext tx, String ownerId, String policyName) {
    return PolicyInternal.toPolicy(policyInternalDAO.getByOwnerIdAndName(tx, ownerId, policyName));
  }

  public List<Policy> getAll() {
    try (TransactionContext tx = policyInternalDAO.createTransactionContext()) {
      return getAll(tx);
    }
  }

  public List<Policy> getAll(TransactionContext tx) {
    return PolicyInternal.toPolicies(policyInternalDAO.getAll(tx));
  }

  /**
   * Gets all policies whose owner id is among the specified collection.
   */
  public List<Policy> getByOwnerIds(Set<String> ownerIds) {
    return PolicyInternal.toPolicies(policyInternalDAO.getByOwnerIds(ownerIds));
  }

  public void insert(Policy policy) {
    try (TransactionContext tx = policyInternalDAO.createTransactionContext()) {
      tx.begin();
      insert(tx, policy);
      tx.commit();
    }
  }

  public int insert(TransactionContext tx, Policy policy) {
    String ownerId = policy.getOwnerId();

    ValidationResult validationResult = policyValidator.validate(tx, policy, ownerId);
    if (validationResult != null && !validationResult.isValid()) {
      throw new InvalidPolicyException(validationResult);
    }

    PolicyInternal existingPolicy = policyInternalDAO.getByOwnerIdAndName(tx, ownerId, policy.getName());
    if (existingPolicy != null) {
      throw new InvalidPolicyException("A policy with name '" + existingPolicy.getName() + "' already exists");
    }

    validateNameWithinHierarchy(tx, ownerId, policy);

    // Allocate unique ids to constraints
    for (Constraint constraint : policy.getConstraints()) {
      constraint.setId(newUUID());
    }

    // We need the policy id to be set before the drools code is generated
    if (StringUtils.isBlank(policy.getId())) {
      policy.setId(newUUID());
    }
    DroolsGenerator.generate(tx, policy);

    PolicyInternal policyInternal = PolicyInternal.fromPolicy(policy);
    int inserted = policyInternalDAO.insert(tx, policyInternal);
    policy.setId(policyInternal.getId());

    return inserted;
  }

  public void update(Policy policy) {
    update(policy, true);
  }

  public void update(Policy policy, boolean validate) {
    try (TransactionContext tx = policyInternalDAO.createTransactionContext()) {
      tx.begin();
      update(tx, policy, validate);
      tx.commit();
    }
  }

  public void update(TransactionContext tx, Policy policy) {
    update(tx, policy, true);
  }

  private void update(TransactionContext tx, Policy policy, boolean validate) {
    String ownerId = policy.getOwnerId();

    if (validate) {
      ValidationResult validationResult = policyValidator.validate(tx, policy, ownerId);
      if (validationResult != null && !validationResult.isValid()) {
        throw new InvalidPolicyException(validationResult);
      }
    }

    PolicyInternal existingPolicyByName = policyInternalDAO.getByOwnerIdAndName(tx, ownerId, policy.getName());
    if (existingPolicyByName != null && !policy.getId().equals(existingPolicyByName.getId())) {
      throw new InvalidPolicyException("A policy with name '" + existingPolicyByName.getName() + "' already exists");
    }

    Policy existingPolicyById = PolicyInternal.toPolicy(policyInternalDAO.getByIdNotNull(tx, policy.getId()));

    if (!existingPolicyById.getName().equals(policy.getName())) {
      // only validate name if the name of the policy has changed
      // no validations needed if the name has not been modified
      validateNameWithinHierarchy(tx, ownerId, policy);
    }

    // Allocate ids to new constraints
    for (Constraint constraint : policy.getConstraints()) {
      if (existingPolicyById.getConstraintById(constraint.getId()) == null) {
        // This is a new constraint
        constraint.setId(newUUID());
      }
    }

    DroolsGenerator.generate(tx, policy);

    PolicyInternal policyInternal = PolicyInternal.fromPolicy(policy);
    policyInternalDAO.update(tx, policyInternal);
  }

  public void delete(Policy policy) {
    try (TransactionContext tx = policyInternalDAO.createTransactionContext()) {
      tx.begin();
      delete(tx, policy);
      tx.commit();
    }
  }

  public void delete(TransactionContext tx, Policy policy) {
    PolicyInternal policyInternal = PolicyInternal.fromPolicy(policy);
    policyInternalDAO.delete(tx, policyInternal);
  }

  public void deleteByOwnerId(TransactionContext tx, String ownerId) {
    for (PolicyInternal policy : policyInternalDAO.getByOwnerId(tx, ownerId)) {
      policyInternalDAO.delete(tx, policy);
    }
  }

  private static String newUUID() {
    return UUID.randomUUID().toString().replace("-", "");
  }

  public List<Policy> getApplicableByOwnerIdWithHierarchy(String ownerId) {
    try (TransactionContext tx = policyInternalDAO.createTransactionContext()) {
      return getApplicableByOwnerIdWithHierarchy(tx, ownerId);
    }
  }

  public List<Policy> getApplicableByOwnerIdWithHierarchy(TransactionContext tx, String ownerId) {
    return PolicyInternal.toPolicies(policyInternalDAO.getApplicableByOwnerIdWithHierarchy(tx, ownerId));
  }

  private void validateNameWithinHierarchy(
      TransactionContext tx,
      final String ownerId,
      final Policy policy) throws InvalidPolicyException
  {
    List<String> conflicts = policyInternalDAO.getAncestorOrDescendantWithPolicyNameMatching(
        tx, ownerId, policy.getName());

    if (!conflicts.isEmpty()) {
      Owner conflictingOwner = ownerDAO.getByIdNotNull(conflicts.get(0));
      throw new InvalidPolicyException("A policy with the same name already exists for "
          + conflictingOwner.getType() + " '" + conflictingOwner.getName() + "'");
    }
  }

  public List<Policy> getByName(String name) {
    return PolicyInternal.toPolicies(policyInternalDAO.getByName(name));
  }

  public TransactionContext createTransactionContext() {
    return policyInternalDAO.createTransactionContext();
  }
}
