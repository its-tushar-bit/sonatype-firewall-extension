/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import java.util.List;
import java.util.Map;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService;
import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.api.v2.service.ConfigurationProperty;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.json.store.JsonUtils;

/**
 * @since 1.189
 */
@Named
@Singleton
public class FeaturePropertiesInfo
{
  private final ApiConfigurationService apiConfigurationService;

  private final ApiConfigFeaturesService apiConfigFeaturesService;

  @Inject
  FeaturePropertiesInfo(
      ApiConfigurationService apiConfigurationService,
      ApiConfigFeaturesService apiConfigFeaturesService)
  {
    this.apiConfigFeaturesService = apiConfigFeaturesService;
    this.apiConfigurationService = apiConfigurationService;
  }

  public String getSystemConfigPropertiesJson() {
    return JsonUtils.format(getSystemConfigProperties());
  }

  private Map<String, Object> getSystemConfigProperties() {
    Map<String, ConfigurationProperty> configProperties =
        ConfigurationProperty.getBooleanConfigurationPropertiesByName();
    return apiConfigurationService.getConfigurationNoAuthz(configProperties.keySet());
  }

  public String getFeatureConfigPropertiesJson() {
    return JsonUtils.format(getFeatureConfigProperties(null));
  }

  public Map<String, Boolean> getFeatureConfigProperties(
      List<SystemConfigurationPropertyFeature> filteredFeatures)
  {
    return apiConfigFeaturesService.getAllSystemConfigurationPropertyFeatureWithValue(filteredFeatures);
  }
}
