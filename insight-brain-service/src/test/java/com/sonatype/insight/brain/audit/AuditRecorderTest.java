/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import java.util.Collections;
import java.util.function.Consumer;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.HttpMethod;

import com.sonatype.insight.brain.audit.AuditRecorder.HttpStatusString;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.security.SecurityModule;
import com.sonatype.insight.brain.service.ErrorResponseGenerator;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AuditRecorderTest
{
  @Before
  public void before() {
    AuditData.set(null);
  }

  @Test
  public void testRecordUserEvent() {
    HttpServletRequest httpRequest = mockHttpServletRequest();
    AuditRecorder auditRecorder = mock(AuditRecorder.class, Mockito.CALLS_REAL_METHODS);
    AuditSession auditSession = auditRecorder.recordUserEvent(httpRequest);
    assertThat(auditSession, is(notNullValue()));

    AuditData auditData = AuditData.get();
    assertThat(auditData, is(instanceOf(ProxyAuditData.class)));
    ProxyAuditData proxyAuditData = (ProxyAuditData) auditData;

    AuditData childAuditData = proxyAuditData.auditData;
    assertThat(childAuditData, is(instanceOf(RecordingAuditData.class)));
    RecordingAuditData recordingAuditData = (RecordingAuditData) childAuditData;
    RequestData requestData = recordingAuditData.getRequestData();
    assertThat(requestData.getMethod(), is(httpRequest.getMethod()));

    // ensure that commitAuditData is called and correct httpRequest value set
    auditSession.close();
    ArgumentCaptor<RecordingAuditData> argumentCaptor = ArgumentCaptor.forClass(RecordingAuditData.class);
    verify(auditRecorder).commitAuditData(argumentCaptor.capture());
    assertThat(argumentCaptor.getValue().getRequestData().getMethod(), is(httpRequest.getMethod()));

    // ensure recordAuditData is called and httpRequest value is still set
    ArgumentCaptor<RecordingAuditData> argumentCaptor2 = ArgumentCaptor.forClass(RecordingAuditData.class);
    verify(auditRecorder).commitAuditData(argumentCaptor2.capture());
    assertThat(argumentCaptor.getAllValues().get(0).getRequestData().getMethod(), is(httpRequest.getMethod()));
  }

  @Test
  public void testRecordSystemEvent() {
    AuditEvent auditEvent = AuditEvent.LOGIN;  // may not be a proper system event
    AuditRecorder auditRecorder = mock(AuditRecorder.class, Mockito.CALLS_REAL_METHODS);
    doNothing().when(auditRecorder).recordAuditData(isA(RecordingAuditData.class), isNull());
    AuditSession auditSession = auditRecorder.recordSystemEvent(auditEvent);
    assertThat(auditSession, is(notNullValue()));

    AuditData auditData = AuditData.get();
    assertThat(auditData, is(instanceOf(ProxyAuditData.class)));
    ProxyAuditData proxyAuditData = (ProxyAuditData) auditData;

    AuditData childAuditData = proxyAuditData.auditData;
    assertThat(childAuditData, is(instanceOf(RecordingAuditData.class)));
    RecordingAuditData recordingAuditData = (RecordingAuditData) childAuditData;

    AuditEvent event = recordingAuditData.getEvent();
    assertThat(event.getType(), is(AuditEvent.LOGIN.getType()));
    assertThat(event.getDomain(), is(AuditEvent.LOGIN.getDomain()));
    assertThat(recordingAuditData.getUsername(), is(MDCUsernameScope.SYSTEM));

    auditSession.close();
    // ensure that commitAuditData is called and correct httpRequest value set
    ArgumentCaptor<RecordingAuditData> argumentCaptor = ArgumentCaptor.forClass(RecordingAuditData.class);
    verify(auditRecorder).commitAuditData(argumentCaptor.capture());
    assertThat(argumentCaptor.getValue().getEvent(), is(AuditEvent.LOGIN));

    // ensure recordAuditData is called and httpRequest value is still set
    ArgumentCaptor<RecordingAuditData> argumentCaptor2 = ArgumentCaptor.forClass(RecordingAuditData.class);
    verify(auditRecorder).recordAuditData(argumentCaptor2.capture(), isNull());
    assertThat(argumentCaptor2.getValue().getEvent(), is(AuditEvent.LOGIN));
  }

  @Test
  public void testRecordUserEvent_AuthenticationFailure_UnAuthenticated() {
    runRecordUserEventTest(
        recordingAuditData -> recordingAuditData.setHttpStatus(401),
        httpServletRequest -> {},
        AuditEvent.AUTHENTICATION_FAILURE,
        HttpStatusString.UNAUTHENTICATED.getLogString());
  }

  @Test
  public void testRecordUserEvent_AuthenticationFailure_BadAuthentication() {
    runRecordUserEventTest(
        recordingAuditData -> {
          recordingAuditData.setHttpStatus(401);
          recordingAuditData.setUsername("TestUserName");
        },
        httpServletRequest -> {},
        AuditEvent.AUTHENTICATION_FAILURE,
        HttpStatusString.BAD_AUTHENTICATION.getLogString());
  }

  @Test
  public void testRecordUserEvent_AuthenticationFailure_BadSession() {
    runRecordUserEventTest(
        recordingAuditData -> recordingAuditData.setHttpStatus(401),
        httpServletRequest -> when(httpServletRequest.getCookies()).thenReturn(
            new Cookie[] { new Cookie(SecurityModule.SESSION_COOKIE_NAME, "AuthCookie") }),
        AuditEvent.AUTHENTICATION_FAILURE,
        HttpStatusString.BAD_SESSION.getLogString());
  }

  @Test
  public void testRecordUserEvent_BadRequest() {
    runRecordUserEventTest(
        recordingAuditData -> recordingAuditData.setHttpStatus(400),
        httpServletRequest -> {},
        null,
        HttpStatusString.BAD_REQUEST.getLogString());
  }

  @Test
  public void testRecordUserEvent_Unlicensed() {
    runRecordUserEventTest(
        recordingAuditData -> recordingAuditData.setHttpStatus(402),
        httpServletRequest -> {},
        null,
        HttpStatusString.UNLICENSED.getLogString());
  }

  @Test
  public void testRecordUserEvent_Unauthorized() {
    runRecordUserEventTest(
        recordingAuditData -> recordingAuditData.setHttpStatus(403),
        httpServletRequest -> {},
        null,
        HttpStatusString.UNAUTHORIZED.getLogString());
  }

  @Test
  public void testRecordUserEvent_NotFound() {
    runRecordUserEventTest(
        recordingAuditData -> recordingAuditData.setHttpStatus(404),
        httpServletRequest -> {},
        null,
        HttpStatusString.NOT_FOUND.getLogString());
  }

  @Test
  public void testRecordUserEvent_ServiceUnavailable() {
    runRecordUserEventTest(
        recordingAuditData -> recordingAuditData.setHttpStatus(503),
        httpServletRequest -> {},
        null,
        HttpStatusString.SERVICE_UNAVAILABLE.getLogString());
  }

  @Test
  public void testRecordUserEvent_GatewayTimeout() {
    runRecordUserEventTest(
        recordingAuditData -> recordingAuditData.setHttpStatus(504),
        httpServletRequest -> {},
        null,
        HttpStatusString.GATEWAY_TIMEOUT.getLogString());
  }

  @Test
  public void testRecordUserEvent_Unspecified500Error() {
    runRecordUserEventTest(
        recordingAuditData -> recordingAuditData.setHttpStatus(505),
        httpServletRequest -> {},
        null,
        HttpStatusString.SERVER_ERROR.getLogString());
  }

  @Test
  public void testRecordUserEvent_Unspecified400Error() {
    runRecordUserEventTest(
        recordingAuditData -> recordingAuditData.setHttpStatus(405),
        httpServletRequest -> {},
        null,
        HttpStatusString.CLIENT_ERROR.getLogString());
  }

  @Test
  public void testRecordUserEvent_ErrorSet() {
    runRecordUserEventTest(
        recordingAuditData -> {
          recordingAuditData.setEvent(AuditEvent.LOGIN);
          recordingAuditData.setError("ruh roh");
        },
        httpServletRequest -> {},
        AuditEvent.LOGIN,
        "ruh roh");
  }

  @Test
  public void testRecordUserEvent_ExceptionSet() {
    runRecordUserEventTest(
        recordingAuditData -> {
          recordingAuditData.setEvent(AuditEvent.LOGIN);
          recordingAuditData.setException(new Exception("ruh roh"));
        },
        httpServletRequest -> {},
        AuditEvent.LOGIN,
        HttpStatusString.SERVER_ERROR.getLogString());
  }

  @Test
  public void testRecordUserEvent_UnknownError() {
    runRecordUserEventTest(
        recordingAuditData -> {
          recordingAuditData.setEvent(AuditEvent.LOGIN);
        },
        httpServletRequest -> {},
        AuditEvent.LOGIN,
        null);
  }

  private void runRecordUserEventTest(Consumer<RecordingAuditData> recordingAuditDataConsumer,
                                      Consumer<HttpServletRequest> servletRequestConsumer,
                                      AuditEvent expectedEvent,
                                      String expectedStatus)
  {
    AuditRecorder auditRecorder = spy(new AuditRecorder(new ErrorResponseGenerator()));
    doNothing().when(auditRecorder).logData(isA(RecordingAuditData.class), any());
    HttpServletRequest httpRequest = mockHttpServletRequest();
    servletRequestConsumer.accept(httpRequest);

    AuditSession auditSession = auditRecorder.recordUserEvent(httpRequest);
    assertThat(auditSession, is(notNullValue()));

    ProxyAuditData proxyAuditData = (ProxyAuditData) AuditData.get();
    RecordingAuditData recordingAuditData = (RecordingAuditData) proxyAuditData.auditData;

    recordingAuditDataConsumer.accept(recordingAuditData);

    auditSession.close();
    // ensure that commitAuditData is called and error is set
    ArgumentCaptor<RecordingAuditData> argumentCaptor = ArgumentCaptor.forClass(RecordingAuditData.class);
    verify(auditRecorder).commitAuditData(argumentCaptor.capture());
    assertThat(argumentCaptor.getValue().getEvent(), is(expectedEvent));

    // ensure recordAuditData is called and AuditEvent and error string is set
    ArgumentCaptor<RecordingAuditData> recordingAuditDataArg = ArgumentCaptor.forClass(RecordingAuditData.class);
    ArgumentCaptor<String> stringArg = ArgumentCaptor.forClass(String.class);
    if (expectedEvent != null) {
      verify(auditRecorder).recordAuditData(recordingAuditDataArg.capture(), stringArg.capture());
      assertThat(recordingAuditDataArg.getValue(), is(recordingAuditData));
      assertThat(stringArg.getValue(), expectedStatus == null ? nullValue() :
          containsString(expectedStatus));
    }
    else {
      verify(auditRecorder, never()).recordAuditData(any(RecordingAuditData.class), anyString());
    }
  }

  @Test
  public void testRecordAuditData_ErrorNull() {
    AuditRecorder auditRecorder = spy(new AuditRecorder(new ErrorResponseGenerator()));
    doNothing().when(auditRecorder).logData(isA(RecordingAuditData.class), any());

    RecordingAuditData parent = spy(new RecordingAuditData(auditData -> {}, null));
    RecordingAuditData child = (RecordingAuditData) parent.forSubEvent(AuditEvent.LOGIN, false);

    auditRecorder.recordAuditData(parent, null);

    verify(auditRecorder).recordAuditData(parent, null);
    verify(auditRecorder).recordAuditData(child, null);
  }

  @Test
  public void testRecordAuditData_ErrorNotNull() {
    String error = "Error";
    AuditRecorder auditRecorder = spy(new AuditRecorder(new ErrorResponseGenerator()));
    doNothing().when(auditRecorder).logData(isA(RecordingAuditData.class), any());

    RecordingAuditData parent = spy(new RecordingAuditData(auditData -> {}, null));
    RecordingAuditData child = (RecordingAuditData) parent.forSubEvent(AuditEvent.LOGIN, false);

    auditRecorder.recordAuditData(parent, error);

    verify(auditRecorder).recordAuditData(parent, error);
    verify(auditRecorder, never()).recordAuditData(child, null);
  }

  private HttpServletRequest mockHttpServletRequest() {
    HttpServletRequest mockHttpServletRequest = mock(HttpServletRequest.class);
    when(mockHttpServletRequest.getMethod()).thenReturn(HttpMethod.GET);
    when(mockHttpServletRequest.getHeaders(anyString())).thenReturn(Collections.emptyEnumeration());
    return mockHttpServletRequest;
  }
}
