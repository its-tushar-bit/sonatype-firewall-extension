/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.features;

import java.util.Set;

import javax.inject.Inject;

import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.google.inject.Binder;
import org.junit.Test;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FeatureServiceTest
    extends AbstractComponentTest
{
  @Inject
  private FeaturesService featuresService;

  private CLMLicenseManager licenseManager;

  @Override
  public void configure(Binder binder) {
    super.configure(binder);
    licenseManager = mock(CLMLicenseManager.class);
    binder.bind(CLMLicenseManager.class).toInstance(licenseManager);
  }

  @Test
  public void testGetFeatures_Unlicensed() {
    Set<Feature> features = featuresService.getFeatures();
    assertThat(features, is(empty()));
  }

  @Test
  public void testGetFeatures_WithoutPolicyMonitoring() {
    when(licenseManager.isValid()).thenReturn(true);
    when(licenseManager.hasPolicyMonitoring()).thenReturn(false);
    Set<Feature> features = featuresService.getFeatures();
    assertThat(
        features,
        containsInAnyOrder(Feature.LABELS, Feature.NOTIFICATIONS, Feature.POLICY, Feature.POLICY_VIOLATIONS,
            Feature.REEVALUATE_POLICY, Feature.RELEASE_GRAPH));
  }

  @Test
  public void testGetFeatures_WithPolicyMonitoring() {
    when(licenseManager.isValid()).thenReturn(true);
    when(licenseManager.hasPolicyMonitoring()).thenReturn(true);
    Set<Feature> features = featuresService.getFeatures();
    assertThat(features, containsInAnyOrder(Feature.values()));
  }
}
