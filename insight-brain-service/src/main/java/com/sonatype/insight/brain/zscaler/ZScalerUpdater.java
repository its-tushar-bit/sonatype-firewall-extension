/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.zscaler;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightJob;
import com.sonatype.insight.brain.zscaler.ApiZScalerService.ActiveUrls;
import com.sonatype.insight.license.model.LicensedFeature;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
@DisallowConcurrentExecution
public class ZScalerUpdater
    implements InsightJob
{
  private static final Logger log = LoggerFactory.getLogger(ZScalerUpdater.class);

  private static final ObjectMapper MAPPER = new ObjectMapper();

  static final String TASK_NAME = ZScalerUpdater.class.getSimpleName();

  private final ZScalerMaliciousUrlFetcher zScalerMaliciousUrlFetcher;

  private final TaskScheduler taskScheduler;

  private final ApiZScalerService apiZScalerService;

  private final ProductLicense productLicense;

  private final Configuration configuration;

  public boolean disableForTesting = false;

  @Inject
  public ZScalerUpdater(
      final ZScalerMaliciousUrlFetcher zScalerMaliciousUrlFetcher,
      final TaskScheduler taskScheduler,
      final ApiZScalerService apiZScalerService,
      final ProductLicense productLicense,
      final Configuration configuration)
  {
    this.zScalerMaliciousUrlFetcher = zScalerMaliciousUrlFetcher;
    this.taskScheduler = taskScheduler;
    this.apiZScalerService = apiZScalerService;
    this.productLicense = productLicense;
    this.configuration = configuration;
  }

  @Override
  public void register() {
    if (disableForTesting || configuration.getZScalerUpdateTaskPeriod() <= 0) {
      log.info("ZScaler update task is configured not to run");
      return;
    }
    taskScheduler.schedulePeriodicTask(this, Duration.ofHours(configuration.getZScalerUpdateTaskPeriod()));
  }

  @Override
  public String getJobName() {
    return TASK_NAME;
  }

  @Override
  public void execute(final JobExecutionContext context) throws JobExecutionException {
    if (SystemConfigurationPropertyFeature.ZSCALER.isEnabled()) {
      execute(this::updateAllZScalerMaliciousCategoriesInternal,
          log, "Error fetching zScaler malicious URLs");
    }
    else {
      log.debug("zScaler feature not enabled. Skipping update task.");
    }
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void updateZScalerMaliciousCategory(ZScalerSupportedFormat format) {
    if (productLicense.hasFeature(LicensedFeature.FIREWALL)) {
      apiZScalerService.authenticate();
      deleteCategory(format);
      updateCategory(format);
      apiZScalerService.activate();
    }
    else {
      log.debug("zScaler is disabled because the license does not have the required feature");
    }
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void updateAllZScalerMaliciousCategories() {
    updateAllZScalerMaliciousCategoriesInternal();
  }

  void updateAllZScalerMaliciousCategoriesInternal() {
    if (productLicense.hasFeature(LicensedFeature.FIREWALL)) {
      apiZScalerService.authenticate();
      deleteAllCategories();
      updateAllCategories();
      apiZScalerService.activate();
    }
    else {
      log.debug("zScaler is disabled because the license does not have the required feature");
    }
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void deleteAllZScalerMaliciousUrlCategories() {
    if (productLicense.hasFeature(LicensedFeature.FIREWALL)) {
      apiZScalerService.authenticate();
      deleteAllCategories();
      apiZScalerService.activate();
    }
    else {
      log.debug("zScaler is disabled because the license does not have the required feature");
    }
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void deleteZScalerMaliciousUrlCategory(ZScalerSupportedFormat format) {
    if (productLicense.hasFeature(LicensedFeature.FIREWALL)) {
      apiZScalerService.authenticate();
      deleteCategory(format);
      apiZScalerService.activate();
    }
    else {
      log.debug("zScaler is disabled because the license does not have the required feature");
    }
  }

  private void updateAllCategories() {
    List<ZScalerSupportedFormat> configuredFormats = apiZScalerService.getConfiguredFormats();

    for (ZScalerSupportedFormat format : configuredFormats) {
      updateCategory(format);
    }
  }

  private void deleteAllCategories() {
    deleteCategory(ZScalerSupportedFormat.MAVEN);
    deleteCategory(ZScalerSupportedFormat.PYPI);
    deleteCategory(ZScalerSupportedFormat.NPM);
    deleteCategory(ZScalerSupportedFormat.NUGET);
  }

  private void deleteCategory(ZScalerSupportedFormat format) {
    log.debug("deleting zScaler category: {}", format);
    apiZScalerService.updateCategory(format, List.of("placeholder.com/" + format.toString().toLowerCase()));
  }

  void updateCategory(ZScalerSupportedFormat format) {
    log.debug("Updating zScaler for {}", format);
    if (!productLicense.hasFeature(LicensedFeature.FIREWALL)) {
      throw new InvalidLicenseException("zScaler requires a valid license");
    }

    InputStream inputStream = zScalerMaliciousUrlFetcher.fetchMaliciousUrls(format);
    if (inputStream == null) {
      log.warn("No zScaler malicious URLs found for format: {}", format);
      return;
    }

    apiZScalerService.updateCategory(format, getActiveUrls(inputStream));
  }

  private static List<String> getActiveUrls(final InputStream inputStream) {
    if (inputStream == null) {
      return Collections.emptyList();
    }

    try {
      ActiveUrls activeUrls = MAPPER.readValue(inputStream, ActiveUrls.class);
      return activeUrls.getActiveThreatUrls() == null ? Collections.emptyList() : convertActiveUrls(
          activeUrls.getActiveThreatUrls());
    }
    catch (IOException e) {
      return Collections.emptyList();
    }
  }

  private static List<String> convertActiveUrls(final List<String> activeThreatUrls) {
    return activeThreatUrls.stream()
        .map(url -> url.replaceAll("https?://", ""))
        .toList();
  }
}
