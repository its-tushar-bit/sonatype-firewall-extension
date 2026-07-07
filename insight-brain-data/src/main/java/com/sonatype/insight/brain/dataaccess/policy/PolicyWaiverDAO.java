/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver;
import com.sonatype.insight.brain.policy.comparison.ConstraintFactsListComparator;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.SortField;
import org.jooq.Table;
import org.jooq.impl.DSL;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.Application.APPLICATION;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.Policy.POLICY;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.PolicyWaiver.POLICY_WAIVER;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.PolicyWaiverReason.POLICY_WAIVER_REASON;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.Repository.REPOSITORY;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.ALL_COMPONENTS;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.ALL_VERSIONS;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.EXACT_COMPONENT;

@Named
@Singleton
public class PolicyWaiverDAO
    extends AbstractOperationalSqlDAO<PolicyWaiver>
{
  private final OwnerDAO ownerDAO;

  @Inject
  public PolicyWaiverDAO(
      final OperationalDataStore operationalDataStore,
      final OwnerDAO ownerDAO)
  {
    super(operationalDataStore);
    this.ownerDAO = ownerDAO;
  }

  /**
   * Gets policy waivers filtered by component name and/or repository public ID for dashboard display.
   * Used by Firewall waivers page text filters.
   *
   * @param tx the transaction context
   * @param ownerIds the set of owner IDs to filter by
   * @param componentName optional component name filter (case-insensitive contains match on package URL)
   * @param repositoryPublicId optional repository public ID filter (case-insensitive contains match on repository name)
   * @param activeOnly if true, only return active (non-expired) waivers
   * @param expiryNotBefore if non-null, only waivers with expiry_time &gt;= this date are returned (lower bound)
   * @param expiryNotAfter if non-null, only waivers with expiry_time &lt;= this date are returned (upper bound)
   * @param neverExpiringOnly if true, only waivers with null expiry_time are returned (overrides bounds)
   * @param filteredPolicyIds if non-null and non-empty, only waivers for these policy IDs are returned
   * @param page zero-based page number
   * @param pageSize number of results per page
   * @return list of policy waivers matching the filters, with pageSize + 1 results to detect hasNextPage
   */
  public List<PolicyWaiver> getFilteredForDashboard(
      TransactionContext tx,
      Set<String> ownerIds,
      String componentName,
      String repositoryPublicId,
      boolean activeOnly,
      Date expiryNotBefore,
      Date expiryNotAfter,
      boolean neverExpiringOnly,
      Set<String> filteredPolicyIds,
      int page,
      int pageSize,
      String orderBy)
  {
    if (ownerIds == null || ownerIds.isEmpty() || page < 0) {
      return Collections.emptyList();
    }

    boolean hasRepositoryFilter = repositoryPublicId != null && !repositoryPublicId.isBlank();
    boolean hasComponentFilter = componentName != null && !componentName.isBlank();

    // Note: getListWithSqlInClause is not used here because pagination (LIMIT/OFFSET) requires the
    // full ownerIds set in a single query — chunking would produce incorrect page boundaries.
    // The ownerIds set is bounded by the org hierarchy (repos + repo managers + orgs) which in practice
    // stays well below PostgreSQL's 65535 bind parameter limit.
    var condition = POLICY_WAIVER.OWNER_ID.in(ownerIds);

    if (neverExpiringOnly) {
      condition = condition.and(POLICY_WAIVER.EXPIRY_TIME.isNull());
    }
    else if (activeOnly) {
      condition = condition.and(POLICY_WAIVER.EXPIRY_TIME.isNull()
          .or(POLICY_WAIVER.EXPIRY_TIME.gt(new Date())));
    }

    if (!neverExpiringOnly) {
      if (expiryNotBefore != null) {
        condition = condition.and(POLICY_WAIVER.EXPIRY_TIME.isNotNull()
            .and(POLICY_WAIVER.EXPIRY_TIME.ge(expiryNotBefore)));
      }
      if (expiryNotAfter != null) {
        condition = condition.and(POLICY_WAIVER.EXPIRY_TIME.isNotNull()
            .and(POLICY_WAIVER.EXPIRY_TIME.le(expiryNotAfter)));
      }
    }

    // containsIgnoreCase handles SQL wildcard escaping (%, _) correctly
    if (hasComponentFilter) {
      condition = condition.and(POLICY_WAIVER.ASSOCIATED_PACKAGE_URL.containsIgnoreCase(componentName));
    }

    if (hasRepositoryFilter) {
      condition = condition.and(REPOSITORY.PUBLIC_ID.containsIgnoreCase(repositoryPublicId));
    }

    if (filteredPolicyIds != null && !filteredPolicyIds.isEmpty()) {
      condition = condition.and(POLICY_WAIVER.POLICY_ID.in(filteredPolicyIds));
    }

    int offset = page * pageSize;
    int limit = pageSize + 1; // +1 to detect hasNextPage

    SortField<?> dbSortField = resolveDbSortField(orderBy);
    boolean needsPolicyJoin = requiresPolicyJoin(orderBy);

    // Repository filter intentionally only matches repository-scoped waivers. Organization-scoped
    // waivers are excluded because the user is searching by repository name specifically — org-level
    // waivers don't belong to any single repository and would be noise in repository-filtered results.
    if (hasRepositoryFilter) {
      var query = tx.dsl()
          .select(POLICY_WAIVER.fields())
          .from(POLICY_WAIVER)
          .leftJoin(REPOSITORY)
          .on(POLICY_WAIVER.OWNER_ID.eq(REPOSITORY.REPOSITORY_ID));
      if (needsPolicyJoin) {
        query = query.join(POLICY).on(POLICY_WAIVER.POLICY_ID.eq(POLICY.POLICY_ID));
      }
      return query
          .where(condition)
          .orderBy(dbSortField)
          .limit(limit)
          .offset(offset)
          .fetch(r -> toEntity(r.into(POLICY_WAIVER)));
    }

    if (needsPolicyJoin) {
      return tx.dsl()
          .select(POLICY_WAIVER.fields())
          .from(POLICY_WAIVER)
          .join(POLICY)
          .on(POLICY_WAIVER.POLICY_ID.eq(POLICY.POLICY_ID))
          .where(condition)
          .orderBy(dbSortField)
          .limit(limit)
          .offset(offset)
          .fetch(r -> toEntity(r.into(POLICY_WAIVER)));
    }

    return tx.dsl()
        .select(POLICY_WAIVER.fields())
        .from(POLICY_WAIVER)
        .where(condition)
        .orderBy(dbSortField)
        .limit(limit)
        .offset(offset)
        .fetch(r -> toEntity(r.into(POLICY_WAIVER)));
  }

  private SortField<?> resolveDbSortField(String orderBy) {
    if (orderBy == null) {
      return POLICY_WAIVER.EXPIRY_TIME.asc().nullsLast();
    }
    boolean isDesc = orderBy.startsWith("-");
    String field = isDesc ? orderBy.substring(1) : orderBy;
    return switch (field) {
      case "EXPIRATION_DATE" -> isDesc
          ? POLICY_WAIVER.EXPIRY_TIME.desc().nullsLast()
          : POLICY_WAIVER.EXPIRY_TIME.asc().nullsLast();
      case "CREATION_DATE" -> isDesc
          ? POLICY_WAIVER.CREATE_TIME.desc().nullsLast()
          : POLICY_WAIVER.CREATE_TIME.asc().nullsLast();
      case "COMPONENT_SCOPE" -> isDesc
          ? POLICY_WAIVER.ASSOCIATED_PACKAGE_URL.desc().nullsLast()
          : POLICY_WAIVER.ASSOCIATED_PACKAGE_URL.asc().nullsLast();
      case "POLICY_NAME" -> isDesc
          ? POLICY.NAME.desc().nullsLast()
          : POLICY.NAME.asc().nullsLast();
      case "THREAT_LEVEL" -> isDesc
          ? POLICY.THREAT_LEVEL.desc()
          : POLICY.THREAT_LEVEL.asc();
      // OWNER_SCOPE requires owner hierarchy resolution; fall back to EXPIRY_TIME ASC.
      default -> POLICY_WAIVER.EXPIRY_TIME.asc().nullsLast();
    };
  }

  private boolean requiresPolicyJoin(String orderBy) {
    if (orderBy == null) {
      return false;
    }
    String field = orderBy.startsWith("-") ? orderBy.substring(1) : orderBy;
    return "POLICY_NAME".equals(field) || "THREAT_LEVEL".equals(field);
  }

  public List<PolicyWaiver> getByOwnerId(String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerId(tx, ownerId);
    }
  }

  public List<PolicyWaiver> getActiveByOwnerId(String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getActiveByOwnerId(tx, ownerId);
    }
  }

  /**
   * Gets all Active (non expired) policy waivers that target the specified component hash in the context of the given
   * app/org. Note that a component can be subject to a waiver that refers to its specific hash or to a waiver that
   * applies to the entire app/org.
   */
  public List<PolicyWaiver> getApplicableToComponent(String ownerId, String hash) {
    try (TransactionContext tx = createTransactionContext()) {
      List<PolicyWaiver> waivers = new ArrayList<>();
      waivers.addAll(getActiveByOwnerIdAndHash(tx, ownerId, hash, EXACT_COMPONENT));
      waivers.addAll(getActiveByOwnerIdAndHash(tx, ownerId, null, ALL_COMPONENTS));
      return waivers;
    }
  }

  /**
   * Gets all Active (non expired) policy waivers that target the specified component hash in the context of the given
   * app/org and a packageURL. Note that a component can be subject to a waiver that refers to its specific hash or to a
   * waiver that applies to the entire app/org.
   */
  public List<PolicyWaiver> getApplicableToComponentIncludingAllVersions(
      String ownerId,
      String hash,
      PackageUrlIdentifier purl)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return getApplicableToComponentIncludingAllVersions(tx, ownerId, hash, purl);
    }
  }

  /**
   * Gets all Active (non expired) policy waivers that target the specified component hash in the context of the given
   * app/org and a packageURL. Note that a component can be subject to a waiver that refers to its specific hash or to a
   * waiver that applies to the entire app/org.
   */
  public List<PolicyWaiver> getApplicableToComponentIncludingAllVersions(
      TransactionContext tx,
      String ownerId,
      String hash,
      PackageUrlIdentifier purl)
  {
    List<PolicyWaiver> waivers = new ArrayList<>(getActiveByOwnerIdAndHash(tx, ownerId, hash, EXACT_COMPONENT));
    waivers.addAll(getActiveByOwnerIdAndHash(tx, ownerId, null, ALL_COMPONENTS));
    waivers.addAll(getApplicableToComponentOnlyAllVersions(tx, ownerId, purl));
    return waivers;
  }

  /**
   * Gets all expired policy waivers that target the specified component hash in the context of the given app/org and a
   * packageURL. Note that a component can be subject to a waiver that refers to its specific hash, to all versions of
   * this component or to a waiver that applies to the entire app/org.
   */
  public List<PolicyWaiver> getExpiredToComponentIncludingAllVersions(
      String ownerId,
      String hash,
      PackageUrlIdentifier purl)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return getExpiredToComponentIncludingAllVersions(tx, ownerId, hash, purl);
    }
  }

  /**
   * Gets all expired policy waivers that target the specified component hash in the context of the given app/org and a
   * packageURL. Note that a component can be subject to a waiver that refers to its specific hash, to all versions of
   * this component or to a waiver that applies to the entire app/org.
   */
  public List<PolicyWaiver> getExpiredToComponentIncludingAllVersions(
      TransactionContext tx,
      String ownerId,
      String hash,
      PackageUrlIdentifier purl)
  {
    List<PolicyWaiver> waivers = new ArrayList<>(getExpiredByOwnerIdAndHash(tx, ownerId, hash, EXACT_COMPONENT));
    waivers.addAll(getExpiredByOwnerIdAndHash(tx, ownerId, null, ALL_COMPONENTS));
    waivers.addAll(getExpiredByOwnerIdAndPurl(tx, ownerId, purl));
    return waivers;
  }

  /**
   * Groups applicable waivers by owner ID. No hierarchy scoping is applied here; callers must pass only owner IDs
   * already resolved from the caller's owner hierarchy.
   */
  public Map<String, List<PolicyWaiver>> getApplicableToComponentIncludingAllVersionsByOwnerIds(
      TransactionContext tx,
      Collection<String> ownerIds,
      String hash,
      PackageUrlIdentifier purl)
  {
    if (ownerIds.isEmpty()) {
      return Collections.emptyMap();
    }
    List<PolicyWaiver> waivers = new ArrayList<>(getActiveByOwnerIdsAndHash(tx, ownerIds, hash, EXACT_COMPONENT));
    waivers.addAll(getActiveByOwnerIdsAndHash(tx, ownerIds, null, ALL_COMPONENTS));
    waivers.addAll(keepMatchingWildcardPurl(getActiveByOwnerIdsAndHash(tx, ownerIds, null, ALL_VERSIONS), purl));
    return waivers.stream().collect(Collectors.groupingBy(PolicyWaiver::getOwnerId));
  }

  public Map<String, List<PolicyWaiver>> getApplicableToComponentIncludingAllVersionsByOwnerIds(
      Collection<String> ownerIds,
      String hash,
      PackageUrlIdentifier purl)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return getApplicableToComponentIncludingAllVersionsByOwnerIds(tx, ownerIds, hash, purl);
    }
  }

  /**
   * Groups expired waivers by owner ID. No hierarchy scoping is applied here; callers must pass only owner IDs
   * already resolved from the caller's owner hierarchy.
   */
  public Map<String, List<PolicyWaiver>> getExpiredToComponentIncludingAllVersionsByOwnerIds(
      TransactionContext tx,
      Collection<String> ownerIds,
      String hash,
      PackageUrlIdentifier purl)
  {
    if (ownerIds.isEmpty()) {
      return Collections.emptyMap();
    }
    List<PolicyWaiver> waivers = new ArrayList<>(getExpiredByOwnerIdsAndHash(tx, ownerIds, hash, EXACT_COMPONENT));
    waivers.addAll(getExpiredByOwnerIdsAndHash(tx, ownerIds, null, ALL_COMPONENTS));
    waivers.addAll(keepMatchingWildcardPurl(getExpiredByOwnerIdsAndHash(tx, ownerIds, null, ALL_VERSIONS), purl));
    return waivers.stream().collect(Collectors.groupingBy(PolicyWaiver::getOwnerId));
  }

  public Map<String, List<PolicyWaiver>> getExpiredToComponentIncludingAllVersionsByOwnerIds(
      Collection<String> ownerIds,
      String hash,
      PackageUrlIdentifier purl)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return getExpiredToComponentIncludingAllVersionsByOwnerIds(tx, ownerIds, hash, purl);
    }
  }

  private List<PolicyWaiver> keepMatchingWildcardPurl(List<PolicyWaiver> allVersionWaivers, PackageUrlIdentifier purl) {
    if (purl == null) {
      return Collections.emptyList();
    }
    ComponentIdentifier wildcardComponentIdentifier = purl.toComponentIdentifier().createAlternativeVersion("*");
    return allVersionWaivers.stream()
        .filter(waiver -> waiver.getComponentIdentifier()
            .createAlternativeVersion("*")
            .equals(wildcardComponentIdentifier))
        .collect(Collectors.toList());
  }

  private List<PolicyWaiver> getActiveByOwnerIdsAndHash(
      TransactionContext tx,
      Collection<String> ownerIds,
      String hash,
      ComponentMatcherStrategyForWaiver matcherStrategy)
  {
    return getListWithSqlInClause(ownerIds,
        chunk -> tx.dsl()
            .selectFrom(POLICY_WAIVER)
            .where(POLICY_WAIVER.OWNER_ID.in(chunk))
            .and(hash != null ? POLICY_WAIVER.HASH.eq(hash) : POLICY_WAIVER.HASH.isNull())
            .and(POLICY_WAIVER.EXPIRY_TIME.isNull()
                .or(POLICY_WAIVER.EXPIRY_TIME.gt(new Date())))
            .and(POLICY_WAIVER.COMPONENT_MATCH_STRATEGY.eq(matcherStrategy.name()))
            .fetch(this::toEntity));
  }

  private List<PolicyWaiver> getExpiredByOwnerIdsAndHash(
      TransactionContext tx,
      Collection<String> ownerIds,
      String hash,
      ComponentMatcherStrategyForWaiver matcherStrategy)
  {
    return getListWithSqlInClause(ownerIds,
        chunk -> tx.dsl()
            .selectFrom(POLICY_WAIVER)
            .where(POLICY_WAIVER.OWNER_ID.in(chunk))
            .and(hash != null ? POLICY_WAIVER.HASH.eq(hash) : POLICY_WAIVER.HASH.isNull())
            .and(POLICY_WAIVER.EXPIRY_TIME.lt(new Date()))
            .and(POLICY_WAIVER.COMPONENT_MATCH_STRATEGY.eq(matcherStrategy.name()))
            .fetch(this::toEntity));
  }

  public List<PolicyWaiver> getApplicableToComponentOnlyAllVersions(
      TransactionContext tx,
      String ownerId,
      PackageUrlIdentifier purl)
  {
    if (purl != null) {
      ComponentIdentifier wildcardComponentIdentifier = purl.toComponentIdentifier().createAlternativeVersion("*");
      Predicate<PolicyWaiver> keepOnlyMatchingWildcardPurl =
          waiver -> waiver.getComponentIdentifier().createAlternativeVersion("*").equals(wildcardComponentIdentifier);

      List<PolicyWaiver> appliesToAllVersionsOfSomeComponent =
          getActiveByOwnerIdAndHash(tx, ownerId, null, ALL_VERSIONS);
      return appliesToAllVersionsOfSomeComponent.stream()
          .filter(keepOnlyMatchingWildcardPurl)
          .collect(Collectors.toList());
    }
    return Collections.emptyList();
  }

  private List<PolicyWaiver> getExpiredByOwnerIdAndPurl(
      TransactionContext tx,
      String ownerId,
      PackageUrlIdentifier purl)
  {
    if (purl == null) {
      return Collections.emptyList();
    }

    ComponentIdentifier wildcardComponentIdentifier = purl.toComponentIdentifier().createAlternativeVersion("*");
    Predicate<PolicyWaiver> keepOnlyMatchingWildcardPurl =
        waiver -> waiver.getComponentIdentifier()
            .createAlternativeVersion("*")
            .equals(wildcardComponentIdentifier);

    List<PolicyWaiver> appliesToAllVersionsOfSomeComponent =
        getExpiredByOwnerIdAndHash(tx, ownerId, null, ALL_VERSIONS);
    return appliesToAllVersionsOfSomeComponent.stream()
        .filter(keepOnlyMatchingWildcardPurl)
        .toList();
  }

  public List<PolicyWaiver> getByOwnerHierarchyAndPolicyId(Owner owner, String policyId) {
    List<PolicyWaiver> policyWaivers = new ArrayList<>();

    loadByOwnerAndPolicyId(policyWaivers, owner, policyId);

    return policyWaivers;
  }

  private void loadByOwnerAndPolicyId(List<PolicyWaiver> policyWaivers, Owner owner, String policyId) {
    if (owner == null) {
      return;
    }

    Owner parentOwner = ownerDAO.getById(owner.getParentOwnerId());
    loadByOwnerAndPolicyId(policyWaivers, parentOwner, policyId);
    policyWaivers.addAll(getByOwnerIdAndPolicyId(owner.getId(), policyId));
  }

  private List<PolicyWaiver> getByOwnerIdAndPolicyId(String ownerId, String policyId) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(POLICY_WAIVER)
          .where(POLICY_WAIVER.OWNER_ID.eq(ownerId))
          .and(POLICY_WAIVER.POLICY_ID.eq(policyId))
          .and(POLICY_WAIVER.IS_FOR_CONTAINER_IMAGE.eq(false))
          .fetch(this::toEntity);
    }
  }

  public List<PolicyWaiver> getActiveApplicableByOwnerId(String ownerId) {
    List<PolicyWaiver> policyWaivers = new ArrayList<>();

    loadActiveByOwnerId(policyWaivers, ownerId);

    return policyWaivers;
  }

  private void loadActiveByOwnerId(List<PolicyWaiver> policyWaivers, String ownerId) {
    if (ownerId == null) {
      return;
    }

    Owner owner = ownerDAO.getById(ownerId);
    loadActiveByOwnerId(policyWaivers, owner.getParentOwnerId());
    policyWaivers.addAll(getActiveByOwnerId(ownerId));
  }

  public List<PolicyWaiver> getActiveByOwnerIdAndHash(
      TransactionContext tx,
      String ownerId,
      String hash,
      ComponentMatcherStrategyForWaiver matcherStrategy)
  {
    return tx.dsl()
        .selectFrom(POLICY_WAIVER)
        .where(POLICY_WAIVER.OWNER_ID.eq(ownerId))
        .and(hash != null ? POLICY_WAIVER.HASH.eq(hash) : POLICY_WAIVER.HASH.isNull())
        .and(POLICY_WAIVER.EXPIRY_TIME.isNull()
            .or(POLICY_WAIVER.EXPIRY_TIME.gt(new Date())))
        .and(POLICY_WAIVER.COMPONENT_MATCH_STRATEGY.eq(matcherStrategy.name()))
        .fetch(this::toEntity);
  }

  private List<PolicyWaiver> getExpiredByOwnerIdAndHash(
      TransactionContext tx,
      String ownerId,
      String hash,
      ComponentMatcherStrategyForWaiver matcherStrategy)
  {
    return tx.dsl()
        .selectFrom(POLICY_WAIVER)
        .where(POLICY_WAIVER.OWNER_ID.eq(ownerId))
        .and(hash != null ? POLICY_WAIVER.HASH.eq(hash) : POLICY_WAIVER.HASH.isNull())
        .and(POLICY_WAIVER.EXPIRY_TIME.lt(new Date()))
        .and(POLICY_WAIVER.COMPONENT_MATCH_STRATEGY.eq(matcherStrategy.name()))
        .fetch(this::toEntity);
  }

  public List<PolicyWaiver> getByOwnerIdAndHash(TransactionContext tx, String ownerId, String hash) {
    return tx.dsl()
        .selectFrom(POLICY_WAIVER)
        .where(POLICY_WAIVER.OWNER_ID.eq(ownerId))
        .and(POLICY_WAIVER.HASH.eq(hash))
        .fetch(this::toEntity);
  }

  /**
   * Batch loads policy waivers for multiple component hashes within a single owner context.
   * Returns a map keyed by hash for O(1) lookup during processing.
   * <p>
   * This method is useful for avoiding N+1 queries when processing multiple components
   * that all belong to the same application/organization.
   *
   * @param tx the transaction context
   * @param ownerId the owner ID (application or organization ID)
   * @param hashes set of component hashes to fetch waivers for
   * @return map of hash to list of policy waivers, or empty map if hashes is null/empty
   */
  public Map<String, List<PolicyWaiver>> getByOwnerIdAndHashes(
      TransactionContext tx,
      String ownerId,
      Set<String> hashes)
  {
    if (hashes == null || hashes.isEmpty()) {
      return Collections.emptyMap();
    }

    try (var stream = getStreamWithSqlInClause(hashes, partition -> tx.dsl()
        .selectFrom(POLICY_WAIVER)
        .where(POLICY_WAIVER.OWNER_ID.eq(ownerId))
        .and(POLICY_WAIVER.HASH.in(partition))
        .fetchStream()
        .map(this::toEntity)))
    {
      return stream.collect(Collectors.groupingBy(PolicyWaiver::getHash));
    }
  }

  public List<PolicyWaiver> getByOwnerId(TransactionContext tx, String ownerId) {
    return tx.dsl()
        .selectFrom(POLICY_WAIVER)
        .where(POLICY_WAIVER.OWNER_ID.eq(ownerId))
        .fetch(this::toEntity);
  }

  public List<PolicyWaiver> getActiveByOwnerId(TransactionContext tx, String ownerId) {
    return tx.dsl()
        .selectFrom(POLICY_WAIVER)
        .where(POLICY_WAIVER.OWNER_ID.eq(ownerId))
        .and(POLICY_WAIVER.EXPIRY_TIME.isNull()
            .or(POLICY_WAIVER.EXPIRY_TIME.gt(new Date())))
        .fetch(this::toEntity);
  }

  public List<PolicyWaiver> getByPolicyId(String policyId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByPolicyId(tx, policyId);
    }
  }

  public List<PolicyWaiver> getByPolicyId(TransactionContext tx, String policyId) {
    return tx.dsl()
        .selectFrom(POLICY_WAIVER)
        .where(POLICY_WAIVER.POLICY_ID.eq(policyId))
        .fetch(this::toEntity);
  }

  public List<PolicyWaiver> getActiveByPolicyId(String policyId) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(POLICY_WAIVER)
          .where(POLICY_WAIVER.POLICY_ID.eq(policyId))
          .and(POLICY_WAIVER.EXPIRY_TIME.isNull()
              .or(POLICY_WAIVER.EXPIRY_TIME.gt(new Date())))
          .fetch(this::toEntity);
    }
  }

  /**
   * Gets all waivers that will expire within the specified time window.
   * This is used by the waiver expiration webhook feature to detect waivers that are expiring soon
   * and send advance warning notifications.
   *
   * @param tx transaction context
   * @param fromTime start of the time window (inclusive) - typically now
   * @param toTime end of the time window (exclusive) - typically now + 7 days
   * @return list of waivers that will expire in the time window
   * @since 1.179.0
   */
  public List<PolicyWaiver> getUpcomingExpiringWaivers(TransactionContext tx, Date fromTime, Date toTime) {
    return tx.dsl()
        .selectFrom(POLICY_WAIVER)
        .where(POLICY_WAIVER.EXPIRY_TIME.ge(fromTime))
        .and(POLICY_WAIVER.EXPIRY_TIME.lt(toTime))
        .orderBy(POLICY_WAIVER.EXPIRY_TIME.asc())
        .fetch(this::toEntity);
  }

  public List<PolicyWaiver> getByPolicyIdAndOwnerIds(TransactionContext tx, String policyId, Set<String> ownerIds) {
    if (ownerIds == null || ownerIds.isEmpty()) {
      return java.util.Collections.emptyList();
    }
    return tx.dsl()
        .selectFrom(POLICY_WAIVER)
        .where(POLICY_WAIVER.POLICY_ID.eq(policyId))
        .and(POLICY_WAIVER.OWNER_ID.in(ownerIds))
        .fetch(this::toEntity);
  }

  public List<PolicyWaiver> getByIds(Set<String> waiverIds) {
    if (waiverIds == null || waiverIds.isEmpty()) {
      return List.of();
    }
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(POLICY_WAIVER)
          .where(POLICY_WAIVER.POLICY_WAIVER_ID.in(waiverIds))
          .fetch(this::toEntity);
    }
  }

  @Override
  public int insert(TransactionContext tx, PolicyWaiver entity) {
    setComponentMatchStrategyIfNeeded(entity);

    PolicyWaiver other = getActiveByHashAndPolicyIdAndOwnerIdAndConstraintFacts(tx, entity.getHash(),
        entity.getPolicyId(), entity.getOwnerId(), entity.getConstraintFacts(),
        entity.getAssociatedPackageUrl(), entity.getComponentMatchStrategy());
    if (other != null) {
      throw new BadRequestException("This policy waiver already exists.");
    }
    if (entity.getComment() != null && entity.getComment().length() > 1000) {
      throw new BadRequestException("Comment length must not exceed 1000 characters.");
    }

    if (entity.getCreateTime() == null) {
      entity.setCreateTime(new Date());
    }

    return super.insert(tx, entity);
  }

  private void setComponentMatchStrategyIfNeeded(PolicyWaiver entity) {
    if (entity.getComponentMatchStrategy() == null) {
      entity.setComponentMatchStrategy(entity.getHash() == null ? ALL_COMPONENTS : EXACT_COMPONENT);
    }
  }

  @Override
  public int update(TransactionContext tx, PolicyWaiver entity) {
    setComponentMatchStrategyIfNeeded(entity);

    PolicyWaiver other = getActiveByHashAndPolicyIdAndOwnerIdAndConstraintFacts(tx, entity.getHash(),
        entity.getPolicyId(), entity.getOwnerId(), entity.getConstraintFacts(),
        entity.getAssociatedPackageUrl(), entity.getComponentMatchStrategy());
    if (other != null && !other.getId().equals(entity.getId())) {
      throw new BadRequestException("A policy waiver for the same policy violation already exists.");
    }
    if (entity.getComment() != null && entity.getComment().length() > 1000) {
      throw new BadRequestException("Comment length must not exceed 1000 characters.");
    }

    return super.update(tx, entity);
  }

  /**
   * Updates a waiver without the duplicate-violation check. Used for renewal where the waiver already exists
   * and we are only extending its expiry time.
   */
  public void updateForRenewal(TransactionContext tx, PolicyWaiver entity) {
    setComponentMatchStrategyIfNeeded(entity);
    if (entity.getComment() != null && entity.getComment().length() > 1000) {
      throw new BadRequestException("Comment length must not exceed 1000 characters.");
    }
    super.update(tx, entity);
  }

  /**
   * This method should only be used when you want to perform update without any validations. Please be very careful
   * when using this method.
   *
   * @deprecated Use {@link #update(TransactionContext, PolicyWaiver)}
   */
  @Deprecated
  public void updateWithNoChecks(PolicyWaiver entity) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      super.update(tx, entity);
      tx.commit();
    }
  }

  public PolicyWaiver getByIdAndOwnerIdNotNull(String policyWaiverId, String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByIdAndOwnerIdNotNull(tx, policyWaiverId, ownerId);
    }
  }

  public Map<LocalDate, Long> getCountByOwnerIdAndDate(String ownerId, Date date) {
    try (TransactionContext tx = createTransactionContext()) {
      var pw = POLICY_WAIVER.as("pw");
      var createDate = DSL.cast(pw.CREATE_TIME, java.sql.Date.class);

      return tx.dsl()
          .select(createDate, DSL.count())
          .from(pw)
          .where(pw.OWNER_ID.eq(ownerId))
          .and(pw.CREATE_TIME.ge(date))
          .groupBy(createDate)
          .fetchStream()
          .collect(Collectors.toMap(
              r -> ((java.sql.Date) r.value1()).toLocalDate(),
              r -> ((Number) r.value2()).longValue()));
    }
  }

  private PolicyWaiver getByIdAndOwnerIdNotNull(TransactionContext tx, String policyWaiverId, String ownerId) {
    PolicyWaiver policyWaiver = getByIdAndOwnerId(tx, policyWaiverId, ownerId);
    if (policyWaiver == null) {
      String errorMessage = "Cannot find a waiver with ID " + policyWaiverId + " for owner " + ownerId + ".";
      throw new NotFoundException(errorMessage);
    }
    return policyWaiver;
  }

  private PolicyWaiver getByIdAndOwnerId(TransactionContext tx, String policyWaiverId, String ownerId) {
    return toEntity(tx.dsl()
        .selectFrom(POLICY_WAIVER)
        .where(POLICY_WAIVER.POLICY_WAIVER_ID.eq(policyWaiverId))
        .and(POLICY_WAIVER.OWNER_ID.eq(ownerId))
        .fetchOne());
  }

  PolicyWaiver getActiveByHashAndPolicyIdAndOwnerIdAndConstraintFacts(
      TransactionContext tx,
      String hash,
      String policyId,
      String ownerId,
      List<ConstraintFact> constraintFacts,
      String associatedPackageURL,
      ComponentMatcherStrategyForWaiver componentMatchStrategy)
  {
    // Build the base condition
    var baseCondition = (hash != null ? POLICY_WAIVER.HASH.eq(hash) : POLICY_WAIVER.HASH.isNull())
        .and(POLICY_WAIVER.POLICY_ID.eq(policyId))
        .and(POLICY_WAIVER.OWNER_ID.eq(ownerId))
        .and(POLICY_WAIVER.EXPIRY_TIME.isNull()
            .or(POLICY_WAIVER.EXPIRY_TIME.gt(new Date())));

    // Add strategy-specific conditions
    if (Objects.nonNull(associatedPackageURL) && Objects.nonNull(componentMatchStrategy)) {
      // Applies to exact or all versions waivers
      baseCondition = baseCondition.and(POLICY_WAIVER.COMPONENT_MATCH_STRATEGY.eq(componentMatchStrategy.name()));
    }
    else if (Objects.nonNull(componentMatchStrategy)) {
      // applies to all components waivers
      baseCondition = baseCondition
          .and(POLICY_WAIVER.ASSOCIATED_PACKAGE_URL.isNull())
          .and(POLICY_WAIVER.COMPONENT_MATCH_STRATEGY.eq(componentMatchStrategy.name()));
    }
    else {
      // default case for legacy waivers
      baseCondition = baseCondition
          .and(POLICY_WAIVER.ASSOCIATED_PACKAGE_URL.isNull())
          .and(POLICY_WAIVER.COMPONENT_MATCH_STRATEGY.isNull());
    }

    if (Objects.isNull(constraintFacts)) {
      // Should apply only to legacy waivers
      baseCondition = baseCondition.and(POLICY_WAIVER.CONSTRAINT_FACTS_JSON.isNull());
      return toEntity(tx.dsl()
          .selectFrom(POLICY_WAIVER)
          .where(baseCondition)
          .fetchOne());
    }

    List<PolicyWaiver> policyWaivers = tx.dsl()
        .selectFrom(POLICY_WAIVER)
        .where(baseCondition)
        .fetch(this::toEntity);

    Predicate<PolicyWaiver> waiverFilter = policyWaiver -> policyWaiver.getConstraintFacts() != null &&
        ConstraintFactsListComparator.CONSTRAINT_FACTS_LIST_COMPARATOR.compare(constraintFacts,
            policyWaiver.getConstraintFacts()) == 0;

    if (associatedPackageURL != null && ALL_VERSIONS.equals(componentMatchStrategy)) {
      final ComponentIdentifier wildcardVersionIdentifier =
          new PackageUrlIdentifier(associatedPackageURL)
              .toComponentIdentifier()
              .createAlternativeVersion("*");

      Predicate<PolicyWaiver> purlFilter = policyWaiver -> policyWaiver.getComponentIdentifier() != null &&
          policyWaiver
              .getComponentIdentifier()
              .createAlternativeVersion("*")
              .equals(wildcardVersionIdentifier);

      waiverFilter = purlFilter.and(waiverFilter);
    }
    return policyWaivers.stream()
        .filter(waiverFilter)
        .findFirst()
        .orElse(null);
  }

  public List<WaiverReasonData> getPolicyWaiverReasonMappings() {
    try (TransactionContext tx = createTransactionContext()) {
      var waiver = POLICY_WAIVER.as("waiver");
      var reason = POLICY_WAIVER_REASON.as("reason");

      return tx.dsl()
          .select(waiver.POLICY_WAIVER_ID, reason.REASON_TEXT)
          .from(waiver)
          .join(reason)
          .on(waiver.WAIVER_REASON_ID.eq(reason.WAIVER_REASON_ID))
          .where(waiver.WAIVER_REASON_ID.isNotNull())
          .orderBy(waiver.POLICY_WAIVER_ID)
          .fetch(r -> new WaiverReasonData(r.value1(), r.value2()));
    }
  }

  public List<PolicyWaiver> getAllForContainerImageByOwnerId(String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getAllForContainerImageByOwnerId(tx, ownerId);
    }
  }

  public List<PolicyWaiver> getAllForContainerImageByOwnerId(TransactionContext tx, String ownerId) {
    return tx.dsl()
        .selectFrom(POLICY_WAIVER)
        .where(POLICY_WAIVER.OWNER_ID.eq(ownerId))
        .and(POLICY_WAIVER.IS_FOR_CONTAINER_IMAGE.eq(true)
            .or(POLICY_WAIVER.IS_FOR_CONTAINER_IMAGE_COMPONENT.eq(true)))
        .fetch(this::toEntity);
  }

  public void deleteAllForContainerImage(String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      deleteAllForContainerImage(tx, ownerId);
      tx.commit();
    }
  }

  public void deleteAllForContainerImage(TransactionContext tx, String ownerId) {
    tx.dsl()
        .deleteFrom(POLICY_WAIVER)
        .where(POLICY_WAIVER.OWNER_ID.eq(ownerId))
        .and(POLICY_WAIVER.IS_FOR_CONTAINER_IMAGE.eq(true)
            .or(POLICY_WAIVER.IS_FOR_CONTAINER_IMAGE_COMPONENT.eq(true)))
        .execute();
  }

  public List<PolicyContainerWaiverData> getAllContainerPolicyWaivers(
      final int page,
      final int pageSize,
      final Set<String> accessibleOwnerIds)
  {
    if (accessibleOwnerIds != null && accessibleOwnerIds.isEmpty()) {
      return Collections.emptyList();
    }

    int offset = (page - 1) * pageSize;
    try (TransactionContext tx = createTransactionContext()) {
      var pw = POLICY_WAIVER.as("pw");
      var agg = buildContainerPolicyWaiversSubquery(tx).asTable("agg");

      var aggOwnerId = agg.field("owner_id", String.class);
      var aggMaxThreatLevel = agg.field("max_threat_level", Short.class);
      var aggApplicationScope = agg.field("application_scope", String.class);
      var aggUniquePolicyCount = agg.field("unique_policy_count", Long.class);
      var aggUniqueComponentCount = agg.field("unique_component_count", Long.class);

      var condition = pw.IS_FOR_CONTAINER_IMAGE.eq(true);
      if (accessibleOwnerIds != null) {
        condition = condition.and(pw.OWNER_ID.in(accessibleOwnerIds));
      }

      return tx.dsl()
          .select(
              pw.POLICY_WAIVER_ID,
              pw.CREATE_TIME,
              pw.EXPIRY_TIME,
              pw.OWNER_ID,
              aggMaxThreatLevel,
              aggApplicationScope,
              aggUniquePolicyCount,
              aggUniqueComponentCount)
          .from(pw)
          .leftJoin(agg)
          .on(aggOwnerId.eq(pw.OWNER_ID))
          .where(condition)
          .orderBy(
              aggMaxThreatLevel.desc().nullsLast(),
              pw.POLICY_WAIVER_ID)
          .limit(pageSize)
          .offset(offset)
          .fetch(r -> new PolicyContainerWaiverData(
              r.value1(),
              r.value2(),
              r.value3(),
              r.value4(),
              r.value5() != null ? r.value5().intValue() : 0,
              r.value6(),
              r.value7() != null ? r.value7() : 0L,
              r.value8() != null ? r.value8() : 0L));
    }
  }

  public List<PolicyContainerWaiverData> getAllContainerPolicyWaivers(final int page, final int pageSize) {
    return getAllContainerPolicyWaivers(page, pageSize, null);
  }

  /**
   * Counts active dashboard policy waivers scoped to the given owners (CLM-40927).
   * Excludes container-image-component waivers ({@code IS_FOR_CONTAINER_IMAGE_COMPONENT=true}),
   * matching the classic dashboard waiver tab. Container-image group waivers
   * ({@code IS_FOR_CONTAINER_IMAGE=true}) are included.
   * <p>
   * {@code accessibleOwnerIds} is never {@code null} on the dashboard metrics path;
   * {@code null} means unscoped (internal callers only).
   */
  public long selectCount(final Set<String> accessibleOwnerIds) {
    if (accessibleOwnerIds != null && accessibleOwnerIds.isEmpty()) {
      return 0L;
    }
    if (accessibleOwnerIds == null) {
      return selectCountInternal(null);
    }
    return getListWithSqlInClause(
        accessibleOwnerIds,
        chunk -> List.of(selectCountInternal(new HashSet<>(chunk))))
            .stream()
            .mapToLong(Long::longValue)
            .sum();
  }

  private long selectCountInternal(final Set<String> accessibleOwnerIds) {
    try (TransactionContext tx = createTransactionContext()) {
      var condition = POLICY_WAIVER.IS_FOR_CONTAINER_IMAGE_COMPONENT.eq(false)
          .and(POLICY_WAIVER.EXPIRY_TIME.isNull()
              .or(POLICY_WAIVER.EXPIRY_TIME.gt(new Date())));
      if (accessibleOwnerIds != null) {
        condition = condition.and(POLICY_WAIVER.OWNER_ID.in(accessibleOwnerIds));
      }
      return tx.dsl()
          .selectCount()
          .from(POLICY_WAIVER)
          .where(condition)
          .fetchOne(0, Long.class);
    }
  }

  public long getContainerPolicyWaiversCount(final Set<String> accessibleOwnerIds) {
    if (accessibleOwnerIds != null && accessibleOwnerIds.isEmpty()) {
      return 0L;
    }

    try (TransactionContext tx = createTransactionContext()) {
      var pw = POLICY_WAIVER.as("pw");
      var agg = buildContainerPolicyWaiversSubquery(tx).asTable("agg");
      var aggOwnerId = agg.field("owner_id", String.class);

      var condition = pw.IS_FOR_CONTAINER_IMAGE.eq(true);
      if (accessibleOwnerIds != null) {
        condition = condition.and(pw.OWNER_ID.in(accessibleOwnerIds));
      }

      return tx.dsl()
          .selectCount()
          .from(pw)
          .leftJoin(agg)
          .on(aggOwnerId.eq(pw.OWNER_ID))
          .where(condition)
          .fetchOne(0, Long.class);
    }
  }

  public long getContainerPolicyWaiversCount() {
    return getContainerPolicyWaiversCount(null);
  }

  private org.jooq.Select<?> buildContainerPolicyWaiversSubquery(final TransactionContext tx) {
    var pw2 = POLICY_WAIVER.as("pw2");
    var p2 = POLICY.as("p2");
    var a = APPLICATION.as("a");

    return tx.dsl()
        .select(
            pw2.OWNER_ID.as("owner_id"),
            DSL.max(p2.THREAT_LEVEL).as("max_threat_level"),
            DSL.countDistinct(pw2.POLICY_ID).as("unique_policy_count"),
            DSL.countDistinct(pw2.HASH).as("unique_component_count"),
            a.NAME.as("application_scope"))
        .from(pw2)
        .join(p2)
        .on(pw2.POLICY_ID.eq(p2.POLICY_ID))
        .join(a)
        .on(pw2.OWNER_ID.eq(a.APPLICATION_ID))
        .where(pw2.IS_FOR_CONTAINER_IMAGE_COMPONENT.eq(true))
        .groupBy(pw2.OWNER_ID, a.NAME);
  }

  public record WaiverReasonData(String policyWaiverId, String reasonText)
  {
  }

  public record PolicyContainerWaiverData(
      String policyWaiverId,
      Date createTime,
      Date expiryTime,
      String ownerId,
      int maxThreatLevel,
      String applicationScope,
      Long uniquePolicyCount,
      Long uniqueComponentCount)
  {
  }

  @Override
  public Table<?> getJooqTable() {
    return POLICY_WAIVER;
  }

  @Override
  public Class<PolicyWaiver> getEntityClass() {
    return PolicyWaiver.class;
  }
}
