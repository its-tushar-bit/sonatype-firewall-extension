/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.persistence.EntityManager;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.tag.ApplicationTagDAO;
import com.sonatype.insight.brain.dataaccess.tag.PolicyTagDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.InvalidPolicyException;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.ValidationResult;
import com.sonatype.insight.brain.model.tag.ApplicationTag;
import com.sonatype.insight.brain.model.tag.PolicyTag;

public class PolicyDAO
{
  private static PolicyInternalDAO policyInternalDAO = new PolicyInternalDAO();

  public Policy getByOwnerIdAndPolicyId(String ownerId, String policyId) {
    List<Policy> policies = getByOwnerId(ownerId);
    for (Policy policy : policies) {
      if (policy.getId().equals(policyId)) {
        return policy;
      }
    }

    return null;
  }

  public Policy getById(String id) {
    return PolicyInternal.toPolicy(policyInternalDAO.getById(id));
  }

  public Policy getById(EntityManager em, String id) {
    return PolicyInternal.toPolicy(policyInternalDAO.getById(em, id));
  }

  public Policy getByIdNotNull(String id) {
    return PolicyInternal.toPolicy(policyInternalDAO.getByIdNotNull(id));
  }

  public List<Policy> getByOwnerId(final String ownerId) {
    return PolicyInternal.toPolicies(policyInternalDAO.getByOwnerId(ownerId));
  }

  public void insert(Policy policy) {
    EntityManager em = policyInternalDAO.createEntityManager();
    try {
      em.getTransaction().begin();
      insert(em, policy);
      em.getTransaction().commit();
    }
    finally {
      PolicyInternalDAO.close(em);
    }
  }

  public void insert(EntityManager em, Policy policy) {
    String ownerId = policy.getOwnerId();
    
    ValidationResult validationResult = policy.validate(ownerId);
    if (validationResult != null && !validationResult.isValid()) {
      throw new InvalidPolicyException(validationResult);
    }

    PolicyInternal existingPolicy = policyInternalDAO.getByOwnerIdAndName(em, ownerId, policy.getName());
    if (existingPolicy != null) {
      throw new InvalidPolicyException("A policy with name '" + existingPolicy.getName() + "' already exists");
    }

    validateNameWithinHierarchy(em, ownerId, policy.getName());

    // Allocate unique ids to constraints
    for (Constraint constraint : policy.getConstraints()) {
      constraint.setId(newUUID());
    }
    
    PolicyInternal policyInternal = PolicyInternal.fromPolicy(policy);
    policyInternalDAO.insert(em, policyInternal);
    policy.setId(policyInternal.getId());
  }

  public void update(Policy policy) {
    EntityManager em = policyInternalDAO.createEntityManager();
    try {
      em.getTransaction().begin();
      update(em, policy);
      em.getTransaction().commit();
    }
    finally {
      PolicyInternalDAO.close(em);
    }
  }

  public void update(EntityManager em, Policy policy) {
    String ownerId = policy.getOwnerId();

    ValidationResult validationResult = policy.validate(ownerId);
    if (validationResult != null && !validationResult.isValid()) {
      throw new InvalidPolicyException(validationResult);
    }

    PolicyInternal existingPolicyByName = policyInternalDAO.getByOwnerIdAndName(em, ownerId, policy.getName());
    if (existingPolicyByName != null && !policy.getId().equals(existingPolicyByName.getId())) {
      throw new InvalidPolicyException("A policy with name '" + existingPolicyByName.getName() + "' already exists");
    }

    Policy existingPolicyById = PolicyInternal.toPolicy(policyInternalDAO.getByIdNotNull(em, policy.getId()));

    validateNameWithinHierarchy(em, ownerId, policy.getName());

    // Allocate ids to new constraints
    for (Constraint constraint : policy.getConstraints()) {
      if (existingPolicyById.getConstraintById(constraint.getId()) == null) {
        // This is a new constraint
        constraint.setId(newUUID());
      }
    }

    PolicyInternal policyInternal = PolicyInternal.fromPolicy(policy);
    policyInternalDAO.update(em, policyInternal);
  }

  public void delete(Policy policy) {
    EntityManager em = policyInternalDAO.createEntityManager();
    try {
      em.getTransaction().begin();
      delete(em, policy);
      em.getTransaction().commit();
    }
    finally {
      PolicyInternalDAO.close(em);
    }
  }

  public void delete(EntityManager em, Policy policy) {
    PolicyInternal policyInternal = PolicyInternal.fromPolicy(policy);
    policyInternalDAO.delete(em, policyInternal);
  }

  public void deleteByOwnerId(EntityManager em, String ownerId) {
    for (PolicyInternal policy : policyInternalDAO.getByOwnerId(em, ownerId)) {
      policyInternalDAO.delete(em, policy);
    }
  }

  private static String newUUID() {
    return UUID.randomUUID().toString().replace("-", "");
  }

  public List<Policy> getApplicableByOwnerId(final String ownerId) {
    List<Policy> result = new ArrayList<Policy>();

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

  private void validateNameWithinHierarchy(EntityManager em, final String ownerId, final String name)
      throws InvalidPolicyException
  {
    ApplicationDAO applicationDAO = new ApplicationDAO();
    Application parentApplication = applicationDAO.getById(em, ownerId);
    if (parentApplication != null) {
      // The owner is an application
      if (policyInternalDAO.getByOwnerIdAndName(em, parentApplication.getOrganizationId(), name) != null) {
        throw new InvalidPolicyException("A policy with the same name already exists" + " for the parent organization");
      }
    }
    else {
      // The owner is an organization
      List<Application> applications = applicationDAO.getByOrganizationId(em, ownerId);
      for (Application application : applications) {
        if (policyInternalDAO.getByOwnerIdAndName(em, application.getId(), name) != null) {
          throw new InvalidPolicyException("A policy with the same name already exists" + " for application '"
              + application.getName() + "'");
        }
      }
    }
  }

  public EntityManager createEntityManager() {
    return policyInternalDAO.createEntityManager();
  }

  public static void close(EntityManager em) {
    PolicyInternalDAO.close(em);
  }
}
