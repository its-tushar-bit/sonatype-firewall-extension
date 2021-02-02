/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.utils.ExecutorThreadPools.ThreadPools;
import com.sonatype.insight.dataaccess.TransactionContext;

import static com.sonatype.insight.brain.utils.ExecutorThreadPools.getThreadPool;
import static java.util.stream.Collectors.toList;

/**
 * @since 1.11
 */
public class PolicyViolationDAO
    extends AbstractOperationalSqlDAO<PolicyViolation>
{
  static final int DELETE_BATCH_SIZE = 100;

  @Override
  protected PolicyViolation getById(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM PolicyViolation entity" + //
        " WHERE entity.id=?1";
    return get(tx, sQuery, id);
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
        " AND entity.fixTime IS NULL AND entity.waiveTime IS NULL AND entity.grandfatherTime IS NULL";
    return getList(sQuery, applicationId, stageTypeId);
  }

  public List<PolicyViolation> getActiveByApplicationIdAndStageIdAndHash(String applicationId,
                                                                         String stageTypeId,
                                                                         String hash)
  {
    String sQuery = "SELECT entity FROM PolicyViolation entity" + //
        " WHERE entity.applicationId=?1 AND entity.stageTypeId=?2 AND entity.hash=?3" + //
        " AND entity.fixTime IS NULL AND entity.waiveTime IS NULL AND entity.grandfatherTime IS NULL";
    return getList(sQuery, applicationId, stageTypeId, hash);
  }

  public List<PolicyViolation> getUnfixedByApplicationIdsOpenedAfterDate(Collection<String> applicationIds,
                                                                         Date minDate)
  {
    return getUnfixedByApplicationIdsOpenedAfterDate(applicationIds, minDate, false);
  }

  public List<PolicyViolation> getActiveByApplicationIdsOpenedAfterDate(Collection<String> applicationIds,
                                                                        Date minDate)
  {
    return getUnfixedByApplicationIdsOpenedAfterDate(applicationIds, minDate, true);
  }

  private List<PolicyViolation> getUnfixedByApplicationIdsOpenedAfterDate(Collection<String> applicationIds,
                                                                          Date minDate,
                                                                          boolean onlyActiveViolations)
  {
    String sQuery = "SELECT entity FROM PolicyViolation entity" + //
        " WHERE entity.applicationId=?1" + //
        " AND entity.openTime >= ?2" + //
        " AND entity.fixTime IS NULL" + //
        (onlyActiveViolations ? " AND entity.waiveTime IS NULL AND entity.grandfatherTime IS NULL " : "");
    return getUnfixed(sQuery, applicationIds, minDate);
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
        " WHERE entity.applicationId=?1" + //
        " AND entity.fixTime IS NULL" + //
        (onlyActiveViolations ? " AND entity.waiveTime IS NULL " : "") + //
        (onlyActiveViolations ? " AND entity.grandfatherTime IS NULL " : "");
    return getUnfixed(sQuery, applicationIds);
  }

  public List<PolicyViolation> getUnfixedByApplicationId(TransactionContext tx, String applicationId) {
    String sQuery = "SELECT entity FROM PolicyViolation entity" + //
        " WHERE entity.applicationId=?1" + //
        " AND entity.fixTime IS NULL";
    return getList(tx, sQuery, applicationId);
  }

  public List<PolicyViolation> getUnfixedByApplicationIdsAndStageIdsOpenedAfterDate(Collection<String> applicationIds,
                                                                                    Collection<String> stageTypeIds,
                                                                                    Date minDate)
  {
    return getUnfixedByApplicationIdsAndStageIdsOpenedAfterDate(applicationIds, stageTypeIds, minDate, false);
  }

  public List<PolicyViolation> getActiveByApplicationIdsAndStageIdsOpenedAfterDate(Collection<String> applicationIds,
                                                                                   Collection<String> stageTypeIds,
                                                                                   Date minDate)
  {
    return getUnfixedByApplicationIdsAndStageIdsOpenedAfterDate(applicationIds, stageTypeIds, minDate, true);
  }

  private List<PolicyViolation> getUnfixedByApplicationIdsAndStageIdsOpenedAfterDate(Collection<String> applicationIds,
                                                                                     Collection<String> stageTypeIds,
                                                                                     Date minDate,
                                                                                     boolean onlyActiveViolations)
  {
    String sQuery = "SELECT entity FROM PolicyViolation entity" + //
        " WHERE entity.applicationId=?1 AND entity.stageTypeId IN (?2)" + //
        " AND entity.openTime >= ?3" + //
        " AND entity.fixTime IS NULL" + //
        (onlyActiveViolations ? " AND entity.waiveTime IS NULL AND entity.grandfatherTime IS NULL " : "");
    return getUnfixed(sQuery, applicationIds, stageTypeIds, minDate);
  }

  public List<PolicyViolation> getUnfixedByApplicationIdsAndStageIds(Collection<String> applicationIds,
                                                                     Collection<String> stageTypeIds)
  {
    return getUnfixedByApplicationIdsAndStageIds(applicationIds, stageTypeIds, false);
  }

  public List<PolicyViolation> getActiveByApplicationIdsAndStageIds(Collection<String> applicationIds,
                                                                    Collection<String> stageTypeIds)
  {
    return getUnfixedByApplicationIdsAndStageIds(applicationIds, stageTypeIds, true);
  }

  private List<PolicyViolation> getUnfixedByApplicationIdsAndStageIds(Collection<String> applicationIds,
                                                                      Collection<String> stageTypeIds,
                                                                      boolean onlyActiveViolations)
  {
    String sQuery = "SELECT entity FROM PolicyViolation entity" + //
        " WHERE entity.applicationId=?1 AND entity.stageTypeId IN (?2)" + //
        " AND entity.fixTime IS NULL" + //
        (onlyActiveViolations ? " AND entity.waiveTime IS NULL " : "") + //
        (onlyActiveViolations ? " AND entity.grandfatherTime IS NULL " : "");
    return getUnfixed(sQuery, applicationIds, stageTypeIds);
  }

  private List<PolicyViolation> getUnfixed(String sQuery,
                                           Collection<String> applicationIds,
                                           Object... otherParameters)
  {
    // H2 won't utilize the index for the application id when the query uses an IN operator with multiple values (and
    // has additional filter criteria like the fix_time), doing an expensive table scan instead.
    // So we make one query per app to ensure the index is used (and all the fixed violations aren't scanned).

    return CompletableFuture.supplyAsync(() -> {
      return applicationIds.stream().parallel().map(applicationId -> {
        Object[] parameters = new Object[otherParameters.length + 1];
        System.arraycopy(otherParameters, 0, parameters, 1, otherParameters.length);
        parameters[0] = applicationId;
        return getList(sQuery, parameters);
      }).flatMap(Collection::stream).collect(toList());
    }, getThreadPool(ThreadPools.DAO)).join();
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
        "    AND (entity.grandfatherTime > entity.openTime OR entity.grandfatherTime IS NULL)) " + // not grandfathered
        "   OR " + //
        "   (entity.openTime < ?3 " + // opened before time range
        "    AND CASE WHEN entity.fixTime <= ?3 THEN false" + // not fixed before time range
        "             WHEN entity.waiveTime <= ?3 THEN false" + // not waived before time range
        "             WHEN entity.grandfatherTime <= ?3 THEN false" + // not grandfathered before time range
        "             ELSE true " + //
        "        END = true))";
    return getList(sQuery, appId, stageTypeIds, from, to);
  }

  public List<PolicyViolation> getUnfixedGrandfatheredByApplicationId(TransactionContext tx, String appId) {
    String sQuery = "SELECT entity FROM PolicyViolation entity" + //
        " WHERE entity.applicationId=?1" + //
        " AND entity.fixTime IS NULL AND entity.grandfatherTime IS NOT NULL";
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

  public long getCount() {
    String sQuery = "SELECT COUNT(entity) FROM PolicyViolation entity";
    return getSingle(Long.class, sQuery);
  }
}
