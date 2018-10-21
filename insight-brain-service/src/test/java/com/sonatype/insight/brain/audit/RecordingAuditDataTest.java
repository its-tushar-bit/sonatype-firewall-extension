/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;

public class RecordingAuditDataTest
{
  @Test(expected = UnsupportedOperationException.class)
  public void testContinueAsync_Unsupported() {
    new RecordingAuditData(null, null).continueAsync(null);
  }

  private void awaitNextTimestamp() throws Exception {
    long now = System.currentTimeMillis();
    while (System.currentTimeMillis() <= now) {
      Thread.sleep(1);
    }
  }

  @Test
  public void testForSubEvent_Dependent() throws Exception {
    List<RecordingAuditData> committed = new ArrayList<>();
    RequestData requestData = mock(RequestData.class);
    RecordingAuditData recordingAuditData = new RecordingAuditData(committed::add, requestData);
    recordingAuditData.setUsername("username");
    awaitNextTimestamp();

    AuditData auditData = recordingAuditData.forSubEvent(AuditEvent.LOGIN, false);

    assertThat(auditData, is(instanceOf(RecordingAuditData.class)));
    assertThat(auditData, is(not(recordingAuditData)));
    assertThat(recordingAuditData.getChildren(), contains(auditData));
    RecordingAuditData childRecordingAuditData = (RecordingAuditData) auditData;
    assertThat(childRecordingAuditData.getTimestamp(), is(recordingAuditData.getTimestamp()));
    assertThat(childRecordingAuditData.getRequestData(), is(requestData));
    assertThat(childRecordingAuditData.getUsername(), is("username"));
  }

  @Test
  public void testForSubEvent_Independent() throws Exception {
    List<RecordingAuditData> committed = new ArrayList<>();
    RequestData requestData = mock(RequestData.class);
    RecordingAuditData recordingAuditData = new RecordingAuditData(committed::add, requestData);
    recordingAuditData.setUsername("username");
    awaitNextTimestamp();

    AuditData auditData = recordingAuditData.forSubEvent(AuditEvent.LOGIN, true);

    assertThat(auditData, is(instanceOf(RecordingAuditData.class)));
    assertThat(auditData, is(not(recordingAuditData)));
    assertThat(recordingAuditData.getChildren(), is(empty()));
    RecordingAuditData childRecordingAuditData = (RecordingAuditData) auditData;
    assertThat(childRecordingAuditData.getTimestamp(), greaterThan(recordingAuditData.getTimestamp()));
    assertThat(childRecordingAuditData.getRequestData(), is(requestData));
    assertThat(childRecordingAuditData.getUsername(), is("username"));
  }

  @Test
  public void testCommit_Self() {
    List<RecordingAuditData> committed = new ArrayList<>();
    RecordingAuditData recordingAuditData = new RecordingAuditData(committed::add, null);

    recordingAuditData.commit();

    assertThat(committed, contains(recordingAuditData));
  }

  @Test
  public void testCommit_DependentSubEvent() {
    List<RecordingAuditData> committed = new ArrayList<>();
    RecordingAuditData recordingAuditData = new RecordingAuditData(committed::add, null);
    AuditData childAuditData = recordingAuditData.forSubEvent(AuditEvent.LOGIN, false);

    childAuditData.commit();

    assertThat(committed, is(empty()));
  }

  @Test
  public void testCommit_IndependentSubEvent() {
    List<RecordingAuditData> committed = new ArrayList<>();
    RecordingAuditData recordingAuditData = new RecordingAuditData(committed::add, null);
    AuditData childAuditData = recordingAuditData.forSubEvent(AuditEvent.LOGIN, true);

    childAuditData.commit();

    assertThat(committed, contains(childAuditData));
  }

  @Test
  public void testCommitSubEvents() {
    List<RecordingAuditData> committedChildren = new ArrayList<>();
    RecordingAuditData auditData = new RecordingAuditData(committedChildren::add, null);
    AuditData child1 = auditData.forSubEvent(AuditEvent.LOGIN, false);
    AuditData child2 = auditData.forSubEvent(AuditEvent.LOGIN, false);
    AuditData child3 = auditData.forSubEvent(AuditEvent.LOGIN, false);
    assertThat(auditData.getChildren(), contains(child1, child2, child3));
    auditData.commitSubEvents();
    assertThat(auditData.getChildren(), is(empty()));
    assertThat(committedChildren, contains(child1, child2, child3));
  }

  @Test
  public void testSetData_NullValue() {
    RecordingAuditData auditData = new RecordingAuditData(null, null);
    auditData.setData("some-key", "some-value");
    auditData.setData("some-key", null);
    assertThat(auditData.getData().keySet(), is(empty()));
  }
}
