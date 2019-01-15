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

  @Inject
  private InsightConfig insightConfig;

  private CLMLicenseManager licenseManager;

  private RootOrganizationConfigMigrationUtils rootOrganizationConfigMigrationUtils;

  @Override
  public void configure(Binder binder) {
    super.configure(binder);
    licenseManager = mock(CLMLicenseManager.class);
    rootOrganizationConfigMigrationUtils = mock(RootOrganizationConfigMigrationUtils.class);
    binder.bind(CLMLicenseManager.class).toInstance(licenseManager);
    binder.bind(RootOrganizationConfigMigrationUtils.class).toInstance(rootOrganizationConfigMigrationUtils);
  }

  @Test
  public void testGetFeatures_Unlicensed() {
    Set<Feature> features = featuresService.getFeatures();
    assertThat(features, is(empty()));
  }

  @Test
  public void testGetFeatures_WithoutPolicyMonitoring() {
    when(rootOrganizationConfigMigrationUtils.isMigrated()).thenReturn(true);
    when(licenseManager.isValid()).thenReturn(true);
    when(licenseManager.hasPolicyMonitoring()).thenReturn(false);
    enableLifecycleFeatures();
    Set<Feature> features = featuresService.getFeatures();
    assertThat(
        features,
        containsInAnyOrder(Feature.LABELS, Feature.NOTIFICATIONS, Feature.POLICY, Feature.POLICY_VIOLATIONS,
            Feature.REEVALUATE_POLICY, Feature.RELEASE_GRAPH, Feature.ROOT_ORG, Feature.ALLOW_EXTERNAL_HYPERLINKS,
            Feature.WEBHOOKS, Feature.POLICY_GRANDFATHERING, Feature.ENFORCEMENT));
  }

  @Test
  public void testGetFeatures_WithPolicyMonitoring() {
    when(rootOrganizationConfigMigrationUtils.isMigrated()).thenReturn(true);
    when(licenseManager.isValid()).thenReturn(true);
    when(licenseManager.hasPolicyMonitoring()).thenReturn(true);
    when(licenseManager.hasRepositoryFirewall()).thenReturn(true);
    enableLifecycleFeatures();
    Set<Feature> features = featuresService.getFeatures();
    EnumSet<Feature> expectedFeatures = EnumSet.allOf(Feature.class);
    expectedFeatures.remove(Feature.DASHBOARD);
    expectedFeatures.remove(Feature.ROOT_ORG_MIGRATE);
    assertThat(features, containsInAnyOrder(expectedFeatures.toArray()));
  }

  @Test
  public void testGetFeatures_WithoutDashboard() {
    when(rootOrganizationConfigMigrationUtils.isMigrated()).thenReturn(true);
    when(licenseManager.isValid()).thenReturn(true);
    when(licenseManager.hasDashboard()).thenReturn(false);
    enableLifecycleFeatures();
    Set<Feature> features = featuresService.getFeatures();
    assertThat(
        features,
        containsInAnyOrder(Feature.LABELS, Feature.NOTIFICATIONS, Feature.POLICY, Feature.POLICY_VIOLATIONS,
            Feature.REEVALUATE_POLICY, Feature.RELEASE_GRAPH, Feature.ROOT_ORG, Feature.ALLOW_EXTERNAL_HYPERLINKS,
            Feature.WEBHOOKS, Feature.POLICY_GRANDFATHERING, Feature.ENFORCEMENT));
  }

  @Test
  public void testGetFeatures_WithDashboard() {
    when(rootOrganizationConfigMigrationUtils.isMigrated()).thenReturn(true);
    when(licenseManager.isValid()).thenReturn(true);
    when(licenseManager.hasDashboard()).thenReturn(true);
    when(licenseManager.hasRepositoryFirewall()).thenReturn(true);
    enableLifecycleFeatures();
    Set<Feature> features = featuresService.getFeatures();
    EnumSet<Feature> expectedFeatures = EnumSet.allOf(Feature.class);
    expectedFeatures.remove(Feature.POLICY_MONITORING);
    expectedFeatures.remove(Feature.ROOT_ORG_MIGRATE);
    assertThat(features, containsInAnyOrder(expectedFeatures.toArray()));
  }

  @Test
  public void testGetFeatures_WithRootMigrated() {
    when(rootOrganizationConfigMigrationUtils.isMigrated()).thenReturn(true);
    when(licenseManager.isValid()).thenReturn(true);
    Set<Feature> features = featuresService.getFeatures();
    assertTrue(features.contains(Feature.ROOT_ORG));
    assertFalse(features.contains(Feature.ROOT_ORG_MIGRATE));
  }

  @Test
  public void testGetFeatures_WithoutRootMigrated() {
    when(rootOrganizationConfigMigrationUtils.isMigrated()).thenReturn(false);
    when(licenseManager.isValid()).thenReturn(true);
    Set<Feature> features = featuresService.getFeatures();
    assertTrue(features.contains(Feature.ROOT_ORG_MIGRATE));
    assertFalse(features.contains(Feature.ROOT_ORG));
  }

  @Test
  public void testGetFeatures_WithRootMigratedScheduled() {
    when(rootOrganizationConfigMigrationUtils.isMigrated()).thenReturn(false);
    when(rootOrganizationConfigMigrationUtils.isMigrationScheduled()).thenReturn(true);
    when(licenseManager.isValid()).thenReturn(true);
    Set<Feature> features = featuresService.getFeatures();
    assertFalse(features.contains(Feature.ROOT_ORG_MIGRATE));
    assertFalse(features.contains(Feature.ROOT_ORG));
  }

  @Test
  public void testGetFeatures_WithoutAllowExternalLinks() {
    when(rootOrganizationConfigMigrationUtils.isMigrated()).thenReturn(true);
    when(licenseManager.isValid()).thenReturn(true);
    insightConfig.setExternalHyperlinksAllowed(false);
    enableLifecycleFeatures();

    Set<Feature> features = featuresService.getFeatures();
    assertThat(
        features,
        containsInAnyOrder(Feature.LABELS, Feature.NOTIFICATIONS, Feature.POLICY, Feature.POLICY_VIOLATIONS,
            Feature.REEVALUATE_POLICY, Feature.RELEASE_GRAPH, Feature.ROOT_ORG, Feature.WEBHOOKS, 
            Feature.POLICY_GRANDFATHERING, Feature.ENFORCEMENT));
  }

  @Test
  public void testGetFeatures_WithAllowExternalLinks() {
    when(rootOrganizationConfigMigrationUtils.isMigrated()).thenReturn(true);
    when(licenseManager.isValid()).thenReturn(true);
    enableLifecycleFeatures();

    Set<Feature> features = featuresService.getFeatures();
    EnumSet<Feature> expectedFeatures = EnumSet.allOf(Feature.class);
    expectedFeatures.remove(Feature.FIREWALL);
    expectedFeatures.remove(Feature.POLICY_MONITORING);
    expectedFeatures.remove(Feature.ROOT_ORG_MIGRATE);
    expectedFeatures.remove(Feature.DASHBOARD);
    assertThat(features, containsInAnyOrder(expectedFeatures.toArray()));
  }

  @Test
  public void testGetFeatures_WithoutLifecycleLight() {
    when(rootOrganizationConfigMigrationUtils.isMigrated()).thenReturn(true);
    when(licenseManager.isValid()).thenReturn(true);
    when(licenseManager.hasPolicyMonitoring()).thenReturn(true);
    when(licenseManager.hasRepositoryFirewall()).thenReturn(true);
    enableLifecycleFeatures();
    Set<Feature> features = featuresService.getFeatures();
    EnumSet<Feature> expectedFeatures = EnumSet.allOf(Feature.class);
    expectedFeatures.remove(Feature.DASHBOARD);
    expectedFeatures.remove(Feature.ROOT_ORG_MIGRATE);
    assertThat(features, containsInAnyOrder(expectedFeatures.toArray()));
  }

  @Test
  public void testGetFeatures_WithLifecycleLight() {
    when(rootOrganizationConfigMigrationUtils.isMigrated()).thenReturn(true);
    when(licenseManager.isValid()).thenReturn(true);
    when(licenseManager.hasDashboard()).thenReturn(true);

    Set<Feature> features = featuresService.getFeatures();
    assertThat(
        features,
        containsInAnyOrder(Feature.LABELS, Feature.POLICY, Feature.POLICY_VIOLATIONS, Feature.REEVALUATE_POLICY, 
            Feature.RELEASE_GRAPH, Feature.ROOT_ORG, Feature.ALLOW_EXTERNAL_HYPERLINKS, Feature.DASHBOARD));
  }

  @Test
  public void testGetFeatures_WithoutFirewall() {
    when(rootOrganizationConfigMigrationUtils.isMigrated()).thenReturn(true);
    when(licenseManager.isValid()).thenReturn(true);
    enableLifecycleFeatures();
    Set<Feature> features = featuresService.getFeatures();
    assertThat(
        features,
        containsInAnyOrder(Feature.LABELS, Feature.NOTIFICATIONS, Feature.POLICY, Feature.POLICY_VIOLATIONS,
            Feature.REEVALUATE_POLICY, Feature.RELEASE_GRAPH, Feature.ROOT_ORG, Feature.ALLOW_EXTERNAL_HYPERLINKS,
            Feature.WEBHOOKS, Feature.POLICY_GRANDFATHERING, Feature.ENFORCEMENT));
  }

  @Test
  public void testGetFeatures_WithFirewall() {
    when(rootOrganizationConfigMigrationUtils.isMigrated()).thenReturn(true);
    when(licenseManager.isValid()).thenReturn(true);
    when(licenseManager.hasRepositoryFirewall()).thenReturn(true);
    Set<Feature> features = featuresService.getFeatures();
    EnumSet<Feature> expectedFeatures = EnumSet.allOf(Feature.class);
    expectedFeatures.remove(Feature.POLICY_MONITORING);
    expectedFeatures.remove(Feature.DASHBOARD);
    expectedFeatures.remove(Feature.ROOT_ORG_MIGRATE);
    expectedFeatures.remove(Feature.ENFORCEMENT);
    expectedFeatures.remove(Feature.NOTIFICATIONS);
    expectedFeatures.remove(Feature.POLICY_GRANDFATHERING);
    expectedFeatures.remove(Feature.WEBHOOKS);
    assertThat(features, containsInAnyOrder(expectedFeatures.toArray()));
  }

  private void enableLifecycleFeatures() {
    when(licenseManager.hasEnforcement()).thenReturn(true);
    when(licenseManager.hasNotifications()).thenReturn(true);
    when(licenseManager.hasPolicyGrandfathering()).thenReturn(true);
    when(licenseManager.hasWebhooks()).thenReturn(true);
  }
}
