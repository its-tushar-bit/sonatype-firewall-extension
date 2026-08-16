/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.features;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.service.HdsMockServerRule;
import com.sonatype.insight.brain.service.InsightConfig.Feature;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.license.model.LicensedFeature;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

@ComponentH2Test
public class FeaturesServiceTest
    extends AbstractComponentH2Test
{
  public static HdsMockServerRule hdsMockServer = new HdsMockServerRule();

  @Inject
  private FeaturesService featuresService;

  @Mock
  private ProductLicense productLicense;

  @Inject
  private ApiConfigurationService configurationService;

  @BeforeEach
  public void before() {
    hdsMockServer.reset();
    setHdsUrl(hdsMockServer.getHttpUrl());
  }

  @Test
  public void testGetFeatures_Unlicensed() {
    assertThat(featuresService.getFeatures()).isEmpty();
  }

  @Test
  public void testGetFeatures_WithVersionSpecificFeatures() {
    Set<NonLicensedFeature> features = EnumSet.of(NonLicensedFeature.POLICY, NonLicensedFeature.LABELS,
        NonLicensedFeature.RELEASE_GRAPH, NonLicensedFeature.REEVALUATE_POLICY);
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
    try {
      configurationService.setConfigurationInDatabaseNoAuthz(SystemConfigurationProperty.EXTERNAL_HYPERLINKS_ALLOWED,
          false);
      configurationService.applyConfigurationToClients(SystemConfigurationProperty.EXTERNAL_HYPERLINKS_ALLOWED);
      assertThat(featuresService.getFeatures()).doesNotContain(NonLicensedFeature.ALLOW_EXTERNAL_HYPERLINKS);
    }
    finally {
      configurationService.deleteConfigurationInDatabaseNoAuthz(
          SystemConfigurationProperty.EXTERNAL_HYPERLINKS_ALLOWED);
      configurationService.applyConfigurationToClients(SystemConfigurationProperty.EXTERNAL_HYPERLINKS_ALLOWED);
    }
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

  @Test
  public void testGetFeatures_NoDuplicates() {
    List<com.sonatype.insight.license.model.Feature> allFeatures = new ArrayList<>();
    allFeatures.addAll(Arrays.asList(LicensedFeature.values()));
    allFeatures.addAll(Arrays.asList(NonLicensedFeature.values()));
    allFeatures.addAll(Arrays.asList(Feature.values()));
    List<String> allFeatureIdsList = allFeatures.stream()
        .map(com.sonatype.insight.license.model.Feature::getId)
        .collect(Collectors.toList());
    Set<String> allFeatureIdsSet = new LinkedHashSet<>(allFeatureIdsList);

    assertThat(allFeatureIdsSet).hasSize(allFeatureIdsList.size());
  }

  @Test
  public void testGetFeatures_PrioritizedFindingsReportEnabledByDefault() {
    when(productLicense.isValid()).thenReturn(true);
    assertThat(featuresService.getFeatures())
        .contains(SystemConfigurationPropertyFeature.PRIORITIZED_FINDINGS_REPORT);
  }

  @Test
  public void testGetFeatures_DeveloperBulkRecommendationsEnabled() {
    when(productLicense.isValid()).thenReturn(true);
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.DEVELOPER_BULK_RECOMMENDATIONS, "true");
    assertThat(featuresService.getFeatures())
        .contains(SystemConfigurationPropertyFeature.DEVELOPER_BULK_RECOMMENDATIONS);
  }

  @Test
  public void testGetFeatures_DeveloperBulkRecommendationsDisabled() {
    when(productLicense.isValid()).thenReturn(true);
    assertThat(featuresService.getFeatures())
        .doesNotContain(SystemConfigurationPropertyFeature.DEVELOPER_BULK_RECOMMENDATIONS);
  }

  @Test
  public void testGetFeatures_API_PAGE() {
    when(productLicense.isValid()).thenReturn(true);

    // Both LicensedFeature.API_PAGE and SystemConfigurationPropertyFeature.API_PAGE
    SystemConfigurationPropertyFeature.API_PAGE.setEnabled(true);
    when(productLicense.getFeatures()).thenReturn(Collections.singleton(LicensedFeature.API_PAGE));
    assertThat(featuresService.getFeatures()).contains(LicensedFeature.API_PAGE);

    // Only LicensedFeature.API_PAGE
    SystemConfigurationPropertyFeature.API_PAGE.setEnabled(false);
    when(productLicense.getFeatures()).thenReturn(Collections.singleton(LicensedFeature.API_PAGE));
    assertThat(featuresService.getFeatures()).doesNotContain(LicensedFeature.API_PAGE,
        SystemConfigurationPropertyFeature.API_PAGE);

    // Only SystemConfigurationPropertyFeature.API_PAGE
    SystemConfigurationPropertyFeature.API_PAGE.setEnabled(true);
    when(productLicense.getFeatures()).thenReturn(Collections.emptySet());
    assertThat(featuresService.getFeatures()).doesNotContain(LicensedFeature.API_PAGE,
        SystemConfigurationPropertyFeature.API_PAGE);

    // Neither LicensedFeature.API_PAGE or SystemConfigurationPropertyFeature.API_PAGE
    SystemConfigurationPropertyFeature.API_PAGE.setEnabled(false);
    when(productLicense.getFeatures()).thenReturn(Collections.emptySet());
    assertThat(featuresService.getFeatures()).doesNotContain(LicensedFeature.API_PAGE,
        SystemConfigurationPropertyFeature.API_PAGE);
  }

  @Test
  public void testGetFeatures_SamlEnabled() {
    when(productLicense.isValid()).thenReturn(true);

    assertThat(featuresService.getFeatures())
        .contains(SystemConfigurationPropertyFeature.SAML_ENABLED);

    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.SAML_ENABLED, "true");
    assertThat(featuresService.getFeatures())
        .contains(SystemConfigurationPropertyFeature.SAML_ENABLED);

    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.SAML_ENABLED, "false");
    assertThat(featuresService.getFeatures())
        .doesNotContain(SystemConfigurationPropertyFeature.SAML_ENABLED);
  }

  @Test
  public void testGetFeatures_UserManagementPagesEnabled() {
    when(productLicense.isValid()).thenReturn(true);
    assertThat(featuresService.getFeatures())
        .contains(SystemConfigurationPropertyFeature.USER_MANAGEMENT_PAGES);
  }

  @Test
  public void testGetFeatures_UserManagementPagesDisabled() {
    when(productLicense.isValid()).thenReturn(true);
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.USER_MANAGEMENT_PAGES, "false");
    assertThat(featuresService.getFeatures())
        .doesNotContain(SystemConfigurationPropertyFeature.USER_MANAGEMENT_PAGES);
  }
}
