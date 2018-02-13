/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * @since 1.11
 */
public class PolicyViolationDAO
    extends AbstractOperationalSqlDAO<PolicyViolation>
{
  static final int IN_OPERATOR_THRESHOLD = 2000;

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
        " AND entity.fixTime IS NULL AND entity.waiveTime IS NULL";
    return getList(sQuery, applicationId, stageTypeId);
  }

  public List<PolicyViolation> getActiveByApplicationIdAndStageIdAndHash(String applicationId,
                                                                         String stageTypeId,
                                                                         String hash)
  {
    String sQuery = "SELECT entity FROM PolicyViolation entity" + //
        " WHERE entity.applicationId=?1 AND entity.stageTypeId=?2 AND entity.hash=?3" + //
        " AND entity.fixTime IS NULL AND entity.waiveTime IS NULL";
    return getList(sQuery, applicationId, stageTypeId, hash);
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
        " WHERE entity.applicationId IN (?1)" + //
        " AND entity.fixTime IS NULL" + //
        (onlyActiveViolations ? " AND entity.waiveTime IS NULL " : "");
    return getListChunked(sQuery, applicationIds);
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
        " WHERE entity.applicationId IN (?1) AND entity.stageTypeId IN (?2)" + //
        " AND entity.fixTime IS NULL" + //
        (onlyActiveViolations ? " AND entity.waiveTime IS NULL " : "");
    return getListChunked(sQuery, applicationIds, stageTypeIds);
  }

  private List<PolicyViolation> getListChunked(String sQuery, Collection<String> ids, Object... otherParameters) {
    Object[] parameters = new Object[otherParameters.length + 1];
    System.arraycopy(otherParameters, 0, parameters, 1, otherParameters.length);
    if (ids.size() >= IN_OPERATOR_THRESHOLD) {
      // As measurements have shown (cf. CLM-6085), H2 doesn't handle an {@code IN} operator with a huge list of values
      // well and query time increases superlinear. Making multiple queries with smaller chunks of the input set keeps
      // the performance more linear. The chunk size below has been found to be a good compromise between DB query
      // overhead and individual query time.
      List<PolicyViolation> violations = new ArrayList<>();
      int chunkSize = 200;
      List<String> chunk = new ArrayList<>(chunkSize);
      parameters[0] = chunk;
      for (String id : ids) {
        chunk.add(id);
        if (chunk.size() >= chunkSize) {
          violations.addAll(getList(sQuery, parameters));
          chunk.clear();
        }
      }
      if (!chunk.isEmpty()) {
        violations.addAll(getList(sQuery, parameters));
      }
      return violations;
    }
    parameters[0] = ids;
    return getList(sQuery, parameters);
  }

  public int replacePolicyId(String fromPolicyId, String toPolicyId) {
    String sQuery = "UPDATE PolicyViolation entity" + //
        " SET entity.policyId=?2" + //
        " WHERE entity.policyId=?1";
    Query query = createQuery(sQuery, fromPolicyId, toPolicyId);
    return query.executeUpdate();
  }

  public int replacePolicyId(TransactionContext tx, String applicationId, String fromPolicyId, String toPolicyId) {
    String sQuery = "UPDATE PolicyViolation entity" + //
        " SET entity.policyId=?3" + //
        " WHERE entity.applicationId=?1 AND entity.policyId=?2";
    Query query = createQuery(sQuery, applicationId, fromPolicyId, toPolicyId);
    return query.executeUpdate(tx);
  }
}
