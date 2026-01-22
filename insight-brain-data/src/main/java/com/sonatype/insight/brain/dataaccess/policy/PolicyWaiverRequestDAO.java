/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

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
  public void insert(TransactionContext tx, PolicyWaiverRequest entity) {
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

    super.insert(tx, entity);
  }

  private void setComponentMatchStrategyIfNeeded(PolicyWaiverRequest entity) {
    if (entity.getComponentMatchStrategy() == null) {
      entity.setComponentMatchStrategy(entity.getHash() == null ? ALL_COMPONENTS : EXACT_COMPONENT);
    }
  }

  @Override
  public void update(TransactionContext tx, PolicyWaiverRequest entity) {
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

    super.update(tx, entity);
  }

  @Override
  public final void delete(PolicyWaiverRequest entity) {
    // WARNING: Don't add any business logic to this method because, for performance reasons,
    // we bypass this method when deleting policy waiver requests by foreign key cascaded delete.
    super.delete(entity);
  }

  @Override
  public final void delete(TransactionContext tx, PolicyWaiverRequest entity) {
    // WARNING: Don't add any business logic to this method because, for performance reasons,
    // we bypass this method when deleting policy waiver requests by foreign key cascaded delete.
    super.delete(tx, entity);
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
    String sQuery = getActiveByHashAndPolicyIdAndOwnerIdAndConstraintFactsQuery(associatedPackageURL,
        componentMatchStrategy, constraintFacts);

    if (Objects.isNull(constraintFacts)) {
      Function<String, PolicyWaiverRequest> getFunction =
          getPolicyWaiverRequestFunction(tx, hash, policyId, ownerId, componentMatchStrategy);

      return getFunction.apply(sQuery);
    }

    Function<String, List<PolicyWaiverRequest>> getListFunction =
        getPolicyWaiverRequestListFunction(tx, hash, policyId, ownerId, componentMatchStrategy);
    List<PolicyWaiverRequest> policyWaiverRequests = getListFunction.apply(sQuery);

    Predicate<PolicyWaiverRequest> filter = policyWaiverRequest -> policyWaiverRequest.getConstraintFacts() != null
        && ConstraintFactsListComparator.CONSTRAINT_FACTS_LIST_COMPARATOR.compare(constraintFacts,
            policyWaiverRequest.getConstraintFacts()) == 0;

    if (associatedPackageURL != null && ALL_VERSIONS.equals(componentMatchStrategy)) {
      final ComponentIdentifier wildcardVersionIdentifier =
          new PackageUrlIdentifier(associatedPackageURL).toComponentIdentifier().createAlternativeVersion("*");

      Predicate<PolicyWaiverRequest> purlFilter =
          policyWaiverRequest -> policyWaiverRequest.getComponentIdentifier() != null && policyWaiverRequest
              .getComponentIdentifier().createAlternativeVersion("*").equals(wildcardVersionIdentifier);

      filter = purlFilter.and(filter);
    }
    return policyWaiverRequests.stream().filter(filter).findFirst().orElse(null);
  }

  private String getActiveByHashAndPolicyIdAndOwnerIdAndConstraintFactsQuery(
      String associatedPackageURL,
      ComponentMatcherStrategyForWaiver componentMatchStrategy,
      List<ConstraintFact> constraintFacts)
  {
    String query = """
        SELECT entity FROM PolicyWaiverRequest entity
        WHERE entity.hash=?1 AND entity.policyId=?2 AND entity.ownerId=?3
        AND (entity.expiryTime is null OR entity.expiryTime > CURRENT_TIMESTAMP)""";

    if (Objects.nonNull(associatedPackageURL) && Objects.nonNull(componentMatchStrategy)) {
      // Applies to exact or all versions waivers
      query += " AND entity.componentMatchStrategy=?4";
    }
    else if (Objects.nonNull(componentMatchStrategy)) {
      // applies to all components waivers
      query += " AND entity.associatedPackageUrl IS NULL AND entity.componentMatchStrategy=?4";
    }
    else {
      // default case for legacy waivers
      query += " AND entity.associatedPackageUrl IS NULL AND entity.componentMatchStrategy IS NULL";
    }

    if (Objects.isNull(constraintFacts)) {
      // Should apply only to legacy waivers
      query += " AND entity.constraintFactsJson IS NULL";
    }
    return query;
  }

  private Function<String, PolicyWaiverRequest> getPolicyWaiverRequestFunction(
      TransactionContext tx,
      String hash,
      String policyId,
      String ownerId,
      ComponentMatcherStrategyForWaiver componentMatchStrategy)
  {
    return (String query) -> get(tx, query, hash, policyId, ownerId, componentMatchStrategy);
  }

  private Function<String, List<PolicyWaiverRequest>> getPolicyWaiverRequestListFunction(
      TransactionContext tx,
      String hash,
      String policyId,
      String ownerId,
      ComponentMatcherStrategyForWaiver componentMatchStrategy)
  {
    return (String query) -> getList(tx, query, hash, policyId, ownerId, componentMatchStrategy);
  }

  public List<PolicyWaiverRequest> getByPolicyId(String policyId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByPolicyId(tx, policyId);
    }
  }

  public List<PolicyWaiverRequest> getByPolicyId(TransactionContext tx, String policyId) {
    String sQuery = """
        SELECT entity FROM PolicyWaiverRequest entity
        WHERE entity.policyId=?1""";
    return getList(tx, sQuery, policyId);
  }

  public List<PolicyWaiverRequest> getByOwnerId(String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerId(tx, ownerId);
    }
  }

  public List<PolicyWaiverRequest> getByOwnerId(TransactionContext tx, String ownerId) {
    String sQuery = """
        SELECT entity FROM PolicyWaiverRequest entity
        WHERE entity.ownerId=?1""";
    return getList(tx, sQuery, ownerId);
  }

  public List<PolicyWaiverRequest> getActiveByPolicyId(String policyId) {
    String sQuery = """
        SELECT entity FROM PolicyWaiverRequest entity
        WHERE entity.policyId=?1
        AND (entity.expiryTime is null OR entity.expiryTime > CURRENT_TIMESTAMP)
        """;
    return getList(sQuery, policyId);
  }

  public PolicyWaiverRequest getByIdAndOwnerIdNotNull(String policyWaiverRequestId, String ownerId) {
    String sQuery = """
        SELECT entity FROM PolicyWaiverRequest entity
        WHERE entity.id=?1
        AND entity.ownerId=?2""";
    PolicyWaiverRequest policyWaiverRequest = get(sQuery, policyWaiverRequestId, ownerId);
    if (policyWaiverRequest == null) {
      String errorMessage =
          "Cannot find a policy waiver request with ID " + policyWaiverRequestId + " for owner " + ownerId + ".";
      throw new NotFoundException(errorMessage);
    }
    return policyWaiverRequest;
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
    String sQuery = """
        SELECT entity FROM PolicyWaiverRequest entity
        WHERE entity.ownerId=?1 AND entity.policyId=?2""";
    return getList(sQuery, ownerId, policyId);
  }
}
