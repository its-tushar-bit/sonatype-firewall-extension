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
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.license.model.Feature;
import com.sonatype.insight.license.model.LicensedFeature;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService.SystemConfigurationPropertyFeature.*;
import static com.sonatype.insight.brain.features.TenantFeature.MULTI_TENANT;
import static com.sonatype.insight.brain.features.TenantFeature.SINGLE_TENANT;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_ENABLED;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.QUARANTINED_COMPONENT_VIEW_ANONYMOUS_ACCESS;
import static com.sonatype.insight.brain.successmetrics.SuccessMetricsService.PROPERTY_ENABLED;
import static com.sonatype.insight.license.model.LicensedFeature.*;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.concat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
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

  @Captor
  ArgumentCaptor<String> propertyKeyCaptor;

  private TenantUtil tenantUtil = new TenantUtil();

  @Before
  public void setup() {
    tenantUtil.setGlobalTenant();
    underTest = new TestableMTIQFeatureService(productLicense, configuration, systemConfigurationPropertyDAO, service);
    ApiConfigFeaturesService.injectDependencies(systemConfigurationPropertyDAO);
  }

  @Test
  public void testRegister_setsFeatureFlags() {
    underTest.register();

    verify(service, times(20)).disableFeatureNoAuthz(propertyKeyCaptor.capture());

    List<String> flagsSet = propertyKeyCaptor.getAllValues();

    assertThat(flagsSet).containsExactlyInAnyOrder(getDisabledSystemConfigurationPropertyFeatures());
  }

  @Test
  public void testRegister_setsUserConfig() {
    underTest.register();

    verify(systemConfigurationPropertyDAO).set(PROPERTY_ENABLED, "false");
    verify(systemConfigurationPropertyDAO).set(AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_ENABLED, "true");
    verify(systemConfigurationPropertyDAO).set(QUARANTINED_COMPONENT_VIEW_ANONYMOUS_ACCESS, "false");
  }

  @Test
  public void testGetFeatures_onlyIncludesAllowedLicenseFeatures() {
    Set<Feature> features = underTest.getFeatures();

    Feature[] expectedFeatures = concat(ImmutableSet.of(
            MULTI_TENANT,
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
            SAML_USER_TOKENS,
            WEBHOOKS_FOR_APPLICATIONS).stream(),

        //Add all licensed Features
        stream(LicensedFeature.values())
            .filter(f -> !f.equals(DATA_INSIGHTS) && !f.equals(ADVANCED_LEGAL_PACK)))
        .collect(toSet()).toArray(new Feature[]{});

    assertThat(features).containsExactlyInAnyOrder(expectedFeatures);
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
    String featureName = SystemConfigurationPropertyFeature.API_PAGE.getId();

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
    String featureName = SystemConfigurationPropertyFeature.ENABLE_SSO_ONLY.getId();

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

  private String[] getDisabledSystemConfigurationPropertyFeatures() {
    return Arrays.stream(SystemConfigurationPropertyFeature.values())
        .filter(f -> !f.equals(DASHBOARD_CAN_BE_ENABLED))
        .filter(f -> !f.equals(REPORTS_LIST_CAN_BE_ENABLED))
        .filter(f -> !f.equals(EMAIL_CONFIGURATION))
        .filter(f -> !f.equals(LOGOUT_AUTH0_ON_LOGOUT))
        .filter(f -> !f.equals(WEBHOOK_CONFIGURATION))
        .filter(f -> !f.equals(ENABLE_SSO_ONLY))
        .filter(f -> !f.equals(AUTOMATIC_SCM_CONFIGURATION))
        .filter(f -> !f.equals(DEFAULT_BRANCH_MONITORING))
        .filter(f -> !f.equals(PR_COMMENTING))
        .filter(f -> !f.equals(PR_LINE_COMMENTING))
        .filter(f -> !f.equals(INTERNAL_FIREWALL_ONBOARDING_ENABLED))
        .filter(f -> !f.equals(AUTOMATIC_APPLICATION_CONFIGURATION))
        .filter(f -> !f.equals(INNER_SOURCE_REPOSITORY_INTEGRATION))
        .filter(f -> !f.equals(INNER_SOURCE_TRANSITIVE_WAIVER))
        .filter(f -> !f.equals(SAAS_PRE_REGISTER_ALL_TENANTS))
        .filter(f -> !f.equals(SAAS_LIFECYCLE_SCM_ENABLED))
        .map(SystemConfigurationPropertyFeature::getPropertyName)
        .collect(Collectors.toList()).toArray(new String[]{});
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
      Set<Feature> features = stream(LicensedFeature.values())
          .collect(toSet());

      features.add(SINGLE_TENANT);
      features.add(DASHBOARD_CAN_BE_ENABLED);

      return features;
    }
  }
}
