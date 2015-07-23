/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.tag.ApplicationTagDAO;
import com.sonatype.insight.brain.dataaccess.tag.PolicyTagDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.ValidationResult;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.InvalidPolicyException;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.tag.ApplicationTag;
import com.sonatype.insight.brain.model.tag.PolicyTag;
import com.sonatype.insight.brain.policy.DroolsGenerator;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.codehaus.plexus.util.StringUtils;

public class PolicyDAO
{
  private static PolicyInternalDAO policyInternalDAO = new PolicyInternalDAO();

  private static OrganizationDAO orgDAO = new OrganizationDAO();

  private static OwnerDAO ownerDAO = new OwnerDAO();

  public Policy getById(String id) {
    return PolicyInternal.toPolicy(policyInternalDAO.getById(id));
  }

  public Policy getById(TransactionContext tx, String id) {
    return PolicyInternal.toPolicy(policyInternalDAO.getById(tx, id));
  }

  public Policy getByIdNotNull(String id) {
    return PolicyInternal.toPolicy(policyInternalDAO.getByIdNotNull(id));
  }

  public List<Policy> getByOwnerId(final String ownerId) {
    return PolicyInternal.toPolicies(policyInternalDAO.getByOwnerId(ownerId));
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
  public List<Policy> getByOwnerIds(Collection<String> ownerIds) {
    return PolicyInternal.toPolicies(policyInternalDAO.getByOwnerIds(ownerIds));
  }

  public void insert(Policy policy) {
    try (TransactionContext tx = policyInternalDAO.createTransactionContext()) {
      tx.begin();
      insert(tx, policy);
      tx.commit();
    }
  }

  public void insert(TransactionContext tx, Policy policy) {
    String ownerId = policy.getOwnerId();

    ValidationResult validationResult = policy.validate(ownerId);
    if (validationResult != null && !validationResult.isValid()) {
      throw new InvalidPolicyException(validationResult);
    }

    PolicyInternal existingPolicy = policyInternalDAO.getByOwnerIdAndName(tx, ownerId, policy.getName());
    if (existingPolicy != null) {
      throw new InvalidPolicyException("A policy with name '" + existingPolicy.getName() + "' already exists");
    }

    validateNameWithinHierarchy(tx, ownerId, policy.getName());

    // Allocate unique ids to constraints
    for (Constraint constraint : policy.getConstraints()) {
      constraint.setId(newUUID());
    }

    // We need the policy id to be set before the drools code is generated
    if (StringUtils.isBlank(policy.getId())) {
      policy.setId(newUUID());
    }
    DroolsGenerator.generate(policy);

    PolicyInternal policyInternal = PolicyInternal.fromPolicy(policy);
    policyInternalDAO.insert(tx, policyInternal);
    policy.setId(policyInternal.getId());
  }

  public void update(Policy policy) {
    try (TransactionContext tx = policyInternalDAO.createTransactionContext()) {
      tx.begin();
      update(tx, policy);
      tx.commit();
    }
  }

  public void update(TransactionContext tx, Policy policy) {
    String ownerId = policy.getOwnerId();

    ValidationResult validationResult = policy.validate(ownerId);
    if (validationResult != null && !validationResult.isValid()) {
      throw new InvalidPolicyException(validationResult);
    }

    PolicyInternal existingPolicyByName = policyInternalDAO.getByOwnerIdAndName(tx, ownerId, policy.getName());
    if (existingPolicyByName != null && !policy.getId().equals(existingPolicyByName.getId())) {
      throw new InvalidPolicyException("A policy with name '" + existingPolicyByName.getName() + "' already exists");
    }

    Policy existingPolicyById = PolicyInternal.toPolicy(policyInternalDAO.getByIdNotNull(tx, policy.getId()));

    validateNameWithinHierarchy(tx, ownerId, policy.getName());

    // Allocate ids to new constraints
    for (Constraint constraint : policy.getConstraints()) {
      if (existingPolicyById.getConstraintById(constraint.getId()) == null) {
        // This is a new constraint
        constraint.setId(newUUID());
      }
    }

    DroolsGenerator.generate(policy);

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

  public List<Policy> getApplicableByOwnerId(final String ownerId) {
    List<Policy> result = new ArrayList<>();

    result.addAll(getByOwnerId(ownerId));

    Application application = new ApplicationDAO().getById(ownerId);
    if (application != null) {
      // ownerId is an app id
      List<ApplicationTag> appTags = new ApplicationTagDAO().getByApplicationId(ownerId);
      PolicyTagDAO policyTagDAO = new PolicyTagDAO();
      List<Policy> orgPolicies = getByOwnerId(application.getOrganizationId());
      for (Policy orgPolicy : orgPolicies) {
        List<PolicyTag> policyTags = policyTagDAO.getByPolicyId(orgPolicy.getId());
        if (policyTags.isEmpty() || intersects(policyTags, appTags)) {
          result.add(orgPolicy);
        }
      }
    }
    return result;
  }

  private boolean intersects(List<PolicyTag> policyTags, List<ApplicationTag> appTags) {
    if (appTags.isEmpty()) {
      return false;
    }

    for (PolicyTag policyTag : policyTags) {
      for (ApplicationTag appTag : appTags) {
        if (policyTag.getTagId().equals(appTag.getTagId())) {
          return true;
        }
      }
    }
    return false;
  }

  private void validateNameWithinHierarchy(TransactionContext tx, final String ownerId, final String name)
      throws InvalidPolicyException
  {
    Owner owner = ownerDAO.getById(tx, ownerId);

    validateNameWithinHierarchyUp(tx, owner.getParentOrganizationId(), name);
    validateNameWithinHierarchyDown(tx, owner, name);
  }

  private void validateNameWithinHierarchyUp(TransactionContext tx, String parentId, String name)
      throws InvalidPolicyException
  {
    if (parentId == null) {
      return; // no parent, we're done
    }
    Organization parentOrganization = orgDAO.getByIdNotNull(parentId);
    if (policyInternalDAO.getByOwnerIdAndName(tx, parentOrganization.getId(), name) != null) {
      throw new InvalidPolicyException("A policy with the same name already exists for organization '"
          + parentOrganization.getName() + "'");
    }
    validateNameWithinHierarchyUp(tx, parentOrganization.getParentOrganizationId(), name);
  }

  private void validateNameWithinHierarchyDown(TransactionContext tx, Owner owner, String name)
      throws InvalidPolicyException
  {
    if (!owner.canHaveChildren()) {
      return;
    }
    List<Owner> childOwners = ownerDAO.getChildOwners(tx, owner);
    for (Owner childOwner : childOwners) {
      if (policyInternalDAO.getByOwnerIdAndName(tx, childOwner.getId(), name) != null) {
        throw new InvalidPolicyException("A policy with the same name already exists for " + childOwner.getType()
            + " '" + childOwner.getName() + "'");
      }

      validateNameWithinHierarchyDown(tx, childOwner, name);
    }
  }
}
