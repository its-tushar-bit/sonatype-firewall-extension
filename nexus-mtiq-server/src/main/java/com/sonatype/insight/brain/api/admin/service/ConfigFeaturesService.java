/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin.service;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.PathParam;

import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.service.banning.MTIQFeatureService;
import com.sonatype.insight.brain.tenancy.TenantValidator;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.license.model.Feature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides means to inspect the available features of the tenant.
 */
@Named
public class ConfigFeaturesService
{
  private static final Logger log = LoggerFactory.getLogger(ConfigFeaturesService.class);

  private final MTIQFeatureService mtiqFeatureService;

  private final TenantValidator tenantValidator;

  @Inject
  public ConfigFeaturesService(MTIQFeatureService mtiqFeatureService, TenantValidator tenantValidator) {
    this.mtiqFeatureService = mtiqFeatureService;
    this.tenantValidator = tenantValidator;
  }

  /**
   * Gets a list of all SystemConfigurationPropertyFeature supported by MTIQ server
   */
  public Set<Feature> getAllFeatures(String tenantSlug) {
    if (!tenantValidator.validateTenantExists(tenantSlug)) {
      log.error("Cannot get features, Tenant {} does not exist", tenantSlug);
      throw new NotFoundException(String.format("Tenant %s does not exist", tenantSlug));
    }

    Set<Feature> features = Arrays.stream(SystemConfigurationPropertyFeature.values())
        .filter(feature -> !mtiqFeatureService.isBanned(feature))
        .collect(Collectors.toSet());

    log.debug("Found all features: {}", features);
    return features;
  }

  /**
   * Gets a list of enabled SystemConfigurationPropertyFeature for the tenant
   */
  public Set<Feature> getFeatures(String tenantSlug) {
    if (!tenantValidator.validateTenantExists(tenantSlug)) {
      log.error("Cannot get features, Tenant {} does not exist", tenantSlug);
      throw new NotFoundException(String.format("Tenant %s does not exist", tenantSlug));
    }

    Set<Feature> features = Arrays.stream(SystemConfigurationPropertyFeature.values())
        .filter(mtiqFeatureService::isEnabled)
        .collect(Collectors.toSet());

    log.debug("Found features: {}", features);
    return features;
  }

  public void enableFeature(@PathParam("tenantSlug") String tenantSlug, @PathParam("feature") String feature) {
    if (!tenantValidator.validateTenantExists(tenantSlug)) {
      log.error("Cannot enable feature {}, Tenant {} does not exist", feature, tenantSlug);
      throw new NotFoundException(String.format("Tenant %s does not exist", tenantSlug));
    }

    mtiqFeatureService.enableFeature(feature);
  }

  public void disableFeature(@PathParam("tenantSlug") String tenantSlug, @PathParam("feature") String feature) {
    if (!tenantValidator.validateTenantExists(tenantSlug)) {
      log.error("Cannot disable feature {}, Tenant {} does not exist", feature, tenantSlug);
      throw new NotFoundException(String.format("Tenant %s does not exist", tenantSlug));
    }

    mtiqFeatureService.disableFeature(feature);
  }
}
