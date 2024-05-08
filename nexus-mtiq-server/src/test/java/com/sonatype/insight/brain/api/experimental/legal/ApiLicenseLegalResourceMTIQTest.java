/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.legal;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalApplicationDashboardResultDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.LicenseLegalFilterDTO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.service.AbstractMultiTenantBaseIntegrationTest;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiLicenseLegalResourceMTIQTest
    extends AbstractMultiTenantBaseIntegrationTest
{
  @Test
  public void testGetLicenseLegalApplicationsDashboard_feature_SAAS_ALP_ENABLED() throws Exception {
    testAsTestTenant(tenant -> {
      setFeatures(LicensedFeature.ADVANCED_LEGAL_PACK);

      SystemConfigurationPropertyFeature.SAAS_ALP_ENABLED.setEnabled(true);
      assertThat(SystemConfigurationPropertyFeature.SAAS_ALP_ENABLED.isEnabled()).isTrue();

      LicenseLegalFilterDTO filter = new LicenseLegalFilterDTO();
      filter.page = 1;
      filter.pageSize = 10;

      HttpResponse response =
          restRequest().path(PublicApiPaths.LICENSE_LEGAL_RESOURCE_PATH)
              .path(ApiLicenseLegalResource.DASHBOARD_APPLICATIONS_PATH).body(filter).auth().post();

      assertResponseStatus(200, response);
      ApiLicenseLegalApplicationDashboardResultDTO result =
          response.getBody(ApiLicenseLegalApplicationDashboardResultDTO.class);
      assertThat(result).isNotNull();
      assertThat(result.results).isEmpty();
      assertThat(result.totalResultsCount).isZero();
    });
  }

  @Test
  public void testGetLicenseLegalApplicationsDashboard_feature_SAAS_ALP_ENABLED_disabled() throws Exception {
    testAsTestTenant(tenant -> {
      setFeatures(LicensedFeature.ADVANCED_LEGAL_PACK);

      SystemConfigurationPropertyFeature.SAAS_ALP_ENABLED.setEnabled(false);
      assertThat(SystemConfigurationPropertyFeature.SAAS_ALP_ENABLED.isEnabled()).isFalse();

      LicenseLegalFilterDTO filter = new LicenseLegalFilterDTO();
      filter.page = 1;
      filter.pageSize = 10;

      HttpResponse response =
          restRequest().path(PublicApiPaths.LICENSE_LEGAL_RESOURCE_PATH)
              .path(ApiLicenseLegalResource.DASHBOARD_APPLICATIONS_PATH).body(filter).auth().post();

      assertResponseStatus(404, response);
    });
  }
}
