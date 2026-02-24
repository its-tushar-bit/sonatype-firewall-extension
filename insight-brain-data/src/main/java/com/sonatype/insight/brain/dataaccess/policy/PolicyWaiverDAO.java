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
import java.util.stream.IntStream;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

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

  /**
   * Gets all expired policy waivers that target the specified component hash in the context of the given
   * app/org and a packageURL. Note that a component can be subject to a waiver that refers to its specific hash,
   * to all versions of this component or to a waiver that applies to the entire app/org.
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
   * Gets all expired policy waivers that target the specified component hash in the context of the given
   * app/org and a packageURL. Note that a component can be subject to a waiver that refers to its specific hash,
   * to all versions of this component or to a waiver that applies to the entire app/org.
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
    String sQuery = """
        SELECT entity FROM PolicyWaiver entity
        WHERE entity.ownerId=?1 AND entity.policyId=?2 AND entity.isForContainerImage=?3""";
    return getList(sQuery, ownerId, policyId, false);
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
    String sQuery = """
        SELECT entity FROM PolicyWaiver entity
        WHERE entity.ownerId=?1 AND entity.hash=?2
        AND (entity.expiryTime is null OR entity.expiryTime > CURRENT_TIMESTAMP)
        AND entity.componentMatchStrategy=?3""";

    return getList(tx, sQuery, ownerId, hash, matcherStrategy);
  }

  private List<PolicyWaiver> getExpiredByOwnerIdAndHash(
      TransactionContext tx,
      String ownerId,
      String hash,
      ComponentMatcherStrategyForWaiver matcherStrategy)
  {
    String sQuery = """
        SELECT entity FROM PolicyWaiver entity
        WHERE entity.ownerId=?1 AND entity.hash=?2
        AND entity.expiryTime < CURRENT_TIMESTAMP
        AND entity.componentMatchStrategy=?3""";

    return getList(tx, sQuery, ownerId, hash, matcherStrategy);
  }

  public List<PolicyWaiver> getByOwnerIdAndHash(TransactionContext tx, String ownerId, String hash) {
    String sQuery = """
        SELECT entity FROM PolicyWaiver entity
        WHERE entity.ownerId=?1 AND entity.hash=?2""";
    return getList(tx, sQuery, ownerId, hash);
  }

  public List<PolicyWaiver> getByOwnerId(TransactionContext tx, String ownerId) {
    String sQuery = """
        SELECT entity FROM PolicyWaiver entity
        WHERE entity.ownerId=?1""";
    return getList(tx, sQuery, ownerId);
  }

  public List<PolicyWaiver> getActiveByOwnerId(TransactionContext tx, String ownerId) {
    String sQuery = """
        SELECT entity FROM PolicyWaiver entity
        WHERE entity.ownerId=?1
        AND (entity.expiryTime is null OR entity.expiryTime > CURRENT_TIMESTAMP)
        """;

    return getList(tx, sQuery, ownerId);
  }

  public List<PolicyWaiver> getByPolicyId(String policyId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByPolicyId(tx, policyId);
    }
  }

  public List<PolicyWaiver> getByPolicyId(TransactionContext tx, String policyId) {
    String sQuery = """
        SELECT entity FROM PolicyWaiver entity
        WHERE entity.policyId=?1""";
    return getList(tx, sQuery, policyId);
  }

  public List<PolicyWaiver> getActiveByPolicyId(String policyId) {
    String sQuery = """
        SELECT entity FROM PolicyWaiver entity
        WHERE entity.policyId=?1
        AND (entity.expiryTime is null OR entity.expiryTime > CURRENT_TIMESTAMP)
        """;
    return getList(sQuery, policyId);
  }

  public List<PolicyWaiver> getByPolicyIdAndOwnerIds(TransactionContext tx, String policyId, Set<String> ownerIds) {
    String sQuery = """
        SELECT entity FROM PolicyWaiver entity
        WHERE entity.policyId=?1 AND entity.ownerId IN (?2)""";
    return getList(tx, sQuery, policyId, ownerIds);
  }

  public List<PolicyWaiver> getByIds(Set<String> waiverIds) {
    if (waiverIds == null || waiverIds.isEmpty()) {
      return List.of();
    }
    String sQuery = """
        SELECT entity FROM PolicyWaiver entity
        WHERE entity.id IN (?1)""";
    return getListWithSqlInClause(waiverIds, ids -> getList(sQuery, ids));
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
    String sQuery = """
        SELECT entity FROM PolicyWaiver entity
        WHERE entity.id=?1
        AND entity.ownerId=?2""";
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
    String query = """
        SELECT entity FROM PolicyWaiver entity
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

  public List<WaiverReasonData> getPolicyWaiverReasonMappings() {
    try (TransactionContext tx = createTransactionContext()) {
      String sQuery = String.format("""
        SELECT waiver.policy_waiver_id AS policyWaiverId, reason.reason_text AS reasonText
        FROM %1$s.policy_waiver waiver
        JOIN %1$s.policy_waiver_reason reason ON waiver.waiver_reason_id = reason.waiver_reason_id
        WHERE waiver.waiver_reason_id IS NOT NULL
        ORDER BY waiver.policy_waiver_id
          """, getDatabaseSchema());

      jakarta.persistence.Query query = tx.createNativeQuery(sQuery);

      return ((Stream<Object[]>) query.getResultStream())
          .map(array -> new WaiverReasonData((String) array[0], (String) array[1]))
          .toList();
    }
  }

  public List<PolicyWaiver> getAllForContainerImageByOwnerId(String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getAllForContainerImageByOwnerId(tx, ownerId);
    }
  }

  public List<PolicyWaiver> getAllForContainerImageByOwnerId(TransactionContext tx, String ownerId) {
    String sQuery = """
        SELECT entity FROM PolicyWaiver entity
        WHERE entity.ownerId=?1
        AND (entity.isForContainerImage = TRUE OR entity.isForContainerImageComponent = TRUE)
        """;
    return getList(tx, sQuery, ownerId);
  }

  public void deleteAllForContainerImage(String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      deleteAllForContainerImage(tx, ownerId);
      tx.commit();
    }
  }

  public void deleteAllForContainerImage(TransactionContext tx, String ownerId) {
    String sQuery = """
        DELETE FROM PolicyWaiver entity
        WHERE entity.ownerId=?1
        AND (entity.isForContainerImage = TRUE OR entity.isForContainerImageComponent = TRUE)
        """;

    createQuery(sQuery, ownerId).executeUpdate(tx);
  }

  /**
   * Get all container policy waivers filtered by accessible owner IDs.
   *
   * @param page Page number (1-based)
   * @param pageSize Number of items per page
   * @param accessibleOwnerIds Set of owner IDs user has access to (null means no filtering)
   * @return List of container waivers
   */
  public List<PolicyContainerWaiverData> getAllContainerPolicyWaivers(
      int page,
      int pageSize,
      Set<String> accessibleOwnerIds)
  {
    if (accessibleOwnerIds != null && accessibleOwnerIds.isEmpty()) {
      return Collections.emptyList();
    }

    String subquery = getContainerPolicyWaiversSubquery();
    String whereClause;

    if (accessibleOwnerIds == null) {
      whereClause = "WHERE pw.is_for_container_image = true";
    }
    else {
      String placeholders = String.join(", ",
          IntStream.rangeClosed(1, accessibleOwnerIds.size())
              .mapToObj(i -> "?" + i)
              .toList());
      whereClause = "WHERE pw.is_for_container_image = true" +
          " AND pw.owner_id IN (" + placeholders + ")";
    }

    String sQuery = String.format("""
        SELECT
          pw.policy_waiver_id,
          pw.create_time,
          pw.expiry_time,
          pw.owner_id,
          agg.max_threat_level,
          agg.application_scope,
          agg.unique_policy_count,
          agg.unique_component_count
        FROM %1$s.policy_waiver pw
        LEFT JOIN (%2$s) agg ON agg.owner_id = pw.owner_id
        %3$s
        ORDER BY pw.policy_waiver_id
        """, getDatabaseSchema(), subquery, whereClause);

    int offset = (page - 1) * pageSize;
    try (TransactionContext tx = createTransactionContext()) {
      jakarta.persistence.Query query = createNativePaginationQuery(tx, sQuery, offset, pageSize);

      if (accessibleOwnerIds != null) {
        List<String> ownerIdList = new ArrayList<>(accessibleOwnerIds);
        for (int i = 0; i < ownerIdList.size(); i++) {
          query.setParameter(i + 1, ownerIdList.get(i));
        }
      }

      return ((Stream<Object[]>) query.getResultStream())
          .map(array -> new PolicyContainerWaiverData(
              (String) array[0],
              (Date) array[1],
              (Date) array[2],
              (String) array[3],
              array[4] != null ? ((Number) array[4]).intValue() : 0,
              (String) array[5],
              array[6] != null ? ((Number) array[6]).longValue() : 0L,
              array[7] != null ? ((Number) array[7]).longValue() : 0L))
          .toList();
    }
  }

  public List<PolicyContainerWaiverData> getAllContainerPolicyWaivers(int page, int pageSize) {
    return getAllContainerPolicyWaivers(page, pageSize, null);
  }

  /**
   * Get count of container policy waivers filtered by accessible owner IDs.
   *
   * @param accessibleOwnerIds Set of owner IDs user has access to (null means no filtering)
   * @return Count of container waivers
   */
  public long getContainerPolicyWaiversCount(Set<String> accessibleOwnerIds) {
    if (accessibleOwnerIds != null && accessibleOwnerIds.isEmpty()) {
      return 0L;
    }

    String subquery = getContainerPolicyWaiversSubquery();
    String whereClause;

    if (accessibleOwnerIds == null) {
      whereClause = "WHERE pw.is_for_container_image = true";
    }
    else {
      String placeholders = String.join(", ",
          IntStream.rangeClosed(1, accessibleOwnerIds.size())
              .mapToObj(i -> "?" + i)
              .toList());
      whereClause = "WHERE pw.is_for_container_image = true" +
          " AND pw.owner_id IN (" + placeholders + ")";
    }

    String sQuery = String.format("""
        SELECT COUNT(*)
        FROM %1$s.policy_waiver pw
        LEFT JOIN (%2$s) agg ON agg.owner_id = pw.owner_id
        %3$s
        """, getDatabaseSchema(), subquery, whereClause);

    try (TransactionContext tx = createTransactionContext()) {
      jakarta.persistence.Query query = tx.createNativeQuery(sQuery);

      if (accessibleOwnerIds != null) {
        List<String> ownerIdList = new ArrayList<>(accessibleOwnerIds);
        for (int i = 0; i < ownerIdList.size(); i++) {
          query.setParameter(i + 1, ownerIdList.get(i));
        }
      }

      return ((Number) query.getSingleResult()).longValue();
    }
  }

  public long getContainerPolicyWaiversCount() {
    return getContainerPolicyWaiversCount(null);
  }

  private String getContainerPolicyWaiversSubquery() {
    return String.format("""
        SELECT
          pw2.owner_id,
          MAX(p2.threat_level) AS max_threat_level,
          COUNT(DISTINCT pw2.policy_id) AS unique_policy_count,
          COUNT(DISTINCT pw2.hash) AS unique_component_count,
          a.name AS application_scope
        FROM %1$s.policy_waiver pw2
        JOIN %1$s.policy p2 ON pw2.policy_id = p2.policy_id
        JOIN %1$s.application a ON pw2.owner_id = a.application_id
        WHERE pw2.is_for_container_image_component = true
        GROUP BY pw2.owner_id, a.name
        """, getDatabaseSchema());
  }

  public static record WaiverReasonData(String policyWaiverId, String reasonText) {  }

  public static record PolicyContainerWaiverData(
      String policyWaiverId,
      Date createTime,
      Date expiryTime,
      String ownerId,
      int maxThreatLevel,
      String applicationScope,
      Long uniquePolicyCount,
      Long uniqueComponentCount) {}
}
