/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.zscaler;

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
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class ZScalerClient
{
  private final Logger log = LoggerFactory.getLogger(ZScalerClient.class);

  private static final String V1_URL = "/api/v1/";

  private static final String URL_CATEGORIES = V1_URL + "urlCategories";

  private static final String URL_CATEGORIES_QUOTA = URL_CATEGORIES + "/urlQuota";

  private static final String ACTIVATE = V1_URL + "status/activate";

  private static final String AUTHENTICATED_SESSION = V1_URL + "authenticatedSession";

  private final HttpClient client;

  @Inject
  public ZScalerClient() {
    this.client = HttpClient.newBuilder()
        .cookieHandler(new CookieManager())
        .build();
  }

  public ZScalerClient(HttpClient client) {
    this.client = client;
  }

  public void authenticate(String baseUrl, String username, String password, String apiKey, String timestamp) {
    ZScalerValidator.validateHostName(baseUrl);

    Map<String, String> authPayload = new HashMap<>();
    authPayload.put("username", username);
    authPayload.put("password", password);
    authPayload.put("apiKey", apiKey);
    authPayload.put("timestamp", timestamp);

    String body;
    try {
      body = JsonUtils.writeUnformatted(authPayload);
    }
    catch (Exception e) {
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

      log.info("Authentication successful");
    }
    catch (BadRequestException e) {
      throw e;
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
          JsonUtils.parse(response.body(), new TypeReference<>() { });
      log.info("Fetched {} URL categories", zScalerCategories.size());
      return zScalerCategories;
    }
    catch (Exception e) {
      log.warn("Exception fetching URL categories: {}", e.getMessage());
      return Collections.emptyList();
    }
  }

  public ZScalerQuota getZScalerQuota(final String baseUrl) {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(baseUrl + URL_CATEGORIES_QUOTA))
        .GET()
        .header("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
        .build();

    try {
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        log.warn("Failed to fetch URL categories quota: {}", response.body());
        return null;
      }

      ZScalerQuota quota =
          JsonUtils.parse(response.body(), ZScalerQuota.class);
      log.debug("Fetched URL categories quota");
      return quota;
    }
    catch (Exception e) {
      log.warn("Exception fetching URL categories quota: {}", e.getMessage());
      return null;
    }
  }

  public ZScalerOperationResult<Void> updateCustomUrlCategories(
      String baseUrl, String category, String categoryId, List<String> urls)
  {
    String body;
    try {
      body = JsonUtils.writeUnformatted(new ZScalerUpdateCategory(category, urls));
    }
    catch (Exception e) {
      log.warn("Exception during update category: {}", e.getMessage());
      return ZScalerOperationResult.failure("Failed to serialize update category request: " + e.getMessage());
    }

    log.info("Updating URL categories");
    ZScalerOperationResult<String> result = sendCustomUrlCategoriesMessage(baseUrl, categoryId, body);

    if (result.isSuccess()) {
      return ZScalerOperationResult.success(result.getStatusCode());
    }
    else
    {
      return ZScalerOperationResult.failure(result.getStatusCode(), result.getMessage());
    }
  }

  public ZScalerOperationResult<ZScalerCategory> createCustomUrlCategory(
      final String baseUrl, final String category, final List<String> urls)
  {
    ZScalerCreateCategory create = new ZScalerCreateCategory(category, "USER_DEFINED", urls, "URL_CATEGORY", true);
    String body;
    try {
      body = JsonUtils.writeUnformatted(create);
    }
    catch (Exception e) {
      log.warn("Exception during serialization: {}", e.getMessage());
      return ZScalerOperationResult.failure("Failed to serialize create category request: " + e.getMessage());
    }

    log.info("Creating URL categories");
    ZScalerOperationResult<String> result = sendCustomUrlCategoriesMessage(baseUrl, null, body);

    if (!result.isSuccess()) {
      return ZScalerOperationResult.failure(result.getStatusCode(), result.getMessage());
    }

    try {
      ZScalerCategory createdCategory = JsonUtils.parse(result.getData().orElse(""), ZScalerCategory.class);
      return ZScalerOperationResult.success(result.getStatusCode(), createdCategory);
    }
    catch (Exception e) {
      log.warn("Exception parsing create category response: {}", e.getMessage());
      return ZScalerOperationResult.failure("Failed to parse created category response: " + e.getMessage());
    }
  }

  public ZScalerOperationResult<Void> deleteCustomUrlCategory(final String baseUrl, final String categoryId) {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(baseUrl + URL_CATEGORIES + "/" + categoryId))
        .header("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
        .DELETE()
        .build();

    try {
      HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
      if (response.statusCode() != 204 && response.statusCode() != 200) {
        String errorMsg = "Failed to delete URL category " + categoryId + ": " + response.body();
        log.warn(errorMsg);
        return ZScalerOperationResult.failure(response.statusCode(), errorMsg);
      }
      else {
        log.info("Successfully deleted URL category: {}", categoryId);
        return ZScalerOperationResult.success(response.statusCode());
      }
    }
    catch (Exception e) {
      log.warn("Failed to delete URL category {}: {}", categoryId, e.getMessage());
      return ZScalerOperationResult.failure("Failed to delete URL category " + categoryId + ": " + e.getMessage());
    }
  }

  public void activateChanges(String baseUrl) {
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

  private ZScalerOperationResult<String> sendCustomUrlCategoriesMessage(
      final String baseUrl,
      final String categoryId,
      final String body)
  {
    HttpRequest request;
    if (categoryId == null) {
      request = HttpRequest.newBuilder()
          .uri(URI.create(baseUrl + URL_CATEGORIES))
          .header("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
          .POST(BodyPublishers.ofString(body))
          .build();
    }
    else {
      request = HttpRequest.newBuilder()
          .uri(URI.create(baseUrl + URL_CATEGORIES + '/' + categoryId))
          .header("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
          .PUT(BodyPublishers.ofString(body))
          .build();
    }

    try {
      HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        String errorMsg = "Failed to update URL category: " + response.body();
        log.error(errorMsg);
        return ZScalerOperationResult.failure(response.statusCode(), errorMsg);
      }
      else {
        log.info("Successfully updated URL category: {}", categoryId);
        return ZScalerOperationResult.success(response.statusCode(), response.body());
      }
    }
    catch (Exception e) {
      log.error("Failed to send update URL category: {}", e.getMessage());
      return ZScalerOperationResult.failure("Failed to send update URL category: " + e.getMessage());
    }
  }
}
