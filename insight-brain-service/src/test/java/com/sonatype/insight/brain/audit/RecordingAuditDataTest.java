/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import java.util.function.Consumer;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;

public class RecordingAuditDataTest
{
  @Test(expected = UnsupportedOperationException.class)
  public void testContinueAsync_Unsupported() {
    new RecordingAuditData(null, null).continueAsync(null);
  }

  @Test
  public void testForSubEvent_Dependent() throws Exception {
    String[] result = new String[1];
    Consumer<RecordingAuditData> recorder = recordingAuditData -> result[0] = recordingAuditData.toString();
    RequestData requestData = mock(RequestData.class);
    RecordingAuditData recordingAuditData = new RecordingAuditData(recorder, requestData);
    recordingAuditData.setUsername("username");
    Thread.sleep(10);

    AuditData auditData = recordingAuditData.forSubEvent(AuditEvent.AUTHENTICATION_FAILURE, false);

    assertThat(auditData, is(instanceOf(RecordingAuditData.class)));
    RecordingAuditData childRecordingAuditData = (RecordingAuditData) auditData;
    assertThat(childRecordingAuditData.getTimestamp(), is(recordingAuditData.getTimestamp()));
    childRecordingAuditData.commit();
    assertThat(result[0], is(nullValue()));
    assertThat(childRecordingAuditData.getRequestData(), is(requestData));
    assertThat(childRecordingAuditData.getUsername(), is("username"));
    assertThat(recordingAuditData, not(auditData));
    assertThat(recordingAuditData.getChildren(), contains(auditData));
  }

  @Test
  public void testForSubEvent_Independent() throws Exception {
    String[] result = new String[1];
    Consumer<RecordingAuditData> recorder = recordingAuditData -> result[0] = recordingAuditData.toString();
    RequestData requestData = mock(RequestData.class);
    RecordingAuditData recordingAuditData = new RecordingAuditData(recorder, requestData);
    recordingAuditData.setUsername("username");
    Thread.sleep(10);

    AuditData auditData = recordingAuditData.forSubEvent(AuditEvent.AUTHENTICATION_FAILURE, true);

    assertThat(auditData, is(instanceOf(RecordingAuditData.class)));
    RecordingAuditData childRecordingAuditData = (RecordingAuditData) auditData;
    assertThat(childRecordingAuditData.getTimestamp(), greaterThan(recordingAuditData.getTimestamp()));
    childRecordingAuditData.commit();
    assertThat(result[0], is(childRecordingAuditData.toString()));
    assertThat(childRecordingAuditData.getRequestData(), is(requestData));
    assertThat(childRecordingAuditData.getUsername(), is("username"));
    assertThat(recordingAuditData, not(auditData));
    assertThat(recordingAuditData.getChildren(), is(empty()));
  }

  @Test
  public void testCommit_Dependent() {
    String[] result = new String[1];
    Consumer<RecordingAuditData> recorder = recordingAuditData -> result[0] = recordingAuditData.toString();
    RecordingAuditData recordingAuditData = new RecordingAuditData(recorder, null);
    RecordingAuditData childRecordingAuditData = (RecordingAuditData) recordingAuditData
        .forSubEvent(AuditEvent.AUTHENTICATION_FAILURE, false);

    childRecordingAuditData.commit();

    assertThat(result[0], is(nullValue()));
  }

  @Test
  public void testCommit_Independent() {
    String[] result = new String[1];
    Consumer<RecordingAuditData> recorder = recordingAuditData -> result[0] = recordingAuditData.toString();
    RecordingAuditData recordingAuditData = new RecordingAuditData(recorder, null);
    RecordingAuditData childRecordingAuditData = (RecordingAuditData) recordingAuditData
        .forSubEvent(AuditEvent.AUTHENTICATION_FAILURE, true);

    childRecordingAuditData.commit();

    assertThat(result[0], is(childRecordingAuditData.toString()));
  }
}
