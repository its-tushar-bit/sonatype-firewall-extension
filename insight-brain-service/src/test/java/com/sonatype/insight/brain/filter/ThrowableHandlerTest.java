/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.filter;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import java.io.PrintWriter;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.jaxrs.error.JaxRsExceptionMapper;

import com.google.inject.Binder;
import org.eclipse.jetty.server.Handler;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@Category(SlowTest.class)
public class ThrowableHandlerTest
    extends AbstractComponentTest
{
  @Inject
  private ThrowableHandler throwableHandler;

  @Mock
  private Handler mockHandler;

  @Mock
  private HttpServletRequest mockHttpServletRequest;

  @Mock
  private HttpServletResponse mockHttpServletResponse;

  @Mock
  private PrintWriter mockPrintWriter;

  @Mock
  private JaxRsExceptionMapper mockJaxRsExceptionMapper;

  @Mock
  private Response mockResponse;

  @Override
  public void configure(Binder binder) {
    super.configure(binder);
    binder.bind(JaxRsExceptionMapper.class).toInstance(mockJaxRsExceptionMapper);
  }

  @Test
  public void testHandle() throws Exception {
    RuntimeException runtimeException = new RuntimeException("some exception");
    doThrow(runtimeException).when(mockHandler)
        .handle(any(), any(), eq(mockHttpServletRequest), eq(mockHttpServletResponse));
    throwableHandler.setHandler(mockHandler);
    when(mockResponse.getStatus()).thenReturn(400);
    when(mockResponse.getMediaType()).thenReturn(MediaType.TEXT_PLAIN_TYPE);
    when(mockResponse.getEntity()).thenReturn("some entity");
    when(mockJaxRsExceptionMapper.toResponse(runtimeException)).thenReturn(mockResponse);
    when(mockHttpServletResponse.getWriter()).thenReturn(mockPrintWriter);

    throwableHandler.handle(null, null, mockHttpServletRequest, mockHttpServletResponse);

    verify(mockHttpServletResponse).setStatus(400);
    verify(mockHttpServletResponse).setContentType("text/plain");
    InOrder inOrder = inOrder(mockPrintWriter);
    inOrder.verify(mockPrintWriter).print((Object) "some entity");
    inOrder.verify(mockPrintWriter).close();
  }

  @Test
  public void testHandle_AlreadyCommitted() throws Exception {
    RuntimeException runtimeException = new RuntimeException("some exception");
    doThrow(runtimeException).when(mockHandler)
        .handle(any(), any(), eq(mockHttpServletRequest), eq(mockHttpServletResponse));
    throwableHandler.setHandler(mockHandler);
    when(mockHttpServletResponse.isCommitted()).thenReturn(true);
    lenient().when(mockResponse.getStatus()).thenReturn(400);
    lenient().when(mockResponse.getMediaType()).thenReturn(MediaType.TEXT_PLAIN_TYPE);
    lenient().when(mockResponse.getEntity()).thenReturn("some entity");
    lenient().when(mockJaxRsExceptionMapper.toResponse(runtimeException)).thenReturn(mockResponse);
    lenient().when(mockHttpServletResponse.getWriter()).thenReturn(mockPrintWriter);

    throwableHandler.handle(null, null, mockHttpServletRequest, mockHttpServletResponse);

    verify(mockHttpServletResponse, never()).setStatus(400);
    verify(mockHttpServletResponse, never()).setContentType("text/plain");
    verifyNoInteractions(mockPrintWriter);
  }
}
