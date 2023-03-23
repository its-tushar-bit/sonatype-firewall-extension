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

import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.service.banning.MTIQFeatureService;
import com.sonatype.insight.license.model.Feature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides means to inspect the available features of the tenant.
 */
public class ConfigFeaturesService
{
  private static final Logger log = LoggerFactory.getLogger(ConfigFeaturesService.class);

  private final MTIQFeatureService mtiqFeatureService;

  @Inject
  public ConfigFeaturesService(MTIQFeatureService mtiqFeatureService) {
    this.mtiqFeatureService = mtiqFeatureService;
  }

  /**
   * Gets a list of all features supported by this server instance
   */
  public Set<Feature> getAllFeatures() {
    Set<Feature> features = Arrays.stream(SystemConfigurationPropertyFeature.values())
        .filter(mtiqFeatureService::isEnabled)
        .collect(Collectors.toSet());

    log.debug("Found all features: {}", features);
    return features;
  }

  /**
   * Gets a list of enabled features for the tenant
   */
  public Set<Feature> getFeatures() {
    Set<Feature> features = Arrays.stream(SystemConfigurationPropertyFeature.values())
        .filter(SystemConfigurationPropertyFeature::isEnabled)
        .filter(mtiqFeatureService::isEnabled)
        .collect(Collectors.toSet());

    log.debug("Found features: {}", features);
    return features;
  }
}
