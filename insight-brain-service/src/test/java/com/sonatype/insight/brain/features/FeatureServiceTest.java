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
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.license.model.LicensedFeature;

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.Mock;

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

  @Mock
  private ProductLicense productLicense;

  private RootOrganizationConfigMigrationUtils rootOrganizationConfigMigrationUtils;

  @Override
  public void configure(Binder binder) {
    rootOrganizationConfigMigrationUtils = mock(RootOrganizationConfigMigrationUtils.class);
    binder.bind(ProductLicense.class).toInstance(productLicense);
    binder.bind(RootOrganizationConfigMigrationUtils.class).toInstance(rootOrganizationConfigMigrationUtils);
    super.configure(binder);
  }

  @Test
  public void testGetFeatures_Unlicensed() {
    assertThat(featuresService.getFeatures()).isEmpty();
  }

  @Test
  public void testGetFeatures_WithVersionSpecificFeatures() {
    Set<NonLicensedFeature> features = EnumSet.of(NonLicensedFeature.POLICY, NonLicensedFeature.LABELS,
        NonLicensedFeature.RELEASE_GRAPH, NonLicensedFeature.POLICY_VIOLATIONS, NonLicensedFeature.REEVALUATE_POLICY);
    when(productLicense.isValid()).thenReturn(true);
    assertThat(featuresService.getFeatures()).containsAll(features);
  }

  @Test
  public void testGetFeatures_WithLicenseSpecificFeatures() {
    Set<LicensedFeature> features =
        EnumSet.of(LicensedFeature.QUALITY, LicensedFeature.POLICY_MONITORING, LicensedFeature.DASHBOARD,
            LicensedFeature.CLI_INTEGRATION, LicensedFeature.ENFORCEMENT, LicensedFeature.NOTIFICATIONS,
            LicensedFeature.POLICY_GRANDFATHERING, LicensedFeature.WEBHOOKS_FOR_APPLICATIONS, LicensedFeature.FIREWALL);
    when(productLicense.isValid()).thenReturn(true);
    when(productLicense.getFeatures()).thenReturn(features);
    assertThat(featuresService.getFeatures()).containsAll(features);
  }

  @Test
  public void testGetFeatures_WithRootMigrated() {
    when(productLicense.isValid()).thenReturn(true);
    when(rootOrganizationConfigMigrationUtils.isMigrated()).thenReturn(true);
    assertThat(featuresService.getFeatures()).contains(NonLicensedFeature.ROOT_ORG)
        .doesNotContain(NonLicensedFeature.ROOT_ORG_MIGRATE);
  }

  @Test
  public void testGetFeatures_WithoutRootMigrated() {
    when(productLicense.isValid()).thenReturn(true);
    when(rootOrganizationConfigMigrationUtils.isMigrated()).thenReturn(false);
    assertThat(featuresService.getFeatures()).contains(NonLicensedFeature.ROOT_ORG_MIGRATE)
        .doesNotContain(NonLicensedFeature.ROOT_ORG);
  }

  @Test
  public void testGetFeatures_WithRootMigratedScheduled() {
    when(productLicense.isValid()).thenReturn(true);
    when(rootOrganizationConfigMigrationUtils.isMigrated()).thenReturn(false);
    when(rootOrganizationConfigMigrationUtils.isMigrationScheduled()).thenReturn(true);
    assertThat(featuresService.getFeatures()).doesNotContain(NonLicensedFeature.ROOT_ORG_MIGRATE,
        NonLicensedFeature.ROOT_ORG);
  }

  @Test
  public void testGetFeatures_WithoutAllowExternalLinks() {
    when(productLicense.isValid()).thenReturn(true);
    insightConfig.setExternalHyperlinksAllowed(false);
    assertThat(featuresService.getFeatures()).doesNotContain(NonLicensedFeature.ALLOW_EXTERNAL_HYPERLINKS);
  }

  @Test
  public void testGetFeatures_WithAllowExternalLinks() {
    when(productLicense.isValid()).thenReturn(true);
    assertThat(featuresService.getFeatures()).contains(NonLicensedFeature.ALLOW_EXTERNAL_HYPERLINKS);
  }

  @Test
  public void testGetFeatures_WithoutEnablePolicyReportPreviousVersionLink() {
    when(productLicense.isValid()).thenReturn(true);
    insightConfig.setEnablePolicyReportPreviousVersionLink(false);
    assertThat(featuresService.getFeatures())
        .doesNotContain(NonLicensedFeature.ENABLE_POLICY_REPORT_PREVIOUS_VERSION_LINK);
  }

  @Test
  public void testGetFeatures_WithEnablePolicyReportPreviousVersionLink() {
    when(productLicense.isValid()).thenReturn(true);
    assertThat(featuresService.getFeatures()).contains(NonLicensedFeature.ENABLE_POLICY_REPORT_PREVIOUS_VERSION_LINK);
  }

  @Test
  public void testGetFeatures_WithFirewallForArtifactoryFeature_BecomesFirewallFeature() {
    Set<LicensedFeature> features = EnumSet.of(LicensedFeature.FIREWALL_FOR_ARTIFACTORY);
    when(productLicense.isValid()).thenReturn(true);
    when(productLicense.getFeatures()).thenReturn(features);
    assertThat(featuresService.getFeatures()).contains(LicensedFeature.FIREWALL)
        .doesNotContain(LicensedFeature.FIREWALL_FOR_ARTIFACTORY);
  }
}
