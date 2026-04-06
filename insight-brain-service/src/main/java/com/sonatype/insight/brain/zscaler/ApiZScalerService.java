/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.zscaler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.api.v2.HasFeature;
import com.sonatype.insight.brain.dataaccess.configuration.ZScalerConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ZscalerFormatDAO;
import com.sonatype.insight.brain.dataaccess.zscaler.ZScalerMetricsDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.configuration.ZScalerConfiguration;
import com.sonatype.insight.brain.model.configuration.ZscalerFormat;
import com.sonatype.insight.brain.model.zscaler.ZScalerMetrics;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.error.exception.BadRequestException;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
@HasFeature(SystemConfigurationPropertyFeature.ZSCALER)
public class ApiZScalerService
{
  private static final String QUOTA_KEY = "QUOTA_KEY";

  private static final int DEFAULT_MAX_URLS_PER_CATEGORY = 25000;

  private final Logger log = LoggerFactory.getLogger(ApiZScalerService.class);

  private final ZScalerConfigurationDAO zScalerConfigurationDAO;

  private final ZscalerFormatDAO zscalerFormatDAO;

  private Cache<String, ZScalerQuota> quotaCache;

  private final ZScalerMetricsDAO zScalerMetricsDAO;

  private PasswordHandler passwordHandler;

  private ZScalerClient zScalerClient;

  private ZScalerPermissionValidator zScalerPermissionValidator;

  private final com.sonatype.insight.brain.service.Configuration configuration;

  @Inject
  public ApiZScalerService(
      final ZScalerConfigurationDAO zScalerConfigurationDAO,
      final ZscalerFormatDAO zscalerFormatDAO,
      final ZScalerMetricsDAO zScalerMetricsDAO,
      final PasswordHandler passwordHandler,
      final ZScalerClient zScalerClient,
      final ZScalerPermissionValidator zScalerPermissionValidator,
      final com.sonatype.insight.brain.service.Configuration configuration)
  {
    this.zScalerConfigurationDAO = zScalerConfigurationDAO;
    this.zscalerFormatDAO = zscalerFormatDAO;
    this.zScalerMetricsDAO = zScalerMetricsDAO;
    this.passwordHandler = passwordHandler;
    this.zScalerClient = zScalerClient;
    this.zScalerPermissionValidator = zScalerPermissionValidator;
    this.configuration = configuration;
    this.quotaCache = CacheBuilder.newBuilder()
        .expireAfterWrite(1, TimeUnit.HOURS)
        .maximumSize(1)
        .build();
  }

  public ApiZScalerService(
      final ZScalerConfigurationDAO zScalerConfigurationDAO,
      final ZscalerFormatDAO zscalerFormatDAO,
      final ZScalerMetricsDAO zScalerMetricsDAO,
      final PasswordHandler passwordHandler,
      final ZScalerClient zScalerClient,
      final ZScalerPermissionValidator zScalerPermissionValidator,
      final com.sonatype.insight.brain.service.Configuration configuration,
      final Cache<String, ZScalerQuota> cache)
  {
    this(zScalerConfigurationDAO, zscalerFormatDAO, zScalerMetricsDAO, passwordHandler, zScalerClient,
        zScalerPermissionValidator, configuration);
    this.quotaCache = cache;
  }

  public void authenticate(
      final String hostname,
      final String username,
      final String password,
      final String apiKey)
  {
    if (hostname == null || hostname.trim().isEmpty()) {
      throw new BadRequestException("The hostname is required.");
    }
    if (username == null || username.trim().isEmpty()) {
      throw new BadRequestException("The username is required.");
    }
    if (password == null || password.trim().isEmpty()) {
      throw new BadRequestException("The password is required.");
    }

    String timestamp = String.valueOf(System.currentTimeMillis());
    String obfuscatedKey = obfuscateApiKey(apiKey, timestamp);

    zScalerClient.authenticate(hostname, username, password, obfuscatedKey, timestamp);
  }

