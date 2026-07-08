/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequest;
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequestStatus;
import com.sonatype.insight.brain.policy.comparison.ConstraintFactsListComparator;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.PolicyWaiverRequest.POLICY_WAIVER_REQUEST;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.ALL_COMPONENTS;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.ALL_VERSIONS;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.EXACT_COMPONENT;

@Named
@Singleton
public class PolicyWaiverRequestDAO
    extends AbstractOperationalSqlDAO<PolicyWaiverRequest>
{
  private final OwnerDAO ownerDAO;

  @Inject
  public PolicyWaiverRequestDAO(OperationalDataStore operationalDataStore, OwnerDAO ownerDAO) {
    super(operationalDataStore);
    this.ownerDAO = ownerDAO;
  }

  @Override
  public int insert(TransactionContext tx, PolicyWaiverRequest entity) {
    if (entity.getStatus() == null) {
      entity.setStatus(PolicyWaiverRequestStatus.REQUESTED);
    }

    setComponentMatchStrategyIfNeeded(entity);

    PolicyWaiverRequest other = getActiveByHashAndPolicyIdAndOwnerIdAndConstraintFacts(tx, entity.getHash(),
        entity.getPolicyId(), entity.getOwnerId(), entity.getConstraintFacts(), entity.getAssociatedPackageUrl(),
        entity.getComponentMatchStrategy());
    if (other != null) {
      throw new BadRequestException("This policy waiver request already exists.");
    }
    if (entity.getComment() != null && entity.getComment().length() > 1000) {
      throw new BadRequestException("Comment length must not exceed 1000 characters.");
    }

    if (entity.getRequestTime() == null) {
      entity.setRequestTime(new Date());
    }

    return super.insert(tx, entity);
  }

  private void setComponentMatchStrategyIfNeeded(PolicyWaiverRequest entity) {
    if (entity.getComponentMatchStrategy() == null) {
      entity.setComponentMatchStrategy(entity.getHash() == null ? ALL_COMPONENTS : EXACT_COMPONENT);
    }
  }

  @Override
  public int update(TransactionContext tx, PolicyWaiverRequest entity) {
    if (entity.getStatus() == null) {
      throw new BadRequestException("Cannot create a policy waiver request with null status.");
    }
    PolicyWaiverRequest previous = getById(tx, entity.getId());
    if (previous == null) {
      throw new BadRequestException("Cannot find a policy waiver request with ID " + entity.getId() + ".");
    }
    if (PolicyWaiverRequestStatus.APPROVED.equals(previous.getStatus())) {
      throw new BadRequestException("Cannot update an approved policy waiver request.");
    }

    setComponentMatchStrategyIfNeeded(entity);

    PolicyWaiverRequest other = getActiveByHashAndPolicyIdAndOwnerIdAndConstraintFacts(tx, entity.getHash(),
        entity.getPolicyId(), entity.getOwnerId(), entity.getConstraintFacts(), entity.getAssociatedPackageUrl(),
        entity.getComponentMatchStrategy());
    if (other != null && !other.getId().equals(entity.getId())) {
      throw new BadRequestException("A policy waiver request for the same policy violation already exists.");
    }
    if (entity.getComment() != null && entity.getComment().length() > 1000) {
      throw new BadRequestException("Comment length must not exceed 1000 characters.");
    }

    return super.update(tx, entity);
  }

  @Override
  public Table<?> getJooqTable() {
    return POLICY_WAIVER_REQUEST;
  }

  private PolicyWaiverRequest getActiveByHashAndPolicyIdAndOwnerIdAndConstraintFacts(
      TransactionContext tx,
      String hash,
      String policyId,
      String ownerId,
      List<ConstraintFact> constraintFacts,
      String associatedPackageURL,
      ComponentMatcherStrategyForWaiver componentMatchStrategy)
  {
    if (Objects.isNull(constraintFacts)) {
      return getPolicyWaiverRequestWithNullConstraintFacts(tx, hash, policyId, ownerId, componentMatchStrategy);
    }

    List<PolicyWaiverRequest> policyWaiverRequests =
        getPolicyWaiverRequestList(tx, hash, policyId, ownerId, componentMatchStrategy);

    Predicate<PolicyWaiverRequest> filter = policyWaiverRequest -> policyWaiverRequest.getConstraintFacts() != null
        && ConstraintFactsListComparator.CONSTRAINT_FACTS_LIST_COMPARATOR.compare(constraintFacts,
            policyWaiverRequest.getConstraintFacts()) == 0;

    if (associatedPackageURL != null && ALL_VERSIONS.equals(componentMatchStrategy)) {
      final ComponentIdentifier wildcardVersionIdentifier =
          new PackageUrlIdentifier(associatedPackageURL).toComponentIdentifier().createAlternativeVersion("*");

      Predicate<PolicyWaiverRequest> purlFilter =
          policyWaiverRequest -> policyWaiverRequest.getComponentIdentifier() != null && policyWaiverRequest
              .getComponentIdentifier()
              .createAlternativeVersion("*")
              .equals(wildcardVersionIdentifier);

      filter = purlFilter.and(filter);
    }
    return policyWaiverRequests.stream().filter(filter).findFirst().orElse(null);
  }

  private PolicyWaiverRequest getPolicyWaiverRequestWithNullConstraintFacts(
      TransactionContext tx,
      String hash,
      String policyId,
      String ownerId,
      ComponentMatcherStrategyForWaiver componentMatchStrategy)
  {
    var condition = (hash != null ? POLICY_WAIVER_REQUEST.HASH.eq(hash) : POLICY_WAIVER_REQUEST.HASH.isNull())
        .and(POLICY_WAIVER_REQUEST.POLICY_ID.eq(policyId))
        .and(POLICY_WAIVER_REQUEST.OWNER_ID.eq(ownerId))
        .and(POLICY_WAIVER_REQUEST.STATUS.notEqual(PolicyWaiverRequestStatus.REJECTED.name()))
        .and(POLICY_WAIVER_REQUEST.EXPIRY_TIME.isNull()
            .or(POLICY_WAIVER_REQUEST.EXPIRY_TIME.greaterThan(new Date())));

    if (componentMatchStrategy != null) {
      condition = condition.and(POLICY_WAIVER_REQUEST.COMPONENT_MATCH_STRATEGY.eq(componentMatchStrategy.name()));
    }

    return toEntity(tx.dsl()
        .selectFrom(POLICY_WAIVER_REQUEST)
        .where(condition)
        .and(POLICY_WAIVER_REQUEST.CONSTRAINT_FACTS_JSON.isNull())
        .fetchOne());
  }

  private List<PolicyWaiverRequest> getPolicyWaiverRequestList(
      TransactionContext tx,
      String hash,
      String policyId,
      String ownerId,
      ComponentMatcherStrategyForWaiver componentMatchStrategy)
  {
    var condition = (hash != null ? POLICY_WAIVER_REQUEST.HASH.eq(hash) : POLICY_WAIVER_REQUEST.HASH.isNull())
        .and(POLICY_WAIVER_REQUEST.POLICY_ID.eq(policyId))
        .and(POLICY_WAIVER_REQUEST.OWNER_ID.eq(ownerId))
        .and(POLICY_WAIVER_REQUEST.STATUS.notEqual(PolicyWaiverRequestStatus.REJECTED.name()))
        .and(POLICY_WAIVER_REQUEST.EXPIRY_TIME.isNull()
            .or(POLICY_WAIVER_REQUEST.EXPIRY_TIME.greaterThan(new Date())));

    if (componentMatchStrategy != null) {
      condition = condition.and(POLICY_WAIVER_REQUEST.COMPONENT_MATCH_STRATEGY.eq(componentMatchStrategy.name()));
    }

    return tx.dsl()
        .selectFrom(POLICY_WAIVER_REQUEST)
        .where(condition)
        .fetch(this::toEntity);
  }

  public List<PolicyWaiverRequest> getByPolicyId(String policyId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByPolicyId(tx, policyId);
    }
  }

  public List<PolicyWaiverRequest> getByPolicyId(TransactionContext tx, String policyId) {
    return tx.dsl()
        .selectFrom(POLICY_WAIVER_REQUEST)
        .where(POLICY_WAIVER_REQUEST.POLICY_ID.eq(policyId))
        .fetch(this::toEntity);
  }

  public List<PolicyWaiverRequest> getByOwnerId(String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerId(tx, ownerId);
    }
  }

  public List<PolicyWaiverRequest> getByOwnerId(TransactionContext tx, String ownerId) {
    return tx.dsl()
        .selectFrom(POLICY_WAIVER_REQUEST)
        .where(POLICY_WAIVER_REQUEST.OWNER_ID.eq(ownerId))
        .fetch(this::toEntity);
  }

  /** Batch-fetch waiver requests for many owners in a single chunked IN-clause query. */
  public List<PolicyWaiverRequest> getByOwnerIds(Collection<String> ownerIds) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerIds(tx, ownerIds);
    }
  }

  public List<PolicyWaiverRequest> getByOwnerIds(TransactionContext tx, Collection<String> ownerIds) {
    return getListWithSqlInClause(ownerIds,
        idChunk -> tx.dsl()
            .selectFrom(POLICY_WAIVER_REQUEST)
            .where(POLICY_WAIVER_REQUEST.OWNER_ID.in(idChunk))
            .fetch(this::toEntity));
  }

  public List<PolicyWaiverRequest> getActiveByPolicyId(String policyId) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(POLICY_WAIVER_REQUEST)
          .where(POLICY_WAIVER_REQUEST.POLICY_ID.eq(policyId))
          .and(POLICY_WAIVER_REQUEST.EXPIRY_TIME.isNull()
              .or(POLICY_WAIVER_REQUEST.EXPIRY_TIME.greaterThan(new Date())))
          .fetch(this::toEntity);
    }
  }

  /**
   * Counts pending (REQUESTED) active waiver requests scoped to the given owners (CLM-40927).
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
      var condition = POLICY_WAIVER_REQUEST.STATUS.eq(PolicyWaiverRequestStatus.REQUESTED.name())
          .and(POLICY_WAIVER_REQUEST.EXPIRY_TIME.isNull()
              .or(POLICY_WAIVER_REQUEST.EXPIRY_TIME.greaterThan(new Date())));
      if (accessibleOwnerIds != null) {
        condition = condition.and(POLICY_WAIVER_REQUEST.OWNER_ID.in(accessibleOwnerIds));
      }
      return tx.dsl()
          .selectCount()
          .from(POLICY_WAIVER_REQUEST)
          .where(condition)
          .fetchOne(0, Long.class);
    }
  }

  /**
   * Atomically delete a policy waiver request only if its current status matches the expected value.
   *
   * <p>
   * The current row is re-read under {@code SELECT … FOR UPDATE} to close the TOCTOU window
   * between the caller's status check and the delete: if a concurrent reviewer transitions the
   * request to APPROVED or REJECTED, this method returns {@code false} without deleting. Without
   * this guard, a concurrent approve + withdraw could leave a {@code PolicyWaiver} pointing at a
   * deleted {@code PolicyWaiverRequest}, breaking the historical trail.
   *
   * @return {@code true} if the row was deleted, {@code false} if it does not exist or its status
   *         did not match {@code expected}
   */
  public boolean deleteIfStatusEquals(String policyWaiverRequestId, PolicyWaiverRequestStatus expected) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      PolicyWaiverRequest current = toEntity(tx.dsl()
          .selectFrom(POLICY_WAIVER_REQUEST)
          .where(POLICY_WAIVER_REQUEST.POLICY_WAIVER_REQUEST_ID.eq(policyWaiverRequestId))
          .forUpdate()
          .fetchOne());
      if (current == null || !expected.equals(current.getStatus())) {
        tx.commit();
        return false;
      }
      delete(tx, current);
      tx.commit();
      return true;
    }
  }

  public PolicyWaiverRequest getByIdAndOwnerIdNotNull(String policyWaiverRequestId, String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      PolicyWaiverRequest policyWaiverRequest = toEntity(tx.dsl()
          .selectFrom(POLICY_WAIVER_REQUEST)
          .where(POLICY_WAIVER_REQUEST.POLICY_WAIVER_REQUEST_ID.eq(policyWaiverRequestId))
          .and(POLICY_WAIVER_REQUEST.OWNER_ID.eq(ownerId))
          .fetchOne());

      if (policyWaiverRequest == null) {
        String errorMessage =
            "Cannot find a policy waiver request with ID " + policyWaiverRequestId + " for owner " + ownerId + ".";
        throw new NotFoundException(errorMessage);
      }
      return policyWaiverRequest;
    }
  }

  public List<PolicyWaiverRequest> getByOwnerHierarchyAndPolicyId(Owner owner, String policyId) {
    List<PolicyWaiverRequest> policyWaiverRequests = new ArrayList<>();

    loadByOwnerAndPolicyId(policyWaiverRequests, owner, policyId);

    return policyWaiverRequests;
  }

  private void loadByOwnerAndPolicyId(
      List<PolicyWaiverRequest> policyWaiverRequests,
      Owner owner,
      String policyId)
  {
    if (owner == null) {
      return;
    }

    Owner parentOwner = ownerDAO.getById(owner.getParentOwnerId());
    loadByOwnerAndPolicyId(policyWaiverRequests, parentOwner, policyId);
    policyWaiverRequests.addAll(getByOwnerIdAndPolicyId(owner.getId(), policyId));
  }

  private List<PolicyWaiverRequest> getByOwnerIdAndPolicyId(String ownerId, String policyId) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(POLICY_WAIVER_REQUEST)
          .where(POLICY_WAIVER_REQUEST.OWNER_ID.eq(ownerId))
          .and(POLICY_WAIVER_REQUEST.POLICY_ID.eq(policyId))
          .fetch(this::toEntity);
    }
  }

  @Override
  public Class<PolicyWaiverRequest> getEntityClass() {
    return PolicyWaiverRequest.class;
  }
}
