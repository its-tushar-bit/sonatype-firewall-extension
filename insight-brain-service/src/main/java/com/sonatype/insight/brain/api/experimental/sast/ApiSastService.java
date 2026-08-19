/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.sast;

import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.features.FeaturesService;
import com.sonatype.insight.license.model.Feature;
import com.sonatype.insight.license.model.LicensedFeature;

@Named
public class ApiSastService
{
  private final FeaturesService featuresService;

  @Inject
  public ApiSastService(FeaturesService featuresService) {
    this.featuresService = featuresService;
  }

  public SastValidateResponseDTO validate() {
    Set<Feature> features = featuresService.getFeatures();
    boolean hasFeature = features.contains(LicensedFeature.DEVELOPER_DASHBOARD);
    SastValidateResponseDTO response = new SastValidateResponseDTO();
    response.setValid(hasFeature);
    return response;
  }
}
