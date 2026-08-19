/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import java.io.InputStream;

import jakarta.ws.rs.core.Response.Status;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

public class HttpHelper
{
  public static HttpResponse createMockResponse(Exception e) throws Exception {
    HttpResponse mockResponse = mock(HttpResponse.class);
    Header mockHeader = mock(Header.class);
    lenient().when(mockHeader.getValue()).thenReturn("text/plain");
    lenient().when(mockResponse.getFirstHeader(org.apache.http.HttpHeaders.CONTENT_TYPE)).thenReturn(mockHeader);
    HttpEntity mockEntity = mock(HttpEntity.class);
    lenient().when(mockEntity.getContent()).thenThrow(e);
    lenient().when(mockResponse.getEntity()).thenReturn(mockEntity);
    StatusLine mockStatusLine = mock(StatusLine.class);
    lenient().when(mockStatusLine.getReasonPhrase()).thenReturn("reason");
    lenient().when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
    return mockResponse;
  }

  public static HttpResponse createMockResponse(Status status) {
    HttpResponse mockResponse = mock(HttpResponse.class);
    StatusLine mockStatusLine = mock(StatusLine.class);
    lenient().when(mockStatusLine.getReasonPhrase()).thenReturn(status.getReasonPhrase());
    lenient().when(mockStatusLine.getStatusCode()).thenReturn(status.getStatusCode());
    lenient().when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
    return mockResponse;
  }

  public static HttpResponse createMockResponse(Status status, InputStream content) throws Exception {
    HttpResponse mockResponse = createMockResponse(status);
    HttpEntity mockEntity = mock(HttpEntity.class);
    lenient().when(mockEntity.getContent()).thenReturn(content);
    lenient().when(mockResponse.getEntity()).thenReturn(mockEntity);
    return mockResponse;
  }
}
