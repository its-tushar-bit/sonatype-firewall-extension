/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import java.util.Arrays;
import java.util.Collections;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import com.sonatype.insight.brain.security.SecurityModule;
import com.sonatype.insight.brain.service.ErrorResponseGenerator;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.net.HttpHeaders;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MultiTenantAuditRecorderTest
{
  @Mock
  ErrorResponseGenerator errorResponseGenerator;

  @Test
  public void testAuditLogIncludesTenantAndLevel() {
    MultiTenantAuditRecorder recorder = new MultiTenantAuditRecorder(errorResponseGenerator);

    ObjectNode result = recorder.toObjectNode(recordingAuditData(), "error");

    String tenantSlug = TenantThreadLocal.getTenant().tenantSlug;

    assertThat(result.toString()).contains("\"mdc\":{\"tenant\":\"" + tenantSlug + "\"}");
    assertThat(result.toString()).contains("\"level\":\"INFO\"");
    assertThat(result.toString()).contains("\"logType\":\"AuditLog\"");
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
    when(mockHttpServletRequest.getMethod()).thenReturn("GET");
    when(mockHttpServletRequest.getRequestURI()).thenReturn("requestUri");
    when(mockHttpServletRequest.getQueryString()).thenReturn("queryString");
    when(mockHttpServletRequest.getRemoteAddr()).thenReturn("remoteAddr");
    when(mockHttpServletRequest.getHeaders(HttpHeaders.FORWARDED))
        .thenReturn(Collections.enumeration(Arrays.asList("forwarded1", "forwarded2", "forwarded3")));
    when(mockHttpServletRequest.getHeader(HttpHeaders.USER_AGENT)).thenReturn("userAgent");
    when(mockHttpServletRequest.getCookies()).thenReturn(new Cookie[]{
        new Cookie("cookieName1", "cookieValue1"),
        new Cookie(SecurityModule.SESSION_COOKIE_NAME, "sessionId"),
        new Cookie("cookieName3", "cookieValue3")
    });
    return RequestData.newInstance(mockHttpServletRequest);
  }
}
