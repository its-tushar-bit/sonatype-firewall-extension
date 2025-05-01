/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.zscaler;

import com.sonatype.insight.brain.dataaccess.configuration.ZScalerConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.ZScalerConfiguration;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.test.LogOutput;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ApiZScalerServiceTest
{
  private ZScalerConfiguration config;

  private InputStream urls;

  private String url = "http://example-url";

  private String timestamp = "1234567890";

  @Mock
  private HttpResponse<String> mockResponse;

  @Mock
  private HttpClient client;

  @Rule
  public LogOutput logOutput = new LogOutput(ApiZScalerService.class);

  @Mock
  private ZScalerConfigurationDAO dao;

  @Spy
  @InjectMocks
  private ApiZScalerService underTest;

  @Before
  public void setUp() throws Exception {
    config = new ZScalerConfiguration();
    config.setUsername("user");
    config.setPassword("password");
    config.setApikey("abcdefghijkl");
    config.setHostname("host");

    urls = new ByteArrayInputStream(url.getBytes(StandardCharsets.UTF_8));

    underTest = Mockito.spy(new ApiZScalerService(dao, client));
  }

  @Test
  public void testUpdateCategories() throws Exception {
    when(dao.get()).thenReturn(config);
    doNothing().when(underTest).authenticate(anyString(), anyString(), anyString(), anyString(), anyString());
    doNothing().when(underTest).updateCategories(anyString(), any(ZScalerFormat.class), any(InputStream.class));
    doNothing().when(underTest).activateChanges(anyString());

    underTest.updateCategories(ZScalerFormat.NPM, urls);

    verify(dao).get();
    verify(underTest).authenticate(anyString(), anyString(), anyString(), anyString(), anyString());
    verify(underTest).updateCategories(anyString(), any(ZScalerFormat.class), any(InputStream.class));
    verify(underTest).activateChanges(anyString());
  }

  @Test
  public void testUpdateCategories_noConfiguration() throws Exception {
    when(dao.get()).thenReturn(null);

    assertThrows("No zScaler configuration found", BadRequestException.class,
        () -> underTest.updateCategories(ZScalerFormat.NPM, urls));

    assertThat(logOutput).atWarnLevel().contains("No zScaler configuration found");
  }

  @Test
  public void testUpdateCategories_exceptionAuthenticating() throws Exception {
    when(dao.get()).thenReturn(config);
    doThrow(BadRequestException.class)
        .when(underTest)
        .authenticate(anyString(), anyString(), anyString(), anyString(), anyString());

    assertThrows("Authentication failed: ", BadRequestException.class,
        () -> underTest.updateCategories(ZScalerFormat.NPM, urls));

    verify(dao).get();
    verify(underTest).authenticate(anyString(), anyString(), anyString(), anyString(), anyString());
  }

  @Test
  public void testUpdateCategories_exceptionUpdatingCategories() throws Exception {
    when(dao.get()).thenReturn(config);
    doNothing().when(underTest).authenticate(anyString(), anyString(), anyString(), anyString(), anyString());
    doThrow(RuntimeException.class)
        .when(underTest)
        .updateCategories(anyString(), any(ZScalerFormat.class), any(InputStream.class));

    assertThrows(RuntimeException.class, () -> underTest.updateCategories(ZScalerFormat.NPM, urls));

    verify(dao).get();
    verify(underTest).authenticate(anyString(), anyString(), anyString(), anyString(), anyString());
    verify(underTest).updateCategories(anyString(), any(ZScalerFormat.class), any(InputStream.class));
  }

  @Test
  public void testUpdateCategories_exceptionActivatingChanges() throws Exception {
    when(dao.get()).thenReturn(config);
    doNothing().when(underTest).authenticate(anyString(), anyString(), anyString(), anyString(), anyString());
    doNothing().when(underTest).updateCategories(anyString(), any(ZScalerFormat.class), any(InputStream.class));
    doThrow(RuntimeException.class)
        .when(underTest)
        .activateChanges(anyString());

    assertThrows(RuntimeException.class, () -> underTest.updateCategories(ZScalerFormat.NPM, urls));

    verify(dao).get();
    verify(underTest).authenticate(anyString(), anyString(), anyString(), anyString(), anyString());
    verify(underTest).updateCategories(anyString(), any(ZScalerFormat.class), any(InputStream.class));
    verify(underTest).activateChanges(anyString());
  }

  @Test
  public void testAuthenticate() throws Exception {
    when(mockResponse.statusCode()).thenReturn(200);
    when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);

    underTest.authenticate(url, config.getUsername(), config.getPassword(), config.getApikey(), timestamp);

    assertThrows(RuntimeException.class, () -> underTest.updateCategories(ZScalerFormat.NPM, urls));
    verify(client).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    assertThat(logOutput).atInfoLevel().contains("Authenticated successfully");
  }

  @Test
  public void testAuthenticate_failed() throws Exception {
    when(mockResponse.statusCode()).thenReturn(400);
    when(mockResponse.body()).thenReturn("Authentication failed");

    when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);

    assertThrows(BadRequestException.class,
        () -> underTest.authenticate(url, config.getUsername(), config.getPassword(), config.getApikey(),
            timestamp));

    verify(client).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    assertThat(logOutput).atWarnLevel().contains("Authentication failed: Authentication failed");
  }

  @Test
  public void testAuthenticate_exception() throws Exception {
    doThrow(IOException.class)
        .when(client)
        .send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));

    assertThrows(BadRequestException.class,
        () -> underTest.authenticate(url, config.getUsername(), config.getPassword(), config.getApikey(),
            timestamp));

    verify(client).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    assertThat(logOutput).atWarnLevel().contains("Exception during authentication: ");
  }

  @Test
  public void testGetCustomUrlCategories() throws Exception {
    String jsonResponse = "[{\"id\":\"1\",\"configuredName\":\"maven2\"}]";

    when(mockResponse.statusCode()).thenReturn(200);
    when(mockResponse.body()).thenReturn(jsonResponse);
    when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);

    List<ZScalerCategory> result = underTest.getCustomUrlCategories(url);

    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals("maven2", result.get(0).getConfiguredName());
    verify(client).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    assertThat(logOutput).atInfoLevel().contains("Fetched 1 URL categories");
  }

  @Test
  public void testGetCustomUrlCategories_failed() throws Exception {
    when(mockResponse.statusCode()).thenReturn(500);
    when(mockResponse.body()).thenReturn("Server error");
    when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);

    List<ZScalerCategory> result = underTest.getCustomUrlCategories(url);

    assertNotNull(result);
    assertTrue(result.isEmpty());
    verify(client).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    assertThat(logOutput).atWarnLevel().contains("Failed to fetch URL categories: Server error");
  }

  @Test
  public void testGetCustomUrlCategories_exception() throws Exception {
    when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenThrow(new IOException("Connection error"));

    // Act
    List<ZScalerCategory> result = underTest.getCustomUrlCategories(url);

    // Assert
    assertNotNull(result);
    assertTrue(result.isEmpty());
    verify(client, Mockito.times(1))
        .send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    assertThat(logOutput).atWarnLevel().contains("Exception fetching URL categories: Connection error");
  }

  @Test
  public void testUpdateCustomUrlCategories() throws Exception {
    when(mockResponse.statusCode()).thenReturn(200);
    when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

    underTest.updateCustomUrlCategories(url, "npm", "npm-category", List.of(url));

    verify(client).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    assertThat(logOutput).atInfoLevel().contains("Successfully updated URL category: npm-category");
  }

  @Test
  public void testUpdateCustomUrlCategories_failed() throws Exception {
    when(mockResponse.statusCode()).thenReturn(400);
    when(mockResponse.body()).thenReturn("Bad Request");
    when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

    underTest.updateCustomUrlCategories(url, "maven2", "maven-category", List.of(url));

    verify(client).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    assertThat(logOutput).atWarnLevel().contains("Failed to update URL category: Bad Request");
  }

  @Test
  public void testUpdateCustomUrlCategories_exception() throws Exception {
    when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenThrow(
        new IOException("Connection error"));

    underTest.updateCustomUrlCategories(url, "maven2", "maven-category", List.of(url));

    verify(client).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    assertThat(logOutput).atWarnLevel().contains("Failed to update URL category: Connection error");
  }

  @Test
  public void testUpdateCategories_categoryExistsAndUpdatedSuccessfully() {
    ZScalerCategory existingCategory = new ZScalerCategory();
    existingCategory.setId("npm-category");
    existingCategory.setConfiguredName("npm");
    existingCategory.setCustomCategory(true);
    existingCategory.setUrls(List.of("https://npmjs.org/npm/"));

    when(underTest.getCustomUrlCategories(url)).thenReturn(List.of(existingCategory));
    doNothing().when(underTest).updateCustomUrlCategories(anyString(), anyString(), anyString(), anyList());

    underTest.updateCategories(url, ZScalerFormat.NPM, urls);

    verify(underTest).getCustomUrlCategories(url);
    verify(underTest).updateCustomUrlCategories(eq(url), eq("npm"), eq("npm-category"), eq(List.of(url)));
    verify(underTest, never()).createCustomUrlCategory(anyString(), anyString(), anyList());
    assertThat(logOutput).atInfoLevel().contains("npm category with id npm-category already exists, updating it");
  }

  @Test
  public void testUpdateCategories_categoryDoesNotExistAndCreatedSuccessfully() {
    when(underTest.getCustomUrlCategories(url)).thenReturn(Collections.emptyList());
    doNothing().when(underTest).createCustomUrlCategory(anyString(), anyString(), anyList());

    underTest.updateCategories(url, ZScalerFormat.NPM, urls);

    verify(underTest).getCustomUrlCategories(url);
    verify(underTest, never()).updateCustomUrlCategories(anyString(), anyString(), anyString(), anyList());
    verify(underTest).createCustomUrlCategory(eq(url), eq("npm"), anyList());
    assertThat(logOutput).atInfoLevel().contains("npm category does not exist, creating it");
  }

  @Test
  public void testUpdateCategories_exceptionDuringGetCustomUrlCategories() {
    when(underTest.getCustomUrlCategories(url)).thenThrow(new RuntimeException("Error fetching categories"));

    assertThrows(RuntimeException.class, () -> underTest.updateCategories(url, ZScalerFormat.NPM, urls));

    verify(underTest, times(1)).getCustomUrlCategories(url);
    verify(underTest, never()).updateCustomUrlCategories(anyString(), anyString(), anyString(), anyList());
    verify(underTest, never()).createCustomUrlCategory(anyString(), anyString(), anyList());
  }

  @Test
  public void testUpdateCategories_exceptionDuringUpdateCustomUrlCategories() {
    ZScalerCategory existingCategory = new ZScalerCategory();
    existingCategory.setId("npm-category");
    existingCategory.setConfiguredName("npm");
    existingCategory.setCustomCategory(true);
    existingCategory.setUrls(List.of("https://npmjs.org/npm/"));
    when(underTest.getCustomUrlCategories(url)).thenReturn(List.of(existingCategory));
    doThrow(new RuntimeException("Error updating category"))
        .when(underTest).updateCustomUrlCategories(anyString(), anyString(), anyString(), anyList());

    assertThrows(RuntimeException.class, () -> underTest.updateCategories(url, ZScalerFormat.NPM, urls));

    verify(underTest, times(1)).getCustomUrlCategories(url);
    verify(underTest, times(1)).updateCustomUrlCategories(eq(url), eq("npm"), eq("npm-category"), eq(List.of(url)));
    verify(underTest, never()).createCustomUrlCategory(anyString(), anyString(), anyList());
  }

  @Test
  public void testUpdateCategories_exceptionDuringCreateCustomUrlCategory() {
    when(underTest.getCustomUrlCategories(url)).thenReturn(Collections.emptyList());
    doThrow(new RuntimeException("Error creating category"))
        .when(underTest).createCustomUrlCategory(anyString(), anyString(), anyList());

    assertThrows(RuntimeException.class, () -> underTest.updateCategories(url, ZScalerFormat.NPM, urls));

    verify(underTest, times(1)).getCustomUrlCategories(url);
    verify(underTest, never()).updateCustomUrlCategories(anyString(), anyString(), anyString(), anyList());
    verify(underTest, times(1)).createCustomUrlCategory(eq(url), eq("npm"), anyList());
  }

  @Test
  public void testCreateCustomUrlCategory() throws Exception {
    when(mockResponse.statusCode()).thenReturn(200);
    when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

    underTest.createCustomUrlCategory(url, "npm", List.of(url));

    verify(client).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
  }

  @Test
  public void testCreateCustomUrlCategory_failed() throws Exception {
    when(mockResponse.statusCode()).thenReturn(400);
    when(mockResponse.body()).thenReturn("Bad Request");
    when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

    underTest.createCustomUrlCategory(url, "npm", List.of(url));

    verify(client).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    assertThat(logOutput).atWarnLevel().contains("Failed to create category: Bad Request");
    assertThat(logOutput).atErrorLevel()
        .contains("Exception creating category: Failed to create category: Bad Request");
  }

  @Test
  public void testCreateCustomUrlCategory_exception() throws Exception {
    when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenThrow(
        new IOException("Connection error"));

    underTest.createCustomUrlCategory(url, "maven2", List.of(url));

    verify(client).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    assertThat(logOutput).atErrorLevel().contains("Exception creating category: Connection error");
  }

  @Test
  public void testActivateChanges() throws Exception {
    when(mockResponse.statusCode()).thenReturn(200);
    when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

    underTest.activateChanges(url);

    verify(client).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    assertThat(logOutput).atInfoLevel().contains("Successfully activated changes");
  }

  @Test
  public void testActivateChanges_failed() throws Exception {
    when(mockResponse.statusCode()).thenReturn(400);
    when(mockResponse.body()).thenReturn("Bad Request");
    when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

    underTest.activateChanges(url);

    verify(client).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    assertThat(logOutput).atWarnLevel().contains("Failed to activate changes: Bad Request");
  }

  @Test
  public void testActivateChanges_exception() throws Exception {
    when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenThrow(
        new IOException("Connection error"));

    underTest.activateChanges(url);

    verify(client).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    assertThat(logOutput).atErrorLevel().contains("Exception activating changes: Connection error");
  }

  @Test
  public void testObfuscateApiKey_ValidKeyAndTimestamp() {
    String result = ApiZScalerService.obfuscateApiKey("abcdefghijkl", "1678886400000");

    assertEquals("eaaaaaeccccc", result);
  }

  @Test
  public void testObfuscateApiKey_ShortKey() {
    assertThrows(IndexOutOfBoundsException.class, () -> ApiZScalerService.obfuscateApiKey("abc", "1678886400000"));
  }

  @Test
  public void testObfuscateApiKey_InvalidTimestamp() {
    assertThrows(NumberFormatException.class, () -> ApiZScalerService.obfuscateApiKey("abcdefghijkl", "invalid"));
  }
}