  public String authenticate() {
    ZScalerConfiguration configuration = zScalerConfigurationDAO.get();
    if (configuration == null) {
      log.warn("No zScaler configuration found");
      throw new BadRequestException("No zScaler configuration found");
    }

    String decryptedPassword = passwordHandler.decryptPassword(configuration.getPassword());

    authenticate(configuration.getHostname(), configuration.getUsername(), decryptedPassword,
        configuration.getApikey());
    return configuration.getHostname();
  }

  public void authenticateAndValidatePermissions(
      final String hostname,
      final String username,
      final String password,
      final String apiKey)
  {
    authenticate(hostname, username, password, apiKey);
    zScalerPermissionValidator.validatePermissions(hostname);
  }

  public void activate() {
    ZScalerConfiguration configuration = zScalerConfigurationDAO.get();
    if (configuration == null) {
      log.warn("No zScaler configuration found");
      throw new BadRequestException("No zScaler configuration found");
    }

    zScalerClient.activateChanges(configuration.getHostname());
  }

  public void updateCategory(final ZScalerSupportedFormat format, final List<String> activeUrls) {
    ZScalerConfiguration configuration = zScalerConfigurationDAO.get();
    if (configuration == null) {
      log.warn("No zScaler configuration found");
      throw new BadRequestException("No zScaler configuration found");
    }

    updateCategory(configuration.getHostname(), format, activeUrls);
    this.quotaCache.invalidate(QUOTA_KEY);
  }

  public List<ZScalerSupportedFormat> getConfiguredFormats() {
    ZScalerConfiguration configuration = zScalerConfigurationDAO.get();
    if (configuration == null) {
      log.warn("No zScaler configuration found");
      throw new BadRequestException("No zScaler configuration found");
    }

    List<ZScalerSupportedFormat> formats = new ArrayList<>();
    List<ZscalerFormat> zscalerFormats = zscalerFormatDAO.getAll();
    for (ZscalerFormat format : zscalerFormats) {
      if (format.isEnabled()) {
        formats.add(ZScalerSupportedFormat.valueOf(format.getFormat().toUpperCase()));
      }
    }

    return formats;
  }

