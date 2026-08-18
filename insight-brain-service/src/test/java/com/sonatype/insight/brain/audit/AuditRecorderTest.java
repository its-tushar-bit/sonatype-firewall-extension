/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.net.HttpHeaders;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.service.ErrorResponseGenerator;
import com.sonatype.insight.brain.spring.config.SecurityConfiguration;
import com.sonatype.insight.test.LogOutput;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.HttpMethod;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class AuditRecorderTest
{
  @RegisterExtension
  public LogOutput logOutput = new LogOutput(AuditRecorder.toLoggerName(AuditEvent.LOGIN.getDomain()));

  @RegisterExtension
  public TestAuditSession tempAuditData = new TestAuditSession();

  @Test
  public void testRecordUserEvent() {
    HttpServletRequest httpRequest = mockHttpServletRequest();
    AuditRecorder auditRecorder = mock(AuditRecorder.class, Mockito.CALLS_REAL_METHODS);
    try (AuditSession auditSession = auditRecorder.recordUserEvent(httpRequest)) {
      assertThat(auditSession).isNotNull();

      AuditData auditData = AuditData.get();
      assertThat(auditData).isInstanceOf(ProxyAuditData.class);
      ProxyAuditData proxyAuditData = (ProxyAuditData) auditData;

      AuditData childAuditData = proxyAuditData.getAuditData();
      assertThat(childAuditData).isInstanceOf(RecordingAuditData.class);
      RecordingAuditData recordingAuditData = (RecordingAuditData) childAuditData;
      RequestData requestData = recordingAuditData.getRequestData();
      assertThat(requestData.getMethod()).isEqualTo(httpRequest.getMethod());
    }

    // ensure that commitAuditData is called and correct httpRequest value set
    ArgumentCaptor<RecordingAuditData> argumentCaptor = ArgumentCaptor.forClass(RecordingAuditData.class);
    verify(auditRecorder).commitAuditData(argumentCaptor.capture());
    assertThat(argumentCaptor.getValue().getRequestData().getMethod()).isEqualTo(httpRequest.getMethod());

    // ensure recordAuditData is called and httpRequest value is still set
    ArgumentCaptor<RecordingAuditData> argumentCaptor2 = ArgumentCaptor.forClass(RecordingAuditData.class);
    verify(auditRecorder).commitAuditData(argumentCaptor2.capture());
    assertThat(argumentCaptor.getAllValues().get(0).getRequestData().getMethod()).isEqualTo(httpRequest.getMethod());
  }

  @Test
  public void testRecordSystemEvent() {
    AuditEvent auditEvent = AuditEvent.LOGIN; // may not be a proper system event
    AuditRecorder auditRecorder = mock(AuditRecorder.class, Mockito.CALLS_REAL_METHODS);
    doNothing().when(auditRecorder).recordAuditData(isA(RecordingAuditData.class), isNull());
    try (AuditSession auditSession = auditRecorder.recordSystemEvent(auditEvent)) {
      assertThat(auditSession).isNotNull();

      AuditData auditData = AuditData.get();
      assertThat(auditData).isInstanceOf(ProxyAuditData.class);
      ProxyAuditData proxyAuditData = (ProxyAuditData) auditData;

      AuditData childAuditData = proxyAuditData.getAuditData();
      assertThat(childAuditData).isInstanceOf(RecordingAuditData.class);
      RecordingAuditData recordingAuditData = (RecordingAuditData) childAuditData;

      AuditEvent event = recordingAuditData.getEvent();
      assertThat(event.getType()).isEqualTo(AuditEvent.LOGIN.getType());
      assertThat(event.getDomain()).isEqualTo(AuditEvent.LOGIN.getDomain());
      assertThat(recordingAuditData.getUsername()).isEqualTo(MDCUsernameScope.SYSTEM);
    }

    // ensure that commitAuditData is called and correct httpRequest value set
    ArgumentCaptor<RecordingAuditData> argumentCaptor = ArgumentCaptor.forClass(RecordingAuditData.class);
    verify(auditRecorder).commitAuditData(argumentCaptor.capture());
    assertThat(argumentCaptor.getValue().getEvent()).isEqualTo(AuditEvent.LOGIN);

    // ensure recordAuditData is called and httpRequest value is still set
    ArgumentCaptor<RecordingAuditData> argumentCaptor2 = ArgumentCaptor.forClass(RecordingAuditData.class);
    verify(auditRecorder).recordAuditData(argumentCaptor2.capture(), isNull());
    assertThat(argumentCaptor2.getValue().getEvent()).isEqualTo(AuditEvent.LOGIN);
  }

  @Test
  public void testRecordSystemEvent_Null() {
    assertThrows(NullPointerException.class, () -> new AuditRecorder(null).recordSystemEvent(null));
  }

  @Test
  public void testRecordUserEvent_AuthenticationFailure_UnAuthenticated() {
    runRecordUserEventTest(
        recordingAuditData -> recordingAuditData.setHttpStatus(401),
        httpServletRequest -> {
        },
        AuditEvent.AUTHENTICATION_FAILURE,
        AuditErrorType.UNAUTHENTICATED.getValue());
  }

  @Test
  public void testRecordUserEvent_AuthenticationFailure_BadAuthentication() {
    runRecordUserEventTest(
        recordingAuditData -> {
          recordingAuditData.setHttpStatus(401);
          recordingAuditData.setUsername("TestUserName");
        },
        httpServletRequest -> {
        },
        AuditEvent.AUTHENTICATION_FAILURE,
        AuditErrorType.BAD_AUTHENTICATION.getValue());
  }

  @Test
  public void testRecordUserEvent_AuthenticationFailure_BadSession() {
    runRecordUserEventTest(
        recordingAuditData -> recordingAuditData.setHttpStatus(401),
        httpServletRequest -> when(httpServletRequest.getCookies()).thenReturn(
            new Cookie[]{new Cookie(SecurityConfiguration.SESSION_COOKIE_NAME, "AuthCookie")}),
        AuditEvent.AUTHENTICATION_FAILURE,
        AuditErrorType.BAD_SESSION.getValue());
  }

  @Test
  public void testRecordUserEvent_BadRequest() {
    runRecordUserEventTest(
        recordingAuditData -> recordingAuditData.setHttpStatus(400),
        httpServletRequest -> {
        },
        null,
        AuditErrorType.BAD_REQUEST.getValue());
  }

  @Test
  public void testRecordUserEvent_Unlicensed() {
    runRecordUserEventTest(
        recordingAuditData -> recordingAuditData.setHttpStatus(402),
        httpServletRequest -> {
        },
        null,
        AuditErrorType.UNLICENSED.getValue());
  }

  @Test
  public void testRecordUserEvent_Unauthorized() {
    runRecordUserEventTest(
        recordingAuditData -> recordingAuditData.setHttpStatus(403),
        httpServletRequest -> {
        },
        null,
        AuditErrorType.UNAUTHORIZED.getValue());
  }

  @Test
  public void testRecordUserEvent_NotFound() {
    runRecordUserEventTest(
        recordingAuditData -> recordingAuditData.setHttpStatus(404),
        httpServletRequest -> {
        },
        null,
        AuditErrorType.NOT_FOUND.getValue());
  }

  @Test
  public void testRecordUserEvent_ServiceUnavailable() {
    runRecordUserEventTest(
        recordingAuditData -> recordingAuditData.setHttpStatus(503),
        httpServletRequest -> {
        },
        null,
        AuditErrorType.SERVICE_UNAVAILABLE.getValue());
  }

  @Test
  public void testRecordUserEvent_GatewayTimeout() {
    runRecordUserEventTest(
        recordingAuditData -> recordingAuditData.setHttpStatus(504),
        httpServletRequest -> {
        },
        null,
        AuditErrorType.GATEWAY_TIMEOUT.getValue());
  }

  @Test
  public void testRecordUserEvent_Unspecified500Error() {
    runRecordUserEventTest(
        recordingAuditData -> recordingAuditData.setHttpStatus(505),
        httpServletRequest -> {
        },
        null,
        AuditErrorType.SERVER_ERROR.getValue());
  }

  @Test
  public void testRecordUserEvent_Unspecified400Error() {
    runRecordUserEventTest(
        recordingAuditData -> recordingAuditData.setHttpStatus(405),
        httpServletRequest -> {
        },
        null,
        AuditErrorType.CLIENT_ERROR.getValue());
  }

  @Test
  public void testRecordUserEvent_ErrorSet() {
    runRecordUserEventTest(
        recordingAuditData -> {
          recordingAuditData.setEvent(AuditEvent.LOGIN);
          recordingAuditData.setError("ruh roh");
        },
        httpServletRequest -> {
        },
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
        httpServletRequest -> {
        },
        AuditEvent.LOGIN,
        AuditErrorType.SERVER_ERROR.getValue());
  }

  @Test
  public void testRecordUserEvent_UnknownError() {
    runRecordUserEventTest(
        recordingAuditData -> recordingAuditData.setEvent(AuditEvent.LOGIN),
        httpServletRequest -> {
        },
        AuditEvent.LOGIN,
        null);
  }

  private void runRecordUserEventTest(
      Consumer<RecordingAuditData> recordingAuditDataConsumer,
      Consumer<HttpServletRequest> servletRequestConsumer,
      AuditEvent expectedEvent,
      String expectedStatus)
  {
    AuditRecorder auditRecorder = spy(new AuditRecorder(new ErrorResponseGenerator()));
    HttpServletRequest httpRequest = mockHttpServletRequest();
    servletRequestConsumer.accept(httpRequest);

    RecordingAuditData recordingAuditData;
    try (AuditSession auditSession = auditRecorder.recordUserEvent(httpRequest)) {
      assertThat(auditSession).isNotNull();

      ProxyAuditData proxyAuditData = (ProxyAuditData) AuditData.get();
      recordingAuditData = (RecordingAuditData) proxyAuditData.getAuditData();

      recordingAuditDataConsumer.accept(recordingAuditData);
    }

    // ensure that commitAuditData is called and error is set
    ArgumentCaptor<RecordingAuditData> argumentCaptor = ArgumentCaptor.forClass(RecordingAuditData.class);
    verify(auditRecorder).commitAuditData(argumentCaptor.capture());
    assertThat(argumentCaptor.getValue().getEvent()).isEqualTo(expectedEvent);

    // ensure recordAuditData is called and AuditEvent and error string is set
    ArgumentCaptor<RecordingAuditData> recordingAuditDataArg = ArgumentCaptor.forClass(RecordingAuditData.class);
    ArgumentCaptor<String> stringArg = ArgumentCaptor.forClass(String.class);
    if (expectedEvent != null) {
      verify(auditRecorder).recordAuditData(recordingAuditDataArg.capture(), stringArg.capture());
      assertThat(recordingAuditDataArg.getValue()).isEqualTo(recordingAuditData);
      if (expectedStatus != null) {
        assertThat(stringArg.getValue()).contains(expectedStatus);
      }
      else {
        assertThat(stringArg.getValue()).isNull();
      }
    }
    else {
      verify(auditRecorder, never()).recordAuditData(any(RecordingAuditData.class), anyString());
    }
  }

  @Test
  public void testRecordAuditData_ErrorNull() {
    AuditRecorder auditRecorder = spy(new AuditRecorder(new ErrorResponseGenerator()));

    RecordingAuditData parent = spy(new RecordingAuditData(auditData -> {
    }, mock(RequestData.class)));
    parent.setEvent(AuditEvent.LOGIN);
    RecordingAuditData child = (RecordingAuditData) parent.forSubEvent(AuditEvent.LOGIN, false, false);

    auditRecorder.recordAuditData(parent, null);

    verify(auditRecorder).recordAuditData(parent, null);
    verify(auditRecorder).recordAuditData(child, null);
  }

  @Test
  public void testRecordAuditData_ErrorNotNull() {
    String error = "Error";
    AuditRecorder auditRecorder = spy(new AuditRecorder(new ErrorResponseGenerator()));

    RecordingAuditData parent = spy(new RecordingAuditData(auditData -> {
    }, mock(RequestData.class)));
    parent.setEvent(AuditEvent.LOGIN);
    RecordingAuditData child = (RecordingAuditData) parent.forSubEvent(AuditEvent.LOGIN, false, false);

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

  @Test
  public void testToLogger() {
    assertThat(AuditRecorder.toLogger(AuditEvent.AUTHENTICATION_FAILURE).getName())
        .isEqualTo("com.sonatype.insight.audit.authentication");
  }

  @Test
  public void testToObjectNode() {
    AuditRecorder auditRecorder = mock(AuditRecorder.class, Mockito.CALLS_REAL_METHODS);
    ObjectNode objectNode = auditRecorder.toObjectNode(recordingAuditData(), "derivedError");

    assertThat(objectNode).isNotNull();
    ZonedDateTime parsed = ZonedDateTime
        .parse(objectNode.get("timestamp").asText(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    ZonedDateTime now = ZonedDateTime
        .ofInstant(Instant.ofEpochMilli(System.currentTimeMillis()), ZoneId.systemDefault());
    assertThat(parsed).isBeforeOrEqualTo(now);
    assertThat(objectNode.get("requestMethod").asText()).isEqualTo("GET");
    assertThat(objectNode.get("requestUri").asText()).isEqualTo("requestUri?queryString");
    assertThat(objectNode.get("remoteIpAddress").asText()).isEqualTo("remoteAddr");
    assertThat(objectNode.get("forwarded").asText()).isEqualTo("forwarded1, forwarded2, forwarded3");
    assertThat(objectNode.get("userAgent").asText()).isEqualTo("userAgent");
    assertThat(objectNode.get("username").asText()).isEqualTo("username");
    assertThat(objectNode.get("domain").asText()).isEqualTo("authentication");
    assertThat(objectNode.get("type").asText()).isEqualTo("failure");
    assertThat(objectNode.get("error").asText()).isEqualTo("derivedError");
    assertThat(objectNode.get("data").get("key1").asText()).isEqualTo("value1");
    assertThat(objectNode.get("data").get("key2").asLong()).isEqualTo(1);
  }

  @Test
  public void testToObjectNode_NonAuthentication_ExcludesMethodPath() {
    RecordingAuditData recordingAuditData = recordingAuditData();
    recordingAuditData.setEvent(AuditEvent.EVALUATE_APPLICATION);

    AuditRecorder auditRecorder = mock(AuditRecorder.class, Mockito.CALLS_REAL_METHODS);
    ObjectNode objectNode = auditRecorder.toObjectNode(recordingAuditData, "derivedError");

    assertThat(objectNode.has("requestMethod")).isFalse();
    assertThat(objectNode.has("requestUri")).isFalse();
  }

  @Test
  public void testToObjectNode_NullData() {
    RecordingAuditData recordingAuditData = new RecordingAuditData(null, null);
    recordingAuditData.setEvent(AuditEvent.AUTHENTICATION_FAILURE);

    AuditRecorder auditRecorder = mock(AuditRecorder.class, Mockito.CALLS_REAL_METHODS);
    ObjectNode objectNode = auditRecorder.toObjectNode(recordingAuditData, "derivedError");

    assertThat(objectNode).isNotNull();
    ZonedDateTime parsed = ZonedDateTime
        .parse(objectNode.get("timestamp").asText(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    ZonedDateTime now = ZonedDateTime
        .ofInstant(Instant.ofEpochMilli(System.currentTimeMillis()), ZoneId.systemDefault());
    assertThat(parsed).isBeforeOrEqualTo(now);
    assertThat(objectNode.has("requestMethod")).isFalse();
    assertThat(objectNode.has("requestUri")).isFalse();
    assertThat(objectNode.has("remoteIpAddress")).isFalse();
    assertThat(objectNode.has("forwarded")).isFalse();
    assertThat(objectNode.has("userAgent")).isFalse();
    assertThat(objectNode.get("username").asText()).isEqualTo("*UNKNOWN");
    assertThat(objectNode.get("domain").asText()).isEqualTo("authentication");
    assertThat(objectNode.get("type").asText()).isEqualTo("failure");
    assertThat(objectNode.get("error").asText()).isEqualTo("derivedError");
    assertThat(objectNode.has("data")).isFalse();
  }

  @Test
  public void testLog() {
    RecordingAuditData recordingAuditData = recordingAuditData();

    AuditRecorder auditRecorder = mock(AuditRecorder.class, Mockito.CALLS_REAL_METHODS);
    auditRecorder.log(recordingAuditData, "derivedError");

    assertThat(logOutput).atInfoLevel()
        .contains(auditRecorder.toObjectNode(recordingAuditData, "derivedError").toString());
  }

  private RecordingAuditData recordingAuditData() {
    RecordingAuditData recordingAuditData = new RecordingAuditData(null, requestData());
    recordingAuditData.setEvent(AuditEvent.AUTHENTICATION_FAILURE);
    recordingAuditData.setError("error");
    recordingAuditData.setException(new Exception("exception"));
    recordingAuditData.setUsername("username");
    recordingAuditData.setHttpStatus(500);
    recordingAuditData.setData("key1", "value1");
    recordingAuditData.setData("key2", 1);
    return recordingAuditData;
  }

  private RequestData requestData() {
    HttpServletRequest mockHttpServletRequest = mock(HttpServletRequest.class);
    when(mockHttpServletRequest.getHeaders(HttpHeaders.FORWARDED)).thenReturn(Collections.emptyEnumeration());
    when(mockHttpServletRequest.getHeaders(HttpHeaders.X_FORWARDED_FOR)).thenReturn(Collections.emptyEnumeration());
    when(mockHttpServletRequest.getMethod()).thenReturn("GET");
    when(mockHttpServletRequest.getRequestURI()).thenReturn("requestUri");
    when(mockHttpServletRequest.getQueryString()).thenReturn("queryString");
    when(mockHttpServletRequest.getRemoteAddr()).thenReturn("remoteAddr");
    when(mockHttpServletRequest.getHeaders(HttpHeaders.FORWARDED))
        .thenReturn(Collections.enumeration(Arrays.asList("forwarded1", "forwarded2", "forwarded3")));
    when(mockHttpServletRequest.getHeader(HttpHeaders.USER_AGENT)).thenReturn("userAgent");
    when(mockHttpServletRequest.getCookies()).thenReturn(new Cookie[]{
      new Cookie("cookieName1", "cookieValue1"),
      new Cookie(SecurityConfiguration.SESSION_COOKIE_NAME, "sessionId"),
      new Cookie("cookieName3", "cookieValue3")
    });
    return RequestData.newInstance(mockHttpServletRequest);
  }
}
