/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

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
      return appliesToAllVersionsOfSomeComponent.stream().filter(keepOnlyMatchingWildcardPurl)
          .collect(Collectors.toList());
    }
    return Collections.emptyList();
  }

  public List<PolicyWaiver> getApplicableAndExpiredByOwnerId(String ownerId) {
    List<PolicyWaiver> policyWaivers = new ArrayList<>();

    loadAllByOwnerId(policyWaivers, ownerId);

    return policyWaivers;
  }

  private void loadAllByOwnerId(List<PolicyWaiver> policyWaivers, String ownerId) {
    if (ownerId == null) {
      return;
    }

    Owner owner = ownerDAO.getById(ownerId);
    loadAllByOwnerId(policyWaivers, owner.getParentOwnerId());
    policyWaivers.addAll(getByOwnerId(ownerId));
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
    String sQuery = "SELECT entity FROM PolicyWaiver entity" + //
        " WHERE entity.ownerId=?1 AND entity.hash=?2" + //
        " AND (entity.expiryTime is null OR entity.expiryTime > CURRENT_TIMESTAMP)" + //
        " AND entity.componentMatchStrategy=?3";
    return getList(tx, sQuery, ownerId, hash, matcherStrategy);
  }

  public List<PolicyWaiver> getByOwnerIdAndHash(TransactionContext tx, String ownerId, String hash) {
    String sQuery = "SELECT entity FROM PolicyWaiver entity" + //
        " WHERE entity.ownerId=?1 AND entity.hash=?2";
    return getList(tx, sQuery, ownerId, hash);
  }

  public List<PolicyWaiver> getByOwnerId(TransactionContext tx, String ownerId) {
    String sQuery = "SELECT entity FROM PolicyWaiver entity" + //
        " WHERE entity.ownerId=?1";
    return getList(tx, sQuery, ownerId);
  }

  public List<PolicyWaiver> getActiveByOwnerId(TransactionContext tx, String ownerId) {
    String sQuery = "SELECT entity FROM PolicyWaiver entity" + //
        " WHERE entity.ownerId=?1 AND (entity.expiryTime is null OR entity.expiryTime > CURRENT_TIMESTAMP)";
    return getList(tx, sQuery, ownerId);
  }

  public List<PolicyWaiver> getByPolicyId(String policyId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByPolicyId(tx, policyId);
    }
  }

  public List<PolicyWaiver> getByPolicyId(TransactionContext tx, String policyId) {
    String sQuery = "SELECT entity FROM PolicyWaiver entity" + //
        " WHERE entity.policyId=?1";
    return getList(tx, sQuery, policyId);
  }

  public List<PolicyWaiver> getActiveByPolicyId(String policyId) {
    String sQuery = "SELECT entity FROM PolicyWaiver entity" + //
        " WHERE entity.policyId=?1 AND (entity.expiryTime is null OR entity.expiryTime > CURRENT_TIMESTAMP)";
    return getList(sQuery, policyId);
  }

  public List<PolicyWaiver> getByPolicyIdAndOwnerIds(TransactionContext tx, String policyId, Set<String> ownerIds) {
    String sQuery = "SELECT entity FROM PolicyWaiver entity" + //
        " WHERE entity.policyId=?1 AND entity.ownerId IN (?2)";
    return getList(tx, sQuery, policyId, ownerIds);
  }

  @Override
  public void insert(TransactionContext tx, PolicyWaiver entity) {
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

    super.insert(tx, entity);
  }

  private void setComponentMatchStrategyIfNeeded(PolicyWaiver entity) {
    if (entity.getComponentMatchStrategy() == null) {
      entity.setComponentMatchStrategy(entity.getHash() == null ? ALL_COMPONENTS : EXACT_COMPONENT);
    }
  }

  @Override
  public void update(TransactionContext tx, PolicyWaiver entity) {
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

  @SuppressWarnings("unchecked")
  public Map<LocalDate, Long> getCountByOwnerIdAndDate(String ownerId, Date date) {
    String sQuery = "SELECT CAST(pw.create_time AS DATE), COUNT(1)" + //
        " FROM " + getDatabaseSchema() + ".policy_waiver pw" + //
        " WHERE pw.owner_id = ?1" + //
        " AND pw.create_time >= ?2" + //
        " GROUP BY CAST(pw.create_time AS DATE)";

    try (TransactionContext tx = createTransactionContext()) {
      jakarta.persistence.Query query = tx.createNativeQuery(sQuery);
      query.setParameter(1, ownerId);
      query.setParameter(2, date);

      Stream<Object[]> result = query.getResultStream();
      return result
          .collect(Collectors.toMap(array -> ((java.sql.Date) array[0]).toLocalDate(), array -> (Long) array[1]));
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
    String sQuery = "SELECT waiver FROM PolicyWaiver waiver WHERE waiver.id=?1 AND waiver.ownerId=?2";
    return get(tx, sQuery, policyWaiverId, ownerId);
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

    String sQuery = getActivePolicyWaiverByHashAndPolicyIdAndOwnerIdAndConstraintFactsQuery(associatedPackageURL,
        componentMatchStrategy, constraintFacts);

    if (Objects.isNull(constraintFacts)) {
      Function<String, PolicyWaiver> getFunction =
          getPolicyWaiverFunction(tx, hash, policyId, ownerId, componentMatchStrategy);

      return getFunction.apply(sQuery);
    }

    Function<String, List<PolicyWaiver>> getListFunction =
        getPolicyWaiverListFunction(tx, hash, policyId, ownerId, componentMatchStrategy);
    List<PolicyWaiver> policyWaivers = getListFunction.apply(sQuery);

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

  private String getActivePolicyWaiverByHashAndPolicyIdAndOwnerIdAndConstraintFactsQuery(
      String associatedPackageURL,
      ComponentMatcherStrategyForWaiver componentMatchStrategy,
      List<ConstraintFact> constraintFacts)
  {
    String query = "SELECT entity FROM PolicyWaiver entity" + //
        " WHERE entity.hash=?1 AND entity.policyId=?2 AND entity.ownerId=?3" + //
        " AND (entity.expiryTime is null OR entity.expiryTime > CURRENT_TIMESTAMP)";

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

  private Function<String, PolicyWaiver> getPolicyWaiverFunction(
      TransactionContext tx,
      String hash,
      String policyId,
      String ownerId,
      ComponentMatcherStrategyForWaiver componentMatchStrategy)
  {
    if (Objects.nonNull(componentMatchStrategy)) {
      return (String query) -> get(tx, query, hash, policyId, ownerId, componentMatchStrategy);
    }
    // default case for legacy waivers
    return (String query) -> get(tx, query, hash, policyId, ownerId);
  }

  private Function<String, List<PolicyWaiver>> getPolicyWaiverListFunction(
      TransactionContext tx,
      String hash,
      String policyId,
      String ownerId,
      ComponentMatcherStrategyForWaiver componentMatchStrategy)
  {
    if (Objects.nonNull(componentMatchStrategy)) {
      return (String query) -> getList(tx, query, hash, policyId, ownerId, componentMatchStrategy);
    }
    // default case for legacy waivers
    return (String query) -> getList(tx, query, hash, policyId, ownerId);
  }
}
