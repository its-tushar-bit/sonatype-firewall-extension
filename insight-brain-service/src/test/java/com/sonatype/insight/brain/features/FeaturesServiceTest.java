/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.features;

import java.util.EnumSet;
import java.util.Set;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.license.model.LicensedFeature;

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class FeaturesServiceTest
    extends AbstractComponentTest
{
  @Inject
  private FeaturesService featuresService;

  @Inject
  private InsightConfig insightConfig;

  @Mock
  private ProductLicense productLicense;

  @Inject
  private SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  @Override
  public void configure(Binder binder) {
    binder.bind(ProductLicense.class).toInstance(productLicense);
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
  public void testGetFeatures_WithFirewallForArtifactoryFeature_BecomesFirewallFeature() {
    Set<LicensedFeature> features = EnumSet.of(LicensedFeature.FIREWALL_FOR_ARTIFACTORY);
    when(productLicense.isValid()).thenReturn(true);
    when(productLicense.getFeatures()).thenReturn(features);
    assertThat(featuresService.getFeatures()).contains(LicensedFeature.FIREWALL)
        .doesNotContain(LicensedFeature.FIREWALL_FOR_ARTIFACTORY);
  }

  @Test
  public void testGetFeatures_ReportsListEnabled() {
    when(productLicense.isValid()).thenReturn(true);
    assertThat(systemConfigurationPropertyDAO.getByName(SystemConfigurationProperty.REPORTS_LIST_DISABLED))
        .isNull();

    assertThat(featuresService.getFeatures()).contains(NonLicensedFeature.REPORTS_LIST);
  }

  @Test
  public void testGetFeatures_ReportsListDisabled() {
    when(productLicense.isValid()).thenReturn(true);
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.REPORTS_LIST_DISABLED, "true");

    assertThat(featuresService.getFeatures()).doesNotContain(NonLicensedFeature.REPORTS_LIST);
  }

  @Test
  public void testGetFeatures_DashboardEnabled() {
    when(productLicense.isValid()).thenReturn(true);
    when(productLicense.getFeatures()).thenReturn(EnumSet.of(LicensedFeature.DASHBOARD));
    assertThat(systemConfigurationPropertyDAO.getByName(SystemConfigurationProperty.DASHBOARD_DISABLED)).isNull();

    assertThat(featuresService.getFeatures()).contains(LicensedFeature.DASHBOARD);
  }

  @Test
  public void testGetFeatures_DashboardDisabled() {
    when(productLicense.isValid()).thenReturn(true);
    when(productLicense.getFeatures()).thenReturn(EnumSet.of(LicensedFeature.DASHBOARD));
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.DASHBOARD_DISABLED, "true");

    assertThat(featuresService.getFeatures()).doesNotContain(LicensedFeature.DASHBOARD);
  }
}
