/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.zscaler;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.api.v2.HasFeature;
import com.sonatype.insight.brain.dataaccess.configuration.ZScalerConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.configuration.ZScalerConfiguration;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.error.exception.BadRequestException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;

@Named
@HasFeature(SystemConfigurationPropertyFeature.ZSCALER)
public class ApiZScalerService
{
  private static final String V1_URL = "/api/v1/";

  private static final String URL_CATEGORIES = V1_URL + "urlCategories";

  private static final String ACTIVATE = V1_URL + "status/activate";

  private static final String AUTHENTICATED_SESSION = V1_URL + "authenticatedSession";

  private final Logger log = LoggerFactory.getLogger(ApiZScalerService.class);

  private final ObjectMapper objectMapper = new ObjectMapper();

  private final HttpClient client;

  private final ZScalerConfigurationDAO zScalerConfigurationDAO;

  private final PasswordHandler passwordHandler;

  @Inject
  public ApiZScalerService(
      final ZScalerConfigurationDAO zScalerConfigurationDAO,
      final PasswordHandler passwordHandler)
  {
    this.zScalerConfigurationDAO = zScalerConfigurationDAO;
    this.passwordHandler = passwordHandler;
    this.client = HttpClient.newBuilder()
        .cookieHandler(new CookieManager())
        .build();
  }

  // New constructor allowing HttpClient injection (for testing)
  public ApiZScalerService(
      final ZScalerConfigurationDAO zScalerConfigurationDAO,
      final HttpClient client,
      final PasswordHandler passwordHandler)
  {
    this.zScalerConfigurationDAO = zScalerConfigurationDAO;
    this.client = client;
    this.passwordHandler = passwordHandler;
  }

  public void updateCategories(final ZScalerFormat format, final InputStream urls) {
    ZScalerConfiguration configuration = zScalerConfigurationDAO.get();
    if (configuration == null) {
      log.warn("No zScaler configuration found");
      throw new BadRequestException("No zScaler configuration found");
    }

    String apiKey = configuration.getApikey();
    String timestamp = String.valueOf(System.currentTimeMillis());
    String obfuscatedKey = obfuscateApiKey(apiKey, timestamp);

    authenticate(configuration.getHostname(), configuration.getUsername(),
        passwordHandler.decryptPassword(configuration.getPassword()), obfuscatedKey, timestamp);

    updateCategories(configuration.getHostname(), format, urls);
    activateChanges(configuration.getHostname());
  }

