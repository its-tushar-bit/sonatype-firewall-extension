/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.features;

import java.util.EnumSet;
import java.util.Set;

import javax.inject.Inject;

import com.sonatype.insight.brain.migration.RootOrganizationConfigMigrationUtils;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;

import com.google.inject.Binder;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FeatureServiceTest
    extends AbstractComponentTest
{
  @Inject
  private FeaturesService featuresService;

  @Inject
  private InsightConfig insightConfig;

  private CLMLicenseManager licenseManager;

  private RootOrganizationConfigMigrationUtils rootOrganizationConfigMigrationUtils;

  @Override
  public void configure(Binder binder) {
    licenseManager = mock(CLMLicenseManager.class);
    rootOrganizationConfigMigrationUtils = mock(RootOrganizationConfigMigrationUtils.class);
    binder.bind(CLMLicenseManager.class).toInstance(licenseManager);
    binder.bind(RootOrganizationConfigMigrationUtils.class).toInstance(rootOrganizationConfigMigrationUtils);
    super.configure(binder);
  }

  @Test
  public void testGetFeatures_Unlicensed() {
    assertThat(featuresService.getFeatures()).isEmpty();
  }

  @Test
  public void testGetFeatures_WithVersionSpecificFeatures() {
    Set<Feature> features = EnumSet.of(Feature.POLICY, Feature.LABELS, Feature.RELEASE_GRAPH, Feature.POLICY_VIOLATIONS,
        Feature.REEVALUATE_POLICY);
    when(licenseManager.isValid()).thenReturn(true);
    assertThat(featuresService.getFeatures()).containsAll(features);
  }

  @Test
  public void testGetFeatures_WithLicenseSpecificFeatures() {
    Set<Feature> features = EnumSet.of(Feature.QUALITY, Feature.POLICY_MONITORING, Feature.DASHBOARD,
        Feature.CLI_INTEGRATION, Feature.ENFORCEMENT, Feature.NOTIFICATIONS, Feature.POLICY_GRANDFATHERING,
        Feature.WEBHOOKS_FOR_APPLICATIONS, Feature.FIREWALL);
    when(licenseManager.isValid()).thenReturn(true);
    when(licenseManager.getFeatures()).thenReturn(features);
    assertThat(featuresService.getFeatures()).containsAll(features);
  }

  @Test
  public void testGetFeatures_WithRootMigrated() {
    when(licenseManager.isValid()).thenReturn(true);
    when(rootOrganizationConfigMigrationUtils.isMigrated()).thenReturn(true);
    assertThat(featuresService.getFeatures()).contains(Feature.ROOT_ORG).doesNotContain(Feature.ROOT_ORG_MIGRATE);
  }

  @Test
  public void testGetFeatures_WithoutRootMigrated() {
    when(licenseManager.isValid()).thenReturn(true);
    when(rootOrganizationConfigMigrationUtils.isMigrated()).thenReturn(false);
    assertThat(featuresService.getFeatures()).contains(Feature.ROOT_ORG_MIGRATE).doesNotContain(Feature.ROOT_ORG);
  }

  @Test
  public void testGetFeatures_WithRootMigratedScheduled() {
    when(licenseManager.isValid()).thenReturn(true);
    when(rootOrganizationConfigMigrationUtils.isMigrated()).thenReturn(false);
    when(rootOrganizationConfigMigrationUtils.isMigrationScheduled()).thenReturn(true);
    assertThat(featuresService.getFeatures()).doesNotContain(Feature.ROOT_ORG_MIGRATE, Feature.ROOT_ORG);
  }

  @Test
  public void testGetFeatures_WithoutAllowExternalLinks() {
    when(licenseManager.isValid()).thenReturn(true);
    insightConfig.setExternalHyperlinksAllowed(false);
    assertThat(featuresService.getFeatures()).doesNotContain(Feature.ALLOW_EXTERNAL_HYPERLINKS);
  }

  @Test
  public void testGetFeatures_WithAllowExternalLinks() {
    when(licenseManager.isValid()).thenReturn(true);
    assertThat(featuresService.getFeatures()).contains(Feature.ALLOW_EXTERNAL_HYPERLINKS);
  }
}
