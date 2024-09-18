/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.tenancy.TenantAwareFunction;
import com.sonatype.insight.brain.tenancy.TenantAwareSupplier;
import com.sonatype.insight.brain.utils.ExecutorThreadPools;
import com.sonatype.insight.brain.utils.ExecutorThreadPools.ThreadPools;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.apache.commons.lang3.StringUtils;

import static java.util.stream.Collectors.toList;

/**
 * @since 1.11
 */
@Named
@Singleton
public class PolicyViolationDAO
    extends AbstractOperationalSqlDAO<PolicyViolation>
    implements AbstractPolicyViolationDAO
{
  static final int DELETE_BATCH_SIZE = 100;

  private final PolicyViolationConstraintFactsDAO policyViolationConstraintFactsDAO;

  @Inject
  public PolicyViolationDAO(
      OperationalDataStore operationalDataStore,
      PolicyViolationConstraintFactsDAO policyViolationConstraintFactsDAO)
  {
    super(operationalDataStore);
    this.policyViolationConstraintFactsDAO = policyViolationConstraintFactsDAO;
  }

  @Override
  public void insert(final TransactionContext tx, final PolicyViolation entity) {
    storeConstraints(policyViolationConstraintFactsDAO, tx, entity);
    super.insert(tx, entity);
  }

  @Override
  public void update(final TransactionContext tx, final PolicyViolation entity) {
    storeConstraints(policyViolationConstraintFactsDAO, tx, entity);
    super.update(tx, entity);
  }

  public List<PolicyViolation> getByApplicationId(String applicationId) {
    String sQuery = "SELECT entity FROM PolicyViolation entity" + //
        " WHERE entity.applicationId=?1";
    return getList(sQuery, applicationId);
  }

  public List<PolicyViolation> getByApplicationIdAndPolicyIdAndHash(String applicationId,
                                                                    String policyId,
                                                                    String hash)
  {
    StringBuilder sQueryBuilder = new StringBuilder();
    sQueryBuilder.append("SELECT entity FROM PolicyViolation entity");
    sQueryBuilder.append(" WHERE entity.applicationId=?1 AND entity.policyId=?2");

    if (hash == null) {
      sQueryBuilder.append(" AND entity.hash IS NULL");
      return getList(sQueryBuilder.toString(), applicationId, policyId);
    }
    else {
      sQueryBuilder.append(" AND entity.hash=?3");
      return getList(sQueryBuilder.toString(), applicationId, policyId, hash);
    }
  }

  public List<PolicyViolation> getUnfixedByApplicationIdAndStageId(String applicationId, String stageTypeId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getUnfixedByApplicationIdAndStageId(tx, applicationId, stageTypeId);
    }
  }

  public List<PolicyViolation> getUnfixedByApplicationIdAndStageId(TransactionContext tx,
                                                                   String applicationId,
                                                                   String stageTypeId)
  {
    String sQuery = "SELECT entity FROM PolicyViolation entity" + //
        " WHERE entity.applicationId=?1 AND entity.stageTypeId=?2" + //
        " AND entity.fixTime IS NULL";
    return getList(tx, sQuery, applicationId, stageTypeId);
  }

  public List<PolicyViolation> getActiveByApplicationIdAndStageId(String applicationId, String stageTypeId) {
    String sQuery = "SELECT entity FROM PolicyViolation entity" + //
        " WHERE entity.applicationId=?1 AND entity.stageTypeId=?2" + //
        " AND entity.fixTime IS NULL AND entity.waiveTime IS NULL AND entity.legacyViolationTime IS NULL";
    return getList(sQuery, applicationId, stageTypeId);
  }

  public List<PolicyViolation> getActiveByApplicationIdAndStageIdAndHash(String applicationId,
                                                                         String stageTypeId,
                                                                         String hash)
  {
    String sQuery = "SELECT entity FROM PolicyViolation entity" + //
        " WHERE entity.applicationId=?1 AND entity.stageTypeId=?2 AND entity.hash=?3" + //
        " AND entity.fixTime IS NULL AND entity.waiveTime IS NULL AND entity.legacyViolationTime IS NULL";
    return getList(sQuery, applicationId, stageTypeId, hash);
  }

  public List<PolicyViolation> getUnfixedByApplicationIdsOpenedAfterDate(
      Collection<String> applicationIds,
      Date minDate,
      Integer minThreatLevel,
      Integer maxThreatLevel,
      Collection<PolicyThreatCategory> policyThreatCategories)
  {
    return getUnfixedByApplicationIdsOpenedAfterDate(applicationIds, minDate, false, minThreatLevel, maxThreatLevel,
        policyThreatCategories);
  }

  public List<PolicyViolation> getActiveByApplicationIdsOpenedAfterDate(
      Collection<String> applicationIds,
      Date minDate,
      Integer minThreatLevel,
      Integer maxThreatLevel,
      Collection<PolicyThreatCategory> policyThreatCategories)
  {
    return getUnfixedByApplicationIdsOpenedAfterDate(applicationIds, minDate, true, minThreatLevel, maxThreatLevel,
        policyThreatCategories);
  }

  private List<PolicyViolation> getUnfixedByApplicationIdsOpenedAfterDate(
      Collection<String> applicationIds,
      Date minDate,
      boolean onlyActiveViolations,
      Integer minThreatLevel,
      Integer maxThreatLevel,
      Collection<PolicyThreatCategory> policyThreatCategories)
  {
    minThreatLevel = minThreatLevel == null ? 0 : minThreatLevel;
    maxThreatLevel = maxThreatLevel == null ? 10 : maxThreatLevel;

    policyThreatCategories = getPolicyThreatCategoriesFilter(policyThreatCategories);

    String sQuery = "SELECT entity FROM PolicyViolation entity" + //
        " WHERE " + applicationIdString() + //
        " AND entity.openTime >= ?2" + //
        " AND entity.threatLevel >= ?3" + //
        " AND entity.threatLevel <= ?4" + //
        " AND entity.fixTime IS NULL" + //
        " AND entity.threatCategory IN (?5)" + //
        (onlyActiveViolations ? " AND entity.waiveTime IS NULL AND entity.legacyViolationTime IS NULL " : "");
    return getUnfixed(sQuery, applicationIds, minDate, minThreatLevel, maxThreatLevel, policyThreatCategories);
  }

  private String applicationIdString() {
    return isDatabaseEmbedded() ? "entity.applicationId=?1" : "entity.applicationId IN (?1)";
  }

  public List<PolicyViolation> getUnfixedByApplicationIds(Collection<String> applicationIds) {
    return getUnfixedByApplicationIds(applicationIds, false);
  }

  public List<PolicyViolation> getActiveByApplicationIds(Collection<String> applicationIds) {
    return getUnfixedByApplicationIds(applicationIds, true);
  }

  private List<PolicyViolation> getUnfixedByApplicationIds(Collection<String> applicationIds,
                                                           boolean onlyActiveViolations)
  {
    String sQuery = "SELECT entity FROM PolicyViolation entity" + //
        " WHERE " + applicationIdString() + //
        " AND entity.fixTime IS NULL" + //
        (onlyActiveViolations ? " AND entity.waiveTime IS NULL " : "") + //
        (onlyActiveViolations ? " AND entity.legacyViolationTime IS NULL " : "");
    return getUnfixed(sQuery, applicationIds);
  }

  public List<PolicyViolation> getUnfixedByApplicationId(TransactionContext tx, String applicationId) {
    String sQuery = "SELECT entity FROM PolicyViolation entity" + //
        " WHERE entity.applicationId=?1" + //
        " AND entity.fixTime IS NULL";
    return getList(tx, sQuery, applicationId);
  }

  public List<PolicyViolation> getActiveByApplicationIdsAndStageIdsOpenedAfterDate(
      Collection<String> applicationIds,
      Collection<String> stageTypeIds,
      Date minDate,
      Integer minThreatLevel,
      Integer maxThreatLevel,
      Collection<PolicyThreatCategory> policyThreatCategories)
  {
    minThreatLevel = minThreatLevel == null ? 0 : minThreatLevel;
    maxThreatLevel = maxThreatLevel == null ? 10 : maxThreatLevel;

    policyThreatCategories = getPolicyThreatCategoriesFilter(policyThreatCategories);

    String sQuery = "SELECT entity FROM PolicyViolation entity" + //
        " WHERE " + applicationIdString() + " AND entity.stageTypeId IN (?2)" + //
        " AND entity.openTime >= ?3" + //
        " AND entity.threatLevel >= ?4" + //
        " AND entity.threatLevel <= ?5" + //
        " AND entity.threatCategory IN (?6)" + //
        " AND entity.fixTime IS NULL" + //
        " AND entity.waiveTime IS NULL AND entity.legacyViolationTime IS NULL";
    return getUnfixed(sQuery, applicationIds, stageTypeIds, minDate, minThreatLevel, maxThreatLevel,
        policyThreatCategories);
  }

  public List<PolicyViolation> getActiveByApplicationIdsAndStageIds(
      Collection<String> applicationIds,
      Collection<String> stageTypeIds,
      Integer minThreatLevel,
      Integer maxThreatLevel,
      Collection<PolicyThreatCategory> policyThreatCategories)
  {
    minThreatLevel = minThreatLevel == null ? 0 : minThreatLevel;
    maxThreatLevel = maxThreatLevel == null ? 10 : maxThreatLevel;

    policyThreatCategories = getPolicyThreatCategoriesFilter(policyThreatCategories);

    String sQuery = "SELECT entity FROM PolicyViolation entity" + //
        " WHERE " + applicationIdString() + " AND entity.stageTypeId IN (?2)" + //
        " AND entity.fixTime IS NULL" + //
        " AND entity.threatLevel >= ?3" + //
        " AND entity.threatLevel <= ?4" + //
        " AND entity.threatCategory IN (?5)" + //
        " AND entity.waiveTime IS NULL" + //
        " AND entity.legacyViolationTime IS NULL";

    return getUnfixed(sQuery, applicationIds, stageTypeIds, minThreatLevel, maxThreatLevel, policyThreatCategories);
  }

  public List<PolicyViolation> getUnfixedBy(
      Collection<String> applicationIds,
      Collection<String> stageTypeIds,
      Integer minThreatLevel,
      Integer maxThreatLevel,
      Collection<PolicyThreatCategory> policyThreatCategories,
      Boolean violationStateOpen,
      Boolean violationStateWaived,
      Boolean violationStateLegacyViolation)
  {
    minThreatLevel = minThreatLevel == null ? 0 : minThreatLevel;
    maxThreatLevel = maxThreatLevel == null ? 10 : maxThreatLevel;

    policyThreatCategories = getPolicyThreatCategoriesFilter(policyThreatCategories);

    String sQuery = "SELECT entity FROM PolicyViolation entity" + //
        " WHERE " + applicationIdString() + " AND entity.stageTypeId IN (?2)" + //
        " AND entity.fixTime IS NULL" + //
        " AND entity.threatLevel >= ?3" + //
        " AND entity.threatLevel <= ?4" + //
        " AND entity.threatCategory IN (?5)";

    sQuery += getPolicyStateFilter(violationStateOpen, violationStateWaived, violationStateLegacyViolation);

    return getUnfixed(sQuery, applicationIds, stageTypeIds, minThreatLevel, maxThreatLevel, policyThreatCategories);
  }

  public List<PolicyViolation> getUnfixedBy(
      Collection<String> applicationIds,
      Collection<String> stageTypeIds,
      Date minDate,
      Integer minThreatLevel,
      Integer maxThreatLevel,
      Collection<PolicyThreatCategory> policyThreatCategories,
      Boolean violationStateOpen,
      Boolean violationStateWaived,
      Boolean violationStateLegacyViolation)
  {
    minThreatLevel = minThreatLevel == null ? 0 : minThreatLevel;
    maxThreatLevel = maxThreatLevel == null ? 10 : maxThreatLevel;

    policyThreatCategories = getPolicyThreatCategoriesFilter(policyThreatCategories);

    String sQuery = "SELECT entity FROM PolicyViolation entity" + //
        " WHERE " + applicationIdString() + "AND entity.stageTypeId IN (?2)" + //
        " AND entity.openTime >= ?3" + //
        " AND entity.threatLevel >= ?4" + //
        " AND entity.threatLevel <= ?5" + //
        " AND entity.threatCategory IN (?6)" + //
        " AND entity.fixTime IS NULL";

    sQuery += getPolicyStateFilter(violationStateOpen, violationStateWaived, violationStateLegacyViolation);

    return getUnfixed(sQuery, applicationIds, stageTypeIds, minDate, minThreatLevel, maxThreatLevel,
        policyThreatCategories);
  }

  private String getPolicyStateFilter(
      Boolean violationStateOpen,
      Boolean violationStateWaived,
      Boolean violationStateLegacyViolation)
  {
    String policyStateFilter = "";

    violationStateOpen = violationStateOpen == null || violationStateOpen;
    violationStateWaived = violationStateWaived == null || violationStateWaived;
    violationStateLegacyViolation = violationStateLegacyViolation == null || violationStateLegacyViolation;

    if (!violationStateOpen && !violationStateWaived && !violationStateLegacyViolation) {
      return policyStateFilter;
    }

    if (!violationStateOpen || !violationStateWaived || !violationStateLegacyViolation) {
      policyStateFilter += " AND (";

      List<String> stateQuery = new ArrayList<>();
      if (violationStateOpen) {
        stateQuery.add("(entity.waiveTime IS NULL AND entity.legacyViolationTime IS NULL)");
      }
      if (violationStateWaived) {
        stateQuery.add("entity.waiveTime IS NOT NULL");
      }
      if (violationStateLegacyViolation) {
        stateQuery.add("entity.legacyViolationTime IS NOT NULL");
      }
      policyStateFilter += StringUtils.join(stateQuery.toArray(), " OR ");
      policyStateFilter += ")";
    }

    return policyStateFilter;
  }

  public List<PolicyViolation> getActiveByApplicationIdsAndPolicyIds(
      Collection<String> applicationIds,
      Collection<String> policyIds,
      Date openTimeAfter,
      Date openTimeBefore)
  {
    String sQuery = "SELECT entity FROM PolicyViolation entity" + //
        " WHERE " + applicationIdString() + " AND entity.policyId IN (?2)" + //
        " AND entity.fixTime IS NULL" + //
        " AND entity.waiveTime IS NULL" + //
        " AND entity.legacyViolationTime IS NULL";

    int nextIndex = 3;
    if (openTimeAfter != null) {
      sQuery += " AND entity.openTime >= ?" + nextIndex++;
    }

    if (openTimeBefore != null) {
      sQuery += " AND entity.openTime <= ?" + nextIndex;
    }

    if (openTimeAfter != null) {
      if (openTimeBefore != null) {
        return getUnfixed(sQuery, applicationIds, policyIds, openTimeAfter, openTimeBefore);
      }
      return getUnfixed(sQuery, applicationIds, policyIds, openTimeAfter);
    }
    else {
      if (openTimeBefore != null) {
        return getUnfixed(sQuery, applicationIds, policyIds, openTimeBefore);
      }
      return getUnfixed(sQuery, applicationIds, policyIds);
    }
  }

  private List<PolicyViolation> getUnfixed(String sQuery,
                                           Collection<String> applicationIds,
                                           Object... otherParameters)
  {
    if (isDatabaseEmbedded()) {
      // H2 won't utilize the index for the application id when the query uses an IN operator with multiple values (and
      // has additional filter criteria like the fix_time), doing an expensive table scan instead.
      // So we make one query per app to ensure the index is used (and all the fixed violations aren't scanned).
      TenantAwareFunction<String, List<PolicyViolation>> tenantAwareFunction =
          new TenantAwareFunction<>(applicationId -> {
            Object[] parameters = new Object[otherParameters.length + 1];
            System.arraycopy(otherParameters, 0, parameters, 1, otherParameters.length);
            parameters[0] = applicationId;
            return getList(sQuery, parameters);
          });
      return CompletableFuture.supplyAsync(
          new TenantAwareSupplier<>(() -> applicationIds.stream()
              .parallel()
              .map(tenantAwareFunction)
              .flatMap(Collection::stream)
              .collect(toList())),
          ExecutorThreadPools.getInstance().getThreadPool(ThreadPools.DAO)).join();
    }
    else if (!applicationIds.isEmpty()) {
      return getListWithSqlInClause(applicationIds, appIds ->  {
        Object[] parameters = new Object[otherParameters.length + 1];
        System.arraycopy(otherParameters, 0, parameters, 1, otherParameters.length);
        parameters[0] = appIds;
        return getList(sQuery, parameters);
      });
    }
    else {
      return Collections.emptyList();
    }
  }

  public List<PolicyViolation> getActiveByApplicationIdAndStageIdsAndTimeRange(String appId,
                                                                               Collection<String> stageTypeIds,
                                                                               Date from,
                                                                               Date to)
  {
    String sQuery = "SELECT entity FROM PolicyViolation entity" + //
        " WHERE entity.applicationId = ?1 AND entity.stageTypeId IN (?2)" + //
        " AND (" + //
        "   (entity.openTime >= ?3 AND entity.openTime < ?4" + // opened during time range
        "    AND (entity.waiveTime > entity.openTime OR entity.waiveTime IS NULL) " + // not immediately waived
        "    AND (entity.legacyViolationTime > entity.openTime OR entity.legacyViolationTime IS NULL)) " +
        "   OR " + //
        "   (entity.openTime < ?3 " + // opened before time range
        "    AND CASE WHEN entity.fixTime <= ?3 THEN false" + // not fixed before time range
        "             WHEN entity.waiveTime <= ?3 THEN false" + // not waived before time range
        "             WHEN entity.legacyViolationTime <= ?3 THEN false" + // not legacy status before time range
        "             ELSE true " + //
        "        END = true))";
    return getList(sQuery, appId, stageTypeIds, from, to);
  }

  public List<PolicyViolation> getUnfixedLegacyViolationByApplicationId(TransactionContext tx, String appId) {
    String sQuery = "SELECT entity FROM PolicyViolation entity" + //
        " WHERE entity.applicationId=?1" + //
        " AND entity.fixTime IS NULL AND entity.legacyViolationTime IS NOT NULL";
    return getList(tx, sQuery, appId);
  }

  public int replacePolicyId(String fromPolicyId, String toPolicyId) {
    String sQuery = "UPDATE PolicyViolation entity" + //
        " SET entity.policyId=?2" + //
        " WHERE entity.policyId=?1";
    return createQuery(sQuery, fromPolicyId, toPolicyId).executeUpdate();
  }

  public int replacePolicyId(TransactionContext tx, String applicationId, String fromPolicyId, String toPolicyId) {
    String sQuery = "UPDATE PolicyViolation entity" + //
        " SET entity.policyId=?3" + //
        " WHERE entity.applicationId=?1 AND entity.policyId=?2";
    return createQuery(sQuery, applicationId, fromPolicyId, toPolicyId).executeUpdate(tx);
  }

  public int deleteFixedByApplicationIdAndDate(String applicationId, Date fixedBefore) {
    // For performance reasons, we bypass the standard delete (per entity) method here.
    if (isDatabaseEmbedded()) {
      // Deleting a potentially huge number of records from H2 in one shot consumes a lot of heap and blocks any other
      // database operation for a long time. To avoid this, we split the entire delete up into smaller batches.
      // See https://issues.sonatype.org/browse/CLM-15723 for details
      String sQuery = "SELECT entity.id FROM PolicyViolation entity" + //
          " WHERE entity.applicationId = ?1 AND entity.fixTime < ?2";
      int deletedRows = 0;
      while (true) {
        List<String> ids =
            new Query<String>(sQuery, applicationId, fixedBefore).setMaxResults(DELETE_BATCH_SIZE).getList();
        if (ids.isEmpty()) {
          return deletedRows;
        }
        deletedRows += createQuery("DELETE FROM PolicyViolation entity WHERE entity.id IN (?1)", ids).executeUpdate();
      }
    }
    else {
      // We cannot do this for H2 until we upgrade to a multi-threaded H2 version.
      // See https://issues.sonatype.org/browse/CLM-15723 for details
      return createQuery("DELETE FROM PolicyViolation entity WHERE entity.applicationId = ?1 AND entity.fixTime < ?2",
          applicationId, fixedBefore).executeUpdate();
    }
  }

  @Override
  public final void delete(PolicyViolation entity) {
    // WARNING: Don't add any business logic to this method because, for performance reasons,
    // we bypass this method when deleting all policy violations for an application.
    // See https://issues.sonatype.org/browse/CLM-15648 for details
    super.delete(entity);
  }

  @Override
  public final void delete(TransactionContext tx, PolicyViolation entity) {
    // WARNING: Don't add any business logic to this method because, for performance reasons,
    // we bypass this method when deleting all policy violations for an application.
    // See https://issues.sonatype.org/browse/CLM-15648 for details
    super.delete(tx, entity);
  }

  public void deleteByApplicationId(TransactionContext tx, String appId) {
    if (isDatabaseEmbedded()) {
      // We do not enroll the deletions in the transaction on purpose.
      // This improves performance and keeps db operations (including commits) reasonably short, which means other
      // concurrent db operations are blocked for shorter periods of time (H2 is single threaded).
      // See https://issues.sonatype.org/browse/CLM-15648 for details
      getByApplicationId(appId).forEach(this::delete);
    }
    else {
      // For performance reasons, we bypass the standard delete (per entity) method here.
      // We cannot do this for H2 until we upgrade to a multi-threaded H2 version.
      // See https://issues.sonatype.org/browse/CLM-15648 for details
      String sQuery = "DELETE FROM PolicyViolation entity WHERE entity.applicationId=?1";
      createQuery(sQuery, appId).executeUpdate(tx);
    }
  }

  public int getCountApplicationsWithPolicyActionFailures(final String stageTypeId) {
    final String sQuery = "SELECT COUNT(DISTINCT entity.applicationId)" +
        " FROM PolicyViolation entity" +
        " WHERE entity.stageTypeId = ?1" +
        " AND entity.fixTime IS NULL" +
        " AND entity.waiveTime IS NULL" +
        String.format(" AND entity.actionTypeId = '%s'", Action.ID_FAIL);
    return getSingle(Number.class, sQuery, stageTypeId).intValue();
  }

  public int getCountActiveWaivers() {
    final String sQuery = "SELECT COUNT(entity.id)" +
        " FROM PolicyViolation entity" +
        " WHERE entity.waiveTime IS NOT NULL" +
        " AND entity.fixTime IS NULL";
    return getSingle(Number.class, sQuery).intValue();
  }

  @Override
  public long getCountWhereConstraintFactsJsonNotNull() {
    String sQuery = "SELECT COUNT(entity) FROM " + getEntityName() +
        " entity WHERE entity.constraintFactsJson IS NOT NULL";

    return getSingle(Long.class, sQuery);
  }

  public List<PolicyViolation> getWaivedFixed() {
    final String sQuery = "SELECT entity" +
        " FROM PolicyViolation entity" +
        " WHERE entity.fixTime IS NOT NULL" +
        " OR entity.waiveTime IS NOT NULL";
    return getList(sQuery);
  }

  public long getMeanTimeToRemediate(final int lookBackWindowDays) {
    final String sQuery;

    if (isDatabasePostgresql()) {
      sQuery = "SELECT EXTRACT(EPOCH FROM AVG(LEAST(fix_time, waive_time) - open_time)) * 1000" +
        "  FROM " + getDatabaseSchema() + ".policy_violation" +
        "  WHERE (fix_time IS NOT null OR waive_time IS NOT null)" +
        "  AND open_time > CURRENT_DATE - MAKE_INTERVAL(days => ?1)";
    }
    else {
      // We support h2 1.4. This version does not support EXTRACT (epoch, it also does not correctly subtract two
      // timestamps
      // It does, however have the function TIMESTAMPDIFF (postgres does not), which gets us the same result
      // Keep in mind
      // SELECT DATEADD(mm,-1, '2024-03-29 18:47:52.69') -> 2024-02-29 18:47:52.69, but
      // SELECT DATEADD(mm,-1, '2024-03-30 18:47:52.69') -> 2024-02-29 18:47:52.69
      // TO DO CONSIDER TAKING THIS NUMBER IN DAYS, WEEKS or MS, FOR OTHER ENDPOINTS WE TREAT 3 months as 12 weeks
      sQuery = "SELECT AVG(TIMESTAMPDIFF(MILLISECOND, open_time, LEAST(fix_time, waive_time)))" +
        "  FROM " + getDatabaseSchema() + ".policy_violation" +
        "  WHERE (fix_time IS NOT null OR waive_time IS NOT null)" +
        "  AND policy_violation.open_time > DATEADD(dd, -?1, CURRENT_TIMESTAMP)";
    }

    try (TransactionContext tx = createTransactionContext()) {
      javax.persistence.Query query = tx.createNativeQuery(sQuery, Double.class);

      query.setParameter(1, lookBackWindowDays);
      Double result = (Double)query.getSingleResult();

      if (result == null) {
        return 0L;
      }

      return Math.round(result);
    }
  }

  private Collection<PolicyThreatCategory> getPolicyThreatCategoriesFilter(
      Collection<PolicyThreatCategory> policyThreatCategories)
  {
    if (policyThreatCategories == null || policyThreatCategories.isEmpty()) {
      policyThreatCategories = Arrays.stream(PolicyThreatCategory.values()).collect(Collectors.toSet());
    }
    return policyThreatCategories;
  }
}
