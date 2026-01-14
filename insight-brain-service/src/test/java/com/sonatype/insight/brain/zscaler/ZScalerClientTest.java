/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.zscaler;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ZScalerClientTest
{
  @Mock
  private HttpClient mockHttpClient;

  @Mock
  private HttpResponse<String> mockHttpResponse;

  @Mock
  private HttpResponse<String> mockAdminResponse;

  @Mock
  private  HttpResponse<String> mockRoleResponse;

  private ZScalerClient underTest;

  @Before
  public void setup() {
    underTest = new ZScalerClient(mockHttpClient);
  }

  @Test
  public void shouldAuthenticateSuccessfully() throws Exception {
    // Mock response for authentication
    HttpResponse<String> authResponse = mockHttpResponse;

    when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(authResponse);

    // Authentication response
    when(authResponse.statusCode()).thenReturn(200);

    underTest.authenticate("http://example.com", "user", "pass", "apiKey", "timestamp");

    // Should make only 1 call: auth
    verify(mockHttpClient, times(1)).send(any(), any());
  }

  @Test
  public void shouldThrowBadRequestExceptionWhenAuthenticationFails() throws Exception {
    when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockHttpResponse);
    when(mockHttpResponse.statusCode()).thenReturn(401);
    when(mockHttpResponse.body()).thenReturn("Unauthorized");

    BadRequestException exception = assertThrows(BadRequestException.class, () ->
        underTest.authenticate("http://example.com", "user", "pass", "apiKey", "timestamp")
    );

    assertEquals("Authentication failed: Unauthorized", exception.getMessage());
  }

  @Test
  public void shouldReturnListOfUrlCategoriesWhenResponseIsValid() throws Exception {
    when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockHttpResponse);
    when(mockHttpResponse.statusCode()).thenReturn(200);
    when(mockHttpResponse.body()).thenReturn("[{\"id\":\"1234\",\"urls\":[\"http://badurl.com\"]}]");

    List<ZScalerCategory> categories = underTest.getCustomUrlCategories("http://example.com");

    assertNotNull(categories);
    assertEquals(1, categories.size());
    assertEquals("1234", categories.get(0).getId());
    assertEquals("http://badurl.com", categories.get(0).getUrls().get(0));
  }

  @Test
  public void shouldReturnEmptyListWhenResponseStatusIsNot200() throws Exception {
    when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockHttpResponse);
    when(mockHttpResponse.statusCode()).thenReturn(500);

    List<ZScalerCategory> categories = underTest.getCustomUrlCategories("http://example.com");

    assertNotNull(categories);
    assertTrue(categories.isEmpty());
  }

  @Test
  public void shouldLogSuccessMessageWhenActivationIsSuccessful() throws Exception {
    when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockHttpResponse);
    when(mockHttpResponse.statusCode()).thenReturn(200);

    underTest.activateChanges("http://example.com");

    verify(mockHttpClient).send(any(), any());
  }

  @Test
  public void shouldLogWarningMessageWhenActivationFails() throws Exception {
    when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockHttpResponse);
    when(mockHttpResponse.statusCode()).thenReturn(400);
    when(mockHttpResponse.body()).thenReturn("Bad Request");

    underTest.activateChanges("http://example.com");

    verify(mockHttpClient).send(any(), any());
  }

  @Test
  public void shouldReturnNullWhenQuotaResponseIsNot200() throws Exception {
    when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockHttpResponse);
    when(mockHttpResponse.statusCode()).thenReturn(500);

    ZScalerQuota quota = underTest.getZScalerQuota("http://example.com");

    assertNull(quota);
  }

  @Test
  public void shouldReturnQuotaWhenResponseIsValid() throws Exception {
    when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockHttpResponse);
    when(mockHttpResponse.statusCode()).thenReturn(200);
    when(mockHttpResponse.body()).thenReturn("{\"uniqueUrlsProvisioned\": 12, \"remainingUrlsQuota\":100}");

    ZScalerQuota quota = underTest.getZScalerQuota("http://example.com");

    assertNotNull(quota);
    assertEquals(12, quota.getUniqueUrlsProvisioned());
    assertEquals(100, quota.getRemainingUrlsQuota());
  }

  @Test
  public void testAuthenticate_integratesWithUrlValidation_invalidProtocol() {
    // Test that client layer calls validation before attempting authentication
    BadRequestException exception = assertThrows(BadRequestException.class,
        () -> underTest.authenticate("ftp://badurl.com", "user", "pass", "apiKey", "timestamp"));

    assertThat(exception.getMessage())
        .isEqualTo("Protocol must be http or https");
  }

  @Test
  public void testAuthenticate_integratesWithUrlValidation_nullHostname() {
    // Test that client layer validates null hostnames
    BadRequestException exception = assertThrows(BadRequestException.class,
        () -> underTest.authenticate(null, "user", "pass", "apiKey", "timestamp"));

    assertThat(exception.getMessage()).isEqualTo("Host name is required");
  }
}
