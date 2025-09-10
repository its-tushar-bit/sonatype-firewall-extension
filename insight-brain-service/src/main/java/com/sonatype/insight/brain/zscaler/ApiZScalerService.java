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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

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

  private final Logger log = LoggerFactory.getLogger(ApiZScalerService.class);

  private final ZScalerConfigurationDAO zScalerConfigurationDAO;

  private final ZscalerFormatDAO zscalerFormatDAO;

  private Cache<String, ZScalerQuota> quotaCache;

  private final ZScalerMetricsDAO zScalerMetricsDAO;

  private PasswordHandler passwordHandler;

  private ZScalerClient zScalerClient;

  @Inject
  public ApiZScalerService(
      final ZScalerConfigurationDAO zScalerConfigurationDAO,
      final ZscalerFormatDAO zscalerFormatDAO,
      final ZScalerMetricsDAO zScalerMetricsDAO,
      final PasswordHandler passwordHandler,
      final ZScalerClient zScalerClient)
  {
    this.zScalerConfigurationDAO = zScalerConfigurationDAO;
    this.zscalerFormatDAO = zscalerFormatDAO;
    this.zScalerMetricsDAO = zScalerMetricsDAO;
    this.passwordHandler = passwordHandler;
    this.zScalerClient = zScalerClient;
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
      final Cache<String, ZScalerQuota> cache)
  {
    this(zScalerConfigurationDAO, zscalerFormatDAO, zScalerMetricsDAO, passwordHandler, zScalerClient);
    this.quotaCache = cache;
  }

  public void authenticate(
      final String hostname,
      final String username,
      final String password,
      final String apiKey)
  {
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
    List<ZScalerCategory> categories = zScalerClient.getCustomUrlCategories(baseUrl);

    String category = zscalerCategoryName(selectedFormat);
    ZScalerCategory existingCategory = categories.stream()
        .filter(cat -> cat.getConfiguredName().toLowerCase().equals(category))
        .findFirst()
        .orElse(null);

    int currentCustomCount = existingCategory != null ? existingCategory.getCustomUrlsCount() : 0;
    List<String> allowedUrls = limitUrlsToQuota(baseUrl, currentCustomCount, activeUrls);
    if (allowedUrls.isEmpty()) {
      log.warn("No URLs to update, perhaps the quota is exceeded");
      return;
    }

    if (existingCategory != null) {
      String categoryId = existingCategory.getId();
      log.info("{} category with id {} already exists, updating it", category, categoryId);
      zScalerClient.updateCustomUrlCategories(baseUrl, category, categoryId, allowedUrls);
    }
    else {
      log.info("{} category does not exist, creating it", category);
      zScalerClient.createCustomUrlCategory(baseUrl, category, allowedUrls);
    }

    updateMetrics(selectedFormat, activeUrls, allowedUrls);
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

  private String zscalerCategoryName(final ZScalerSupportedFormat selectedFormat) {
    String formatName = selectedFormat.name().toLowerCase(Locale.getDefault());
    return "sonatype-" + formatName + "-shadow-download-defense";
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

  private static String obfuscateApiKey(String key, String timestamp) {
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
