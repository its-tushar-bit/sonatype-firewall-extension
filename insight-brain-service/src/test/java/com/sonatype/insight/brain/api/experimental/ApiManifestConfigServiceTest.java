/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import javax.inject.Inject;

import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightConfig.Feature;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiManifestConfigServiceTest
    extends AbstractComponentTest
{
  @Inject
  ApiManifestConfigService apiManifestConfigService;

  @Inject
  private InsightConfig config;

  @Before
  public void setup() {
    config.setExperimentalFeatures(ImmutableMap.of(Feature.MANIFEST_SCAN.getFlag(), true));
  }

  @Test
  public void testFeatureFlag() {
    // expect feature flag to be true
    assertThat(apiManifestConfigService.isManifestScanFeatureEnabled()).isTrue();
  }
}
