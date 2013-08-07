/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.InvalidPolicyException;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.ValidationResult;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.json.store.JsonStore;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PolicyDAO
{
  public static final String POLICY_FILENAME = "policy.json";

  private static final Logger log = LoggerFactory.getLogger(PolicyDAO.class);

  private final File workDir;

  private String user;

  private String ip;

  private String where;

  public PolicyDAO(final File workDir) {
    this.workDir = workDir;
  }

  public Policy getByOwnerIdAndPolicyId(String ownerId, String policyId) {
    List<Policy> policies = getByOwnerId(ownerId);
    for (Policy policy : policies) {
      if (policy.getId().equals(policyId)) {
        return policy;
      }
    }

    return null;
  }

  public List<Policy> getByOwnerId(final String ownerId) {
    final JsonStore store = policyStore(ownerId);
    return getByOwnerId(ownerId, store);
  }

  private List<Policy> getByOwnerId(final String ownerId, final JsonStore store) {
    final List<Policy> result = new ArrayList<Policy>();
    try {
      final ArrayNode policies = loadPolicies(store);
      Collections.addAll(result, JsonUtils.asPojo(policies, Policy[].class));
      // The policies may have been saved without an ownerId (i.e. before 1.6), so fill in the owner id here.
      for (Policy policy : result) {
        policy.setOwnerId(ownerId);
      }
    }
    catch (final IOException e) {
      log.error("Failed to load policies", e);
      throw new IllegalStateException(e);
    }
    return result;
  }

  public Policy insert(final String ownerId, final Policy policy) {
    policy.setOwnerId(ownerId);
    ValidationResult validationResult = policy.validate(ownerId);
    if (validationResult != null && !validationResult.isValid()) {
      throw new InvalidPolicyException(validationResult);
    }

    final JsonStore store = policyStore(ownerId);
    try {
      final ArrayNode policiesJson = loadPolicies(store);
      Policy[] existingPolicies = JsonUtils.asPojo(policiesJson, Policy[].class);
      for (Policy existingPolicy : existingPolicies) {
        if (NameHelper.equals(policy.getName(), existingPolicy.getName())) {
          throw new InvalidPolicyException("A policy with name '" + existingPolicy.getName() + "' already exists");
        }
      }

      // Allocate unique ids to the policy and its constraints
      policy.setId(newUUID());
      for (Constraint constraint : policy.getConstraints()) {
        constraint.setId(newUUID());
      }

      policiesJson.add(JsonUtils.asTree(policy));

      List<Lock> readLocks = new ArrayList<Lock>();
      try {
        validateNameWithinHierarchy(ownerId, policy.getName(), readLocks);
        savePolicies(store, policiesJson);
      }
      finally {
        unlock(readLocks);
      }
    }
    catch (final IOException e) {
      log.error("Failed to insert policy {}", policy, e);
      throw new IllegalStateException(e);
    }
    return policy;
  }

  public Policy update(final String ownerId, final Policy policy) {
    policy.setOwnerId(ownerId);
    ValidationResult validationResult = policy.validate(ownerId);
    if (validationResult != null && !validationResult.isValid()) {
      throw new InvalidPolicyException(validationResult);
    }

    final JsonStore store = policyStore(ownerId);
    try {
      boolean updated = false;
      final ArrayNode policiesJson = loadPolicies(store);
      for (int i = 0; i < policiesJson.size(); i++) {
        JsonNode oldPolicyJson = policiesJson.get(i);
        Policy existingPolicy = JsonUtils.asPojo(oldPolicyJson, Policy.class);
        if (policy.getId().equals(existingPolicy.getId())) {
          // Allocate ids to new constraints
          for (Constraint constraint : policy.getConstraints()) {
            if (existingPolicy.getConstraintById(constraint.getId()) == null) {
              // This is a new constraint
              constraint.setId(newUUID());
            }
          }

          // Update the policy
          policiesJson.set(i, JsonUtils.asTree(policy));
          updated = true;
        }
        else {
          if (NameHelper.equals(policy.getName(), existingPolicy.getName())) {
            throw new InvalidPolicyException("A policy with name '" + existingPolicy.getName() + "' already exists");
          }
        }
      }

      if (!updated) {
        throw new InvalidPolicyException("The policy does not exist");
      }

      List<Lock> readLocks = new ArrayList<Lock>();
      try {
        validateNameWithinHierarchy(ownerId, policy.getName(), readLocks);
        savePolicies(store, policiesJson);
      }
      finally {
        unlock(readLocks);
      }
    }
    catch (final IOException e) {
      log.error("Failed to update policy {}", policy, e);
      throw new IllegalStateException(e);
    }
    return policy;
  }

  public void delete(final String ownerId, final String policyId) {
    final JsonStore store = policyStore(ownerId);
    try {
      final ArrayNode policies = loadPolicies(store);
      for (int i = 0; i < policies.size(); i++) {
        Policy policy = JsonUtils.asPojo(policies.get(i), Policy.class);
        if (policyId.equals(policy.getId())) {
          policies.remove(i);
          savePolicies(store, policies);

          // Cascade to policy waivers
          PolicyWaiverDAO policyWaiverDAO = new PolicyWaiverDAO();
          List<PolicyWaiver> policyWaivers = policyWaiverDAO.getByPolicyId(policyId);
          for (PolicyWaiver policyWaiver : policyWaivers) {
            policyWaiverDAO.delete(policyWaiver);
          }

          return;
        }
      }
    }
    catch (final IOException e) {
      log.error("Failed to delete policy {}", policyId, e);
      throw new IllegalStateException(e);
    }
  }

  public void deleteByOwnerId(final String ownerId) {
    final File policyDir = getPolicyDir(ownerId);
    try {
      FileUtils.deleteDirectory(policyDir);
    }
    catch (IOException e) {
      log.error("Failed to bulk delete policies for {}", ownerId, e);
      throw new IllegalStateException(e);
    }
  }

  public PolicyDAO session(final String _user, final String _ip, final String _where) {
    user = _user;
    ip = _ip;
    where = _where;
    return this;
  }

  private static ArrayNode loadPolicies(final JsonStore store) throws IOException {
    final ArrayNode policies = (ArrayNode) store.restore(POLICY_FILENAME);
    return policies != null ? policies : JsonUtils.arrayNode(null);
  }

  private void savePolicies(final JsonStore store, final ArrayNode policies) throws IOException {
    store.commit(POLICY_FILENAME, JsonUtils.stamp(user, ip, where, policies));
  }

  private JsonStore policyStore(final String ownerId) {
    return JsonUtils.fileStore(getPolicyDir(ownerId));
  }

  public File getPolicyDir(final String ownerId) {
    return new File(workDir, "policy/" + ownerId);
  }

  private static String newUUID() {
    return UUID.randomUUID().toString().replace("-", "");
  }

  private Policy getByOwnerIdAndName(final String ownerId, final String name, final List<Lock> readLocks) {
    final JsonStore store = policyStore(ownerId, readLocks);
    try {
      final ArrayNode policies = loadPolicies(store);
      for (JsonNode policyJsonNode : policies) {
        Policy policy = JsonUtils.asPojo(policyJsonNode, Policy.class);
        if (NameHelper.equals(policy.getName(), name)) {
          // The policy may have been saved without an ownerId (i.e. before 1.6), so fill in the owner id
          // here.
          policy.setOwnerId(ownerId);
          return policy;
        }
      }
    }
    catch (final IOException e) {
      log.error("Failed to load policies", e);
      throw new IllegalStateException(e);
    }
    return null;
  }

  public List<Policy> getApplicableByOwnerId(final String ownerId) {
    List<Policy> result = new ArrayList<Policy>();
    result.addAll(getByOwnerId(ownerId));
    Application application = new ApplicationDAO().getById(ownerId);
    if (application != null && application.getOrganizationId() != null) {
      result.addAll(getByOwnerId(application.getOrganizationId()));
    }
    return result;
  }

  public void validateNamesWithinHierarchy(final String orgId, final String appId, final List<Lock> readLocks) {
    final JsonStore orgStore = policyStore(orgId, readLocks);
    final Set<String> orgPolicyNames = new LinkedHashSet<String>();
    for (final Policy policy : getByOwnerId(orgId, orgStore)) {
      orgPolicyNames.add(NameHelper.normalize(policy.getName()));
    }

    final JsonStore appStore = policyStore(appId, readLocks);
    final Map<String, String> appPolicyNames = new LinkedHashMap<String, String>();
    final Set<String> invalidPolicyNames = new LinkedHashSet<String>();
    for (final Policy policy : getByOwnerId(appId, appStore)) {
      appPolicyNames.put(NameHelper.normalize(policy.getName()), policy.getName());
      try {
        NameHelper.validate(policy.getName());
      }
      catch (InvalidNameException e) {
        invalidPolicyNames.add(policy.getName());
      }
    }

    if (!invalidPolicyNames.isEmpty()) {
      throw new BadRequestException("The following policies have invalid names: "
          + StringUtils.join(invalidPolicyNames.iterator(), ", "));
    }

    appPolicyNames.keySet().retainAll(orgPolicyNames);
    if (!appPolicyNames.isEmpty()) {
      throw new BadRequestException("The following policies collide with policies of the parent organization: "
          + StringUtils.join(appPolicyNames.values().iterator(), ", "));
    }
  }

  private JsonStore policyStore(final String ownerId, final List<Lock> readLocks) {
    final JsonStore store = policyStore(ownerId);
    Lock readLock = store.readLock();
    readLocks.add(readLock);
    readLock.lock();
    return store;
  }

  private void validateNameWithinHierarchy(final String ownerId, final String name, final List<Lock> readLocks)
      throws InvalidPolicyException
  {
    ApplicationDAO applicationDAO = new ApplicationDAO();
    Application parentApplication = applicationDAO.getById(ownerId);
    if (parentApplication != null) {
      // The owner is an application
      if (parentApplication.getOrganizationId() != null) {
        if (getByOwnerIdAndName(parentApplication.getOrganizationId(), name, readLocks) != null) {
          throw new InvalidPolicyException("A policy with the same name already exists"
              + " for the parent organization");
        }
      }
    }
    else {
      // The owner is an organization
      List<Application> applications = applicationDAO.getByOrganizationId(ownerId);
      for (Application application : applications) {
        if (getByOwnerIdAndName(application.getId(), name, readLocks) != null) {
          throw new InvalidPolicyException("A policy with the same name already exists" + " for application '"
              + application.getName() + "'");
        }
      }
    }
  }

  public static void unlock(List<Lock> locks) {
    if (locks != null) {
      for (Lock lock : locks) {
        try {
          lock.unlock();
        }
        catch (Exception e) {
          log.warn("Failed to release lock {}", lock, e);
        }
      }
    }
  }
}
