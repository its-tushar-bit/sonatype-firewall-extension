/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sourcecontrol;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.dataaccess.TransactionContext;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SourceControlEventDAO
    extends AbstractOperationalSqlDAO<SourceControlEvent>
{
  private static final Logger log = LoggerFactory.getLogger(SourceControlEventDAO.class);

  private static final int DELETE_BATCH_SIZE = 100;

  private static final String SELECT_ENTITY = "SELECT entity FROM SourceControlEvent entity ";

  private static final String UPDATE_ENTITY = "UPDATE SourceControlEvent entity ";

  private static final String WHERE_ENTITY_ID_MATCHES = "WHERE entity.id=?1";

  private static final String UPDATED_EVENT_WITH_STATUS = "updated event {} with status {}";

  public int reserveEventsForInstance(final String instanceId, final int quantity) {
    String sQuery =
        "SELECT entity.id FROM SourceControlEvent entity WHERE entity.instanceId IS NULL ORDER BY entity.createTime";
    List<String> ids =
        new Query<String>(sQuery).setMaxResults(quantity).getList();
    return createQuery("UPDATE SourceControlEvent entity SET entity.instanceId=?2 WHERE entity.id IN (?1)", ids,
        instanceId).executeUpdate();
  }

  public void clearEventReservations() {
    String sQuery =
        UPDATE_ENTITY + "SET entity.instanceId = null WHERE entity.eventStatus IN ('new', 'in progress')";
    int count = createQuery(sQuery).executeUpdate();
    log.debug("Reset {} reserved events", count);
  }

  @VisibleForTesting
  List<SourceControlEvent> getAvailableEvents() {
    String sQuery = SELECT_ENTITY + "WHERE entity.instanceId IS NULL";
    Query<SourceControlEvent> query = new Query<SourceControlEvent>(sQuery);
    return query.getList();
  }

  public List<SourceControlEvent> selectEventsForInstance(final String instanceId, final int quantity) {
    String sQuery = SELECT_ENTITY +
        "WHERE entity.instanceId = ?1 AND entity.eventStatus = ?2 ORDER BY entity.createTime";
    Query<SourceControlEvent> query =
        new Query<SourceControlEvent>(sQuery, instanceId, SourceControlEvent.EVENT_STATUS_NEW);
    query.setMaxResults(quantity);
    return query.getList();
  }

  public void markEventInProgress(final String eventId) {
    String sQuery = UPDATE_ENTITY +
        "SET entity.eventStatus=?2, entity.startTime=?3 " +
        WHERE_ENTITY_ID_MATCHES;
    createQuery(sQuery, eventId, SourceControlEvent.EVENT_STATUS_IN_PROGRESS, new Timestamp(System.currentTimeMillis()))
        .executeUpdate();
    log.debug(UPDATED_EVENT_WITH_STATUS, eventId, SourceControlEvent.EVENT_STATUS_IN_PROGRESS);
  }

  public void markEventComplete(final String eventId) {
    String sQuery = UPDATE_ENTITY +
        "SET entity.eventStatus=?2, entity.completeTime=?3 " +
        WHERE_ENTITY_ID_MATCHES;
    createQuery(sQuery, eventId, SourceControlEvent.EVENT_STATUS_COMPLETE, new Timestamp(System.currentTimeMillis()))
        .executeUpdate();
    log.debug(UPDATED_EVENT_WITH_STATUS, eventId, SourceControlEvent.EVENT_STATUS_COMPLETE);
  }

  public void markEventHasError(final String eventId, final String errorMessage) {
    String sQuery = UPDATE_ENTITY +
        "SET entity.eventStatus=?2, entity.eventStatusDetails=?3, entity.completeTime=?4 " +
        WHERE_ENTITY_ID_MATCHES;
    createQuery(sQuery, eventId, SourceControlEvent.EVENT_STATUS_ERROR, errorMessage,
        new Timestamp(System.currentTimeMillis())).executeUpdate();
    log.debug(UPDATED_EVENT_WITH_STATUS, eventId, errorMessage);
  }

  @Override
  public SourceControlEvent getById(final String id) {
    return get(SELECT_ENTITY + WHERE_ENTITY_ID_MATCHES, id);
  }

  public void deleteByApplicationId(final String applicationId) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      deleteByApplicationId(tx, applicationId);
      tx.commit();
    }
  }

  public void deleteByApplicationId(final TransactionContext tx, final String applicationId) {
    List<SourceControlEvent> sourceControlEventList = getList(
        tx, SELECT_ENTITY + "WHERE entity.applicationId=?1", applicationId);
    for (SourceControlEvent sourceControlEvent : sourceControlEventList) {
      delete(tx, sourceControlEvent);
    }
  }

  public void deleteByPolicyEvaluationId(final TransactionContext tx, final String policyEvaluationId) {
    List<SourceControlEvent> sourceControlEventList = getList(
        tx, SELECT_ENTITY + "WHERE entity.policyEvaluationId=?1 OR entity.targetPolicyEvaluationId=?1",
        policyEvaluationId);
    for (SourceControlEvent sourceControlEvent : sourceControlEventList) {
      delete(tx, sourceControlEvent);
    }
  }

  public int deleteAllBeforeDate(final Date cutoffDate) {
    String sQuery = "SELECT entity.id FROM SourceControlEvent entity" +
        " WHERE entity.createTime < ?1";
    int deletedRows = 0;
    while (true) {
      List<String> ids =
          new Query<String>(sQuery, cutoffDate).setMaxResults(DELETE_BATCH_SIZE).getList();
      if (ids.isEmpty()) {
        return deletedRows;
      }
      deletedRows +=
          createQuery("DELETE FROM SourceControlEvent entity WHERE entity.id IN (?1)", ids)
              .executeUpdate();
    }
  }

  public boolean hasRemediationEventForBranch(String applicationId, String branchName) {
    String sQuery = "SELECT count(entity) FROM SourceControlEvent entity" +
        " WHERE entity.applicationId = ?1 AND entity.eventType = ?2 AND entity.branchName = ?3";
    return 0 !=
        getSingle(Long.class, sQuery, applicationId, SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT, branchName);
  }

  public List<SourceControlEvent> getAll() {
    return getList(SELECT_ENTITY);
  }
}
