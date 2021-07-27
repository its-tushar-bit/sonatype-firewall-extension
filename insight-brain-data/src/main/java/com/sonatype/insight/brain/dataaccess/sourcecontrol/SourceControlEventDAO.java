/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sourcecontrol;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.persistence.LockModeType;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.dataaccess.TransactionContext;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.EVENT_STATUS_COMPLETE;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.EVENT_STATUS_ERROR;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.EVENT_STATUS_IN_PROGRESS;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.EVENT_STATUS_NEW;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.EVENT_STATUS_PARTIALLY_COMPLETE;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.SOURCE_CONTROL_EVALUATION_EVENT;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.UPDATED_PULL_REQUEST_EVENT;

public class SourceControlEventDAO
    extends AbstractOperationalSqlDAO<SourceControlEvent>
{
  private static final Logger log = LoggerFactory.getLogger(SourceControlEventDAO.class);

  private static final int DELETE_BATCH_SIZE = 100;

  private static final String SELECT_ENTITY = "SELECT entity FROM SourceControlEvent entity ";

  private static final String UPDATE_ENTITY = "UPDATE SourceControlEvent entity ";

  private static final String WHERE_ENTITY_ID_MATCHES = "WHERE entity.id=?1";

  private static final String UPDATED_EVENT_WITH_STATUS = "updated event {} with status {}";

  public int reserveEventsForInstance(final String instanceId) {
    int result = 0;

    try (TransactionContext txn = createTransactionContext()) {
      txn.begin();

      // assign the given instance ID to any events that aren't already assigned IFF there are no active events
      // (i.e. 'new', 'in progress') already assigned to another instance
      result = txn
          .createNativeQuery(
              "UPDATE insight_brain_ods.source_control_event" +
                  " SET instance_id = ?1" +
                  " WHERE source_control_event_id IN (" +
                  "   SELECT unassigned_events.id FROM (" +
                  "     SELECT source_control_event_id AS id" +
                  "       FROM insight_brain_ods.source_control_event" +
                  "       WHERE instance_id IS NULL" +
                  "       FOR UPDATE" +
                  "     ) AS unassigned_events," +
                  "     (" +
                  "       SELECT count(*) AS reserved_count " +
                  "       FROM insight_brain_ods.source_control_event" +
                  "       WHERE instance_id IS NOT NULL" +
                  "       AND instance_id != ?1" +
                  "       AND event_status IN ('new', 'in progress')" +
                  "     ) AS events_reserved_for_other_instances" +
                  "   WHERE events_reserved_for_other_instances.reserved_count = 0" +
                  " );"
          )
          .setParameter(1, instanceId)
          .executeUpdate();
      txn.commit();
    }
    return result;
  }

  @VisibleForTesting
  List<SourceControlEvent> getAvailableEvents() {
    String sQuery = SELECT_ENTITY + "WHERE entity.instanceId IS NULL";
    return getList(sQuery);
  }

  public List<SourceControlEvent> selectUnassignedNewEventsAndAssignToInstance(final String instanceId) {
    List<SourceControlEvent> unassignedEvents;

    String sQuery = SELECT_ENTITY +
        "WHERE entity.instanceId IS NULL AND entity.eventStatus = ?1 " +
        "ORDER BY entity.createTime";
    Query<SourceControlEvent> query = new Query<SourceControlEvent>(sQuery, EVENT_STATUS_NEW);
    // need a 'select for update' type query - this is how to do it in JPA
    query.setLockModeType(LockModeType.PESSIMISTIC_WRITE);

    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();

      unassignedEvents = query.getList(tx);
      unassignedEvents.forEach(event -> {
        event.setInstanceId(instanceId);
        update(tx, event);
      });

      tx.commit();
    }

    return unassignedEvents;
  }

  public List<SourceControlEvent> selectEventsForInstance(final String instanceId, final int quantity) {
    String sQuery = SELECT_ENTITY +
        "WHERE entity.instanceId = ?1 AND entity.eventStatus = ?2 ORDER BY entity.eventPriority, entity.createTime";
    Query<SourceControlEvent> query =
        new Query<SourceControlEvent>(sQuery, instanceId, EVENT_STATUS_NEW);
    query.setMaxResults(quantity);
    return query.getList();
  }

  public List<SourceControlEvent> getPendingOrInProgressSourceControlEvaluationEvents() {
    List<String> statuses = Arrays.asList(EVENT_STATUS_NEW, EVENT_STATUS_IN_PROGRESS);
    String sQuery = SELECT_ENTITY + "WHERE entity.eventType = ?1 AND entity.eventStatus IN ?2";
    return getList(sQuery, SOURCE_CONTROL_EVALUATION_EVENT, statuses);
  }

  public List<SourceControlEvent> getPendingOrInProgressUpdatedPullRequestEvents(
      List<String> appIds,
      int pullRequestNumber)
  {
    List<String> statuses = Arrays.asList(EVENT_STATUS_NEW, EVENT_STATUS_IN_PROGRESS);
    String sQuery = SELECT_ENTITY + //
        "WHERE entity.eventType = ?1 AND entity.eventStatus IN ?2" + //
        " AND entity.applicationId IN ?3 AND entity.pullRequestNumber=?4";
    return getList(sQuery, UPDATED_PULL_REQUEST_EVENT, statuses, appIds, pullRequestNumber);
  }

  public void markEventInProgress(final String eventId) {
    String sQuery = UPDATE_ENTITY +
        "SET entity.eventStatus=?2, entity.startTime=?3 " +
        WHERE_ENTITY_ID_MATCHES;
    createQuery(sQuery, eventId, EVENT_STATUS_IN_PROGRESS, new Timestamp(System.currentTimeMillis()))
        .executeUpdate();
    log.debug(UPDATED_EVENT_WITH_STATUS, eventId, EVENT_STATUS_IN_PROGRESS);
  }

  public void markEventComplete(final String eventId) {
    String sQuery = UPDATE_ENTITY +
        "SET entity.eventStatus=?2, entity.completeTime=?3 " +
        WHERE_ENTITY_ID_MATCHES;
    createQuery(sQuery, eventId, EVENT_STATUS_COMPLETE, new Timestamp(System.currentTimeMillis()))
        .executeUpdate();
    log.debug(UPDATED_EVENT_WITH_STATUS, eventId, EVENT_STATUS_COMPLETE);
  }

  public void markEventHasError(final String eventId, final String errorMessage) {
    markEventFinishedWithMessage(eventId, errorMessage, EVENT_STATUS_ERROR);
  }

  public void markEventPartiallyComplete(final String eventId, final String message) {
    markEventFinishedWithMessage(eventId, message, EVENT_STATUS_PARTIALLY_COMPLETE);
  }

  private void markEventFinishedWithMessage(final String eventId, final String message, final String eventStatus) {
    String sQuery = UPDATE_ENTITY +
        "SET entity.eventStatus=?2, entity.eventStatusDetails=?3, entity.completeTime=?4 " +
        WHERE_ENTITY_ID_MATCHES;
    createQuery(sQuery, eventId, eventStatus, StringUtils.abbreviate(message, 2048),
        new Timestamp(System.currentTimeMillis())).executeUpdate();
    log.debug(UPDATED_EVENT_WITH_STATUS, eventId, eventStatus);
  }

  @Override
  public SourceControlEvent getById(final String id) {
    return get(SELECT_ENTITY + WHERE_ENTITY_ID_MATCHES, id);
  }

  public void resetStaleEvents(Date cutoffTime, String instanceIdToIgnore) {
    String sQuery = UPDATE_ENTITY +
        "SET entity.instanceId = null, entity.eventStatus = 'new' " +
        "WHERE entity.instanceId IS NOT NULL " +
        "AND ((entity.eventStatus = 'new' AND entity.createTime < ?1) " +
        "OR (entity.eventStatus = 'in progress' AND entity.startTime < ?1)) " +
        "AND entity.instanceId <> ?2";
    createQuery(sQuery, cutoffTime, instanceIdToIgnore).executeUpdate();
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
    List<SourceControlEvent> sourceControlEventList = getList(tx, SELECT_ENTITY + "WHERE entity.policyEvaluationId=?1",
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
        getSingle(Long.class, sQuery, applicationId, REMEDIATION_PULL_REQUEST_EVENT, branchName);
  }

  public List<SourceControlEvent> getAll() {
    return getList(SELECT_ENTITY);
  }

  @Override
  public final void delete(TransactionContext tx, SourceControlEvent entity) {
    // WARNING: Don't add any business logic to this method because, for performance reasons,
    // we bypass this method when deleting all expired entities.
    super.delete(tx, entity);
  }

  @Override
  public final void delete(SourceControlEvent entity) {
    // WARNING: Don't add any business logic to this method because, for performance reasons,
    // we bypass this method when deleting all expired entities.
    super.delete(entity);
  }

  public void clearEventsAndInsert(SourceControlEvent sourceControlEvent) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      deleteByApplicationId(tx, sourceControlEvent.getApplicationId());
      insert(tx, sourceControlEvent);
      tx.commit();
    }
  }

  public List<SourceControlEvent> getAllByApplicationId(String applicationId) {
    String sQuery = SELECT_ENTITY + "WHERE entity.applicationId =?1";
    return getList(sQuery, applicationId);
  }
}
