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
import java.util.concurrent.atomic.AtomicInteger;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.EVENT_STATUS_COMPLETE;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.EVENT_STATUS_ERROR;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.EVENT_STATUS_IN_PROGRESS;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.EVENT_STATUS_NEW;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.SOURCE_CONTROL_EVALUATION_EVENT;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.UPDATED_PULL_REQUEST_EVENT;
import static java.lang.System.currentTimeMillis;
import static org.assertj.core.api.Assertions.assertThat;

public class SourceControlEventDAOTest
    extends AbstractDbDAOTest
{
  private final SourceControlEventDAO sourceControlEventDAO = new SourceControlEventDAO();

  private Application app;

  private Application app2;

  private Date testStartTime;

  @Override
  @Before
  public void setup() {
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
  public void testNewInstanceDefaultsToEventStatusNew() {
    // given a new source control event
    SourceControlEvent sourceControlEvent = new SourceControlEvent();

    // then the initial event status is new
    assertThat(sourceControlEvent.getEventStatus()).isEqualTo(EVENT_STATUS_NEW);
  }

  @Test
  public void testResetStaleEvents() {
    // given: new, active and stale source control events
    long cutoffTimeMs = currentTimeMillis() - 1000;
    SourceControlEvent activeNewEvent = getNewSourceControlEvent()
        .setEventStatus("new")
        .setInstanceId("instance1")
        .setCreateTime(new Date(cutoffTimeMs + 500));
    sourceControlEventDAO.insert(activeNewEvent);

    SourceControlEvent activeInProgressEvent = getNewSourceControlEvent()
        .setEventStatus("in progress")
        .setInstanceId("instance2")
        .setCreateTime(new Date(cutoffTimeMs - 500))
        .setStartTime(new Date(cutoffTimeMs + 500));
    sourceControlEventDAO.insert(activeInProgressEvent);

    SourceControlEvent staleNewEvent = getNewSourceControlEvent()
        .setEventStatus("new")
        .setInstanceId("instance3")
        .setCreateTime(new Date(cutoffTimeMs - 500));
    sourceControlEventDAO.insert(staleNewEvent);

    SourceControlEvent staleInProgressEvent = getNewSourceControlEvent()
        .setEventStatus("new")
        .setInstanceId("instance4")
        .setCreateTime(new Date(cutoffTimeMs - 500))
        .setStartTime(new Date(cutoffTimeMs - 500));
    sourceControlEventDAO.insert(staleInProgressEvent);

    SourceControlEvent staleCompletedEvent = getNewSourceControlEvent()
        .setEventStatus("complete")
        .setInstanceId("instance5")
        .setCreateTime(new Date(cutoffTimeMs - 500))
        .setStartTime(new Date(cutoffTimeMs - 400))
        .setCompleteTime(new Date(cutoffTimeMs - 300));
    sourceControlEventDAO.insert(staleCompletedEvent);

    SourceControlEvent stalePartiallyCompletedEvent = getNewSourceControlEvent()
        .setEventStatus("partially complete")
        .setInstanceId("instance6")
        .setCreateTime(new Date(cutoffTimeMs - 500))
        .setStartTime(new Date(cutoffTimeMs - 400))
        .setCompleteTime(new Date(cutoffTimeMs - 300));
    sourceControlEventDAO.insert(stalePartiallyCompletedEvent);

    SourceControlEvent staleErrorEvent = getNewSourceControlEvent()
        .setEventStatus("error")
        .setInstanceId("instance7")
        .setCreateTime(new Date(cutoffTimeMs - 500))
        .setStartTime(new Date(cutoffTimeMs - 400))
        .setCompleteTime(new Date(cutoffTimeMs - 300));
    sourceControlEventDAO.insert(staleErrorEvent);

    SourceControlEvent staleThisInstanceEvent = getNewSourceControlEvent()
        .setEventStatus("in progress")
        .setInstanceId("instance8")
        .setCreateTime(new Date(cutoffTimeMs - 500))
        .setStartTime(new Date(cutoffTimeMs - 400));
    sourceControlEventDAO.insert(staleThisInstanceEvent);

    // when: reset stale events
    sourceControlEventDAO.resetStaleEvents(new Date(cutoffTimeMs), "instance8");

    SourceControlEvent fetchedActiveNewEvent = sourceControlEventDAO.getById(activeNewEvent.getId());
    SourceControlEvent fetchedActiveInProgressEvent = sourceControlEventDAO.getById(activeInProgressEvent.getId());
    SourceControlEvent fetchedStaleNewEvent = sourceControlEventDAO.getById(staleNewEvent.getId());
    SourceControlEvent fetchedStaleInProgressEvent = sourceControlEventDAO.getById(staleInProgressEvent.getId());
    SourceControlEvent fetchedStaleCompletedEvent = sourceControlEventDAO.getById(staleCompletedEvent.getId());
    SourceControlEvent fetchedStalePartiallyCompletedEvent =
        sourceControlEventDAO.getById(stalePartiallyCompletedEvent.getId());
    SourceControlEvent fetchedStaleErrorEvent = sourceControlEventDAO.getById(staleErrorEvent.getId());
    SourceControlEvent fetchedStaleThisInstanceEvent = sourceControlEventDAO.getById(staleThisInstanceEvent.getId());

    // then: active events were unchanged
    assertThat(fetchedActiveNewEvent.getInstanceId()).isEqualTo("instance1");
    assertThat(fetchedActiveNewEvent.getEventStatus()).isEqualTo("new");
    assertThat(fetchedActiveInProgressEvent.getInstanceId()).isEqualTo("instance2");
    assertThat(fetchedActiveInProgressEvent.getEventStatus()).isEqualTo("in progress");

    // and: stale new/in progress events were reset to new
    assertThat(fetchedStaleNewEvent.getInstanceId()).isNull();
    assertThat(fetchedStaleNewEvent.getEventStatus()).isEqualTo("new");
    assertThat(fetchedStaleInProgressEvent.getInstanceId()).isNull();
    assertThat(fetchedStaleInProgressEvent.getEventStatus()).isEqualTo("new");

    // and: complete/error events were NOT reset
    assertThat(fetchedStaleCompletedEvent.getInstanceId()).isEqualTo("instance5");
    assertThat(fetchedStaleCompletedEvent.getEventStatus()).isEqualTo("complete");
    assertThat(fetchedStalePartiallyCompletedEvent.getInstanceId()).isEqualTo("instance6");
    assertThat(fetchedStalePartiallyCompletedEvent.getEventStatus()).isEqualTo("partially complete");
    assertThat(fetchedStaleErrorEvent.getInstanceId()).isEqualTo("instance7");
    assertThat(fetchedStaleErrorEvent.getEventStatus()).isEqualTo("error");

    // and: 'this' instance was stale but was not reset
    assertThat(fetchedStaleThisInstanceEvent.getInstanceId()).isEqualTo("instance8");
    assertThat(fetchedStaleThisInstanceEvent.getEventStatus()).isEqualTo("in progress");
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
      SourceControlEvent.EVENT_STATUSES.forEach(status -> {
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
  public void testMarkEventHasError() {
    // given 4 new source control events
    createNewSourceControlEvents(4);

    // when we mark an event with an error
    sourceControlEventDAO.reserveEventsForInstance("1");
    SourceControlEvent sourceControlEvent = sourceControlEventDAO.selectEventsForInstance("1", 1).get(0);
    sourceControlEventDAO.markEventHasError(sourceControlEvent.getId(), "error message");

    // then the event is marked with the error message and a complete time
    SourceControlEvent sourceControlEventById = sourceControlEventDAO.getById(sourceControlEvent.getId());
    assertThat(sourceControlEventById.getEventStatus()).isEqualTo(SourceControlEvent.EVENT_STATUS_ERROR);
    assertThat(sourceControlEventById.getEventStatusDetails()).isEqualTo("error message");
    assertThat(sourceControlEventById.getCompleteTime()).isAfter(testStartTime);
  }

  @Test
  public void testMarkEventPartiallyComplete() {
    // given 4 new source control events
    createNewSourceControlEvents(4);

    // when we mark an event with an error
    sourceControlEventDAO.reserveEventsForInstance("1");
    SourceControlEvent sourceControlEvent = sourceControlEventDAO.selectEventsForInstance("1", 1).get(0);
    sourceControlEventDAO.markEventPartiallyComplete(sourceControlEvent.getId(), "informational message");

    // then the event is marked with partial completion message and a complete time
    SourceControlEvent sourceControlEventById = sourceControlEventDAO.getById(sourceControlEvent.getId());
    assertThat(sourceControlEventById.getEventStatus()).isEqualTo(SourceControlEvent.EVENT_STATUS_PARTIALLY_COMPLETE);
    assertThat(sourceControlEventById.getEventStatusDetails()).isEqualTo("informational message");
    assertThat(sourceControlEventById.getCompleteTime()).isAfter(testStartTime);
  }

  @Test
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

  private SourceControlEvent getNewSourceControlEvent() {
    return getNewSourceControlEvent(app.getId());
  }

  private SourceControlEvent getNewSourceControlEvent(final String applicationId) {
    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(app.getId(), StageTypes.BUILD.getId(), "scanId2", false, false, false,
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
}