  public void authenticate(String baseUrl, String username, String password, String apiKey, String timestamp) {
    Map<String, String> authPayload = new HashMap<>();
    authPayload.put("username", username);
    authPayload.put("password", password);
    authPayload.put("apiKey", apiKey);
    authPayload.put("timestamp", timestamp);

    String body;
    try {
      body = objectMapper.writeValueAsString(authPayload);
    }
    catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(baseUrl + AUTHENTICATED_SESSION))
        .header("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build();

    try {
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        log.warn("Authentication failed: {}", response.body());
        throw new BadRequestException("Authentication failed: " + response.body());
      }
      log.info("Authenticated successfully");
    }
    catch (Exception e) {
      log.warn("Exception during authentication: {}", e.getMessage());
      throw new BadRequestException(e.getMessage());
    }
  }

  List<ZScalerCategory> getCustomUrlCategories(String baseUrl) {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(baseUrl + URL_CATEGORIES + "?customOnly=true"))
        .header("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
        .GET()
        .build();

    try {
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        log.warn("Failed to fetch URL categories: {}", response.body());
        return Collections.emptyList();
      }

      List<ZScalerCategory> zScalerCategories =
          objectMapper.readValue(response.body(), new TypeReference<>() { });
      log.info("Fetched {} URL categories", zScalerCategories.size());
      return zScalerCategories;
    }
    catch (Exception e) {
      log.warn("Exception fetching URL categories: {}", e.getMessage());
      return Collections.emptyList();
    }
  }

  void updateCustomUrlCategories(String baseUrl, String category, String categoryId, List<String> urls) {
    String body;
    try {
      body = objectMapper.writeValueAsString(new ZScalerUpdateCategory(category, urls));
    }
    catch (JsonProcessingException e) {
      log.warn("Exception during update category: {}", e.getMessage());
      throw new RuntimeException(e);
    }

    HttpRequest put = HttpRequest.newBuilder()
        .uri(URI.create(baseUrl + URL_CATEGORIES + "/" + categoryId))
        .header("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
        .PUT(BodyPublishers.ofString(body))
        .build();

    try {
      HttpResponse<String> response = client.send(put, BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        log.warn("Failed to update URL category: {}", response.body());
      }
      else {
        log.info("Successfully updated URL category: {}", categoryId);
      }
    }
    catch (Exception e) {
      log.warn("Failed to update URL category: {}", e.getMessage());
    }
  }

  void updateCategories(String baseUrl, ZScalerFormat selectedFormat, InputStream urls) {
    List<ZScalerCategory> categories = getCustomUrlCategories(baseUrl);
    String category = selectedFormat.name().toLowerCase();
    ZScalerCategory existingCategory = categories.stream()
        .filter(cat -> cat.getConfiguredName().toLowerCase().equals(category))
        .findFirst()
        .orElse(null);

    List<String> urlsList = new BufferedReader(new InputStreamReader(urls, UTF_8))
        .lines()
        .toList();

    if (existingCategory != null) {
      String categoryId = existingCategory.getId();
      log.info("{} category with id {} already exists, updating it", category, categoryId);
      updateCustomUrlCategories(baseUrl, category, categoryId, urlsList);
    }
    else {
      log.info("{} category does not exist, creating it", category);
      createCustomUrlCategory(baseUrl, category, urlsList);
    }
  }

  void createCustomUrlCategory(final String baseUrl, final String category, final List<String> urls) {
    ZScalerCreateCategory create = new ZScalerCreateCategory(category, "USER_DEFINED", urls, "URL_CATEGORY", true);
    String body;
    try {
      body = objectMapper.writeValueAsString(create);
    }
    catch (JsonProcessingException e) {
      log.warn("Exception during serialization: {}", e.getMessage());
      throw new RuntimeException(e);
    }

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(baseUrl + URL_CATEGORIES))
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .header("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
        .build();

    try {
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        log.warn("Failed to create category: {}", response.body());
        throw new RuntimeException("Failed to create category: " + response.body());
      }
      else {
        log.info("Successfully create category");
      }
    }
    catch (Exception e) {
      log.error("Exception creating category: {}", e.getMessage());
    }
  }

  void activateChanges(String baseUrl) {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(baseUrl + ACTIVATE))
        .POST(HttpRequest.BodyPublishers.noBody())
        .header("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
        .build();

    try {
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        log.warn("Failed to activate changes: {}", response.body());
      }
      else {
        log.info("Successfully activated changes");
      }
    }
    catch (Exception e) {
      log.error("Exception activating changes: {}", e.getMessage());
    }
  }

  public static String obfuscateApiKey(String key, String timestamp) {
    int apiKeySize = 12;
    StringBuilder retVal = new StringBuilder();
    char[] key1Arr = key.substring(0, apiKeySize - 2).toCharArray();
    char[] key2Arr = key.substring(2).toCharArray();
    String hiIndices = timestamp.substring(timestamp.length() - 6);
    int hiTime = Integer.parseInt(hiIndices);
    int loTime = hiTime >> 1;
    String loIndices = String.format("%06d", loTime);
    for (char index : hiIndices.toCharArray()) {
      retVal.append(key1Arr[index - '0']);
    }

    for (char index : loIndices.toCharArray()) {
      retVal.append(key2Arr[index - '0']);
    }

    return retVal.toString();
  }
}