  private void updateCategory(String baseUrl, ZScalerSupportedFormat selectedFormat, List<String> activeUrls) {
    List<ZScalerCategory> allCategories = zScalerClient.getCustomUrlCategories(baseUrl);

    // Find all existing categories for this format
    String categoryPrefix = zscalerCategoryPrefix(selectedFormat);
    List<ZScalerCategory> existingFormatCategories = allCategories.stream()
        .filter(cat -> cat.getConfiguredName().toLowerCase().startsWith(categoryPrefix.toLowerCase()))
        .toList();

    // Clean up legacy categories (without index) before proceeding
    cleanupLegacyCategories(baseUrl, existingFormatCategories, selectedFormat);

    // After cleanup, get only the indexed categories for URL count calculation
    List<ZScalerCategory> indexedCategories = existingFormatCategories.stream()
        .filter(cat -> extractIndexFromCategoryName(cat.getConfiguredName()) >= 0)
        .toList();

    // Calculate current total URLs used by this format (only from indexed categories)
    int currentFormatUrlCount = indexedCategories.stream()
        .mapToInt(ZScalerCategory::getCustomUrlsCount)
        .sum();

    // Determine how many URLs we can push based on quota
    List<String> allowedUrls = limitUrlsToQuota(baseUrl, currentFormatUrlCount, activeUrls);
    if (allowedUrls.isEmpty()) {
      log.warn("No URLs to update for format {}, perhaps the quota is exceeded", selectedFormat);
      return;
    }

    // Calculate how many categories we need
    int maxUrlsPerCategory = getMaxUrlsPerCategory();
    int numCategoriesNeeded = (int) Math.ceil((double) allowedUrls.size() / maxUrlsPerCategory);
    log.info("Need {} categories to accommodate {} URLs for format {} (max {} URLs per category)",
        numCategoriesNeeded, allowedUrls.size(), selectedFormat, maxUrlsPerCategory);

    // Update or create categories as needed
    for (int i = 0; i < numCategoriesNeeded; i++) {
      int startIdx = i * maxUrlsPerCategory;
      int endIdx = Math.min(startIdx + maxUrlsPerCategory, allowedUrls.size());
      List<String> urlsForCategory = allowedUrls.subList(startIdx, endIdx);

      String categoryName = zscalerCategoryName(selectedFormat, i);
      ZScalerCategory existingCategory = indexedCategories.stream()
          .filter(cat -> cat.getConfiguredName().equalsIgnoreCase(categoryName))
          .findFirst()
          .orElse(null);

      ZScalerOperationResult<?> result;
      if (existingCategory != null) {
        String categoryId = existingCategory.getId();
        log.info("Category {} with id {} already exists, updating with {} URLs",
            categoryName, categoryId, urlsForCategory.size());
        result = zScalerClient.updateCustomUrlCategories(baseUrl, categoryName, categoryId, urlsForCategory);
      }
      else {
        log.info("Category {} does not exist, creating with {} URLs", categoryName, urlsForCategory.size());
        result = zScalerClient.createCustomUrlCategory(baseUrl, categoryName, urlsForCategory);
      }

      if (!result.isSuccess()) {
        handleCategoryOperationFailure(result, categoryName, selectedFormat);
      }
    }

    // Clean up unused categories (categories with index >= numCategoriesNeeded)
    cleanupUnusedCategories(baseUrl, indexedCategories, numCategoriesNeeded);
    updateMetrics(selectedFormat, activeUrls, allowedUrls);
  }

  private void handleCategoryOperationFailure(
      final ZScalerOperationResult<?> result,
      final String categoryName,
      final ZScalerSupportedFormat format)
  {
    String errorMessage = result.getMessage() != null ? result.getMessage() : "Unknown error";
    String exceptionMessage;

    if (result.isForbidden()) {
      exceptionMessage = "ZScaler integration is broken due to insufficient permissions. " +
          "The ZScaler user account must have CUSTOM_URL_CAT and OVERRIDE_EXISTING_CAT permissions " +
          "with READ_WRITE access. " +
          "Error: " + errorMessage;

      log.error("Category '{}' for format {}: {}", categoryName, format, exceptionMessage);
      throw new BadRequestException(exceptionMessage);
    }

    if (result.isBadRequest() && result.isQuotaError()) {
      exceptionMessage = "ZScaler quota is full. Cannot update category '" + categoryName + "'. " +
          "Error: " + errorMessage;

      log.error("Category '{}' for format {}: {}", categoryName, format, exceptionMessage);
      throw new BadRequestException(exceptionMessage);
    }

    exceptionMessage = "ZScaler integration failure. Cannot update category '" + categoryName + "'. " +
        "Status: " + result.getStatusCode() + ", Error: " + errorMessage;

    log.error("Category '{}' for format {}: {}", categoryName, format, exceptionMessage);
    throw new BadRequestException(exceptionMessage);
  }

  private void updateMetrics(
      final ZScalerSupportedFormat selectedFormat,
      final List<String> activeUrls,
      final List<String> allowedUrls)
  {
    ZScalerMetrics zScalerMetrics = zScalerMetricsDAO.get();
    if (zScalerMetrics == null) {
      zScalerMetrics = new ZScalerMetrics();
    }

    switch (selectedFormat) {
      case MAVEN:
        zScalerMetrics.setMavenUrlsFromHds(activeUrls.size());
        zScalerMetrics.setMavenUrlsToZscaler(allowedUrls.size());
        break;
      case NPM:
        zScalerMetrics.setNpmUrlsFromHds(activeUrls.size());
        zScalerMetrics.setNpmUrlsToZscaler(allowedUrls.size());
        break;
      case PYPI:
        zScalerMetrics.setPypiUrlsFromHds(activeUrls.size());
        zScalerMetrics.setPypiUrlsToZscaler(allowedUrls.size());
        break;
      case NUGET:
        zScalerMetrics.setNugetUrlsFromHds(activeUrls.size());
        zScalerMetrics.setNugetUrlsToZscaler(allowedUrls.size());
        break;
      default:
        log.warn("Unsupported zScaler format {}", selectedFormat);
        return;
    }
    zScalerMetricsDAO.set(zScalerMetrics);
  }

