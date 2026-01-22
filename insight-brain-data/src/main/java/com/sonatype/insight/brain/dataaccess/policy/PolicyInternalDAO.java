/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

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

  List<PolicyInternal> getByOwnerIds(Set<String> ownerIds) {
    String sQuery = "SELECT entity FROM PolicyInternal entity" + //
        " WHERE entity.ownerId IN (?1)" + //
        " ORDER BY entity.nameLowercaseNoWhitespace";
    int inOperatorThreshold = getInOperatorThreshold();
    if (ownerIds != null && ownerIds.size() >= inOperatorThreshold) {
      return getListWithSqlInClause(ownerIds, c -> getList(sQuery, c));
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
        //   if owner is an application
        "    oa.ownerType = com.sonatype.insight.brain.model.OwnerType.APPLICATION " +
        ///  and the policy is attached to the parent org (not directly to an app)
        "    AND oa.id <> oa.ancestorId" +
        //   and the policy has tags (categories) attached, then only include the policy if the app also has at least
        //   one of the tags
        "    AND (EXISTS (" +
        "            SELECT appTag.tagId" +
        "            FROM ApplicationTag appTag, PolicyTag pTag" +
        "            WHERE appTag.tagId = pTag.tagId" +
        "              AND appTag.applicationId = oa.id" +
        "              AND pTag.policyId = policy.id" +
        "       )" +
        //      or the policy does not have any tags
        "       OR NOT EXISTS(" +
        "           SELECT policyTag FROM PolicyTag policyTag WHERE policyTag.policyId = policy.id" +
        "       )" +
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
    // Cascade to policy waiver requests is done in the db by foreign key constraint
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
