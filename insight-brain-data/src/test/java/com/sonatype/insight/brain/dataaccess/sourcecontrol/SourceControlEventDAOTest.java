/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sourcecontrol;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.error.exception.BadRequestException;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.EVENT_STATUSES;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.EVENT_STATUS_COMPLETE;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.EVENT_STATUS_ERROR;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.EVENT_STATUS_IN_PROGRESS;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.EVENT_STATUS_NEW;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.EVENT_STATUS_PARTIALLY_COMPLETE;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.SOURCE_CONTROL_EVALUATION_EVENT;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.UPDATED_PULL_REQUEST_EVENT;
import static java.lang.System.currentTimeMillis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SourceControlEventDAOTest
    extends AbstractDbDAOTest
{
  private SourceControlEventDAO sourceControlEventDAO;

  private Application app;

  private Application app2;

  private Date testStartTime;

  @Override
  @Before
  public void setup() {
    sourceControlEventDAO = daoFactory.createSourceControlEventDAO();

    app = tempEntity.newApplicationWithParent();
    app2 = tempEntity.newApplicationWithParent();
    testStartTime = toDate(LocalDateTime.now().minusSeconds(1));
  }

  @After
  public void cleanup() {
    sourceControlEventDAO.getAll().stream().forEach(sourceControlEventDAO::delete);
  }

  @Test
  public void testInsert() {
    // give a new source control event
    SourceControlEvent sourceControlEvent = getNewSourceControlEvent();
    assertThat(sourceControlEvent.getId()).isNull();

    // when we insert it into the database
    sourceControlEventDAO.insert(sourceControlEvent);

    // then it is persisted with a new id
    assertThat(sourceControlEvent.getId()).isNotNull();
  }

  @Test
  public void testInsert_InvalidBaseBranchName() {
    SourceControlEvent sourceControlEvent = getNewSourceControlEvent();
    sourceControlEvent.setBaseBranchName("/testBaseBranch");
    assertThatThrownBy(() -> {
      sourceControlEventDAO.insert(sourceControlEvent);
    }).isInstanceOf(BadRequestException.class)
        .hasMessageContaining("The branch name is invalid: cannot begin with a slash.");
  }

  @Test
  public void testInsert_InvalidBranchName() {
    SourceControlEvent sourceControlEvent = getNewSourceControlEvent();
    sourceControlEvent.setBranchName("/testBranch");
    assertThatThrownBy(() -> {
      sourceControlEventDAO.insert(sourceControlEvent);
    }).isInstanceOf(BadRequestException.class)
        .hasMessageContaining("The branch name is invalid: cannot begin with a slash.");
  }

  @Test
  public void testUpdate_InvalidBaseBranchName() {
    SourceControlEvent sourceControlEvent = getNewSourceControlEvent();
    sourceControlEventDAO.insert(sourceControlEvent);
    sourceControlEvent.setBaseBranchName("/testBaseBranch");
    assertThatThrownBy(() -> {
      sourceControlEventDAO.update(sourceControlEvent);
    }).isInstanceOf(BadRequestException.class)
        .hasMessageContaining("The branch name is invalid: cannot begin with a slash.");
  }

  @Test
  public void testUpdate_InvalidBranchName() {
    SourceControlEvent sourceControlEvent = getNewSourceControlEvent();
    sourceControlEventDAO.insert(sourceControlEvent);
    sourceControlEvent.setBranchName("/testBranch");
    assertThatThrownBy(() -> {
      sourceControlEventDAO.update(sourceControlEvent);
    }).isInstanceOf(BadRequestException.class)
        .hasMessageContaining("The branch name is invalid: cannot begin with a slash.");
  }

  @Test
  public void testNewInstanceDefaultsToEventStatusNew() {
    // given a new source control event
    SourceControlEvent sourceControlEvent = new SourceControlEvent();

    // then the initial event status is new
    assertThat(sourceControlEvent.getEventStatus()).isEqualTo(EVENT_STATUS_NEW);
  }

  private static class StaleEventTestData
  {
    static final int staleEventCutoffSeconds = 60;

    static final long cutoffTimeMs = currentTimeMillis() - staleEventCutoffSeconds * 1_000L;

    static final Date beforeCutoff = new Date(cutoffTimeMs - 30_000L);

    static final Date beforeCutoff2 = new Date(cutoffTimeMs - 20_000L);

    static final Date beforeCutoff3 = new Date(cutoffTimeMs - 10_000L);

    static final Date afterCutoff = new Date(cutoffTimeMs + 10_000L);

    static final String activeInstanceId = "activeInstance";

    static final String inactiveInstanceId = "inactiveInstance";

    static final Set<String> activeInstanceIds = ImmutableSet.of(activeInstanceId, "activeInstance2");

    static final Set<String> resettableStatuses = ImmutableSet.of(EVENT_STATUS_NEW, EVENT_STATUS_IN_PROGRESS);
  }

  @Test
  public void testResetStaleEvents() {
    // check all the permutations and combinations of event status, staleness, and instance state
    for (String eventStatus : EVENT_STATUSES) {
      for (boolean isStaleEvent : new boolean[]{false, true}) {
        for (boolean isUnassigned : new boolean[]{false, true}) {
          for (boolean isActiveInstance : new boolean[]{false, true}) {
            testResetStaleEvent(eventStatus, isStaleEvent, isUnassigned, isActiveInstance);
          }
        }
      }
    }
  }

  private void testResetStaleEvent(
      String eventStatus,
      boolean isStaleEvent,
      boolean isUnassigned,
      boolean isActiveInstance)
  {
    // given: an event that satisfies the given parameters
    SourceControlEvent event = getNewSourceControlEvent()
        .setEventStatus(eventStatus)
        .setInstanceId(isUnassigned ? null
            : (isActiveInstance ? StaleEventTestData.activeInstanceId : StaleEventTestData.inactiveInstanceId));

    switch (eventStatus) {
      case EVENT_STATUS_COMPLETE:
      case EVENT_STATUS_PARTIALLY_COMPLETE:
      case EVENT_STATUS_ERROR:
        event.setCreateTime(StaleEventTestData.beforeCutoff);
        event.setStartTime(StaleEventTestData.beforeCutoff2);
        event.setCompleteTime(isStaleEvent ? StaleEventTestData.beforeCutoff3 : StaleEventTestData.afterCutoff);
        break;

      case EVENT_STATUS_IN_PROGRESS:
        event.setCreateTime(StaleEventTestData.beforeCutoff);
        event.setStartTime(isStaleEvent ? StaleEventTestData.beforeCutoff2 : StaleEventTestData.afterCutoff);
        break;

      case EVENT_STATUS_NEW:
        event.setCreateTime(isStaleEvent ? StaleEventTestData.beforeCutoff : StaleEventTestData.afterCutoff);
        break;

      default:
        throw new IllegalArgumentException("Invalid event status " + eventStatus);
    }
    sourceControlEventDAO.insert(event);

    // when:
    sourceControlEventDAO.resetStaleEvents(
        StaleEventTestData.activeInstanceIds,
        StaleEventTestData.staleEventCutoffSeconds);

    // then: fetched event matches expectations
    SourceControlEvent fetchedEvent = sourceControlEventDAO.getById(event.getId());
    assertThat(fetchedEvent).isNotNull();

    // stale 'new' and 'in progress' events for non-active instances should be reset
    // as well as stale unassigned events stuck 'in progress'
    final boolean expectReset = isStaleEvent && (
        isInactiveAndResettable(isActiveInstance, eventStatus) || isUnassignedInProgress(isUnassigned, eventStatus));

    if (expectReset) {
      assertThat(fetchedEvent.getEventStatus()).isEqualTo(EVENT_STATUS_NEW);
      assertThat(fetchedEvent.getInstanceId()).isNull();
    }
    else {
      assertThat(fetchedEvent.getEventStatus()).isEqualTo(event.getEventStatus());
      assertThat(fetchedEvent.getInstanceId()).isEqualTo(event.getInstanceId());
    }
  }

  private boolean isInactiveAndResettable(boolean isActiveInstance, String eventStatus) {
    return !isActiveInstance && StaleEventTestData.resettableStatuses.contains(eventStatus);
  }

  private boolean isUnassignedInProgress(boolean isUnassigned, String eventStatus) {
    return isUnassigned && EVENT_STATUS_IN_PROGRESS.equalsIgnoreCase(eventStatus);
  }

  @Test
  public void testReserveEventsForInstance_exclusive() {
    // given: an event
    createNewSourceControlEvents(2);

    // when: reserve events
    sourceControlEventDAO.reserveEventsForInstance("instance1");

    // then: events should be reserved for instance1
    List<SourceControlEvent> sourceControlEventList = sourceControlEventDAO.selectEventsForInstance("instance1", 5);
    assertThat(sourceControlEventList).hasSize(2);
    sourceControlEventList.forEach(event -> assertThat(event.getInstanceId()).isEqualTo("instance1"));

    // when: add some more new events
    createNewSourceControlEvents(3);

    // then: should not be able to reserve events for instance2
    assertThat(sourceControlEventDAO.reserveEventsForInstance("instance2")).isEqualTo(0);

    // when: reserve events for instance1
    sourceControlEventDAO.reserveEventsForInstance("instance1");

    // then: all events are for instance1
    sourceControlEventList = sourceControlEventDAO.selectEventsForInstance("instance1", 10);
    assertThat(sourceControlEventList).hasSize(5);
    sourceControlEventList.forEach(event -> assertThat(event.getInstanceId()).isEqualTo("instance1"));
  }

  @Test
  public void testReleaseRelatedEvents() {
    // given: a number of events associated with various SCM users and instances
    List<SourceControlEvent> events = new ArrayList<>();
    events.add(createScmUserEvent("instance1", "user1", EVENT_STATUS_NEW));
    events.add(createScmUserEvent("instance3", "user1", EVENT_STATUS_NEW));
    events.add(createScmUserEvent("instance4", "user1", EVENT_STATUS_NEW));
    events.add(createScmUserEvent("instance2", "user1", EVENT_STATUS_IN_PROGRESS));
    events.add(createScmUserEvent("instance3", "user1", EVENT_STATUS_PARTIALLY_COMPLETE));
    events.add(createScmUserEvent("instance4", "user1", EVENT_STATUS_COMPLETE));
    events.add(createScmUserEvent("instance5", "user1", EVENT_STATUS_ERROR));
    events.add(createScmUserEvent("instance1", "user2", EVENT_STATUS_NEW));
    events.add(createScmUserEvent("instance2", "user2", EVENT_STATUS_IN_PROGRESS));
    events.add(createScmUserEvent("instance3", "user2", EVENT_STATUS_PARTIALLY_COMPLETE));
    events.add(createScmUserEvent("instance4", "user2", EVENT_STATUS_COMPLETE));
    events.add(createScmUserEvent("instance5", "user2", EVENT_STATUS_ERROR));

    // when: release related events for user1
    sourceControlEventDAO.releaseRelatedEvents(events.get(0));

    // then: 'new' events associated with the given SCM user have been unassigned to an instance
    for (SourceControlEvent event : events) {
      SourceControlEvent fetchedEvent = sourceControlEventDAO.getById(event.getId());
      assertThat(fetchedEvent.getEventStatus()).isEqualTo(event.getEventStatus());
      assertThat(fetchedEvent.getScmUsername()).isEqualTo(event.getScmUsername());

      if ("user1".equalsIgnoreCase(event.getScmUsername())
          && EVENT_STATUS_NEW.equalsIgnoreCase(event.getEventStatus())) {
        assertThat(fetchedEvent.getInstanceId()).isNull();
      }
      else {
        assertThat(fetchedEvent.getInstanceId()).isEqualTo(event.getInstanceId());
      }
    }
  }

  private SourceControlEvent createScmUserEvent(String instanceId, String scmUsername, String eventStatus) {
    SourceControlEvent event = getNewSourceControlEvent()
        .setInstanceId(instanceId)
        .setScmUsername(scmUsername)
        .setEventStatus(eventStatus)
        .setCreateTime(new Date());
    sourceControlEventDAO.insert(event);
    return event;
  }

  @Test
  public void testReserveEventForInstance_unassigned() {
    // given: an unassigned event
    SourceControlEvent event = createScmUserEvent(null, "user1", EVENT_STATUS_NEW);

    // when:
    sourceControlEventDAO.reserveEventForInstance(event, "instance1");

    // then: the event was reserved
    SourceControlEvent fetchedEvent = sourceControlEventDAO.getById(event.getId());
    assertThat(fetchedEvent.getInstanceId()).isEqualTo("instance1");
    assertThat(fetchedEvent.getEventStatus()).isEqualTo(EVENT_STATUS_NEW);
  }

  @Test
  public void testReserveEventForInstance_alreadyAssigned() {
    // given: an unassigned event
    SourceControlEvent event = createScmUserEvent("instance1", "user1", EVENT_STATUS_NEW);

    // when:
    sourceControlEventDAO.reserveEventForInstance(event, "instance2");

    // then: the event was reserved
    SourceControlEvent fetchedEvent = sourceControlEventDAO.getById(event.getId());
    assertThat(fetchedEvent.getInstanceId()).isEqualTo("instance2");
    assertThat(fetchedEvent.getEventStatus()).isEqualTo(EVENT_STATUS_NEW);
  }

  @Test
  public void testReserveEventsForInstance_eventPriority() {
    // given: a set of prioritized events
    createNewPrioritizedSourceControlEvents(app.getId(), 2, 2, 1, 3, 2);

    // when: reserve the events
    int reserved = sourceControlEventDAO.reserveEventsForInstance("instance-1");

    // then:
    assertThat(reserved).isEqualTo(5);
    List<SourceControlEvent> events = sourceControlEventDAO.selectEventsForInstance("instance-1", 5);
    int priority = 0;
    for (SourceControlEvent event : events) {
      assertThat(event.getEventPriority()).isGreaterThanOrEqualTo(priority);
      priority = event.getEventPriority();
    }
  }

  @Test
  public void testSelectUnassignedNewEventsAndAssignToInstance() {
    // given: events in all combinations of type, status and instance assignment
    List<String> instanceIds = new ArrayList<>();
    instanceIds.add("instance-1");
    instanceIds.add(null);
    AtomicInteger expectedEventCount = new AtomicInteger();

    SourceControlEvent.EVENT_TYPES.forEach(type -> {
      EVENT_STATUSES.forEach(status -> {
        instanceIds.forEach(instanceId -> {
          boolean expectAssignment = status.equals(EVENT_STATUS_NEW) && instanceId == null;
          // using the status details field to record whether or not we expect the event to be assigned
          createNewEvent(app.getId(), type, status, instanceId, Boolean.toString(expectAssignment));
          if (expectAssignment) {
            expectedEventCount.getAndIncrement();
          }
        });
      });
    });

    List<SourceControlEvent> unassignedEvents =
        sourceControlEventDAO.selectUnassignedNewEventsAndAssignToInstance("instance-2");

    assertThat(unassignedEvents.size()).isPositive();
    assertThat(unassignedEvents.size()).isEqualTo(expectedEventCount.get());
    unassignedEvents.forEach(event -> {
      assertThat(event.getEventStatusDetails()).isEqualTo(Boolean.TRUE.toString());
      assertThat(event.getEventStatus()).isEqualTo(EVENT_STATUS_NEW);
      assertThat(event.getInstanceId()).isEqualTo("instance-2");
    });
  }

  @Test
  public void testMarkEventInProgress() {
    // given 4 new source control events
    createNewSourceControlEvents(4);

    // when we pull events for processing
    sourceControlEventDAO.reserveEventsForInstance("1");
    SourceControlEvent sourceControlEvent = sourceControlEventDAO.selectEventsForInstance("1", 1).get(0);
    sourceControlEventDAO.markEventInProgress(sourceControlEvent.getId());

    // then the event is marked as in progress with a start time
    SourceControlEvent sourceControlEventById = sourceControlEventDAO.getById(sourceControlEvent.getId());
    assertThat(sourceControlEventById.getEventStatus()).isEqualTo(SourceControlEvent.EVENT_STATUS_IN_PROGRESS);
    assertThat(sourceControlEventById.getStartTime()).isAfter(testStartTime);
  }

  @Test
  public void testMarkEventCompleted() {
    // given 4 new source control events
    createNewSourceControlEvents(4);

    // when we mark an event as complete
    sourceControlEventDAO.reserveEventsForInstance("1");
    SourceControlEvent sourceControlEvent = sourceControlEventDAO.selectEventsForInstance("1", 1).get(0);
    sourceControlEventDAO.markEventComplete(sourceControlEvent.getId());

    // then the event is marked as complete with a complete time
    SourceControlEvent sourceControlEventById = sourceControlEventDAO.getById(sourceControlEvent.getId());
    assertThat(sourceControlEventById.getEventStatus()).isEqualTo(SourceControlEvent.EVENT_STATUS_COMPLETE);
    assertThat(sourceControlEventById.getCompleteTime()).isAfter(testStartTime);
  }

  @Test
  public void testMarkEventHasError_NullException() {
    testMarkEventHasError(null);
  }

  @Test
  public void testMarkEventHasError_NotNullException() {
    testMarkEventHasError(new Exception("test error message"));
  }

  private void testMarkEventHasError(Exception testException) {
    // given 4 new source control events
    createNewSourceControlEvents(4);

    // when we mark an event with an error
    sourceControlEventDAO.reserveEventsForInstance("1");
    SourceControlEvent sourceControlEvent = sourceControlEventDAO.selectEventsForInstance("1", 1).get(0);
    sourceControlEventDAO.markEventHasError(sourceControlEvent.getId(), "error message", testException);

    // then the event is marked with the error message and a complete time
    SourceControlEvent sourceControlEventById = sourceControlEventDAO.getById(sourceControlEvent.getId());
    assertThat(sourceControlEventById.getEventStatus()).isEqualTo(SourceControlEvent.EVENT_STATUS_ERROR);
    assertThat(sourceControlEventById.getEventStatusDetails()).isEqualTo("error message");
    assertThat(sourceControlEventById.getCompleteTime()).isAfter(testStartTime);
    if (testException == null) {
      assertThat(sourceControlEventById.getEventErrorDetails()).isNull();
    }
    else {
      assertThat(sourceControlEventById.getEventErrorDetails()).isEqualTo(ExceptionUtils.getStackTrace(testException));
    }
  }

  @Test
  public void testMarkEventPartiallyComplete_NullException() {
    testMarkEventPartiallyComplete(null);
  }

  @Test
  public void testMarkEventPartiallyComplete_NotNullException() {
    testMarkEventPartiallyComplete(new Exception("test error message"));
  }

  private void testMarkEventPartiallyComplete(Exception testException) {
    // given 4 new source control events
    createNewSourceControlEvents(4);

    // when we mark an event with an error
    sourceControlEventDAO.reserveEventsForInstance("1");
    SourceControlEvent sourceControlEvent = sourceControlEventDAO.selectEventsForInstance("1", 1).get(0);
    sourceControlEventDAO.markEventPartiallyComplete(sourceControlEvent.getId(), "informational message",
        testException);

    // then the event is marked with partial completion message and a complete time
    SourceControlEvent sourceControlEventById = sourceControlEventDAO.getById(sourceControlEvent.getId());
    assertThat(sourceControlEventById.getEventStatus()).isEqualTo(EVENT_STATUS_PARTIALLY_COMPLETE);
    assertThat(sourceControlEventById.getEventStatusDetails()).isEqualTo("informational message");
    assertThat(sourceControlEventById.getCompleteTime()).isAfter(testStartTime);
    if (testException == null) {
      assertThat(sourceControlEventById.getEventErrorDetails()).isNull();
    }
    else {
      assertThat(sourceControlEventById.getEventErrorDetails()).isEqualTo(ExceptionUtils.getStackTrace(testException));
    }
  }

  public void testDeleteByApplicationId() {
    // when we have events for different application ids
    createNewSourceControlEvents(2);
    Application app2 = tempEntity.newApplicationWithParent();
    createNewSourceControlEvents(2, app2.getId());

    // when we delete the events by application id
    assertThat(sourceControlEventDAO.getAll()).hasSize(4);
    sourceControlEventDAO.deleteByApplicationId(app.getId());

    // then events are deleted for the specified application
    assertThat(sourceControlEventDAO.getAll()).hasSize(2);
  }

  @Test
  public void testDeleteBeforeDate() {
    // when we add source control events
    Date threeDaysEarlier = toDate(LocalDateTime.now().minusDays(3));
    Date twoDaysEarlier = toDate(LocalDateTime.now().minusDays(2));
    Date oneDayEarlier = toDate(LocalDateTime.now().minusDays(1));
    sourceControlEventDAO.insert(getNewSourceControlEvent().setCreateTime(threeDaysEarlier));
    sourceControlEventDAO.insert(getNewSourceControlEvent().setCreateTime(threeDaysEarlier));
    sourceControlEventDAO.insert(getNewSourceControlEvent().setCreateTime(threeDaysEarlier));
    sourceControlEventDAO.insert(getNewSourceControlEvent().setCreateTime(threeDaysEarlier));
    sourceControlEventDAO.insert(getNewSourceControlEvent().setCreateTime(oneDayEarlier));
    sourceControlEventDAO.insert(getNewSourceControlEvent().setCreateTime(oneDayEarlier));
    sourceControlEventDAO.insert(getNewSourceControlEvent());

    // when we delete events that are two days old
    sourceControlEventDAO.deleteAllBeforeDate(twoDaysEarlier);

    // then only new events still exist
    assertThat(sourceControlEventDAO.getAll()).hasSize(3);
  }

  @Test
  public void testGetUnassignedEventsToProcess() {
    // given: a number of events, all assigned
    List<SourceControlEvent> eventList = new ArrayList<>();

    for (String eventStatus : EVENT_STATUSES) {
      SourceControlEvent event = getNewSourceControlEvent().setInstanceId("someInstance").setEventStatus(eventStatus);
      sourceControlEventDAO.insert(event);
      eventList.add(event);
    }

    // when:
    List<SourceControlEvent> unassignedEventsToProcess = sourceControlEventDAO.getUnassignedEventsToProcess();

    // then: at this point all events are assigned, so unassigned should be empty
    assertThat(unassignedEventsToProcess).isEmpty();

    // when: update events to unassigned and refetch unassigned
    for (SourceControlEvent event : eventList) {
      event.setInstanceId(null);
      sourceControlEventDAO.update(event);
    }
    unassignedEventsToProcess = sourceControlEventDAO.getUnassignedEventsToProcess();

    // then: only the event with status 'new' should be picked up for processing
    assertThat(unassignedEventsToProcess).hasSize(1);
    assertThat(unassignedEventsToProcess.get(0).getEventStatus()).isEqualTo(EVENT_STATUS_NEW);
  }

  @Test
  public void testGetPendingOrInProgressSourceControlEvaluationEvents() {
    // given: a set of events, some we're interested in and some not
    createNewSourceControlEvaluationEvent(app.getId(), EVENT_STATUS_NEW);
    createNewSourceControlEvaluationEvent(app2.getId(), EVENT_STATUS_IN_PROGRESS);
    createNewSourceControlEvaluationEvent(app.getId(), EVENT_STATUS_COMPLETE);
    createNewSourceControlEvaluationEvent(app2.getId(), EVENT_STATUS_ERROR);
    createNewSourceControlEvents(3);

    // when:  get new and in progress events
    List<SourceControlEvent> events = sourceControlEventDAO.getPendingOrInProgressSourceControlEvaluationEvents();

    // then:
    assertThat(events).isNotEmpty();
    assertThat(events.size()).isEqualTo(2);
    List<String> pendingEventStatus =
        Collections.unmodifiableList(Arrays.asList(EVENT_STATUS_NEW, EVENT_STATUS_IN_PROGRESS));
    events.forEach(event -> {
      assertThat(pendingEventStatus).contains(event.getEventStatus());
      assertThat(event.getEventType()).isEqualTo(SOURCE_CONTROL_EVALUATION_EVENT);
    });
  }

  @Test
  public void testHasRemediationEventForBranch() {
    // given: no events yet
    final String branchName = "abc/org/repo";

    // then: remediation event for branch doesn't exists
    assertThat(sourceControlEventDAO.hasRemediationEventForBranch(app.getId(), branchName)).isFalse();

    // when: create an event that's not a remediation event and not for the given branch
    sourceControlEventDAO.insert(getNewSourceControlEvent());

    // then: still doesn't exist
    assertThat(sourceControlEventDAO.hasRemediationEventForBranch(app.getId(), branchName)).isFalse();

    // when: create a remediation event, but not for the given branch
    SourceControlEvent event = getNewSourceControlEvent();
    event.setEventType(SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT);
    sourceControlEventDAO.insert(event);

    // then: still doesn't exist
    assertThat(sourceControlEventDAO.hasRemediationEventForBranch(app.getId(), branchName)).isFalse();

    // when: create a non-remediation event for the given branch
    event = getNewSourceControlEvent();
    event.setBranchName("some/other/branch");
    sourceControlEventDAO.insert(event);

    // then: still doesn't exist
    assertThat(sourceControlEventDAO.hasRemediationEventForBranch(app.getId(), branchName)).isFalse();

    // when: create a remediation event for the given branch
    event = getNewSourceControlEvent();
    event.setBranchName(branchName);
    event.setEventType(SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT);
    sourceControlEventDAO.insert(event);

    // then: still doesn't exist
    assertThat(sourceControlEventDAO.hasRemediationEventForBranch(app.getId(), branchName)).isTrue();
  }

  @Test
  public void testHasRemediationEventForBranch_Manual() {
    final String branchName = "manual/fix/branch";

    assertThat(sourceControlEventDAO.hasRemediationEventForBranch(app.getId(), branchName)).isFalse();

    // when: create an event that's not a manual pull request event and not for the given branch
    sourceControlEventDAO.insert(getNewSourceControlEvent());

    // then: still doesn't exist
    assertThat(sourceControlEventDAO.hasRemediationEventForBranch(app.getId(), branchName)).isFalse();

    // when: create a manual remediation event, but not for the given branch
    SourceControlEvent event = getNewSourceControlEvent();
    event.setEventType(SourceControlEvent.MANUAL_REMEDIATION_PULL_REQUEST_EVENT);
    sourceControlEventDAO.insert(event);

    // then: still doesn't exist
    assertThat(sourceControlEventDAO.hasRemediationEventForBranch(app.getId(), branchName)).isFalse();

    // when: create a non-remediation event for the given branch
    event = getNewSourceControlEvent();
    event.setBranchName(branchName);
    sourceControlEventDAO.insert(event);

    // then: still doesn't exist
    assertThat(sourceControlEventDAO.hasRemediationEventForBranch(app.getId(), branchName)).isFalse();

    // when: create a regular remediation event for the given branch
    event = getNewSourceControlEvent();
    event.setBranchName(branchName);
    event.setEventType(SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT);
    sourceControlEventDAO.insert(event);

    // then: should already exist
    assertThat(sourceControlEventDAO.hasRemediationEventForBranch(app.getId(), branchName)).isTrue();

    //when: create a manual remediation event for the given branch
    sourceControlEventDAO.delete(event);
    event = getNewSourceControlEvent();
    event.setBranchName(branchName);
    event.setEventType(SourceControlEvent.MANUAL_REMEDIATION_PULL_REQUEST_EVENT);
    sourceControlEventDAO.insert(event);

    // then: should exist
    assertThat(sourceControlEventDAO.hasRemediationEventForBranch(app.getId(), branchName)).isTrue();
  }

  @Test
  public void testHasRemediationEventForBranchAndStatuses_Single() {
    // given: no events yet
    final String branchName = "abc/org/repo";

    // then: remediation event for branch doesn't exists
    assertThat(sourceControlEventDAO.hasRemediationEventForBranchAndStatuses(app.getId(), branchName, EVENT_STATUS_NEW))
        .isFalse();

    // when: create an event that's not a remediation event and not for the given branch
    sourceControlEventDAO.insert(getNewSourceControlEvent());

    // then: still doesn't exist
    assertThat(sourceControlEventDAO.hasRemediationEventForBranchAndStatuses(app.getId(), branchName, EVENT_STATUS_NEW))
        .isFalse();

    // when: create a remediation event, but not for the given branch
    SourceControlEvent event = getNewSourceControlEvent();
    event.setEventType(SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT);
    sourceControlEventDAO.insert(event);

    // then: still doesn't exist
    assertThat(sourceControlEventDAO.hasRemediationEventForBranchAndStatuses(app.getId(), branchName, EVENT_STATUS_NEW))
        .isFalse();

    // when: create a non-remediation event for the given branch
    event = getNewSourceControlEvent();
    event.setBranchName("some/other/branch");
    sourceControlEventDAO.insert(event);

    // then: still doesn't exist
    assertThat(sourceControlEventDAO.hasRemediationEventForBranchAndStatuses(app.getId(), branchName, EVENT_STATUS_NEW))
        .isFalse();

    // when: create remediation events for the given branch with a different status
    event = getNewSourceControlEvent();
    event.setBranchName(branchName);
    event.setEventType(SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT);
    event.setEventStatus(EVENT_STATUS_COMPLETE);
    sourceControlEventDAO.insert(event);
    event = getNewSourceControlEvent();
    event.setBranchName(branchName);
    event.setEventType(SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT);
    event.setEventStatus(EVENT_STATUS_ERROR);
    sourceControlEventDAO.insert(event);

    // then: still doesn't exist
    assertThat(sourceControlEventDAO.hasRemediationEventForBranchAndStatuses(app.getId(), branchName, EVENT_STATUS_NEW))
        .isFalse();

    // when: create a remediation event for the given branch
    event = getNewSourceControlEvent();
    event.setBranchName(branchName);
    event.setEventType(SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT);
    event.setEventStatus(EVENT_STATUS_NEW);
    sourceControlEventDAO.insert(event);

    // then: should already exist
    assertThat(sourceControlEventDAO.hasRemediationEventForBranchAndStatuses(app.getId(), branchName, EVENT_STATUS_NEW))
        .isTrue();
  }

  @Test
  public void testHasRemediationEventForBranchAndStatuses_Multiple() {
    String branchName = "abc/org/repo";

    // No events
    assertThat(sourceControlEventDAO.hasRemediationEventForBranchAndStatuses(app.getId(), branchName)).isFalse();

    // Not a remediation event
    sourceControlEventDAO.insert(getNewSourceControlEvent());
    assertThat(sourceControlEventDAO.hasRemediationEventForBranchAndStatuses(app.getId(), branchName)).isFalse();

    // Not for the given branch
    SourceControlEvent eventForOtherBranch = getNewSourceControlEvent();
    eventForOtherBranch.setEventType(SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT);
    sourceControlEventDAO.insert(eventForOtherBranch);
    assertThat(sourceControlEventDAO.hasRemediationEventForBranchAndStatuses(app.getId(), branchName)).isFalse();

    SourceControlEvent event1 = getNewSourceControlEvent();
    event1.setEventType(SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT);
    event1.setBranchName(branchName);
    sourceControlEventDAO.insert(event1);
    // Any status
    assertThat(sourceControlEventDAO.hasRemediationEventForBranchAndStatuses(app.getId(), branchName)).isTrue();
    assertThat(sourceControlEventDAO.hasRemediationEventForBranchAndStatuses(app.getId(), branchName,
        EVENT_STATUS_NEW, EVENT_STATUS_IN_PROGRESS, EVENT_STATUS_IN_PROGRESS)).isTrue();
    // Single status
    assertThat(sourceControlEventDAO.hasRemediationEventForBranchAndStatuses(app.getId(), branchName,
        EVENT_STATUS_NEW)).isTrue();
    assertThat(sourceControlEventDAO.hasRemediationEventForBranchAndStatuses(app.getId(), branchName,
        EVENT_STATUS_IN_PROGRESS)).isFalse();
    assertThat(sourceControlEventDAO.hasRemediationEventForBranchAndStatuses(app.getId(), branchName,
        EVENT_STATUS_COMPLETE)).isFalse();
    // Pair statuses
    assertThat(sourceControlEventDAO.hasRemediationEventForBranchAndStatuses(app.getId(), branchName,
        EVENT_STATUS_NEW, EVENT_STATUS_NEW)).isTrue();
    assertThat(sourceControlEventDAO.hasRemediationEventForBranchAndStatuses(app.getId(), branchName,
        EVENT_STATUS_IN_PROGRESS, EVENT_STATUS_IN_PROGRESS)).isFalse();
    assertThat(sourceControlEventDAO.hasRemediationEventForBranchAndStatuses(app.getId(), branchName,
        EVENT_STATUS_COMPLETE, EVENT_STATUS_COMPLETE)).isFalse();
    assertThat(sourceControlEventDAO.hasRemediationEventForBranchAndStatuses(app.getId(), branchName,
        EVENT_STATUS_NEW, EVENT_STATUS_IN_PROGRESS)).isTrue();
    assertThat(sourceControlEventDAO.hasRemediationEventForBranchAndStatuses(app.getId(), branchName,
        EVENT_STATUS_NEW, EVENT_STATUS_COMPLETE)).isTrue();
    assertThat(sourceControlEventDAO.hasRemediationEventForBranchAndStatuses(app.getId(), branchName,
        EVENT_STATUS_IN_PROGRESS, EVENT_STATUS_COMPLETE)).isFalse();

    SourceControlEvent event2 = getNewSourceControlEvent();
    event2.setEventType(SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT);
    event2.setBranchName(branchName);
    event2.setEventStatus(EVENT_STATUS_IN_PROGRESS);
    sourceControlEventDAO.insert(event2);

    // Any status
    assertThat(sourceControlEventDAO.hasRemediationEventForBranchAndStatuses(app.getId(), branchName)).isTrue();
    assertThat(sourceControlEventDAO.hasRemediationEventForBranchAndStatuses(app.getId(), branchName,
        EVENT_STATUS_NEW, EVENT_STATUS_IN_PROGRESS, EVENT_STATUS_IN_PROGRESS)).isTrue();
    // Single status
    assertThat(sourceControlEventDAO.hasRemediationEventForBranchAndStatuses(app.getId(), branchName,
        EVENT_STATUS_NEW)).isTrue();
    assertThat(sourceControlEventDAO.hasRemediationEventForBranchAndStatuses(app.getId(), branchName,
        EVENT_STATUS_IN_PROGRESS)).isTrue();
    assertThat(sourceControlEventDAO.hasRemediationEventForBranchAndStatuses(app.getId(), branchName,
        EVENT_STATUS_COMPLETE)).isFalse();
    // Pair statuses
    assertThat(sourceControlEventDAO.hasRemediationEventForBranchAndStatuses(app.getId(), branchName,
        EVENT_STATUS_NEW, EVENT_STATUS_NEW)).isTrue();
    assertThat(sourceControlEventDAO.hasRemediationEventForBranchAndStatuses(app.getId(), branchName,
        EVENT_STATUS_IN_PROGRESS, EVENT_STATUS_IN_PROGRESS)).isTrue();
    assertThat(sourceControlEventDAO.hasRemediationEventForBranchAndStatuses(app.getId(), branchName,
        EVENT_STATUS_COMPLETE, EVENT_STATUS_COMPLETE)).isFalse();
    assertThat(sourceControlEventDAO.hasRemediationEventForBranchAndStatuses(app.getId(), branchName,
        EVENT_STATUS_NEW, EVENT_STATUS_IN_PROGRESS)).isTrue();
    assertThat(sourceControlEventDAO.hasRemediationEventForBranchAndStatuses(app.getId(), branchName,
        EVENT_STATUS_NEW, EVENT_STATUS_COMPLETE)).isTrue();
    assertThat(sourceControlEventDAO.hasRemediationEventForBranchAndStatuses(app.getId(), branchName,
        EVENT_STATUS_IN_PROGRESS, EVENT_STATUS_COMPLETE)).isTrue();

    SourceControlEvent event3 = getNewSourceControlEvent();
    event3.setEventType(SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT);
    event3.setBranchName(branchName);
    event3.setEventStatus(EVENT_STATUS_COMPLETE);
    sourceControlEventDAO.insert(event3);

    // Any status
    assertThat(sourceControlEventDAO.hasRemediationEventForBranchAndStatuses(app.getId(), branchName)).isTrue();
    assertThat(sourceControlEventDAO.hasRemediationEventForBranchAndStatuses(app.getId(), branchName,
        EVENT_STATUS_NEW, EVENT_STATUS_IN_PROGRESS, EVENT_STATUS_IN_PROGRESS)).isTrue();
    // Single status
    assertThat(sourceControlEventDAO.hasRemediationEventForBranchAndStatuses(app.getId(), branchName,
        EVENT_STATUS_NEW)).isTrue();
    assertThat(sourceControlEventDAO.hasRemediationEventForBranchAndStatuses(app.getId(), branchName,
        EVENT_STATUS_IN_PROGRESS)).isTrue();
    assertThat(sourceControlEventDAO.hasRemediationEventForBranchAndStatuses(app.getId(), branchName,
        EVENT_STATUS_COMPLETE)).isTrue();
    // Pair statuses
    assertThat(sourceControlEventDAO.hasRemediationEventForBranchAndStatuses(app.getId(), branchName,
        EVENT_STATUS_NEW, EVENT_STATUS_NEW)).isTrue();
    assertThat(sourceControlEventDAO.hasRemediationEventForBranchAndStatuses(app.getId(), branchName,
        EVENT_STATUS_IN_PROGRESS, EVENT_STATUS_IN_PROGRESS)).isTrue();
    assertThat(sourceControlEventDAO.hasRemediationEventForBranchAndStatuses(app.getId(), branchName,
        EVENT_STATUS_COMPLETE, EVENT_STATUS_COMPLETE)).isTrue();
    assertThat(sourceControlEventDAO.hasRemediationEventForBranchAndStatuses(app.getId(), branchName,
        EVENT_STATUS_NEW, EVENT_STATUS_IN_PROGRESS)).isTrue();
    assertThat(sourceControlEventDAO.hasRemediationEventForBranchAndStatuses(app.getId(), branchName,
        EVENT_STATUS_NEW, EVENT_STATUS_COMPLETE)).isTrue();
    assertThat(sourceControlEventDAO.hasRemediationEventForBranchAndStatuses(app.getId(), branchName,
        EVENT_STATUS_IN_PROGRESS, EVENT_STATUS_COMPLETE)).isTrue();
  }

  @Test
  public void testHasRemediationEventForBranchAndStatuses_Manual() {
    // given: no events yet
    final String branchName = "abc/org/repo";

    // then: remediation event for branch doesn't exists
    assertThat(sourceControlEventDAO.hasRemediationEventForBranchAndStatuses(app.getId(), branchName, EVENT_STATUS_NEW))
        .isFalse();

    // when: create an event that's not a remediation event and not for the given branch
    sourceControlEventDAO.insert(getNewSourceControlEvent());

    // then: still doesn't exist
    assertThat(sourceControlEventDAO.hasRemediationEventForBranchAndStatuses(app.getId(), branchName, EVENT_STATUS_NEW))
        .isFalse();

    // when: create a remediation event, but not for the given branch
    SourceControlEvent event = getNewSourceControlEvent();
    event.setEventType(SourceControlEvent.MANUAL_REMEDIATION_PULL_REQUEST_EVENT);
    sourceControlEventDAO.insert(event);

    // then: still doesn't exist
    assertThat(sourceControlEventDAO.hasRemediationEventForBranchAndStatuses(app.getId(), branchName, EVENT_STATUS_NEW))
        .isFalse();

    // when: create a non-remediation event for the given branch
    event = getNewSourceControlEvent();
    event.setBranchName("some/other/branch");
    sourceControlEventDAO.insert(event);

    // then: still doesn't exist
    assertThat(sourceControlEventDAO.hasRemediationEventForBranchAndStatuses(app.getId(), branchName, EVENT_STATUS_NEW))
        .isFalse();

    // when: create remediation events for the given branch with a different status
    event = getNewSourceControlEvent();
    event.setBranchName(branchName);
    event.setEventType(SourceControlEvent.MANUAL_REMEDIATION_PULL_REQUEST_EVENT);
    event.setEventStatus(EVENT_STATUS_COMPLETE);
    sourceControlEventDAO.insert(event);
    event = getNewSourceControlEvent();
    event.setBranchName(branchName);
    event.setEventType(SourceControlEvent.MANUAL_REMEDIATION_PULL_REQUEST_EVENT);
    event.setEventStatus(EVENT_STATUS_ERROR);
    sourceControlEventDAO.insert(event);

    // then: still doesn't exist
    assertThat(sourceControlEventDAO.hasRemediationEventForBranchAndStatuses(app.getId(), branchName, EVENT_STATUS_NEW))
        .isFalse();

    // when: create a remediation event for the given branch
    event = getNewSourceControlEvent();
    event.setBranchName(branchName);
    event.setEventType(SourceControlEvent.MANUAL_REMEDIATION_PULL_REQUEST_EVENT);
    event.setEventStatus(EVENT_STATUS_NEW);
    sourceControlEventDAO.insert(event);

    // then: should already exist
    assertThat(sourceControlEventDAO.hasRemediationEventForBranchAndStatuses(app.getId(), branchName, EVENT_STATUS_NEW))
        .isTrue();
  }

  @Test
  public void testClearEventsAndInsert() {
    //Given: Application with 2 existing events
    createNewSourceControlEvents(2);
    assertThat(sourceControlEventDAO.getAllByApplicationId(app.getId())).hasSize(2);

    //When: Clear and add new event
    SourceControlEvent sourceControlEvent = getNewSourceControlEvent();
    sourceControlEventDAO.clearEventsAndInsert(sourceControlEvent);

    //Then: existing events for application is cleared and new event inserted
    List<SourceControlEvent> sourceControlEvents = sourceControlEventDAO.getAllByApplicationId(app.getId());
    assertThat(sourceControlEvents).isNotNull();
    assertThat(sourceControlEvents).hasSize(1);
    assertThat(sourceControlEvents.get(0).getEventStatus()).isEqualTo(EVENT_STATUS_NEW);
  }

  @Test
  public void testGetPendingOrInProgressUpdatedPullRequestEvents() {
    // given: some events we are interested in
    SourceControlEvent expectedEvent1 = createUpdatedPullRequestEvent(app.getId(), EVENT_STATUS_NEW, 1);
    SourceControlEvent expectedEvent2 = createUpdatedPullRequestEvent(app.getId(), EVENT_STATUS_IN_PROGRESS, 1);
    SourceControlEvent expectedEvent3 = createUpdatedPullRequestEvent(app2.getId(), EVENT_STATUS_NEW, 1);
    SourceControlEvent expectedEvent4 = createUpdatedPullRequestEvent(app2.getId(), EVENT_STATUS_IN_PROGRESS, 1);
    // and some events we are not interested in
    createUpdatedPullRequestEvent(app.getId(), EVENT_STATUS_NEW, 2);
    createUpdatedPullRequestEvent(app.getId(), EVENT_STATUS_IN_PROGRESS, 2);
    createUpdatedPullRequestEvent(app2.getId(), EVENT_STATUS_NEW, 2);
    createUpdatedPullRequestEvent(app2.getId(), EVENT_STATUS_IN_PROGRESS, 2);
    Application app3 = tempEntity.newApplicationWithParent();
    createUpdatedPullRequestEvent(app3.getId(), EVENT_STATUS_NEW, 1);
    createUpdatedPullRequestEvent(app3.getId(), EVENT_STATUS_IN_PROGRESS, 1);
    createNewSourceControlEvents(1);

    // when: get the events we are interested in
    List<SourceControlEvent> events = sourceControlEventDAO
        .getPendingOrInProgressUpdatedPullRequestEvents(Arrays.asList(app.getId(), app2.getId()), 1);

    // then:
    assertThat(events).extracting(SourceControlEvent::getId).containsExactlyInAnyOrder(expectedEvent1.getId(),
        expectedEvent2.getId(), expectedEvent3.getId(), expectedEvent4.getId());
  }

  @Test
  public void testGetCompleteOrInProgressRemediationEventsForBranch() {
    // given: no events yet
    final String branchName = "abc/org/repo";

    // then: remediation event for branch doesn't exist
    assertThat(
        sourceControlEventDAO.getRemediationEventsForBranch(app.getId(), branchName)).isEmpty();

    // when: create an event that's not a remediation event and not for the given branch
    sourceControlEventDAO.insert(getNewSourceControlEvent());

    // then: still doesn't exist
    assertThat(
        sourceControlEventDAO.getRemediationEventsForBranch(app.getId(), branchName)).isEmpty();

    // when: create a remediation event, but not for the given branch
    SourceControlEvent event = getNewSourceControlEvent();
    event.setEventType(SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT);
    sourceControlEventDAO.insert(event);

    // then: still doesn't exist
    assertThat(
        sourceControlEventDAO.getRemediationEventsForBranch(app.getId(), branchName)).isEmpty();

    // when: create a non-remediation event for the given branch
    event = getNewSourceControlEvent();
    event.setBranchName("some/other/branch");
    sourceControlEventDAO.insert(event);

    // then: still doesn't exist
    assertThat(
        sourceControlEventDAO.getRemediationEventsForBranch(app.getId(), branchName)).isEmpty();

    // when: create a remediation event for the given branch but other app
    event = getNewSourceControlEvent();
    event.setApplicationId(app2.getId());
    event.setBranchName(branchName);
    event.setEventType(SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT);
    sourceControlEventDAO.insert(event);

    // then: still doesn't exist
    assertThat(
        sourceControlEventDAO.getRemediationEventsForBranch(app.getId(), branchName)).isEmpty();

    // when: create a remediation event for the given branch and error status
    event = getNewSourceControlEvent();
    event.setBranchName(branchName);
    event.setEventType(SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT);
    event.setEventStatus(EVENT_STATUS_ERROR);
    sourceControlEventDAO.insert(event);

    // then: exists
    List<SourceControlEvent> remediationEvents =
        sourceControlEventDAO.getRemediationEventsForBranch(app.getId(), branchName);
    assertThat(remediationEvents).isNotEmpty();
    assertThat(remediationEvents).extracting(SourceControlEvent::getId).contains(event.getId());

    // when: create a remediation event for the given branch and complete status
    event = getNewSourceControlEvent();
    event.setBranchName(branchName);
    event.setEventType(SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT);
    event.setEventStatus(EVENT_STATUS_COMPLETE);
    sourceControlEventDAO.insert(event);

    // then: exists
    remediationEvents =
        sourceControlEventDAO.getRemediationEventsForBranch(app.getId(), branchName);
    assertThat(remediationEvents).isNotEmpty();
    assertThat(remediationEvents).extracting(SourceControlEvent::getId).contains(event.getId());

    // when: create a remediation event for the given branch and in progress status
    event = getNewSourceControlEvent();
    event.setBranchName(branchName);
    event.setEventType(SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT);
    event.setEventStatus(EVENT_STATUS_IN_PROGRESS);
    sourceControlEventDAO.insert(event);

    // then: exists
    remediationEvents =
        sourceControlEventDAO.getRemediationEventsForBranch(app.getId(), branchName);
    assertThat(remediationEvents).isNotEmpty();
    assertThat(remediationEvents).extracting(SourceControlEvent::getId).contains(event.getId());

    // when: create a remediation event for the given branch and new status
    event = getNewSourceControlEvent();
    event.setBranchName(branchName);
    event.setEventType(SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT);
    event.setEventStatus(EVENT_STATUS_NEW);
    sourceControlEventDAO.insert(event);

    // then: exists
    remediationEvents =
        sourceControlEventDAO.getRemediationEventsForBranch(app.getId(), branchName);
    assertThat(remediationEvents).isNotEmpty();
    assertThat(remediationEvents).extracting(SourceControlEvent::getId).contains(event.getId());
  }

  @Test
  public void testGetCompleteOrInProgressRemediationEventsForBranch_Manual() {
    // given: no events yet
    final String branchName = "abc/org/repo";

    // then: remediation event for branch doesn't exist
    assertThat(
        sourceControlEventDAO.getRemediationEventsForBranch(app.getId(), branchName)).isEmpty();

    // when: create an event that's not a remediation event and not for the given branch
    sourceControlEventDAO.insert(getNewSourceControlEvent());

    // then: still doesn't exist
    assertThat(
        sourceControlEventDAO.getRemediationEventsForBranch(app.getId(), branchName)).isEmpty();

    // when: create a remediation event, but not for the given branch
    SourceControlEvent event = getNewSourceControlEvent();
    event.setEventType(SourceControlEvent.MANUAL_REMEDIATION_PULL_REQUEST_EVENT);
    sourceControlEventDAO.insert(event);

    // then: still doesn't exist
    assertThat(
        sourceControlEventDAO.getRemediationEventsForBranch(app.getId(), branchName)).isEmpty();

    // when: create a non-remediation event for the given branch
    event = getNewSourceControlEvent();
    event.setBranchName("some/other/branch");
    sourceControlEventDAO.insert(event);

    // then: still doesn't exist
    assertThat(
        sourceControlEventDAO.getRemediationEventsForBranch(app.getId(), branchName)).isEmpty();

    // when: create a remediation event for the given branch but other app
    event = getNewSourceControlEvent();
    event.setApplicationId(app2.getId());
    event.setBranchName(branchName);
    event.setEventType(SourceControlEvent.MANUAL_REMEDIATION_PULL_REQUEST_EVENT);
    sourceControlEventDAO.insert(event);

    // then: still doesn't exist
    assertThat(
        sourceControlEventDAO.getRemediationEventsForBranch(app.getId(), branchName)).isEmpty();

    // when: create a remediation event for the given branch and error status
    event = getNewSourceControlEvent();
    event.setBranchName(branchName);
    event.setEventType(SourceControlEvent.MANUAL_REMEDIATION_PULL_REQUEST_EVENT);
    event.setEventStatus(EVENT_STATUS_ERROR);
    sourceControlEventDAO.insert(event);

    // then: exists
    List<SourceControlEvent> remediationEvents =
        sourceControlEventDAO.getRemediationEventsForBranch(app.getId(), branchName);
    assertThat(remediationEvents).isNotEmpty();
    assertThat(remediationEvents).extracting(SourceControlEvent::getId).contains(event.getId());

    // when: create a remediation event for the given branch and complete
    event = getNewSourceControlEvent();
    event.setBranchName(branchName);
    event.setEventType(SourceControlEvent.MANUAL_REMEDIATION_PULL_REQUEST_EVENT);
    event.setEventStatus(EVENT_STATUS_COMPLETE);
    sourceControlEventDAO.insert(event);

    // then: exists
    remediationEvents =
        sourceControlEventDAO.getRemediationEventsForBranch(app.getId(), branchName);
    assertThat(remediationEvents).isNotEmpty();
    assertThat(remediationEvents).extracting(SourceControlEvent::getId).contains(event.getId());

    // when: create a remediation event for the given branch and in progress
    event = getNewSourceControlEvent();
    event.setBranchName(branchName);
    event.setEventType(SourceControlEvent.MANUAL_REMEDIATION_PULL_REQUEST_EVENT);
    event.setEventStatus(EVENT_STATUS_IN_PROGRESS);
    sourceControlEventDAO.insert(event);

    // then: exists
    remediationEvents =
        sourceControlEventDAO.getRemediationEventsForBranch(app.getId(), branchName);
    assertThat(remediationEvents).isNotEmpty();
    assertThat(remediationEvents).extracting(SourceControlEvent::getId).contains(event.getId());

    // when: create a remediation event for the given branch and new status
    event = getNewSourceControlEvent();
    event.setBranchName(branchName);
    event.setEventType(SourceControlEvent.MANUAL_REMEDIATION_PULL_REQUEST_EVENT);
    event.setEventStatus(EVENT_STATUS_NEW);
    sourceControlEventDAO.insert(event);

    // then: exists
    remediationEvents =
        sourceControlEventDAO.getRemediationEventsForBranch(app.getId(), branchName);
    assertThat(remediationEvents).isNotEmpty();
    assertThat(remediationEvents).extracting(SourceControlEvent::getId).contains(event.getId());
  }

  @Test
  public void testSelectEventsByCriteria_CreatedOnOrAfterFilter() {
    long cutOffTimeMs = currentTimeMillis() + 3000;
    persistSourceControlEvent(cutOffTimeMs - 1000);
    persistSourceControlEvent(cutOffTimeMs - 2000);
    SourceControlEvent expectedEvent = persistSourceControlEvent(cutOffTimeMs);
    SourceControlEvent expectedEventTwo = persistSourceControlEvent(cutOffTimeMs + 1000);
    boolean ascending = true;
    Set<String> applicationIds = Collections.singleton(app.getId());

    List<SourceControlEvent> fetchedSourceControlEvents = sourceControlEventDAO
        .selectEventsByCriteria(applicationIds, new Date(cutOffTimeMs), ascending, 10, 0);

    assertThat(fetchedSourceControlEvents).extracting(SourceControlEvent::getId)
        .containsExactly(expectedEvent.getId(), expectedEventTwo.getId());
  }

  @Test
  public void testSelectEventsByCriteria_AscendingFilter() {
    long cutOffTimeMs = currentTimeMillis() - 5000;
    SourceControlEvent expectedEvent = persistSourceControlEvent(cutOffTimeMs + 1000);
    SourceControlEvent expectedEventTwo = persistSourceControlEvent(cutOffTimeMs + 2000);
    SourceControlEvent expectedEventThree = persistSourceControlEvent(cutOffTimeMs + 3000);
    boolean ascending = true;
    Set<String> applicationIds = Collections.singleton(app.getId());

    List<SourceControlEvent> fetchedSourceControlEvents = sourceControlEventDAO
        .selectEventsByCriteria(applicationIds, new Date(cutOffTimeMs), ascending, 10, 0);

    assertThat(fetchedSourceControlEvents).extracting(SourceControlEvent::getId)
        .containsExactly(expectedEvent.getId(), expectedEventTwo.getId(), expectedEventThree.getId());
  }

  @Test
  public void testSelectEventsByCriteria_DescendingFilter() {
    long cutOffTimeMs = currentTimeMillis() - 5000;
    SourceControlEvent expectedEvent = persistSourceControlEvent(cutOffTimeMs + 1000);
    SourceControlEvent expectedEventTwo = persistSourceControlEvent(cutOffTimeMs + 2000);
    SourceControlEvent expectedEventThree = persistSourceControlEvent(cutOffTimeMs + 3000);
    boolean ascending = false;
    Set<String> applicationIds = Collections.singleton(app.getId());

    List<SourceControlEvent> fetchedSourceControlEvents = sourceControlEventDAO
        .selectEventsByCriteria(applicationIds, new Date(cutOffTimeMs), ascending, 10, 0);

    assertThat(fetchedSourceControlEvents).extracting(SourceControlEvent::getId)
        .containsExactly(expectedEventThree.getId(), expectedEventTwo.getId(), expectedEvent.getId());
  }

  @Test
  public void testSelectEventsByCriteria_LimitAndAscendingFilter() {
    long cutOffTimeMs = currentTimeMillis() - 5000;
    SourceControlEvent expectedEvent = persistSourceControlEvent(cutOffTimeMs + 1000);
    SourceControlEvent expectedEventTwo = persistSourceControlEvent(cutOffTimeMs + 2000);
    persistSourceControlEvent(cutOffTimeMs + 3000);
    boolean ascending = true;
    Set<String> applicationIds = Collections.singleton(app.getId());

    List<SourceControlEvent> fetchedSourceControlEvents = sourceControlEventDAO
        .selectEventsByCriteria(applicationIds, new Date(cutOffTimeMs), ascending, 2, 0);

    assertThat(fetchedSourceControlEvents).extracting(SourceControlEvent::getId)
        .containsExactly(expectedEvent.getId(), expectedEventTwo.getId());
  }

  @Test
  public void testSelectEventsByCriteria_LimitAndDescendingFilter() {
    long cutOffTimeMs = currentTimeMillis() - 5000;
    persistSourceControlEvent(cutOffTimeMs + 1000);
    SourceControlEvent expectedEvent = persistSourceControlEvent(cutOffTimeMs + 2000);
    SourceControlEvent expectedEventTwo = persistSourceControlEvent(cutOffTimeMs + 3000);
    boolean ascending = false;
    Set<String> applicationIds = Collections.singleton(app.getId());

    List<SourceControlEvent> fetchedSourceControlEvents = sourceControlEventDAO
        .selectEventsByCriteria(applicationIds, new Date(cutOffTimeMs), ascending, 2, 0);

    assertThat(fetchedSourceControlEvents).extracting(SourceControlEvent::getId)
        .containsExactly(expectedEventTwo.getId(), expectedEvent.getId());
  }

  @Test
  public void testSelectEventsByCriteria_LimitOffsetAndAscendingFilter() {
    long cutOffTimeMs = currentTimeMillis() - 5000;
    persistSourceControlEvent(cutOffTimeMs + 1000);
    persistSourceControlEvent(cutOffTimeMs + 2000);
    SourceControlEvent expectedEvent = persistSourceControlEvent(cutOffTimeMs + 3000);
    SourceControlEvent expectedEventTwo = persistSourceControlEvent(cutOffTimeMs + 4000);
    boolean ascending = true;
    Set<String> applicationIds = Collections.singleton(app.getId());

    List<SourceControlEvent> fetchedSourceControlEvents = sourceControlEventDAO
        .selectEventsByCriteria(applicationIds, new Date(cutOffTimeMs), ascending, 2, 2);

    assertThat(fetchedSourceControlEvents).extracting(SourceControlEvent::getId)
        .containsExactly(expectedEvent.getId(), expectedEventTwo.getId());
  }

  @Test
  public void testSelectEventsByCriteria_LimitOffsetAndDescendingFilter() {
    long cutOffTimeMs = currentTimeMillis() - 5000;
    SourceControlEvent expectedEvent = persistSourceControlEvent(cutOffTimeMs + 1000);
    SourceControlEvent expectedEventTwo = persistSourceControlEvent(cutOffTimeMs + 2000);
    persistSourceControlEvent(cutOffTimeMs + 3000);
    persistSourceControlEvent(cutOffTimeMs + 4000);
    boolean ascending = false;
    Set<String> applicationIds = Collections.singleton(app.getId());

    List<SourceControlEvent> fetchedSourceControlEvents = sourceControlEventDAO
        .selectEventsByCriteria(applicationIds, new Date(cutOffTimeMs), ascending, 2, 2);

    assertThat(fetchedSourceControlEvents).extracting(SourceControlEvent::getId)
        .containsExactly(expectedEventTwo.getId(), expectedEvent.getId());
  }

  @Test
  public void testSelectEventsByCriteria_EmptyResult() {
    long cutOffTimeMs = currentTimeMillis() - 5000;
    boolean ascending = true;
    Set<String> applicationIds = Collections.singleton(app.getId());

    List<SourceControlEvent> fetchedSourceControlEvents = sourceControlEventDAO
        .selectEventsByCriteria(applicationIds, new Date(cutOffTimeMs), ascending, 10, 0);

    assertThat(fetchedSourceControlEvents).isEmpty();
  }

  @Test
  @PostgresTest
  public void testSelectEventsByCriteria_CreatedOnOrAfterFilterPostgres() {
    Organization tempOrganization = tempEntity.newOrganization();
    Application tempApplication = tempEntity.newApplication(tempOrganization.getId());
    long cutOffTimeMs = 100000;
    persistSourceControlEvent(0, tempApplication.getId());
    persistSourceControlEvent(0, tempApplication.getId());
    SourceControlEvent expectedEvent = persistSourceControlEvent(cutOffTimeMs + 100000, tempApplication.getId());
    SourceControlEvent expectedEventTwo = persistSourceControlEvent(cutOffTimeMs + 100000, tempApplication.getId());
    boolean ascending = true;
    Set<String> applicationIds = Collections.singleton(tempApplication.getId());

    List<SourceControlEvent> fetchedSourceControlEvents = sourceControlEventDAO
        .selectEventsByCriteria(applicationIds, new Date(cutOffTimeMs + 100000), ascending, 10, 0);

    assertThat(fetchedSourceControlEvents).extracting(SourceControlEvent::getId)
        .containsExactly(expectedEvent.getId(), expectedEventTwo.getId());
  }

  private SourceControlEvent getNewSourceControlEvent() {
    return getNewSourceControlEvent(app.getId());
  }

  private SourceControlEvent getNewSourceControlEvent(final String applicationId) {
    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(applicationId, StageTypes.BUILD.getId(), "scanId2", false, false, false,
            testStartTime,
            "commitHash1235");

    return new SourceControlEvent()
        .setApplicationId(applicationId)
        .setCommitHash("abcdefg")
        .setEventType(SourceControlEvent.DISCOVERED_PULL_REQUEST_EVENT)
        .setPolicyEvaluationId(policyEvaluation.getId())
        .setBranchName("branch")
        .setPullRequestNumber(2)
        .setScmUsername("user")
        .setInitiator("webhook")
        .setCreateTime(testStartTime);
  }

  private SourceControlEvent createNewSourceControlEvaluationEvent(String appId, String eventStatus) {
    SourceControlEvent event = new SourceControlEvent()
        .setApplicationId(appId)
        .setEventType(SOURCE_CONTROL_EVALUATION_EVENT)
        .setEventStatus(eventStatus)
        .setStageTypeId(StageTypes.SOURCE.getId())
        .setCreateTime(testStartTime);

    sourceControlEventDAO.insert(event);
    return event;
  }

  private SourceControlEvent createUpdatedPullRequestEvent(
      String appId,
      String eventStatus,
      int pullRequestNumber)
  {
    SourceControlEvent event = new SourceControlEvent() //
        .setApplicationId(appId) //
        .setEventType(UPDATED_PULL_REQUEST_EVENT) //
        .setEventStatus(eventStatus) //
        .setPullRequestNumber(pullRequestNumber) //
        .setStageTypeId(StageTypes.SOURCE.getId()) //
        .setCreateTime(testStartTime);

    sourceControlEventDAO.insert(event);
    return event;
  }

  private void createNewSourceControlEvents(final int count) {
    createNewSourceControlEvents(count, app.getId());
  }

  private void createNewSourceControlEvents(final int count, final String applicationId) {
    for (int i = 0; i < count; i++) {
      SourceControlEvent newSourceControlEvent = getNewSourceControlEvent(applicationId);
      sourceControlEventDAO.insert(newSourceControlEvent);
    }
  }

  private List<SourceControlEvent> createNewPrioritizedSourceControlEvents(
      final String applicationId,
      int... priorities)
  {
    final List<SourceControlEvent> result = new ArrayList<>();
    LocalDateTime created = LocalDateTime.now();
    for (int priority : priorities) {
      SourceControlEvent sourceControlEvent = getNewSourceControlEvent(applicationId)
          .setEventPriority(priority)
          .setCreateTime(Date.from(created.toInstant(ZoneOffset.UTC)));
      sourceControlEventDAO.insert(sourceControlEvent);
      created = created.plusMinutes(1);
    }
    return result;
  }

  private void createNewEvent(
      String applicationId,
      String eventType,
      String eventStatus,
      String instanceId,
      String statusDetails)
  {
    sourceControlEventDAO.insert(
        new SourceControlEvent()
            .setApplicationId(applicationId)
            .setEventType(eventType)
            .setEventStatus(eventStatus)
            .setInstanceId(instanceId)
            .setEventStatusDetails(statusDetails)
            .setCreateTime(new Date())
    );
  }

  private Date toDate(final LocalDateTime localDateTime) {
    return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
  }

  private SourceControlEvent persistSourceControlEvent(long createTime) {
    return persistSourceControlEvent(createTime, app.getId());
  }

  private SourceControlEvent persistSourceControlEvent(long createTime, String applicationId) {
    SourceControlEvent sourceControlEvent = getNewSourceControlEvent(applicationId);
    sourceControlEvent.setEventStatus("complete")
        .setInstanceId("instance1")
        .setCreateTime(new Date(createTime));
    sourceControlEventDAO.insert(sourceControlEvent);
    return sourceControlEvent;
  }
}
