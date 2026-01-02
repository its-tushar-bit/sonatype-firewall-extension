/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.banning;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.developer.integrationdashboard.DeveloperEnablementService;
import com.sonatype.insight.brain.features.NonLicensedFeature;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.service.AbstractMultiTenantBaseIntegrationTest;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.license.model.Feature;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.brain.common.test.SlowTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static com.sonatype.insight.brain.features.TenantFeature.MULTI_TENANT;
import static com.sonatype.insight.brain.features.TenantFeature.SINGLE_TENANT;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_ENABLED;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.QUARANTINED_COMPONENT_VIEW_ANONYMOUS_ACCESS;
import static com.sonatype.insight.brain.successmetrics.SuccessMetricsService.PROPERTY_ENABLED;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
@Category(SlowTest.class)
public class MTIQFeatureServiceTest
    extends AbstractMultiTenantBaseIntegrationTest
{
  ProductLicense productLicense;

  @Mock
  Configuration configuration;

  @Mock
  ApiConfigFeaturesService service;

  @Mock
  DeveloperEnablementService developerEnablementService;

  SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  MTIQFeatureService underTest;

  @Captor
  ArgumentCaptor<String> propertyKeyCaptor;

  @Before
  public void setup() {
    systemConfigurationPropertyDAO = lookup(SystemConfigurationPropertyDAO.class);
    productLicense = lookup(ProductLicense.class);
    underTest = new MTIQFeatureService(productLicense, configuration, systemConfigurationPropertyDAO, service,
        developerEnablementService);
  }

  @Test
  public void testRegister_setsUserConfig() {
    underTest.register();
    assertThat(systemConfigurationPropertyDAO.getByName(PROPERTY_ENABLED).getValue()).isEqualTo("false");
    assertThat(systemConfigurationPropertyDAO.getByName(AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_ENABLED).getValue())
        .isEqualTo("true");
    assertThat(systemConfigurationPropertyDAO.getByName(QUARANTINED_COMPONENT_VIEW_ANONYMOUS_ACCESS).getValue())
        .isEqualTo("false");
  }

  @Test
  public void testGetFeatures_onlyIncludesAllowedFeatures() {
    Set<Feature> features = underTest.getFeatures();
    Set<Feature> expectedFeatures = getExpectedFeatures();
    // These features are enabled via HDS only, so we cannot expect it to be enabled here.
    expectedFeatures.remove(LicensedFeature.ALLOW_SCM_ON_PUBLIC_REPOS);
    expectedFeatures.remove(LicensedFeature.CPE_MATCHING);

    expectedFeatures.remove(LicensedFeature.MALWARE_DEFENSE);

    // only test if it was enabled that it's expected
    if (!SystemConfigurationPropertyFeature.SAML_ENABLED.isEnabled()) {
      expectedFeatures.remove(SystemConfigurationPropertyFeature.SAML_ENABLED);
    }

    assertThat(features).containsExactlyInAnyOrderElementsOf(expectedFeatures);
  }

  @Test
  public void testRunsRegisterForGlobal() {
    assertThat(underTest.includeGlobalTenantDuringRegistration()).isTrue();
  }

  @Test
  public void testEnableFeature() {
    String featureName = LicensedFeature.FIREWALL.getId();

    underTest.enableFeature(featureName);

    verify(service).enableFeatureNoAuthz(featureName);
  }

  @Test
  public void testEnableFeature_throwsExceptionForUnsupportedFeature() {
    List<Feature> expectedBannedFeatures = List.of(
        LicensedFeature.DATA_INSIGHTS,
        SystemConfigurationPropertyFeature.SUCCESS_METRICS_CONFIGURATION,
        SystemConfigurationPropertyFeature.PRODUCT_LICENSE_CONFIGURATION,
        SystemConfigurationPropertyFeature.SYSTEM_NOTICE_CONFIGURATION,
        SystemConfigurationPropertyFeature.ENABLE_UNAUTHENTICATED_PAGES,
        SystemConfigurationPropertyFeature.PROXY_CONFIGURATION,
        SystemConfigurationPropertyFeature.DEPENDENCY_DATA_IN_API,
        SystemConfigurationPropertyFeature.CROWD_INTEGRATION,
        SystemConfigurationPropertyFeature.COMPONENT_SEARCH_API_WITH_INNERSOURCE,
        SystemConfigurationPropertyFeature.CODE_INSIGHTS,
        SystemConfigurationPropertyFeature.LDAP_CONFIGURATION,
        SystemConfigurationPropertyFeature.SCAN_NPM_DEV_AND_OPT_DEPENDENCIES,
        SystemConfigurationPropertyFeature.SCAN_POM_FILES_IN_META_INF_DIRECTORY,
        SystemConfigurationPropertyFeature.VULNERABILITY_SOURCE,
        SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE,
        SystemConfigurationPropertyFeature.INTERNAL_SOURCE_CONTROL_POLICY_EVALUATIONS
    );

    assertThat(expectedBannedFeatures).allSatisfy(expectedBannedFeature -> {
      assertThatThrownBy(() -> underTest.enableFeature(expectedBannedFeature.getId()))
          .isInstanceOf(BadRequestException.class)
          .hasMessage("Feature not supported: " + expectedBannedFeature.getId());
    });
  }

  @Test
  public void testDisableFeature() {
    String featureName = LicensedFeature.FIREWALL.getId();

    underTest.disableFeature(featureName);

    verify(service).disableFeatureNoAuthz(featureName);
  }

  @Test
  public void testDisableFeature_throwsExceptionForUnsupportedFeature() {
    List<Feature> expectedAlwaysEnabledFeatures = List.of(
        SystemConfigurationPropertyFeature.AUTOMATIC_APPLICATION_CONFIGURATION,
        SystemConfigurationPropertyFeature.INNER_SOURCE_REPOSITORY_INTEGRATION,
        SystemConfigurationPropertyFeature.INNER_SOURCE_TRANSITIVE_WAIVER
    );

    assertThat(expectedAlwaysEnabledFeatures).allSatisfy(expectedAlwaysEnabledFeature -> {
      assertThatThrownBy(() -> underTest.disableFeature(expectedAlwaysEnabledFeature.getId()))
          .isInstanceOf(BadRequestException.class)
          .hasMessage("Feature not supported: " + expectedAlwaysEnabledFeature.getId());
    });
  }

  @Test
  public void testGetFeatures_containsMultiTenant() {
    Set<Feature> features = new MTIQFeatureService(
        productLicense, configuration, systemConfigurationPropertyDAO, service, developerEnablementService
    ).getFeatures();

    assertThat(features).contains(MULTI_TENANT);
    assertThat(features).doesNotContain(SINGLE_TENANT);
  }

  @Test
  public void testGetFeatures_containsAuth0Logout() {
    underTest.register();

    verify(service).enableFeatureNoAuthz(SystemConfigurationPropertyFeature.LOGOUT_AUTH0_ON_LOGOUT.getPropertyName());
  }

  private String[] getDisabledSystemConfigurationPropertyFeatures() {
    List<SystemConfigurationPropertyFeature> enabledFeatures =
        Arrays.asList(getEnabledSystemConfigurationPropertyFeatures());

    return Arrays.stream(SystemConfigurationPropertyFeature.values())
        .filter(f -> !enabledFeatures.contains(f))
        .map(SystemConfigurationPropertyFeature::getPropertyName)
        .toList()
        .toArray(new String[]{});
  }

  private SystemConfigurationPropertyFeature[] getEnabledSystemConfigurationPropertyFeatures() {
    return Stream.of(
        SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION,
        SystemConfigurationPropertyFeature.ADVANCED_SEARCH_ENABLED,
        SystemConfigurationPropertyFeature.AUTOMATIC_APPLICATION_CONFIGURATION,
        SystemConfigurationPropertyFeature.AUTOMATIC_SCM_CONFIGURATION,
        SystemConfigurationPropertyFeature.DASHBOARD_CAN_BE_ENABLED,
        SystemConfigurationPropertyFeature.DEFAULT_BRANCH_MONITORING,
        SystemConfigurationPropertyFeature.DEVELOPMENT_DASHBOARD_METRIC_COLLECTION,
        SystemConfigurationPropertyFeature.EMAIL_CONFIGURATION,
        SystemConfigurationPropertyFeature.ENABLE_SSO_ONLY,
        SystemConfigurationPropertyFeature.INNER_SOURCE_REPOSITORY_INTEGRATION,
        SystemConfigurationPropertyFeature.INNER_SOURCE_TRANSITIVE_WAIVER,
        SystemConfigurationPropertyFeature.LOGOUT_AUTH0_ON_LOGOUT,
        SystemConfigurationPropertyFeature.PR_COMMENTING,
        SystemConfigurationPropertyFeature.PR_LINE_COMMENTING,
        SystemConfigurationPropertyFeature.REPORTS_LIST_CAN_BE_ENABLED,
        SystemConfigurationPropertyFeature.SAAS_LIFECYCLE_SCM_ENABLED,
        SystemConfigurationPropertyFeature.WEBHOOK_CONFIGURATION,
        SystemConfigurationPropertyFeature.PRIORITIZED_FINDINGS_REPORT,
        SystemConfigurationPropertyFeature.DEVELOPER_SUMMARY_TABLE,
        SystemConfigurationPropertyFeature.CLEAN_UP_SBOM_CONTINUOUS_MONITORING_REPORT,
        SystemConfigurationPropertyFeature.SBOM_BINARY_SCANNING,
        SystemConfigurationPropertyFeature.SBOM_CONTINUOUS_MONITORING_UI,
        SystemConfigurationPropertyFeature.TRANSITIVE_SOLVER,
        SystemConfigurationPropertyFeature.DEVELOPER_SUGGEST_NON_BREAKING_VERSION,
        SystemConfigurationPropertyFeature.NON_BREAKING_VERSION_SUGGESTION_TELEMETRY,
        SystemConfigurationPropertyFeature.SBOM_POLICIES,
        SystemConfigurationPropertyFeature.AUTO_WAIVERS,
        SystemConfigurationPropertyFeature.API_PAGE,
        SystemConfigurationPropertyFeature.ZSCALER,
        SystemConfigurationPropertyFeature.THIRD_PARTY_KEV_LOOKUP,
        SystemConfigurationPropertyFeature.SAML_ENABLED,
        SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED,
        SystemConfigurationPropertyFeature.WAIVER_REQUEST_WORKFLOW_ENABLED,
        SystemConfigurationPropertyFeature.EXIT_ON_FATAL_ERROR
    ).collect(toSet()).toArray(new SystemConfigurationPropertyFeature[]{});
  }

  private Set<Feature> getExpectedFeatures() {
    return Stream.of(
        Stream.of(MULTI_TENANT),
        // We're expecting SystemConfigurationPropertyFeature.API_PAGE to be enabled 
        // and LicensedFeature.API_PAGE to be present
        // however we only return LicensedFeature.API_PAGE
        Stream.of(getEnabledSystemConfigurationPropertyFeatures())
            .filter(f -> !f.equals(SystemConfigurationPropertyFeature.API_PAGE)),
        stream(LicensedFeature.values())
            .filter(f -> !f.equals(LicensedFeature.DATA_INSIGHTS))
            .filter(f -> !f.equals(LicensedFeature.FIREWALL_FOR_ARTIFACTORY))
            .filter(f -> !f.equals(LicensedFeature.INFRASTRUCTURE_AS_CODE_PACK))
            .filter(f -> !f.equals(LicensedFeature.INTEGRATED_ENTERPRISE_REPORTING))
            .filter(f -> !f.equals(LicensedFeature.SAML_USER_TOKENS))
            .filter(f -> !f.equals(LicensedFeature.SBOM_MANAGER))
            .filter(f -> !f.equals(LicensedFeature.SCM_UX_IMPROVEMENTS))
            .filter(f -> !f.equals(LicensedFeature.DEVELOPER_DASHBOARD))
            .filter(f -> !f.equals(LicensedFeature.DEVELOPER_VERSION_UPPER_BOUND)),
        stream(NonLicensedFeature.values())
            .filter(f -> !f.equals(NonLicensedFeature.ALLOW_EXTERNAL_HYPERLINKS))
    ).flatMap(i -> i).map(Feature.class::cast).collect(toSet());
  }
}
