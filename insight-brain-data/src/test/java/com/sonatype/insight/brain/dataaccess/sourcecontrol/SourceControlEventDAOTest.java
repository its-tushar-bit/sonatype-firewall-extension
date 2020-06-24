/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sourcecontrol;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SourceControlEventDAOTest
    extends AbstractDbDAOTest
{
  private final SourceControlEventDAO sourceControlEventDAO = new SourceControlEventDAO();

  private Application app;

  private Date testStartTime;

  @Override
  @Before
  public void setup() {
    app = tempEntity.newApplicationWithParent();
    testStartTime = toDate(LocalDateTime.now());
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
    assertThat(sourceControlEvent.getEventStatus()).isEqualTo(SourceControlEvent.EVENT_STATUS_NEW);
  }

  @Test
  public void testReserveEventsForInstance() {
    // given 4 new source control events
    createNewSourceControlEvents(4);

    // when we reserve events for an instance
    sourceControlEventDAO.reserveEventsForInstance("1", 1);

    // then we should have 1 reservation
    List<SourceControlEvent> sourceControlEventList = sourceControlEventDAO.selectEventsForInstance("1", 4);
    assertThat(sourceControlEventList).hasSize(1);
  }

  @Test
  public void testClearEventReservations() {
    // given: new source control events in each of the possible states:
    //            unreserved, reserved, in progress, complete, error
    createNewSourceControlEvents(5);
    String instanceId = UUID.randomUUID().toString();
    sourceControlEventDAO.reserveEventsForInstance(instanceId, 4);
    List<SourceControlEvent> events = sourceControlEventDAO.selectEventsForInstance(instanceId, 4);

    SourceControlEvent inProgressEvent = events.get(0);
    sourceControlEventDAO.markEventInProgress(inProgressEvent.getId());

    SourceControlEvent completeEvent = events.get(1);
    sourceControlEventDAO.markEventComplete(completeEvent.getId());

    SourceControlEvent errorEvent = events.get(2);
    sourceControlEventDAO.markEventHasError(errorEvent.getId(), "simulated");

    SourceControlEvent reservedEvent = events.get(3);
    SourceControlEvent unreservedEvent = sourceControlEventDAO.getAvailableEvents().get(0);

    // validate initial state
    Map<String, SourceControlEvent> eventMap = getEventMap();
    assertThat(eventMap.get(unreservedEvent.getId()).getInstanceId()).isNull();
    assertThat(eventMap.get(unreservedEvent.getId()).getEventStatus()).isEqualTo(SourceControlEvent.EVENT_STATUS_NEW);

    assertThat(eventMap.get(reservedEvent.getId()).getInstanceId()).isEqualTo(instanceId);
    assertThat(eventMap.get(reservedEvent.getId()).getEventStatus()).isEqualTo(SourceControlEvent.EVENT_STATUS_NEW);

    assertThat(eventMap.get(inProgressEvent.getId()).getInstanceId()).isEqualTo(instanceId);
    assertThat(eventMap.get(inProgressEvent.getId()).getEventStatus())
        .isEqualTo(SourceControlEvent.EVENT_STATUS_IN_PROGRESS);

    assertThat(eventMap.get(completeEvent.getId()).getInstanceId()).isEqualTo(instanceId);
    assertThat(eventMap.get(completeEvent.getId()).getEventStatus())
        .isEqualTo(SourceControlEvent.EVENT_STATUS_COMPLETE);

    assertThat(eventMap.get(errorEvent.getId()).getInstanceId()).isEqualTo(instanceId);
    assertThat(eventMap.get(errorEvent.getId()).getEventStatus()).isEqualTo(SourceControlEvent.EVENT_STATUS_ERROR);

    // when: event reservations are cleared
    sourceControlEventDAO.clearEventReservations();

    // then: event info is as expected for each of the above events
    eventMap = getEventMap();
    assertThat(eventMap.get(unreservedEvent.getId()).getInstanceId()).isNull();
    assertThat(eventMap.get(unreservedEvent.getId()).getEventStatus()).isEqualTo(SourceControlEvent.EVENT_STATUS_NEW);

    assertThat(eventMap.get(reservedEvent.getId()).getInstanceId()).isNull();
    assertThat(eventMap.get(reservedEvent.getId()).getEventStatus()).isEqualTo(SourceControlEvent.EVENT_STATUS_NEW);

    assertThat(eventMap.get(inProgressEvent.getId()).getInstanceId()).isNull();
    assertThat(eventMap.get(inProgressEvent.getId()).getEventStatus())
        .isEqualTo(SourceControlEvent.EVENT_STATUS_IN_PROGRESS);

    assertThat(eventMap.get(completeEvent.getId()).getInstanceId()).isEqualTo(instanceId);
    assertThat(eventMap.get(completeEvent.getId()).getEventStatus())
        .isEqualTo(SourceControlEvent.EVENT_STATUS_COMPLETE);

    assertThat(eventMap.get(errorEvent.getId()).getInstanceId()).isEqualTo(instanceId);
    assertThat(eventMap.get(errorEvent.getId()).getEventStatus()).isEqualTo(SourceControlEvent.EVENT_STATUS_ERROR);
  }

  private Map<String, SourceControlEvent> getEventMap() {
    List<SourceControlEvent> events = sourceControlEventDAO.getAll();
    return events.stream().collect(Collectors.toMap(SourceControlEvent::getId, event -> event));
  }

  @Test
  public void testReserveEventsForInstance_multipleEventsReserved() {
    // given 4 new source control events
    createNewSourceControlEvents(4);

    // when we reserve 2 for an instance
    sourceControlEventDAO.reserveEventsForInstance("1", 2);

    // then only 2 are reserved
    assertThat(sourceControlEventDAO.selectEventsForInstance("1", 3)).hasSize(2);
  }

  @Test
  public void testReserveEventsForInstance_multipleReservations() {
    // given 4 new source control events
    createNewSourceControlEvents(4);

    // when we reserve for 2 different instances
    sourceControlEventDAO.reserveEventsForInstance("1", 2);
    sourceControlEventDAO.reserveEventsForInstance("2", 1);

    // then only 2 are reserved for instance 1
    assertThat(sourceControlEventDAO.selectEventsForInstance("1", 3)).hasSize(2);

    // and only 1 is reserved for instance 2
    assertThat(sourceControlEventDAO.selectEventsForInstance("2", 3)).hasSize(1);
  }

  @Test
  public void testMarkEventInProgress() {
    // given 4 new source control events
    createNewSourceControlEvents(4);

    // when we pull events for processing
    sourceControlEventDAO.reserveEventsForInstance("1", 1);
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
    sourceControlEventDAO.reserveEventsForInstance("1", 1);
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
    sourceControlEventDAO.reserveEventsForInstance("1", 1);
    SourceControlEvent sourceControlEvent = sourceControlEventDAO.selectEventsForInstance("1", 1).get(0);
    sourceControlEventDAO.markEventHasError(sourceControlEvent.getId(), "error message");

    // then the event is marked with the error message and a complete time
    SourceControlEvent sourceControlEventById = sourceControlEventDAO.getById(sourceControlEvent.getId());
    assertThat(sourceControlEventById.getEventStatus()).isEqualTo(SourceControlEvent.EVENT_STATUS_ERROR);
    assertThat(sourceControlEventById.getEventStatusDetails()).isEqualTo("error message");
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

  private SourceControlEvent getNewSourceControlEvent() {
    return getNewSourceControlEvent(app.getId());
  }

  private SourceControlEvent getNewSourceControlEvent(final String applicationId) {
    PolicyEvaluation targetPolicyEvaluation =
        tempEntity.newPolicyEvaluation(app.getId(), StageTypes.BUILD.getId(), "scanId1", false, false, false,
            testStartTime,
            "commitHash1234");

    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(app.getId(), StageTypes.BUILD.getId(), "scanId2", false, false, false,
            testStartTime,
            "commitHash1235");

    return new SourceControlEvent()
        .setApplicationId(applicationId)
        .setCommitHash("abcdefg")
        .setEventType(SourceControlEvent.DISCOVERED_PULL_REQUEST_EVENT)
        .setPolicyEvaluationId(policyEvaluation.getId())
        .setTargetPolicyEvaluationId(targetPolicyEvaluation.getId())
        .setBranchName("branch")
        .setPullRequestNumber(2)
        .setScmUsername("user")
        .setInitiator("webhook")
        .setCreateTime(testStartTime);
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

  private Date toDate(final LocalDateTime localDateTime) {
    return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
  }
}
