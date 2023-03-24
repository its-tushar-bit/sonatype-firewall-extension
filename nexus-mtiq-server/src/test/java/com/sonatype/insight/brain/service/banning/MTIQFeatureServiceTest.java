/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.banning;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService;
import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.license.model.Feature;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService.SystemConfigurationPropertyFeature.DASHBOARD_CAN_BE_ENABLED;
import static com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService.SystemConfigurationPropertyFeature.EMAIL_CONFIGURATION;
import static com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService.SystemConfigurationPropertyFeature.LOGOUT_AUTH0_ON_LOGOUT;
import static com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService.SystemConfigurationPropertyFeature.WEBHOOK_CONFIGURATION;
import static com.sonatype.insight.brain.features.TenantFeature.MULTI_TENANT;
import static com.sonatype.insight.brain.features.TenantFeature.SINGLE_TENANT;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.ADVANCED_SEARCH_ENABLED;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.AUTOMATIC_APPLICATION_CREATION_ENABLED;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_ENABLED;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.QUARANTINED_COMPONENT_VIEW_ANONYMOUS_ACCESS;
import static com.sonatype.insight.brain.successmetrics.SuccessMetricsService.PROPERTY_ENABLED;
import static com.sonatype.insight.license.model.LicensedFeature.*;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class MTIQFeatureServiceTest
{
  @Mock
  ProductLicense productLicense;

  @Mock
  Configuration configuration;

  @Mock
  SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  @Mock
  ApiConfigFeaturesService service;

  @Mock
  MTIQFeatureService underTest;

  @Before
  public void setup() {
    underTest = new TestableMTIQFeatureService(productLicense, configuration, systemConfigurationPropertyDAO, service);
  }

  @Test
  public void testRegister_setsFeatureFlags() {
    underTest.register();

    for (SystemConfigurationPropertyFeature feature : getDisabledSystemConfigurationPropertyFeatures()) {
      verify(service).disableFeatureNoAuthz(feature.getPropertyName());
    }
  }

  @Test
  public void testRegister_setsUserConfig() {
    underTest.register();

    verify(systemConfigurationPropertyDAO).set(PROPERTY_ENABLED, "false");
    verify(systemConfigurationPropertyDAO).set(ADVANCED_SEARCH_ENABLED, "false");
    verify(systemConfigurationPropertyDAO).set(AUTOMATIC_APPLICATION_CREATION_ENABLED, "false");
    verify(systemConfigurationPropertyDAO).set(AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_ENABLED, "false");
    verify(systemConfigurationPropertyDAO).set(QUARANTINED_COMPONENT_VIEW_ANONYMOUS_ACCESS, "false");
  }

  @Test
  public void testGetFeatures_onlyIncludesAllowedLicenseFeatures() {
    Set<Feature> features = underTest.getFeatures();

    assertThat(features).containsExactlyInAnyOrder(
        DASHBOARD,
        DASHBOARD_CAN_BE_ENABLED,
        HYGIENE,
        WEBHOOKS_FOR_REPOSITORIES,
        FIREWALL,
        BREAKING_CHANGE,
        EXTERNAL_DATABASE,
        FIREWALL_AUTO_UNQUARANTINE,
        ADVANCED_RECOMMENDATION_STRATEGIES,
        NODE_CLUSTERING,
        POLICY_VIOLATION_LOGGING_FOR_REPOSITORIES,
        QUALITY,
        RELEASE_INTEGRITY,
        RM_STAGING_INTEGRATION,
        SAML_USER_TOKENS);
  }

  @Test
  public void testRunsRegisterForGlobal() {
    assertThat(underTest.includeGlobalTenantDuringRegistration()).isTrue();
  }

  @Test
  public void testEnableFeature() {
    String featureName = FIREWALL.getId();

    underTest.enableFeature(featureName);

    verify(service).enableFeatureNoAuthz(featureName);
  }

  @Test
  public void testEnableFeature_throwsExceptionForUnsupportedFeature() {
    String featureName = SystemConfigurationPropertyFeature.API_PAGE.getPropertyName();

    assertThatThrownBy(() -> underTest.enableFeature(featureName))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Feature not supported: " + featureName);
  }

  @Test
  public void testDisableFeature() {
    String featureName = FIREWALL.getId();

    underTest.disableFeature(featureName);

    verify(service).disableFeatureNoAuthz(featureName);
  }

  @Test
  public void testDisableFeature_throwsExceptionForUnsupportedFeature() {
    String featureName = SystemConfigurationPropertyFeature.API_PAGE.getPropertyName();

    assertThatThrownBy(() -> underTest.disableFeature(featureName))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Feature not supported: " + featureName);
  }

  @Test
  public void testGetFeatures_containsMultiTenant() {
    Set<Feature> features = new MTIQFeatureService(
        productLicense, configuration, systemConfigurationPropertyDAO, service
    ).getFeatures();

    assertThat(features).contains(MULTI_TENANT);
    assertThat(features).doesNotContain(SINGLE_TENANT);
  }

  @Test
  public void testGetFeatures_containsAuth0Logout() {
    underTest.register();

    verify(service).enableFeatureNoAuthz(LOGOUT_AUTH0_ON_LOGOUT.getPropertyName());
  }

  private List<SystemConfigurationPropertyFeature> getDisabledSystemConfigurationPropertyFeatures() {
    return Arrays.stream(SystemConfigurationPropertyFeature.values())
        .filter(f -> !f.equals(DASHBOARD_CAN_BE_ENABLED))
        .filter(f -> !f.equals(EMAIL_CONFIGURATION))
        .filter(f -> !f.equals(LOGOUT_AUTH0_ON_LOGOUT))
        .filter(f -> !f.equals(WEBHOOK_CONFIGURATION))
        .collect(Collectors.toList());
  }

  /**
   * The super class to MTIQFeatureService (FeatureService) has a getFeatures call that ultimately calls out to a DAO
   * needing database access. That is not needed for this test and FeatureService is tested elsewhere so this impl is
   * used to mock out the call to super.getFeatures().
   */
  private static class TestableMTIQFeatureService
      extends MTIQFeatureService
  {
    public TestableMTIQFeatureService(
        ProductLicense productLicense,
        Configuration configuration,
        SystemConfigurationPropertyDAO systemConfigurationPropertyDAO,
        ApiConfigFeaturesService service)
    {
      super(productLicense, configuration, systemConfigurationPropertyDAO, service);
    }

    @Override
    Set<Feature> getBaseFeatures() {
      Set<Feature> features = stream(values())
          .collect(toSet());

      features.add(DASHBOARD_CAN_BE_ENABLED);

      return features;
    }
  }
}
