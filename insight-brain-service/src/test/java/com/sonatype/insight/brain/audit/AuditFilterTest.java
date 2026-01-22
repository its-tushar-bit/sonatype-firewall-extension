/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.sonatype.insight.brain.audit.AuditFilter.ResponseWrapper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AuditFilterTest
{
  @Rule
  public TestAuditSession testAuditSession = new TestAuditSession();

  @Rule
  public MockitoRule mockito = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

  @Mock
  private AuditData auditData;

  private static final String MESSAGE = "message";

  @Mock
  private HttpServletResponse httpServletResponse;

  private ResponseWrapper responseWrapper;

  @Before
  public void before() {
    testAuditSession.set(auditData);
    responseWrapper = new ResponseWrapper(httpServletResponse);
  }

  @Test
  public void testSetStatus_NotFound() {
    responseWrapper.setStatus(HttpServletResponse.SC_NOT_FOUND);

    verify(httpServletResponse).setStatus(HttpServletResponse.SC_NOT_FOUND);
    verify(auditData).setHttpStatus(HttpServletResponse.SC_NOT_FOUND);
  }

  @Test
  public void testSetStatus_Ok() {
    responseWrapper.setStatus(HttpServletResponse.SC_OK);

    verify(httpServletResponse).setStatus(HttpServletResponse.SC_OK);
    verify(auditData, never()).setHttpStatus(anyInt());
  }

  @Test
  public void testSetStatus_BadRequest() {
    responseWrapper.setStatus(HttpServletResponse.SC_BAD_REQUEST);

    verify(httpServletResponse).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    verify(auditData).setHttpStatus(HttpServletResponse.SC_BAD_REQUEST);
  }

  @Test
  public void testSetStatus_WithMessage_NotFound() {
    responseWrapper.setStatus(HttpServletResponse.SC_NOT_FOUND);

    verify(httpServletResponse).setStatus(HttpServletResponse.SC_NOT_FOUND);
    verify(auditData).setHttpStatus(HttpServletResponse.SC_NOT_FOUND);
  }

  @Test
  public void testSetStatus_WithMessage_Ok() {
    responseWrapper.setStatus(HttpServletResponse.SC_OK);

    verify(httpServletResponse).setStatus(HttpServletResponse.SC_OK);
    verify(auditData, never()).setHttpStatus(anyInt());
  }

  @Test
  public void testSetStatus_WithMessage_BadRequest() {
    responseWrapper.setStatus(HttpServletResponse.SC_BAD_REQUEST);

    verify(httpServletResponse).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    verify(auditData).setHttpStatus(HttpServletResponse.SC_BAD_REQUEST);
  }

  @Test
  public void testSendError_NotFound() throws Exception {
    responseWrapper.sendError(HttpServletResponse.SC_NOT_FOUND);

    verify(httpServletResponse).sendError(HttpServletResponse.SC_NOT_FOUND);
    verify(auditData).setHttpStatus(HttpServletResponse.SC_NOT_FOUND);
  }

  @Test
  public void testSendError_Ok() throws Exception {
    responseWrapper.sendError(HttpServletResponse.SC_OK);

    verify(httpServletResponse).sendError(HttpServletResponse.SC_OK);
    verify(auditData, never()).setHttpStatus(anyInt());
  }

  @Test
  public void testSendError_BadRequest() throws Exception {
    responseWrapper.sendError(HttpServletResponse.SC_BAD_REQUEST);

    verify(httpServletResponse).sendError(HttpServletResponse.SC_BAD_REQUEST);
    verify(auditData).setHttpStatus(HttpServletResponse.SC_BAD_REQUEST);
  }

  @Test
  public void testSendError_WithMessage_NotFound() throws Exception {
    responseWrapper.sendError(HttpServletResponse.SC_NOT_FOUND, MESSAGE);

    verify(httpServletResponse).sendError(HttpServletResponse.SC_NOT_FOUND, MESSAGE);
    verify(auditData).setHttpStatus(HttpServletResponse.SC_NOT_FOUND);
  }

  @Test
  public void testSendError_WithMessage_Ok() throws Exception {
    responseWrapper.sendError(HttpServletResponse.SC_OK, MESSAGE);

    verify(httpServletResponse).sendError(HttpServletResponse.SC_OK, MESSAGE);
    verify(auditData, never()).setHttpStatus(anyInt());
  }

  @Test
  public void testSendError_WithMessage_BadRequest() throws Exception {
    responseWrapper.sendError(HttpServletResponse.SC_BAD_REQUEST, MESSAGE);

    verify(httpServletResponse).sendError(HttpServletResponse.SC_BAD_REQUEST, MESSAGE);
    verify(auditData).setHttpStatus(HttpServletResponse.SC_BAD_REQUEST);
  }

  @Test
  public void testDoFilter() throws Exception {
    final HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);
    when(httpServletResponse.getStatus()).thenReturn(200);
    final HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
    final FilterChain filterChain = mock(FilterChain.class, CALLS_REAL_METHODS);
    final AuditSession auditSession = mock(AuditSession.class);
    final AuditRecorder auditRecorder = mock(AuditRecorder.class);
    when(auditRecorder.recordUserEvent(httpServletRequest)).thenReturn(auditSession);

    final AuditFilter auditFilter = new AuditFilter(auditRecorder);

    final ArgumentCaptor<HttpServletResponse> responseArgumentCaptor = ArgumentCaptor
        .forClass(HttpServletResponse.class);
    final ArgumentCaptor<HttpServletRequest> requestArgumentCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);

    auditFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);

    verify(filterChain).doFilter(requestArgumentCaptor.capture(), responseArgumentCaptor.capture());
    assertThat(requestArgumentCaptor.getValue()).isEqualTo(httpServletRequest);
    assertThat(responseArgumentCaptor.getValue()).isInstanceOf(ResponseWrapper.class);
    assertThat(responseArgumentCaptor.getValue().getStatus()).isEqualTo(200);
    verify(auditRecorder).recordUserEvent(httpServletRequest);
    verify(auditSession).close();
  }
}
