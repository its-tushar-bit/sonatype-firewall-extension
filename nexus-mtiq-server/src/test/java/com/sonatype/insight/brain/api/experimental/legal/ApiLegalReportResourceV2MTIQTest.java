/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.legal;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.ApiLegalReportResourceV2;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalApplicationReportDTO;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.service.AbstractMultiTenantBaseIntegrationTest;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiLegalReportResourceV2MTIQTest
    extends AbstractMultiTenantBaseIntegrationTest
{
  private static final String EMPTY_JSON_ARRAY = "[]";

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.LICENSE_LEGAL_RESOURCE_PATH_V2);
  }

  @Test
  public void testGetDefaultLicenseLegalApplicationReport_feature_SAAS_ALP_ENABLED() {
    testAsTestTenant(tenant -> {
      setFeatures(LicensedFeature.ADVANCED_LEGAL_PACK);

      SystemConfigurationPropertyFeature.SAAS_ALP_ENABLED.setEnabled(true);
      assertThat(SystemConfigurationPropertyFeature.SAAS_ALP_ENABLED.isEnabled()).isTrue();

      Application application = tenantTemporaryEntity.newApplicationWithParent();
      PolicyEvaluation policyEvaluation =
          tenantTemporaryEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, TemporaryEntity.uuid());
      mockReport(policyEvaluation, getClass().getSimpleName());
      hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.METADATA_URL);
      hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.LEGAL_COMMENT_URL);
      hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.LEGAL_FILE_URL);
      hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.SOURCE_LINK_URL);

      HttpResponse response =
          restRequest().path(ApiLegalReportResourceV2.APPLICATION_PATH)
              .parameter(application.getId())
              .get();

      assertResponseStatus(200, response);
      ApiLicenseLegalApplicationReportDTO
          apiLicenseLegalApplicationReportDTO = response.getBody(ApiLicenseLegalApplicationReportDTO.class);
      assertThat(apiLicenseLegalApplicationReportDTO).isNotNull();
      assertThat(apiLicenseLegalApplicationReportDTO.components).hasSize(14);
      assertThat(apiLicenseLegalApplicationReportDTO.licenseLegalMetadata).hasSize(8);
    });
  }

  @Test
  public void testGetDefaultLicenseLegalApplicationReport_feature_SAAS_ALP_ENABLED_disabled() {
    testAsTestTenant(tenant -> {
      setFeatures(LicensedFeature.ADVANCED_LEGAL_PACK);

      SystemConfigurationPropertyFeature.SAAS_ALP_ENABLED.setEnabled(false);
      assertThat(SystemConfigurationPropertyFeature.SAAS_ALP_ENABLED.isEnabled()).isFalse();

      Application application = tenantTemporaryEntity.newApplicationWithParent();

      HttpResponse response =
          restRequest().path(ApiLegalReportResourceV2.APPLICATION_PATH)
              .parameter(application.getId())
              .get();

      assertResponseStatus(404, response);
    });
  }
}
