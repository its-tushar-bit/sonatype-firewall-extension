/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sourcecontrol;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.nexus.git.utils.GitBranchNameValidator;
import com.sonatype.nexus.git.utils.InvalidBranchNameException;

import jakarta.persistence.LockModeType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.BATCH_PR_STATE_UPDATE_EVENT;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.EVENT_STATUS_COMPLETE;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.EVENT_STATUS_ERROR;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.EVENT_STATUS_IN_PROGRESS;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.EVENT_STATUS_NEW;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.EVENT_STATUS_PARTIALLY_COMPLETE;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.MANUAL_REMEDIATION_PULL_REQUEST_EVENT;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.PR_STATE_UPDATE_EVENT;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.SOURCE_CONTROL_EVALUATION_EVENT;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.UPDATED_PULL_REQUEST_EVENT;

@Named
@Singleton
public class SourceControlEventDAO
    extends AbstractOperationalSqlDAO<SourceControlEvent>
{
  private static final Logger log = LoggerFactory.getLogger(SourceControlEventDAO.class);

  private static final int DELETE_BATCH_SIZE = 100;

  private static final String AN_INVALID_INSTANCE_ID = "-1";

  private static final String SELECT_ENTITY = "SELECT entity FROM SourceControlEvent entity ";

  private static final String UPDATE_ENTITY = "UPDATE SourceControlEvent entity ";

  private static final String WHERE_ENTITY_ID_MATCHES = "WHERE entity.id=?1";

  private static final String UPDATED_EVENT_WITH_STATUS = "updated event {} with status {}";

  private static final List<String> REMEDIATION_EVENT_TYPES = List.of(
      REMEDIATION_PULL_REQUEST_EVENT,
      MANUAL_REMEDIATION_PULL_REQUEST_EVENT);

  @Inject
  public SourceControlEventDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  /**
   * The purpose of this method is to release (i.e. unassign) events that are 'related' to the given event. Events are
   * related if they are for the same scm user.
   *
   * @param event
   */
  public void releaseRelatedEvents(SourceControlEvent event) {
    try (TransactionContext txn = createTransactionContext()) {
      txn.begin();

      txn.createNativeQuery(
          "UPDATE " + getDatabaseSchema() + ".source_control_event" +
              " SET instance_id = NULL" +
              " WHERE scm_username = ?1 " +
              "   AND event_status = 'new'")
          .setParameter(1, event.getScmUsername())
          .executeUpdate();
      txn.commit();
    }
  }

  public void reserveEventForInstance(SourceControlEvent event, String instanceId) {
    event.setInstanceId(instanceId);
    update(event);
  }

  public int reserveEventsForInstance(final String instanceId) {
    int result = 0;

    try (TransactionContext txn = createTransactionContext()) {
      txn.begin();

      // assign the given instance ID to any events that aren't already assigned IFF there are no active events
      // (i.e. 'new', 'in progress') already assigned to another instance
      result = txn
          .createNativeQuery(
              "UPDATE " + getDatabaseSchema() + ".source_control_event" +
                  " SET instance_id = ?1" +
                  " WHERE source_control_event_id IN (" +
                  "   SELECT unassigned_events.id FROM (" +
                  "     SELECT source_control_event_id AS id" +
                  "       FROM " + getDatabaseSchema() + ".source_control_event" +
                  "       WHERE instance_id IS NULL" +
                  "       FOR UPDATE" +
                  "     ) AS unassigned_events," +
                  "     (" +
                  "       SELECT count(*) AS reserved_count " +
                  "       FROM " + getDatabaseSchema() + ".source_control_event" +
                  "       WHERE instance_id IS NOT NULL" +
                  "       AND instance_id != ?1" +
                  "       AND event_status IN ('new', 'in progress')" +
                  "     ) AS events_reserved_for_other_instances" +
                  "   WHERE events_reserved_for_other_instances.reserved_count = 0" +
                  " );")
          .setParameter(1, instanceId)
          .executeUpdate();
      txn.commit();
    }
    return result;
  }

  public List<SourceControlEvent> selectUnassignedNewEventsAndAssignToInstance(final String instanceId) {
    List<SourceControlEvent> unassignedEvents;

    String sQuery = SELECT_ENTITY +
        "WHERE entity.instanceId IS NULL AND entity.eventStatus = ?1 " +
        "ORDER BY entity.createTime";
    Query<SourceControlEvent> query = new Query<>(sQuery, EVENT_STATUS_NEW);
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

  @SuppressWarnings("unchecked")
  public List<SourceControlEvent> selectEventsByCriteria(
      final Set<String> applicationIds,
      final Date createdOnOrAfter,
      final boolean ascending,
      final int limit,
      final int offset)
  {
    String sQuery = SELECT_ENTITY +
        "WHERE entity.applicationId IN ?1 AND entity.createTime >= ?2 ORDER BY entity.createTime " +
        (ascending ? "ASC " : "DESC ");
    try (TransactionContext tx = createTransactionContext()) {
      final jakarta.persistence.Query paginationQuery = createPaginationQuery(tx, sQuery, offset, limit);
      paginationQuery.setParameter(1, applicationIds);
      paginationQuery.setParameter(2, createdOnOrAfter);
      return paginationQuery.getResultList();
    }
  }

  public List<SourceControlEvent> selectEventsForInstance(final String instanceId, final int quantity) {
    String sQuery = SELECT_ENTITY +
        "WHERE entity.instanceId = ?1 AND entity.eventStatus = ?2 ORDER BY entity.eventPriority, entity.createTime";
    Query<SourceControlEvent> query =
        new Query<>(sQuery, instanceId, EVENT_STATUS_NEW);
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

  public List<SourceControlEvent> getRemediationEventsForBranch(String applicationId, String branchName) {
    String sQuery =
        SELECT_ENTITY + " WHERE entity.applicationId = ?1 AND entity.eventType IN ?2 AND entity.branchName = ?3";
    return getList(sQuery, applicationId, REMEDIATION_EVENT_TYPES, branchName);
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

  public void markEventHasError(final String eventId, final String errorMessage, Exception eventException) {
    markEventFinishedWithMessage(eventId, errorMessage, EVENT_STATUS_ERROR, eventException);
  }

  public void markEventPartiallyComplete(final String eventId, final String message, Exception eventException) {
    markEventFinishedWithMessage(eventId, message, EVENT_STATUS_PARTIALLY_COMPLETE, eventException);
  }

  private void markEventFinishedWithMessage(
      final String eventId,
      final String message,
      final String eventStatus,
      Exception eventException)
  {
    String eventErrorDetails = eventException == null ? null : ExceptionUtils.getStackTrace(eventException);
    String sQuery = UPDATE_ENTITY + //
        "SET entity.eventStatus=?2, entity.eventStatusDetails=?3, " + //
        "entity.eventErrorDetails=?4, entity.completeTime=?5 " + //
        WHERE_ENTITY_ID_MATCHES;
    createQuery(sQuery, eventId, eventStatus, StringUtils.abbreviate(message, 2048), eventErrorDetails,
        new Timestamp(System.currentTimeMillis())).executeUpdate();
    log.debug(UPDATED_EVENT_WITH_STATUS, eventId, eventStatus);
  }

  public void resetStaleEvents(Set<String> activeInstanceIds, int eventsOlderThanSeconds) {
    if (activeInstanceIds.isEmpty()) {
      // the set cannot be empty, so default it to an invalid instance ID, which will cause stale events for any
      // instance to be updated
      activeInstanceIds.add(AN_INVALID_INSTANCE_ID);
    }

    // we ignore events for 'active' instances as well as complete or error events
    String sQuery = UPDATE_ENTITY +
        "SET entity.instanceId = null, entity.eventStatus = 'new' " +
        "WHERE (entity.instanceId IS NOT NULL " +
        "  AND NOT entity.instanceId IN ?1 " +
        "  AND (" +
        "    (entity.eventStatus = 'new' AND entity.createTime < ?2) " +
        "      OR " +
        "    (entity.eventStatus = 'in progress' AND entity.startTime < ?2)" +
        "  )) " +
        "OR ( " +
        "  entity.instanceId IS NULL AND entity.eventStatus = 'in progress' AND entity.startTime < ?2" +
        ")";
    long cutoffTimeMs = System.currentTimeMillis() - eventsOlderThanSeconds * 1_000L;
    Date cutoffTime = new Date(cutoffTimeMs);
    createQuery(sQuery, activeInstanceIds, cutoffTime).executeUpdate();
  }

  public List<SourceControlEvent> getUnassignedEventsToProcess() {
    String sQuery = SELECT_ENTITY + "WHERE entity.instanceId IS NULL AND entity.eventStatus = 'new' " +
        "ORDER BY entity.createTime";
    return getList(sQuery);
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

  public boolean hasRemediationEventForBranchAndStatuses(
      String applicationId,
      String branchName,
      String... eventStatuses)
  {
    String sQuery = "SELECT count(entity) FROM SourceControlEvent entity" +
        " WHERE entity.applicationId = ?1" +
        " AND entity.eventType IN ?2" +
        " AND entity.branchName = ?3";
    List<Object> params = new ArrayList<>();
    params.add(applicationId);
    params.add(REMEDIATION_EVENT_TYPES);
    params.add(branchName);
    if (eventStatuses.length > 0) {
      sQuery += " AND entity.eventStatus IN ?4";
      params.add(Arrays.asList(eventStatuses));
    }
    return 0 != getSingle(Long.class, sQuery, params.toArray());
  }

  public List<SourceControlEvent> getPullRequestStateUpdateEventsForApplication(String applicationId) {
    String sQuery = """
        SELECT entity
        FROM SourceControlEvent entity
        WHERE entity.eventType IN ?1 AND entity.applicationId = ?2
        """;

    return getList(sQuery, List.of(PR_STATE_UPDATE_EVENT, BATCH_PR_STATE_UPDATE_EVENT), applicationId);
  }

  /**
   * Gets completed REMEDIATION_PULL_REQUEST events for a given application and component identifier
   * that were completed before a given time.
   *
   * @param applicationId The ID of the application
   * @param componentIdentifier The component identifier to check
   * @param pullRequestCreationMaxCutoffTime The cutoff time - events must be completed before this time
   * @return A list of matching events
   */
  public List<SourceControlEvent> getCompletedRemediationPullRequestEventsForAppComponent(
      String applicationId,
      ComponentIdentifier componentIdentifier,
      Date pullRequestCreationMinCutoffTime,
      Date pullRequestCreationMaxCutoffTime)
  {
    // Convert the component identifier to its database representation
    String format = componentIdentifier.getFormat();
    String coordinatesJson = ComponentIdentifierAdapter.toJson(componentIdentifier.getCoordinates());

    String sQuery = SELECT_ENTITY + """
        WHERE entity.applicationId = :appId
        AND entity.eventType = :eventType
        AND entity.eventStatus = :eventStatus
        AND entity.componentIdFormat = :format
        AND entity.componentIdCoordinatesJson = :coordinates
        AND entity.completeTime >= :pullRequestCreationMinCutoffTime
        AND entity.completeTime <= :pullRequestCreationMaxCutoffTime""";

    try (TransactionContext tx = createTransactionContext()) {
      jakarta.persistence.Query query = tx.createQuery(sQuery);
      query.setParameter("appId", applicationId);
      query.setParameter("eventType", REMEDIATION_PULL_REQUEST_EVENT);
      query.setParameter("eventStatus", EVENT_STATUS_COMPLETE);
      query.setParameter("format", format);
      query.setParameter("coordinates", coordinatesJson);
      query.setParameter("pullRequestCreationMinCutoffTime", pullRequestCreationMinCutoffTime);
      query.setParameter("pullRequestCreationMaxCutoffTime", pullRequestCreationMaxCutoffTime);

      return query.getResultList();
    }
  }

  public boolean hasRemediationEventForBranch(String applicationId, String branchName) {
    return hasRemediationEventForBranchAndStatuses(applicationId, branchName);
  }

  public boolean hasWaitingOrCompleteRemediationEvent(String applicationId, String branchName) {
    return hasRemediationEventForBranchAndStatuses(
        applicationId,
        branchName,
        SourceControlEvent.EVENT_STATUS_NEW,
        SourceControlEvent.EVENT_STATUS_IN_PROGRESS,
        SourceControlEvent.EVENT_STATUS_COMPLETE);
  }

  @Override
  public final void delete(TransactionContext tx, SourceControlEvent entity) {
    // WARNING: Don't add any business logic to this method because, for performance reasons,
    // we bypass this method when deleting related entities.
    super.delete(tx, entity);
  }

  @Override
  public final void delete(SourceControlEvent entity) {
    // WARNING: Don't add any business logic to this method because, for performance reasons,
    // we bypass this method when deleting related entities.
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

  /**
   * Finds the latest remediation event (PR creation event) for a specific pull request.
   * This is used to retrieve golden status information from the original PR creation.
   */
  public SourceControlEvent getLatestRemediationEventForPullRequest(String applicationId, int pullRequestNumber) {
    String sQuery = SELECT_ENTITY +
        "WHERE entity.applicationId = ?1 AND entity.pullRequestNumber = ?2 " +
        "AND entity.eventType IN ?3 " +
        "ORDER BY entity.createTime DESC";

    Query<SourceControlEvent> query = new Query<>(sQuery, applicationId, pullRequestNumber, REMEDIATION_EVENT_TYPES);
    query.setMaxResults(1);
    List<SourceControlEvent> events = query.getList();
    return events.isEmpty() ? null : events.get(0);
  }

  @Override
  public void insert(TransactionContext tx, SourceControlEvent entity) {
    validateBranchName(entity.getBranchName());
    validateBranchName(entity.getBaseBranchName());
    super.insert(tx, entity);
  }

  @Override
  public void update(TransactionContext tx, SourceControlEvent entity) {
    validateBranchName(entity.getBranchName());
    validateBranchName(entity.getBaseBranchName());
    super.update(tx, entity);
  }

  private static void validateBranchName(String branchName) {
    if (branchName == null) {
      return;
    }

    try {
      GitBranchNameValidator.validate(branchName);
    }
    catch (InvalidBranchNameException e) {
      throw new BadRequestException(e.getMessage(), e);
    }
  }
}
