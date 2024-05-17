/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.search.SearchIndexManager;
import com.sonatype.insight.brain.dataaccess.tag.PolicyTagDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.brain.model.SearchIndexChange.ChangeType;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.tag.PolicyTag;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * @since 1.9
 */
@Named
@Singleton
public class PolicyInternalDAO
    extends AbstractOperationalSqlDAO<PolicyInternal>
{
  private final PolicyWaiverDAO policyWaiverDAO;

  private final PolicyTagDAO policyTagDAO;

  @Inject
  public PolicyInternalDAO(
      final OperationalDataStore operationalDataStore,
      final SearchIndexManager searchIndexManager,
      final PolicyWaiverDAO policyWaiverDAO,
      final PolicyTagDAO policyTagDAO)
  {
    super(operationalDataStore, searchIndexManager);
    this.policyWaiverDAO = policyWaiverDAO;
    this.policyTagDAO = policyTagDAO;
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
    int inOperatorThreshold = getInOperatorThreshold();
    if (ownerIds != null && ownerIds.size() >= inOperatorThreshold) {
      List<String> ownerIdentifications = new ArrayList<>(ownerIds);
      Map<String, PolicyInternal> policiesById = new LinkedHashMap<>();
      for (int i = 0; i < ownerIds.size(); i += inOperatorThreshold) {
        int upperBound = Math.min(i + inOperatorThreshold, ownerIdentifications.size());
        List<PolicyInternal> policies = getList(sQuery, ownerIdentifications.subList(i, upperBound));
        policies.forEach(policy -> policiesById.put(policy.getId(), policy));
      }
      return new ArrayList<>(policiesById.values());
    }
    else {
      return getList(sQuery, ownerIds);
    }
  }

  List<PolicyInternal> getApplicableByOwnerIdWithHierarchy(TransactionContext tx, String ownerId) {
    String sQuery =
        "SELECT policy " +
        "FROM PolicyInternal policy, OwnerAncestor oa " +
        "WHERE oa.id = ?1 AND oa.ancestorId = policy.ownerId " +
        "AND (" +
        "  (" +
        //   if owner is an application, policies attached to parent orgs only apply if the app has all
        //   of the tags that the policy has
        "    oa.ownerType = com.sonatype.insight.brain.model.OwnerType.APPLICATION " +
        "    AND oa.id <> oa.ancestorId AND NOT EXISTS (" +
        "      SELECT policyTag " +
        "      FROM PolicyTag policyTag " +
        "      WHERE policyTag.policyId = policy.id " +
        "      AND policyTag.tagId NOT IN (" +
        "        SELECT appTag.tagId " +
        "        FROM ApplicationTag appTag " +
        "        WHERE appTag.applicationId = oa.id" +
        "      )" +
        "    )" +
        "  ) " +
        "  OR " +
        "  (" +
        //   if owner is a repo, policies attached to parent orgs only apply if the policy has no tags
        "    ( " +
        //     JPQL doesn't seem to support doing this with an IN clause
        "      oa.ownerType = com.sonatype.insight.brain.model.OwnerType.REPOSITORY " +
        "      OR oa.ownerType = com.sonatype.insight.brain.model.OwnerType.REPOSITORY_MANAGER " +
        "      OR oa.ownerType = com.sonatype.insight.brain.model.OwnerType.REPOSITORY_CONTAINER " +
        "    ) " +
        "    AND oa.id <> oa.ancestorId AND NOT EXISTS (" +
        "      SELECT policyTag " +
        "      FROM PolicyTag policyTag " +
        "      WHERE policyTag.policyId = policy.id" +
        "    )" +
        "  ) " +
        // include all ancestor policies when type not app or repo
        "  OR (" +
        "    oa.ownerType <> com.sonatype.insight.brain.model.OwnerType.APPLICATION " +
        "    AND oa.ownerType <> com.sonatype.insight.brain.model.OwnerType.REPOSITORY" +
        "    AND oa.ownerType <> com.sonatype.insight.brain.model.OwnerType.REPOSITORY_MANAGER" +
        "    AND oa.ownerType <> com.sonatype.insight.brain.model.OwnerType.REPOSITORY_CONTAINER" +
        "  ) " +
        // include all policies attached directly to the queried owner
        "  OR oa.id = oa.ancestorId" +
        ") " +
        "ORDER BY oa.ancestorDistance";

    return getList(tx, sQuery, ownerId);
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

  @Override
  public void delete(TransactionContext tx, PolicyInternal policy) {
    // Cascade to policy waivers
    List<PolicyWaiver> policyWaivers = policyWaiverDAO.getByPolicyId(tx, policy.getId());
    for (PolicyWaiver policyWaiver : policyWaivers) {
      policyWaiverDAO.delete(tx, policyWaiver);
    }

    // Cascade to policy tags
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
