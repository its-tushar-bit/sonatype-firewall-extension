/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.legal;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.v2.ApiLegalAttributionReportTemplateResourceV2;
import com.sonatype.insight.brain.api.v2.dto.legal.AttributionReportTemplateDTO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.legal.AttributionReportTemplate;
import com.sonatype.insight.brain.service.AbstractMultiTenantBaseIntegrationTest;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.Test;

import static com.sonatype.insight.brain.api.PublicApiPaths.LICENSE_LEGAL_RESOURCE_PATH_V2;
import static org.assertj.core.api.Assertions.assertThat;

public class ApiLegalAttributionReportTemplateResourceV2MTIQTest
    extends AbstractMultiTenantBaseIntegrationTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(LICENSE_LEGAL_RESOURCE_PATH_V2);
  }

  @Test
  public void testGetAttributionReportTemplateById_feature_SAAS_ALP_ENABLED() {
    testAsTestTenant(tenant -> {
      setFeatures(LicensedFeature.ADVANCED_LEGAL_PACK);

      SystemConfigurationPropertyFeature.SAAS_ALP_ENABLED.setEnabled(true);
      assertThat(SystemConfigurationPropertyFeature.SAAS_ALP_ENABLED.isEnabled()).isTrue();

      AttributionReportTemplate report = tenantTemporaryEntity
          .createNewAttributionReportTemplate("template one", "title");
      tenantTemporaryEntity.createNewAttributionReportTemplate("template two", "second title");

      HttpResponse response = restRequest()
          .path(ApiLegalAttributionReportTemplateResourceV2.REPORT_TEMPLATE_PATH_ID)
          .parameter(report.getId())
          .auth().get();

      assertResponseStatus(200, response);
      AttributionReportTemplateDTO result =
          response.getBody(AttributionReportTemplateDTO.class);
      assertThat(result).isNotNull();
      assertThat(result.getDocumentTitle()).isEqualTo(report.getDocumentTitle());
    });
  }

  @Test
  public void testGetAttributionReportTemplateById_feature_SAAS_ALP_ENABLED_disabled() {
    testAsTestTenant(tenant -> {
      setFeatures(LicensedFeature.ADVANCED_LEGAL_PACK);

      SystemConfigurationPropertyFeature.SAAS_ALP_ENABLED.setEnabled(false);
      assertThat(SystemConfigurationPropertyFeature.SAAS_ALP_ENABLED.isEnabled()).isFalse();

      AttributionReportTemplate report = tenantTemporaryEntity
          .createNewAttributionReportTemplate("template one", "title");
      tenantTemporaryEntity.createNewAttributionReportTemplate("template two", "second title");

      HttpResponse response = restRequest()
          .path(ApiLegalAttributionReportTemplateResourceV2.REPORT_TEMPLATE_PATH_ID)
          .parameter(report.getId())
          .auth().get();

      assertResponseStatus(404, response);
    });
  }
}