  private String zscalerCategoryPrefix(final ZScalerSupportedFormat selectedFormat) {
    String formatName = selectedFormat.name().toLowerCase(Locale.getDefault());
    return "sonatype-" + formatName + "-";
  }

  private String zscalerCategoryName(final ZScalerSupportedFormat selectedFormat, final int index) {
    String formatName = selectedFormat.name().toLowerCase(Locale.getDefault());
    return "sonatype-" + formatName + "-" + index + "-shadow-download-defense";
  }

  private void cleanupLegacyCategories(
      final String baseUrl,
      final List<ZScalerCategory> existingFormatCategories,
      final ZScalerSupportedFormat selectedFormat)
  {
    // Clean up legacy categories (without index in the name)
    // Legacy format: sonatype-<format>-shadow-download-defense
    String legacyCategoryName = zscalerLegacyCategoryName(selectedFormat);

    for (ZScalerCategory category : existingFormatCategories) {
      if (isLegacyCategory(category.getConfiguredName(), legacyCategoryName)) {
        log.info("Deleting legacy category {} with id {} (migrating to indexed format)",
            category.getConfiguredName(), category.getId());
        zScalerClient.deleteCustomUrlCategory(baseUrl, category.getId());
      }
    }
  }

  private String zscalerLegacyCategoryName(final ZScalerSupportedFormat selectedFormat) {
    String formatName = selectedFormat.name().toLowerCase(Locale.getDefault());
    return "sonatype-" + formatName + "-shadow-download-defense";
  }

  private boolean isLegacyCategory(final String categoryName, final String legacyCategoryName) {
    // A category is legacy if it matches the old naming format exactly
    // and doesn't have an index number in it
    return categoryName.equalsIgnoreCase(legacyCategoryName);
  }

  private void cleanupUnusedCategories(
      final String baseUrl,
      final List<ZScalerCategory> existingFormatCategories,
      final int numCategoriesNeeded)
  {
    // Find categories with indices >= numCategoriesNeeded and delete them
    for (ZScalerCategory category : existingFormatCategories) {
      String categoryName = category.getConfiguredName();
      int index = extractIndexFromCategoryName(categoryName);
      if (index >= numCategoriesNeeded) {
        log.info("Deleting unused category {} with id {}", categoryName, category.getId());
        zScalerClient.deleteCustomUrlCategory(baseUrl, category.getId());
      }
    }
  }

  private int extractIndexFromCategoryName(final String categoryName) {
    // Extract index from category name like "sonatype-maven-2-shadow-download-defense"
    // Pattern: sonatype-<format>-<index>-shadow-download-defense
    try {
      String[] parts = categoryName.split("-");
      if (parts.length >= 3) {
        // The index is the part after the format and before "shadow"
        for (int i = 2; i < parts.length - 3; i++) {
          try {
            return Integer.parseInt(parts[i]);
          }
          catch (NumberFormatException e) {
            // Continue looking
          }
        }
      }
      return -1; // Invalid format, treat as index -1 so it won't be deleted
    }
    catch (Exception e) {
      log.warn("Failed to extract index from category name: {}", categoryName);
      return -1;
    }
  }

