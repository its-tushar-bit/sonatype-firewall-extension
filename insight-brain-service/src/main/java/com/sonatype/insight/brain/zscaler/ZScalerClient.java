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
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.error.exception.BadRequestException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

  private static final String ADMIN_USERS_ME = V1_URL + "adminUsers/me";

  private static final String ADMIN_ROLES = V1_URL + "adminRoles/";

  private final ObjectMapper objectMapper = new ObjectMapper();

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

      fetchAdminDetails(baseUrl, response);
    }
    catch (Exception e) {
      log.warn("Exception during authentication: {}", e.getMessage());
      throw new BadRequestException(e.getMessage());
    }
  }

  private void fetchAdminDetails(String baseUrl, HttpResponse<String> authResponse) throws Exception {
    Optional<String> jsessionIdCookie = authResponse.headers()
        .allValues("Set-Cookie")
        .stream()
        .filter(cookie -> cookie.startsWith("JSESSIONID"))
        .findFirst();

    if (jsessionIdCookie.isEmpty()) {
      throw new IllegalStateException("JSESSIONID cookie not found in auth response");
    }

    String jsessionId = jsessionIdCookie.get().split(";", 2)[0];

    HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + ADMIN_USERS_ME))
            .header("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
            .header("Cookie", jsessionId)
            .GET()
            .build();

    HttpResponse<String> adminResponse = client.send(request, HttpResponse.BodyHandlers.ofString());

    if (adminResponse.statusCode() == 200) {
      log.debug("Admin details: {}", adminResponse.body());

      String roleId = extractRoleId(adminResponse.body());
      fetchAdminRoleDetails(baseUrl, jsessionId, roleId);
    }
    else {
      log.warn("Failed to get admin: {}", adminResponse.body());
      throw new BadRequestException("Failed to get admin: " + adminResponse.body());
    }
  }

  private String extractRoleId(String responseBody) throws Exception {
    Map<String, Object> responseMap = objectMapper.readValue(responseBody, new TypeReference<>() {});
    Map<String, Object> role = objectMapper.convertValue(
        responseMap.get("role"),
        new TypeReference<Map<String, Object>>() {}
    );
    return role.get("id").toString();
  }

  private void fetchAdminRoleDetails(String baseUrl, String jsessionId, String roleId) throws Exception {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(baseUrl + ADMIN_ROLES + roleId))
        .header("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
        .header("Cookie", jsessionId)
        .GET()
        .build();

    HttpResponse<String> roleResponse = client.send(request, HttpResponse.BodyHandlers.ofString());

    if (roleResponse.statusCode() == 200) {
      log.debug("Admin role details: {}", roleResponse.body());

      Map<String, Object> roleDetails = objectMapper.readValue(roleResponse.body(), new TypeReference<>() {});
      Map<String, String> featurePermissions = objectMapper.convertValue(
          roleDetails.get("featurePermissions"),
          new TypeReference<Map<String, String>>() {}
      );
      verifyFeaturePermissions(featurePermissions);
    }
    else {
      log.warn("Failed to get admin role details: {}", roleResponse.body());
      throw new BadRequestException("Failed to get admin role details: " + roleResponse.body());
    }
  }

  private void verifyFeaturePermissions(Map<String, String> featurePermissions) {
    List<String> requiredPermissions = List.of("OVERRIDE_EXISTING_CAT", "CUSTOM_URL_CAT");
    for (String permission : requiredPermissions) {
      if (!featurePermissions.containsKey(permission)) {
        throw new BadRequestException("Insufficient permissions: " + permission + " is missing");
      }
      String value = featurePermissions.get(permission);
      if (!"READ_WRITE".equals(value)) {
        throw new BadRequestException("Permission " + permission + " does not have READ and WRITE access");
      }
    }
    log.info("Authenticated successfully with required permissions");
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
          objectMapper.readValue(response.body(), ZScalerQuota.class);
      log.debug("Fetched URL categories quota");
      return quota;
    }
    catch (Exception e) {
      log.warn("Exception fetching URL categories quota: {}", e.getMessage());
      return null;
    }
  }

  public void updateCustomUrlCategories(String baseUrl, String category, String categoryId, List<String> urls) {
    String body;
    try {
      body = objectMapper.writeValueAsString(new ZScalerUpdateCategory(category, urls));
    }
    catch (JsonProcessingException e) {
      log.warn("Exception during update category: {}", e.getMessage());
      throw new RuntimeException(e);
    }

    log.info("Updating URL categories");
    sendCustomUrlCategoriesMessage(baseUrl, categoryId, body);
  }

  public void createCustomUrlCategory(final String baseUrl, final String category, final List<String> urls) {
    ZScalerCreateCategory create = new ZScalerCreateCategory(category, "USER_DEFINED", urls, "URL_CATEGORY", true);
    String body;
    try {
      body = objectMapper.writeValueAsString(create);
    }
    catch (JsonProcessingException e) {
      log.warn("Exception during serialization: {}", e.getMessage());
      throw new RuntimeException(e);
    }

    log.info("Creating URL categories");
    sendCustomUrlCategoriesMessage(baseUrl, null, body);
  }

  public void deleteCustomUrlCategory(final String baseUrl, final String categoryId) {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(baseUrl + URL_CATEGORIES + "/" + categoryId))
        .header("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
        .DELETE()
        .build();

    try {
      HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
      if (response.statusCode() != 204 && response.statusCode() != 200) {
        log.warn("Failed to delete URL category {}: {}", categoryId, response.body());
      }
      else {
        log.info("Successfully deleted URL category: {}", categoryId);
      }
    }
    catch (Exception e) {
      log.warn("Failed to delete URL category {}: {}", categoryId, e.getMessage());
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

  private void sendCustomUrlCategoriesMessage(
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
        log.warn("Failed to update URL category: {}", response.body());
      }
      else {
        log.info("Successfully updated URL category: {}", categoryId);
      }
    }
    catch (Exception e) {
      log.warn("Failed to send update URL category: {}", e.getMessage());
    }
  }
}
