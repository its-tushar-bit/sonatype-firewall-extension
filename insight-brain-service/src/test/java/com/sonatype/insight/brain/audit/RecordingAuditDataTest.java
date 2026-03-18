/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.security.MDCUsernameScope;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
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
    List<AuditData> committed = new ArrayList<>();
    RequestData requestData = mock(RequestData.class);
    RecordingAuditData recordingAuditData = new RecordingAuditData(committed::add, requestData);
    recordingAuditData.setUsername("username");
    awaitNextTimestamp();

    AuditData auditData = recordingAuditData.forSubEvent(AuditEvent.LOGIN, false, false);

    assertThat(auditData).isInstanceOf(RecordingAuditData.class).isNotEqualTo(recordingAuditData);
    RecordingAuditData childRecordingAuditData = (RecordingAuditData) auditData;
    assertThat(recordingAuditData.getChildren()).containsExactly(childRecordingAuditData);
    assertThat(childRecordingAuditData.getTimestamp()).isEqualTo(recordingAuditData.getTimestamp());
    assertThat(childRecordingAuditData.getRequestData()).isEqualTo(requestData);
    assertThat(childRecordingAuditData.getUsername()).isEqualTo("username");
  }

  @Test
  public void testForSubEvent_Independent() throws Exception {
    List<RecordingAuditData> committed = new ArrayList<>();
    RequestData requestData = mock(RequestData.class);
    RecordingAuditData recordingAuditData = new RecordingAuditData(committed::add, requestData);
    recordingAuditData.setUsername("username");
    awaitNextTimestamp();

    AuditData auditData = recordingAuditData.forSubEvent(AuditEvent.LOGIN, true, false);

    assertThat(auditData).isInstanceOf(RecordingAuditData.class).isNotEqualTo(recordingAuditData);
    assertThat(recordingAuditData.getChildren()).isEmpty();
    RecordingAuditData childRecordingAuditData = (RecordingAuditData) auditData;
    assertThat(childRecordingAuditData.getTimestamp()).isGreaterThan(recordingAuditData.getTimestamp());
    assertThat(childRecordingAuditData.getRequestData()).isEqualTo(requestData);
    assertThat(childRecordingAuditData.getUsername()).isEqualTo("username");
  }

  @Test
  public void testCommit_Self() {
    List<AuditData> committed = new ArrayList<>();
    RecordingAuditData recordingAuditData = new RecordingAuditData(committed::add, null);

    recordingAuditData.commit();

    assertThat(committed).containsExactly(recordingAuditData);
  }

  @Test
  public void testCommit_DependentSubEvent() {
    List<AuditData> committed = new ArrayList<>();
    RecordingAuditData recordingAuditData = new RecordingAuditData(committed::add, null);
    AuditData childAuditData = recordingAuditData.forSubEvent(AuditEvent.LOGIN, false, false);

    childAuditData.commit();

    assertThat(committed).isEmpty();
  }

  @Test
  public void testCommit_IndependentSubEvent() {
    List<AuditData> committed = new ArrayList<>();
    RecordingAuditData recordingAuditData = new RecordingAuditData(committed::add, null);
    AuditData childAuditData = recordingAuditData.forSubEvent(AuditEvent.LOGIN, true, false);

    childAuditData.commit();

    assertThat(committed).containsExactly(childAuditData);
  }

  @Test
  public void testCommitSubEvents() {
    List<AuditData> committedChildren = new ArrayList<>();
    RecordingAuditData auditData = new RecordingAuditData(committedChildren::add, null);
    AuditData child1 = auditData.forSubEvent(AuditEvent.LOGIN, false, false);
    AuditData child2 = auditData.forSubEvent(AuditEvent.LOGIN, false, false);
    AuditData child3 = auditData.forSubEvent(AuditEvent.LOGIN, false, false);
    assertThat(auditData.getChildren()).containsExactly((RecordingAuditData) child1, (RecordingAuditData) child2,
        (RecordingAuditData) child3);
    auditData.commitSubEvents();
    assertThat(auditData.getChildren()).isEmpty();
    assertThat(committedChildren).containsExactly(child1, child2, child3);
  }

  @Test
  public void testSetData_NullValue() {
    RecordingAuditData auditData = new RecordingAuditData(null, null);
    auditData.setData("some-key", "some-value");
    auditData.setData("some-key", null);
    assertThat(auditData.getData()).isEmpty();
  }

  @Test
  public void testForSubEvent_SystemUser() {
    RequestData mockRequestData = mock(RequestData.class);
    RecordingAuditData parent = new RecordingAuditData(null, mockRequestData);

    RecordingAuditData child = (RecordingAuditData) parent.forSubEvent(AuditEvent.LOGIN, true, true);

    assertThat(child.getUsername()).isEqualTo(MDCUsernameScope.SYSTEM);
    assertThat(child.getRequestData()).isNull();
  }

  @Test
  public void testForSubEvent_NonSystemUser() {
    RequestData mockRequestData = mock(RequestData.class);
    RecordingAuditData parent = new RecordingAuditData(null, mockRequestData);
    parent.setUsername("user");

    RecordingAuditData child = (RecordingAuditData) parent.forSubEvent(AuditEvent.LOGIN, true, false);

    assertThat(child.getUsername()).isEqualTo(parent.getUsername());
    assertThat(child.getRequestData()).isEqualTo(mockRequestData);
  }

  @Test
  public void testForSubEvent_SameEvent_CopiesData() {
    RecordingAuditData parent = new RecordingAuditData(null, mock(RequestData.class));
    parent.setEvent(AuditEvent.LOGIN);
    parent.setData("key", "value");

    RecordingAuditData child = (RecordingAuditData) parent.forSubEvent(AuditEvent.LOGIN, true, false);

    assertThat(child.getData()).isEqualTo(parent.getData());
  }

  @Test
  public void testForSubEvent_DifferentEvent_DoesNotCopyData() {
    RecordingAuditData parent = new RecordingAuditData(null, mock(RequestData.class));
    parent.setEvent(AuditEvent.LOGIN);
    parent.setData("key", "value");

    RecordingAuditData child = (RecordingAuditData) parent.forSubEvent(AuditEvent.IMPORT, true, false);

    assertThat(child.getData()).isEmpty();
  }
}
