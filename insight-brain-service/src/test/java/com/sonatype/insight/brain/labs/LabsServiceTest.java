/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.labs;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.sonatype.insight.brain.testing.BrainInjectedTest;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.StatusLine;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LabsServiceTest
    extends BrainInjectedTest
{
  @Inject
  private LabsService labsService;

  @Test
  public void testConvertResponse() throws Exception {
    int statusCode = 200;
    StatusLine statusLine = mock(StatusLine.class);
    when(statusLine.getStatusCode()).thenReturn(statusCode);

    org.apache.http.HttpResponse httpMockResponse = mock(org.apache.http.HttpResponse.class);
    when(httpMockResponse.getStatusLine()).thenReturn(statusLine);

    Header customHeader1 = createHeader("customHeaderName1", "customHeaderValue1");
    Header customHeader2 = createHeader("customHeaderName2", "customHeaderValue2");

    Header[] mockHeaders = {customHeader1, customHeader2};
    when(httpMockResponse.getAllHeaders()).thenReturn(mockHeaders);

    Response jaxRSResponse = labsService.convertResponse(httpMockResponse);
    assertThat(jaxRSResponse.getStatus()).isEqualTo(statusCode);
    assertThat(jaxRSResponse.getHeaderString(customHeader1.getName())).isEqualTo(customHeader1.getValue());
    assertThat(jaxRSResponse.getHeaderString(customHeader2.getName())).isEqualTo(customHeader2.getValue());
  }

  @Test
  public void testConvertResponse_WithEntity_ContentTypeOnEntityAndHeader() throws Exception {
    int statusCode = 200;
    String contentType = "text/html";
    String contentString = "content";
    InputStream contentStream = IOUtils.toInputStream(contentString, StandardCharsets.UTF_8);

    StatusLine statusLine = mock(StatusLine.class);
    when(statusLine.getStatusCode()).thenReturn(statusCode);

    org.apache.http.HttpResponse httpMockResponse = mock(org.apache.http.HttpResponse.class);
    when(httpMockResponse.getStatusLine()).thenReturn(statusLine);

    Header contentTypeHeader = createHeader("Content-Type", contentType);

    HttpEntity httpEntity = mock(HttpEntity.class);
    // basically setting Content-Type header twice, here and in headers
    when(httpEntity.getContentType()).thenReturn(contentTypeHeader);
    when(httpEntity.getContent()).thenReturn(contentStream);

    when(httpMockResponse.getEntity()).thenReturn(httpEntity);

    Header customHeader1 = createHeader("customHeaderName1", "customHeaderValue1");
    Header customHeader2 = createHeader("customHeaderName2", "customHeaderValue2");

    Header[] mockHeaders = {contentTypeHeader, customHeader1, customHeader2};
    when(httpMockResponse.getAllHeaders()).thenReturn(mockHeaders);

    Response jaxRSResponse = labsService.convertResponse(httpMockResponse);
    assertThat(jaxRSResponse.getStatus()).isEqualTo(statusCode);
    assertThat(jaxRSResponse.getMediaType()).isEqualTo(MediaType.valueOf(contentType));
    assertThat(jaxRSResponse.getEntity()).isEqualTo(contentStream);
    assertThat(jaxRSResponse.getHeaderString(contentTypeHeader.getName())).isEqualTo(contentTypeHeader.getValue());
    assertThat(jaxRSResponse.getHeaderString(customHeader1.getName())).isEqualTo(customHeader1.getValue());
    assertThat(jaxRSResponse.getHeaderString(customHeader2.getName())).isEqualTo(customHeader2.getValue());
  }

  @Test
  public void testConvertResponse_NoEntity_NoHeadersSet() throws Exception {
    int statusCode = 204;

    StatusLine statusLine = mock(StatusLine.class);
    when(statusLine.getStatusCode()).thenReturn(statusCode);

    org.apache.http.HttpResponse httpMockResponse = mock(org.apache.http.HttpResponse.class);
    when(httpMockResponse.getStatusLine()).thenReturn(statusLine);

    when(httpMockResponse.getEntity()).thenReturn(null);

    Response jaxRSResponse = labsService.convertResponse(httpMockResponse);
    assertThat(jaxRSResponse.getStatus()).isEqualTo(statusCode);
    assertThat(jaxRSResponse.getEntity()).isNull();
    assertThat(jaxRSResponse.getHeaderString("Content-Type")).isNull();
  }

  @Test
  public void testConvertResponse_WithEntity_NoContentTypeSetOnEntity() throws Exception {
    int statusCode = 200;
    String contentType = "text/html";
    String contentString = "content";
    InputStream contentStream = IOUtils.toInputStream(contentString, StandardCharsets.UTF_8);

    StatusLine statusLine = mock(StatusLine.class);
    when(statusLine.getStatusCode()).thenReturn(statusCode);

    org.apache.http.HttpResponse httpMockResponse = mock(org.apache.http.HttpResponse.class);
    when(httpMockResponse.getStatusLine()).thenReturn(statusLine);

    Header contentTypeHeader = createHeader("Content-Type", contentType);

    HttpEntity httpEntity = mock(HttpEntity.class);
    // not setting content type on entity
    when(httpEntity.getContentType()).thenReturn(null);
    when(httpEntity.getContent()).thenReturn(contentStream);

    when(httpMockResponse.getEntity()).thenReturn(httpEntity);

    Header[] mockHeaders = {contentTypeHeader};
    when(httpMockResponse.getAllHeaders()).thenReturn(mockHeaders);

    Response jaxRSResponse = labsService.convertResponse(httpMockResponse);
    assertThat(jaxRSResponse.getStatus()).isEqualTo(statusCode);
    // Jersey Jax-RS implementation media type is set if Content-Type header is set, we are able to read contentStream
    assertThat(jaxRSResponse.getMediaType()).isEqualTo(MediaType.valueOf(contentType));
    assertThat(jaxRSResponse.getEntity()).isEqualTo(contentStream);

    assertThat(jaxRSResponse.getHeaderString(contentTypeHeader.getName())).isEqualTo(contentTypeHeader.getValue());
  }

  @Test
  public void testConvertResponse_WithEntity_ContentTypeHeaderNotDirectlySet() throws Exception {
    int statusCode = 200;
    String contentType = "text/html";
    String contentString = "content";
    InputStream contentStream = IOUtils.toInputStream(contentString, StandardCharsets.UTF_8);

    StatusLine statusLine = mock(StatusLine.class);
    when(statusLine.getStatusCode()).thenReturn(statusCode);

    org.apache.http.HttpResponse httpMockResponse = mock(org.apache.http.HttpResponse.class);
    when(httpMockResponse.getStatusLine()).thenReturn(statusLine);

    Header contentTypeHeader = createHeader("Content-Type", contentType);

    HttpEntity httpEntity = mock(HttpEntity.class);
    when(httpEntity.getContentType()).thenReturn(contentTypeHeader);
    when(httpEntity.getContent()).thenReturn(contentStream);

    when(httpMockResponse.getEntity()).thenReturn(httpEntity);

    Response jaxRSResponse = labsService.convertResponse(httpMockResponse);
    assertThat(jaxRSResponse.getStatus()).isEqualTo(statusCode);
    assertThat(jaxRSResponse.getMediaType()).isEqualTo(MediaType.valueOf(contentType));
    assertThat(jaxRSResponse.getEntity()).isEqualTo(contentStream);
    // header added by Jax-RS if set on entity
    assertThat(jaxRSResponse.getHeaderString(contentTypeHeader.getName())).isEqualTo(contentTypeHeader.getValue());
  }

  private Header createHeader(String name, String value) {
    return new Header()
    {
      @Override
      public HeaderElement[] getElements() throws ParseException {
        return new HeaderElement[0];
      }

      @Override
      public String getName() {
        return name;
      }

      @Override
      public String getValue() {
        return value;
      }
    };
  }
}
