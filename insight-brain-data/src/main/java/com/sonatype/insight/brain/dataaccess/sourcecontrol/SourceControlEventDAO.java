/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sourcecontrol;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.nexus.git.utils.GitBranchNameValidator;
import com.sonatype.nexus.git.utils.InvalidBranchNameException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jooq.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.SourceControlEvent.SOURCE_CONTROL_EVENT;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.*;

@Named
@Singleton
public class SourceControlEventDAO
    extends AbstractOperationalSqlDAO<SourceControlEvent>
{
  private static final Logger log = LoggerFactory.getLogger(SourceControlEventDAO.class);

  private static final int DELETE_BATCH_SIZE = 100;

  private static final String AN_INVALID_INSTANCE_ID = "-1";

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
      txn.dsl()
          .update(SOURCE_CONTROL_EVENT)
          .setNull(SOURCE_CONTROL_EVENT.INSTANCE_ID)
          .where(SOURCE_CONTROL_EVENT.SCM_USERNAME.eq(event.getScmUsername()))
          .and(SOURCE_CONTROL_EVENT.EVENT_STATUS.eq(EVENT_STATUS_NEW))
          .execute();
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

      // First check if there are any events reserved for other instances
      List<String> activeStatuses = Arrays.asList(EVENT_STATUS_NEW, EVENT_STATUS_IN_PROGRESS);
      long reservedCount = txn.dsl()
          .selectCount()
          .from(SOURCE_CONTROL_EVENT)
          .where(SOURCE_CONTROL_EVENT.INSTANCE_ID.isNotNull())
          .and(SOURCE_CONTROL_EVENT.INSTANCE_ID.ne(instanceId))
          .and(SOURCE_CONTROL_EVENT.EVENT_STATUS.in(activeStatuses))
          .fetchOne(0, Long.class);

      if (reservedCount == 0) {
        // Get unassigned event IDs
        List<String> unassignedIds = txn.dsl()
            .select(SOURCE_CONTROL_EVENT.SOURCE_CONTROL_EVENT_ID)
            .from(SOURCE_CONTROL_EVENT)
            .where(SOURCE_CONTROL_EVENT.INSTANCE_ID.isNull())
            .forUpdate()
            .fetchInto(String.class);

        if (!unassignedIds.isEmpty()) {
          result = txn.dsl()
              .update(SOURCE_CONTROL_EVENT)
              .set(SOURCE_CONTROL_EVENT.INSTANCE_ID, instanceId)
              .where(SOURCE_CONTROL_EVENT.SOURCE_CONTROL_EVENT_ID.in(unassignedIds))
              .execute();
        }
      }

      txn.commit();
    }
    return result;
  }

  public List<SourceControlEvent> selectUnassignedNewEventsAndAssignToInstance(final String instanceId) {
    List<SourceControlEvent> unassignedEvents;

    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();

      unassignedEvents = tx.dsl()
          .selectFrom(SOURCE_CONTROL_EVENT)
          .where(SOURCE_CONTROL_EVENT.INSTANCE_ID.isNull())
          .and(SOURCE_CONTROL_EVENT.EVENT_STATUS.eq(EVENT_STATUS_NEW))
          .orderBy(SOURCE_CONTROL_EVENT.CREATE_TIME)
          .forUpdate()
          .fetch(this::toEntity);

      unassignedEvents.forEach(event -> {
        event.setInstanceId(instanceId);
        update(tx, event);
      });

      tx.commit();
    }

    return unassignedEvents;
  }

  public List<SourceControlEvent> selectEventsByCriteria(
      final Set<String> applicationIds,
      final Date createdOnOrAfter,
      final boolean ascending,
      final int limit,
      final int offset)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(SOURCE_CONTROL_EVENT)
          .where(SOURCE_CONTROL_EVENT.APPLICATION_ID.in(applicationIds))
          .and(SOURCE_CONTROL_EVENT.CREATE_TIME.ge(createdOnOrAfter))
          .orderBy(ascending ? SOURCE_CONTROL_EVENT.CREATE_TIME.asc() : SOURCE_CONTROL_EVENT.CREATE_TIME.desc())
          .offset(offset)
          .limit(limit)
          .fetch(this::toEntity);
    }
  }

  public List<SourceControlEvent> selectEventsForInstance(final String instanceId, final int quantity) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(SOURCE_CONTROL_EVENT)
          .where(SOURCE_CONTROL_EVENT.INSTANCE_ID.eq(instanceId))
          .and(SOURCE_CONTROL_EVENT.EVENT_STATUS.eq(EVENT_STATUS_NEW))
          .orderBy(SOURCE_CONTROL_EVENT.EVENT_PRIORITY, SOURCE_CONTROL_EVENT.CREATE_TIME)
          .limit(quantity)
          .fetch(this::toEntity);
    }
  }

  public List<SourceControlEvent> getPendingOrInProgressSourceControlEvaluationEvents() {
    List<String> statuses = Arrays.asList(EVENT_STATUS_NEW, EVENT_STATUS_IN_PROGRESS);
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(SOURCE_CONTROL_EVENT)
          .where(SOURCE_CONTROL_EVENT.EVENT_TYPE.eq(SOURCE_CONTROL_EVALUATION_EVENT))
          .and(SOURCE_CONTROL_EVENT.EVENT_STATUS.in(statuses))
          .fetch(this::toEntity);
    }
  }

  public List<SourceControlEvent> getPendingOrInProgressUpdatedPullRequestEvents(
      List<String> appIds,
      int pullRequestNumber)
  {
    List<String> statuses = Arrays.asList(EVENT_STATUS_NEW, EVENT_STATUS_IN_PROGRESS);
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(SOURCE_CONTROL_EVENT)
          .where(SOURCE_CONTROL_EVENT.EVENT_TYPE.eq(UPDATED_PULL_REQUEST_EVENT))
          .and(SOURCE_CONTROL_EVENT.EVENT_STATUS.in(statuses))
          .and(SOURCE_CONTROL_EVENT.APPLICATION_ID.in(appIds))
          .and(SOURCE_CONTROL_EVENT.PULL_REQUEST_NUMBER.eq(pullRequestNumber))
          .fetch(this::toEntity);
    }
  }

  public List<SourceControlEvent> getRemediationEventsForBranch(String applicationId, String branchName) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(SOURCE_CONTROL_EVENT)
          .where(SOURCE_CONTROL_EVENT.APPLICATION_ID.eq(applicationId))
          .and(SOURCE_CONTROL_EVENT.EVENT_TYPE.in(REMEDIATION_EVENT_TYPES))
          .and(SOURCE_CONTROL_EVENT.BRANCH_NAME.eq(branchName))
          .fetch(this::toEntity);
    }
  }

  public void markEventInProgress(final String eventId) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      tx.dsl()
          .update(SOURCE_CONTROL_EVENT)
          .set(SOURCE_CONTROL_EVENT.EVENT_STATUS, EVENT_STATUS_IN_PROGRESS)
          .set(SOURCE_CONTROL_EVENT.START_TIME, new Date())
          .where(SOURCE_CONTROL_EVENT.SOURCE_CONTROL_EVENT_ID.eq(eventId))
          .execute();
      tx.commit();
    }
    log.debug(UPDATED_EVENT_WITH_STATUS, eventId, EVENT_STATUS_IN_PROGRESS);
  }

  public void markEventComplete(final String eventId) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      tx.dsl()
          .update(SOURCE_CONTROL_EVENT)
          .set(SOURCE_CONTROL_EVENT.EVENT_STATUS, EVENT_STATUS_COMPLETE)
          .set(SOURCE_CONTROL_EVENT.COMPLETE_TIME, new Date())
          .where(SOURCE_CONTROL_EVENT.SOURCE_CONTROL_EVENT_ID.eq(eventId))
          .execute();
      tx.commit();
    }
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
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      tx.dsl()
          .update(SOURCE_CONTROL_EVENT)
          .set(SOURCE_CONTROL_EVENT.EVENT_STATUS, eventStatus)
          .set(SOURCE_CONTROL_EVENT.EVENT_STATUS_DETAILS, StringUtils.abbreviate(message, 2048))
          .set(SOURCE_CONTROL_EVENT.EVENT_ERROR_DETAILS, eventErrorDetails)
          .set(SOURCE_CONTROL_EVENT.COMPLETE_TIME, new Date())
          .where(SOURCE_CONTROL_EVENT.SOURCE_CONTROL_EVENT_ID.eq(eventId))
          .execute();
      tx.commit();
    }
    log.debug(UPDATED_EVENT_WITH_STATUS, eventId, eventStatus);
  }

  public void resetStaleEvents(Set<String> activeInstanceIds, int eventsOlderThanSeconds) {
    if (activeInstanceIds.isEmpty()) {
      // the set cannot be empty, so default it to an invalid instance ID, which will cause stale events for any
      // instance to be updated
      activeInstanceIds.add(AN_INVALID_INSTANCE_ID);
    }

    // we ignore events for 'active' instances as well as complete or error events
    long cutoffTimeMs = System.currentTimeMillis() - eventsOlderThanSeconds * 1_000L;
    Date cutoffTime = new Date(cutoffTimeMs);

    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      tx.dsl()
          .update(SOURCE_CONTROL_EVENT)
          .setNull(SOURCE_CONTROL_EVENT.INSTANCE_ID)
          .set(SOURCE_CONTROL_EVENT.EVENT_STATUS, EVENT_STATUS_NEW)
          .where(
              SOURCE_CONTROL_EVENT.INSTANCE_ID.isNotNull()
                  .and(SOURCE_CONTROL_EVENT.INSTANCE_ID.notIn(activeInstanceIds))
                  .and(
                      SOURCE_CONTROL_EVENT.EVENT_STATUS.eq(EVENT_STATUS_NEW)
                          .and(SOURCE_CONTROL_EVENT.CREATE_TIME.lt(cutoffTime))
                          .or(
                              SOURCE_CONTROL_EVENT.EVENT_STATUS.eq(EVENT_STATUS_IN_PROGRESS)
                                  .and(SOURCE_CONTROL_EVENT.START_TIME.lt(cutoffTime))))
                  .or(
                      SOURCE_CONTROL_EVENT.INSTANCE_ID.isNull()
                          .and(SOURCE_CONTROL_EVENT.EVENT_STATUS.eq(EVENT_STATUS_IN_PROGRESS))
                          .and(SOURCE_CONTROL_EVENT.START_TIME.lt(cutoffTime))))
          .execute();
      tx.commit();
    }
  }

  public List<SourceControlEvent> getUnassignedEventsToProcess() {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(SOURCE_CONTROL_EVENT)
          .where(SOURCE_CONTROL_EVENT.INSTANCE_ID.isNull())
          .and(SOURCE_CONTROL_EVENT.EVENT_STATUS.eq(EVENT_STATUS_NEW))
          .orderBy(SOURCE_CONTROL_EVENT.CREATE_TIME)
          .fetch(this::toEntity);
    }
  }

  public void deleteByApplicationId(final String applicationId) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      deleteByApplicationId(tx, applicationId);
      tx.commit();
    }
  }

  public void deleteByApplicationId(final TransactionContext tx, final String applicationId) {
    List<SourceControlEvent> sourceControlEventList = tx.dsl()
        .selectFrom(SOURCE_CONTROL_EVENT)
        .where(SOURCE_CONTROL_EVENT.APPLICATION_ID.eq(applicationId))
        .fetch(this::toEntity);
    for (SourceControlEvent sourceControlEvent : sourceControlEventList) {
      delete(tx, sourceControlEvent);
    }
  }

  public int deleteAllBeforeDate(final Date cutoffDate) {
    int deletedRows = 0;
    while (true) {
      try (TransactionContext tx = createTransactionContext()) {
        List<String> ids = tx.dsl()
            .select(SOURCE_CONTROL_EVENT.SOURCE_CONTROL_EVENT_ID)
            .from(SOURCE_CONTROL_EVENT)
            .where(SOURCE_CONTROL_EVENT.CREATE_TIME.lt(cutoffDate))
            .limit(DELETE_BATCH_SIZE)
            .fetchInto(String.class);
        if (ids.isEmpty()) {
          return deletedRows;
        }
        tx.begin();
        deletedRows += tx.dsl()
            .deleteFrom(SOURCE_CONTROL_EVENT)
            .where(SOURCE_CONTROL_EVENT.SOURCE_CONTROL_EVENT_ID.in(ids))
            .execute();
        tx.commit();
      }
    }
  }

  public boolean hasRemediationEventForBranchAndStatuses(
      String applicationId,
      String branchName,
      String... eventStatuses)
  {
    try (TransactionContext tx = createTransactionContext()) {
      var condition = SOURCE_CONTROL_EVENT.APPLICATION_ID.eq(applicationId)
          .and(SOURCE_CONTROL_EVENT.EVENT_TYPE.in(REMEDIATION_EVENT_TYPES))
          .and(SOURCE_CONTROL_EVENT.BRANCH_NAME.eq(branchName));

      if (eventStatuses.length > 0) {
        condition = condition.and(SOURCE_CONTROL_EVENT.EVENT_STATUS.in(Arrays.asList(eventStatuses)));
      }

      return tx.dsl()
          .selectCount()
          .from(SOURCE_CONTROL_EVENT)
          .where(condition)
          .fetchOne(0, Long.class) > 0;
    }
  }

  public List<SourceControlEvent> getPullRequestStateUpdateEventsForApplication(String applicationId) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(SOURCE_CONTROL_EVENT)
          .where(SOURCE_CONTROL_EVENT.EVENT_TYPE.in(List.of(PR_STATE_UPDATE_EVENT, BATCH_PR_STATE_UPDATE_EVENT)))
          .and(SOURCE_CONTROL_EVENT.APPLICATION_ID.eq(applicationId))
          .fetch(this::toEntity);
    }
  }

  /**
   * Gets completed REMEDIATION_PULL_REQUEST events for a given application and component identifier that were completed
   * before a given time.
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

    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(SOURCE_CONTROL_EVENT)
          .where(SOURCE_CONTROL_EVENT.APPLICATION_ID.eq(applicationId))
          .and(SOURCE_CONTROL_EVENT.EVENT_TYPE.eq(REMEDIATION_PULL_REQUEST_EVENT))
          .and(SOURCE_CONTROL_EVENT.EVENT_STATUS.eq(EVENT_STATUS_COMPLETE))
          .and(SOURCE_CONTROL_EVENT.COMPONENT_ID_FORMAT.eq(format))
          .and(SOURCE_CONTROL_EVENT.COMPONENT_ID_COORDINATES_JSON.eq(coordinatesJson))
          .and(SOURCE_CONTROL_EVENT.COMPLETE_TIME.ge(pullRequestCreationMinCutoffTime))
          .and(SOURCE_CONTROL_EVENT.COMPLETE_TIME.le(pullRequestCreationMaxCutoffTime))
          .fetch(this::toEntity);
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

  public void clearEventsAndInsert(SourceControlEvent sourceControlEvent) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      deleteByApplicationId(tx, sourceControlEvent.getApplicationId());
      insert(tx, sourceControlEvent);
      tx.commit();
    }
  }

  public List<SourceControlEvent> getAllByApplicationId(String applicationId) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(SOURCE_CONTROL_EVENT)
          .where(SOURCE_CONTROL_EVENT.APPLICATION_ID.eq(applicationId))
          .fetch(this::toEntity);
    }
  }

  /**
   * Finds the latest remediation event (PR creation event) for a specific pull request. This is used to retrieve golden
   * status information from the original PR creation.
   */
  public SourceControlEvent getLatestRemediationEventForPullRequest(String applicationId, int pullRequestNumber) {
    try (TransactionContext tx = createTransactionContext()) {
      return toEntity(tx.dsl()
          .selectFrom(SOURCE_CONTROL_EVENT)
          .where(SOURCE_CONTROL_EVENT.APPLICATION_ID.eq(applicationId))
          .and(SOURCE_CONTROL_EVENT.PULL_REQUEST_NUMBER.eq(pullRequestNumber))
          .and(SOURCE_CONTROL_EVENT.EVENT_TYPE.in(REMEDIATION_EVENT_TYPES))
          .orderBy(SOURCE_CONTROL_EVENT.CREATE_TIME.desc())
          .limit(1)
          .fetchOne());
    }
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

  @Override
  public Table<?> getJooqTable() {
    return SOURCE_CONTROL_EVENT;
  }

  @Override
  public Class<SourceControlEvent> getEntityClass() {
    return SourceControlEvent.class;
  }
}
