/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.features;

import java.util.EnumSet;
import java.util.Set;

import javax.inject.Inject;

import com.sonatype.insight.brain.organization.RootOrganizationConfigMigrationService;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.google.inject.Binder;
import org.junit.Test;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FeatureServiceTest
    extends AbstractComponentTest
{
  @Inject
  private FeaturesService featuresService;

  private CLMLicenseManager licenseManager;

  private RootOrganizationConfigMigrationService rootOrgMigrationService;

  @Override
  public void configure(Binder binder) {
    super.configure(binder);
    licenseManager = mock(CLMLicenseManager.class);
    rootOrgMigrationService = mock(RootOrganizationConfigMigrationService.class);
    binder.bind(CLMLicenseManager.class).toInstance(licenseManager);
    binder.bind(RootOrganizationConfigMigrationService.class).toInstance(rootOrgMigrationService);
  }

  @Test
  public void testGetFeatures_Unlicensed() {
    Set<Feature> features = featuresService.getFeatures();
    assertThat(features, is(empty()));
  }

  @Test
  public void testGetFeatures_WithoutPolicyMonitoring() {
    when(rootOrgMigrationService.isMigrated()).thenReturn(true);
    when(licenseManager.isValid()).thenReturn(true);
    when(licenseManager.hasPolicyMonitoring()).thenReturn(false);
    Set<Feature> features = featuresService.getFeatures();
    assertThat(
        features,
        containsInAnyOrder(Feature.LABELS, Feature.NOTIFICATIONS, Feature.POLICY, Feature.POLICY_VIOLATIONS,
            Feature.REEVALUATE_POLICY, Feature.RELEASE_GRAPH, Feature.ROOT_ORG));
  }

  @Test
  public void testGetFeatures_WithPolicyMonitoring() {
    when(rootOrgMigrationService.isMigrated()).thenReturn(true);
    when(licenseManager.isValid()).thenReturn(true);
    when(licenseManager.hasPolicyMonitoring()).thenReturn(true);
    Set<Feature> features = featuresService.getFeatures();
    EnumSet<Feature> expectedFeatures = EnumSet.allOf(Feature.class);
    expectedFeatures.remove(Feature.DASHBOARD);
    expectedFeatures.remove(Feature.ROOT_ORG_MIGRATE);
    assertThat(features, containsInAnyOrder(expectedFeatures.toArray()));
  }

  @Test
  public void testGetFeatures_WithoutDashboard() {
    when(rootOrgMigrationService.isMigrated()).thenReturn(true);
    when(licenseManager.isValid()).thenReturn(true);
    when(licenseManager.hasDashboard()).thenReturn(false);
    Set<Feature> features = featuresService.getFeatures();
    assertThat(
        features,
        containsInAnyOrder(Feature.LABELS, Feature.NOTIFICATIONS, Feature.POLICY, Feature.POLICY_VIOLATIONS,
            Feature.REEVALUATE_POLICY, Feature.RELEASE_GRAPH, Feature.ROOT_ORG));
  }

  @Test
  public void testGetFeatures_WithDashboard() {
    when(rootOrgMigrationService.isMigrated()).thenReturn(true);
    when(licenseManager.isValid()).thenReturn(true);
    when(licenseManager.hasDashboard()).thenReturn(true);
    Set<Feature> features = featuresService.getFeatures();
    EnumSet<Feature> expectedFeatures = EnumSet.allOf(Feature.class);
    expectedFeatures.remove(Feature.POLICY_MONITORING);
    expectedFeatures.remove(Feature.ROOT_ORG_MIGRATE);
    assertThat(features, containsInAnyOrder(expectedFeatures.toArray()));
  }

  @Test
  public void testGetFeatures_WithRootMigrated() {
    when(rootOrgMigrationService.isMigrated()).thenReturn(true);
    when(licenseManager.isValid()).thenReturn(true);
    Set<Feature> features = featuresService.getFeatures();
    assertTrue(features.contains(Feature.ROOT_ORG));
    assertFalse(features.contains(Feature.ROOT_ORG_MIGRATE));
  }

  @Test
  public void testGetFeatures_WithoutRootMigrated() {
    when(rootOrgMigrationService.isMigrated()).thenReturn(false);
    when(licenseManager.isValid()).thenReturn(true);
    Set<Feature> features = featuresService.getFeatures();
    assertTrue(features.contains(Feature.ROOT_ORG_MIGRATE));
    assertFalse(features.contains(Feature.ROOT_ORG));
  }

  @Test
  public void testGetFeatures_WithRootMigratedScheduled() {
    when(rootOrgMigrationService.isMigrated()).thenReturn(false);
    when(rootOrgMigrationService.isMigrationScheduled()).thenReturn(true);
    when(licenseManager.isValid()).thenReturn(true);
    Set<Feature> features = featuresService.getFeatures();
    assertFalse(features.contains(Feature.ROOT_ORG_MIGRATE));
    assertFalse(features.contains(Feature.ROOT_ORG));
  }
}
