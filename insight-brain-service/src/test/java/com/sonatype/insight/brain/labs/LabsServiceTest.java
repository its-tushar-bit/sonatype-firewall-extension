/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.labs;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.sonatype.insight.test.InjectedTest;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LabsServiceTest extends InjectedTest
{
  @Inject
  private LabsService labsService;

  @Test
  public void testConvertResponse() throws Exception {
    int statusCode = 200;
    String contentType = "text/html";
    String contentString = "content";
    InputStream contentStream = IOUtils.toInputStream(contentString, StandardCharsets.UTF_8);

    StatusLine statusLine = mock(StatusLine.class);
    when(statusLine.getStatusCode()).thenReturn(statusCode);

    org.apache.http.HttpResponse httpResponse = mock(org.apache.http.HttpResponse.class);
    when(httpResponse.getStatusLine()).thenReturn(statusLine);

    Header header = mock(Header.class);
    when(header.getValue()).thenReturn(contentType);

    HttpEntity httpEntity = mock(HttpEntity.class);
    when(httpEntity.getContentType()).thenReturn(header);
    when(httpEntity.getContent()).thenReturn(contentStream);

    when(httpResponse.getEntity()).thenReturn(httpEntity);

    Response httpResponseTest = labsService.convertResponse(httpResponse);
    assertThat(httpResponseTest.getStatus()).isEqualTo(statusCode);
    assertThat(httpResponseTest.getMediaType()).isEqualTo(MediaType.valueOf(contentType));
    assertThat(httpResponseTest.getEntity()).isEqualTo(contentStream);
  }
}
