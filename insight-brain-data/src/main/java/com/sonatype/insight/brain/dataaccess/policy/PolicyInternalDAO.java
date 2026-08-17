/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.Collection;
import java.util.List;
import java.util.Set;

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

import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Record1;
import org.jooq.Table;
import org.jooq.impl.DSL;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.ApplicationTag.APPLICATION_TAG;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.OwnerAncestor.OWNER_ANCESTOR;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.Policy.POLICY;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.PolicyTag.POLICY_TAG;

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

  @Override
  public List<PolicyInternal> getByIds(Collection<String> ids) {
    if (ids == null || ids.isEmpty()) {
      return java.util.Collections.emptyList();
    }
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(POLICY)
          .where(POLICY.POLICY_ID.in(ids))
          .fetch(this::toEntity);
    }
  }

  List<PolicyInternal> getByOwnerIds(Set<String> ownerIds) {
    if (ownerIds == null || ownerIds.isEmpty()) {
      return java.util.Collections.emptyList();
    }
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(POLICY)
          .where(POLICY.OWNER_ID.in(ownerIds))
          .orderBy(POLICY.NAME_LOWERCASE_NO_WHITESPACE)
          .fetch(this::toEntity);
    }
  }

  long selectCountByOwnerIds(@Nullable Set<String> ownerIds) {
    return countByField(POLICY, POLICY.OWNER_ID, ownerIds);
  }

  List<PolicyInternal> getApplicableByOwnerIdWithHierarchy(TransactionContext tx, String ownerId) {
    // Complex query joining Policy with OwnerAncestor and conditional logic for tags
    // Using jOOQ to implement the same logic as the original JPA query

    var appTagForPolicy = tx.dsl()
        .select(APPLICATION_TAG.TAG_ID)
        .from(APPLICATION_TAG)
        .join(POLICY_TAG)
        .on(APPLICATION_TAG.TAG_ID.eq(POLICY_TAG.TAG_ID))
        .where(APPLICATION_TAG.APPLICATION_ID.eq(OWNER_ANCESTOR.OWNER_ID))
        .and(POLICY_TAG.POLICY_ID.eq(POLICY.POLICY_ID));

    var policyHasTags = tx.dsl()
        .selectOne()
        .from(POLICY_TAG)
        .where(POLICY_TAG.POLICY_ID.eq(POLICY.POLICY_ID));

    // Build the complex condition
    var isApplication = OWNER_ANCESTOR.OWNER_TYPE.eq("APPLICATION");
    var isRepository = OWNER_ANCESTOR.OWNER_TYPE.eq("REPOSITORY");
    var isRepositoryManager = OWNER_ANCESTOR.OWNER_TYPE.eq("REPOSITORY_MANAGER");
    var isRepositoryContainer = OWNER_ANCESTOR.OWNER_TYPE.eq("REPOSITORY_CONTAINER");
    var isHostedRepositoryComponent = OWNER_ANCESTOR.OWNER_TYPE.eq("HOSTED_REPOSITORY_COMPONENT");
    var isNotDirectlyAttached = OWNER_ANCESTOR.OWNER_ID.ne(OWNER_ANCESTOR.ANCESTOR_ID);
    var isDirectlyAttached = OWNER_ANCESTOR.OWNER_ID.eq(OWNER_ANCESTOR.ANCESTOR_ID);

    var applicationCondition = isApplication
        .and(isNotDirectlyAttached)
        .and(DSL.exists(appTagForPolicy).or(DSL.notExists(policyHasTags)));

    // Repository-family scopes (Repository, RepositoryManager, RepositoryContainer, and
    // HostedRepositoryComponent) share the same policy-filtering rule: only untagged
    // ancestor policies apply. Policy tags are an application-only concept — repository
    // artifacts have no tag axis, so tagged ancestor policies are excluded here.
    var repositoryCondition = isRepository.or(isRepositoryManager)
        .or(isRepositoryContainer)
        .or(isHostedRepositoryComponent)
        .and(isNotDirectlyAttached)
        .and(DSL.notExists(policyHasTags));

    var orgCondition = isApplication.not()
        .and(isRepository.not())
        .and(isRepositoryManager.not())
        .and(isRepositoryContainer.not())
        .and(isHostedRepositoryComponent.not());

    return tx.dsl()
        .select(POLICY.fields())
        .from(POLICY)
        .join(OWNER_ANCESTOR)
        .on(OWNER_ANCESTOR.ANCESTOR_ID.eq(POLICY.OWNER_ID))
        .where(OWNER_ANCESTOR.OWNER_ID.eq(ownerId))
        .and(applicationCondition
            .or(repositoryCondition)
            .or(orgCondition)
            .or(isDirectlyAttached))
        .orderBy(OWNER_ANCESTOR.ANCESTOR_DISTANCE)
        .fetch(r -> toEntity(r.into(POLICY)));
  }

  /**
   * Returns the IDs of all ancestors and descendants of {@code ownerId} that have a policy
   * matching {@code name}.
   */
  List<String> getAncestorOrDescendantWithPolicyNameMatching(
      TransactionContext tx,
      String ownerId,
      String name)
  {
    String normalizedName = NameHelper.normalize(name);

    var ancestor = OWNER_ANCESTOR.as("oa_ancestor");
    var descendant = OWNER_ANCESTOR.as("oa_descendant");

    // All owner IDs in the hierarchy of ownerId: ancestors (upward) UNION descendants (downward).
    // owner_ancestor covers all owner types: ORGANIZATION, APPLICATION, REPOSITORY_CONTAINER,
    // REPOSITORY_MANAGER, REPOSITORY, and HOSTED_REPOSITORY_COMPONENT.
    //
    // Self-exclusion uses ANCESTOR_ID != ownerId rather than ancestor_distance > 0
    // to be robust against future view changes and for clarity.
    var hierarchyOwnerIds = tx.dsl()
        .select(ancestor.ANCESTOR_ID)
        .from(ancestor)
        .where(ancestor.OWNER_ID.eq(ownerId))
        .and(ancestor.ANCESTOR_ID.ne(ownerId)) // exclude self
        .union(
            tx.dsl()
                .select(descendant.OWNER_ID)
                .from(descendant)
                .where(descendant.ANCESTOR_ID.eq(ownerId))
                .and(descendant.OWNER_ID.ne(ownerId)));

    return tx.dsl()
        .select(POLICY.OWNER_ID)
        .from(POLICY)
        .where(POLICY.OWNER_ID.in(hierarchyOwnerIds))
        .and(POLICY.NAME_LOWERCASE_NO_WHITESPACE.eq(normalizedName))
        .orderBy(POLICY.OWNER_ID)
        .fetch(Record1::value1);
  }

  List<PolicyInternal> getByOwnerId(String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerId(tx, ownerId);
    }
  }

  List<PolicyInternal> getByOwnerId(TransactionContext tx, String ownerId) {
    return tx.dsl()
        .selectFrom(POLICY)
        .where(POLICY.OWNER_ID.eq(ownerId))
        .orderBy(POLICY.NAME_LOWERCASE_NO_WHITESPACE)
        .fetch(this::toEntity);
  }

  PolicyInternal getByOwnerIdAndName(TransactionContext tx, String ownerId, String name) {
    name = NameHelper.normalize(name);
    return toEntity(tx.dsl()
        .selectFrom(POLICY)
        .where(POLICY.OWNER_ID.eq(ownerId))
        .and(POLICY.NAME_LOWERCASE_NO_WHITESPACE.eq(name))
        .fetchOne());
  }

  List<PolicyInternal> getByName(String name) {
    name = NameHelper.normalize(name);
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(POLICY)
          .where(POLICY.NAME_LOWERCASE_NO_WHITESPACE.eq(name))
          .fetch(this::toEntity);
    }
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

  @Override
  public Table<?> getJooqTable() {
    return POLICY;
  }

  @Override
  public Class<PolicyInternal> getEntityClass() {
    return PolicyInternal.class;
  }
}