  private List<String> limitUrlsToQuota(
      final String baseUrl,
      final int currentCustomCount,
      final List<String> urlsList)
  {
    ZScalerQuota quota = zScalerClient.getZScalerQuota(baseUrl);

    if (quota == null) {
      log.warn("Quota is null, returning empty list");
      return Collections.emptyList();
    }

    log.debug("Quota: {} remaining urls available, {} urls present for current format. Active urls for format {}",
        quota.getRemainingUrlsQuota(), currentCustomCount, urlsList.size());

    // We are going to be overriding the categories urls so need to remove the current urls
    // count for the selected format
    int maxUrlsAllowed = quota.getRemainingUrlsQuota() + quota.getUniqueUrlsProvisioned();
    int provisionedUrlsWithFormatRemoved = quota.getUniqueUrlsProvisioned() - currentCustomCount;
    int urlsThatCanBeAdded = maxUrlsAllowed - provisionedUrlsWithFormatRemoved;

    if (urlsThatCanBeAdded <= 0) {
      log.warn("Quota exceeded, no urls can be added");
      return Collections.emptyList();
    }
    // Reduce the size of the list to the remaining urls count
    else if (urlsList.size() > urlsThatCanBeAdded) {
      log.warn("Quota exceeded, reducing the list size to {}", urlsThatCanBeAdded);
      return urlsList.subList(0, urlsThatCanBeAdded);
    }
    else {
      log.info("Quota is sufficient, returning the full list ({} urls)", urlsList.size());
      return urlsList;
    }
  }

  public ApiZScalerQuotaDTO getQuota() {
    if (zScalerConfigurationDAO.get() == null) {
      return new ApiZScalerQuotaDTO(0, 0, "none");
    }

    ZScalerQuota zScalerQuota = this.quotaCache.getIfPresent(QUOTA_KEY);
    if (zScalerQuota == null) {
      String hostname = authenticate();
      zScalerQuota = zScalerClient.getZScalerQuota(hostname);
      if (zScalerQuota == null) {
        log.warn("Unable to retrieve zScalerQuota");
        throw new BadRequestException("Unable to retrieve zScalerQuota");
      }
      this.quotaCache.put(QUOTA_KEY, zScalerQuota);
    }

    return new ApiZScalerQuotaDTO(
        zScalerQuota.getRemainingUrlsQuota() + zScalerQuota.getUniqueUrlsProvisioned(),
        zScalerQuota.getRemainingUrlsQuota(), determineStatus(zScalerQuota));
  }

  private String determineStatus(final ZScalerQuota zScalerQuota) {
    if (zScalerQuota == null) {
      return "none";
    }
    else if (zScalerQuota.getRemainingUrlsQuota() == 0) {
      return "over";
    }
    return "under";
  }

  private int getMaxUrlsPerCategory() {
    Integer configured = configuration.getZScalerMaxUrlsPerCategory();
    if (configured == null || configured <= 0) {
      log.debug("Using default max URLs per category: {}", DEFAULT_MAX_URLS_PER_CATEGORY);
      return DEFAULT_MAX_URLS_PER_CATEGORY;
    }
    log.debug("Using configured max URLs per category: {}", configured);
    return configured;
  }

  private static String obfuscateApiKey(String key, String timestamp) {
    int apiKeySize = 12;
    if (key == null || key.length() != apiKeySize) {
      throw new BadRequestException("The apiKey must be exactly 12 characters.");
    }

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

  public record ApiZScalerQuotaDTO(int totalAllowedUrls, int remainingUrls, String status)
  {
  }

  public static class ActiveUrls
  {
    private List<String> activeThreatUrls;

    public ActiveUrls() {
      // empty
    }

    public ActiveUrls(List<String> activeThreatUrls) {
      this.activeThreatUrls = activeThreatUrls;
    }

    public List<String> getActiveThreatUrls() {
      return activeThreatUrls;
    }

    public void setActiveThreatUrls(final List<String> activeThreatUrls) {
      this.activeThreatUrls = activeThreatUrls;
    }
  }
}
