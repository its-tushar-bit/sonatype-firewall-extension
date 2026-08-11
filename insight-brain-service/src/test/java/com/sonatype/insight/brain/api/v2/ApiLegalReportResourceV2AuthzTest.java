/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.experimental.legal.ApiLicenseLegalHdsService;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.legal.AttributionReportTemplate;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;

import org.junit.Before;
import org.junit.Test;

public class ApiLegalReportResourceV2AuthzTest
    extends AbstractResourceAuthzTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.LICENSE_LEGAL_RESOURCE_PATH_V2);
  }

  @Before
  public void setup() {
    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, TemporaryEntity.uuid());
    mockReport(policyEvaluation, "ApiLegalReportResourceV2Test");
    hdsRespondWith("[]").atUri(ApiLicenseLegalHdsService.METADATA_URL);
    hdsRespondWith("[]").atUri(ApiLicenseLegalHdsService.LEGAL_COMMENT_URL);
    hdsRespondWith("[]").atUri(ApiLicenseLegalHdsService.LEGAL_FILE_URL);
    hdsRespondWith("[]").atUri(ApiLicenseLegalHdsService.SOURCE_LINK_URL);
  }

  @Test
  public void testPostCustomLicenseLegalApplicationReport_FromTemplate_noPerm() throws Exception {
    AttributionReportTemplate template =
        tempEntity.createNewAttributionReportTemplate("template name", "title");

    HttpResponse response =
        restRequest().path(ApiLegalReportResourceV2.APPLICATION_REPORT_FROM_TEMPLATE_PATH)
            .parameter(app.getId(), BuildStageType.ID, template.getId())
            .post();

    assertResponseStatus(401, response);
  }

  @Test
  public void testPostCustomLicenseLegalApplicationReport_FromTemplate_permOnApp() throws Exception {
    grantReadPermission(app.getId());

    AttributionReportTemplate template =
        tempEntity.createNewAttributionReportTemplate("template name", "title");

    HttpResponse response =
        restRequest().path(ApiLegalReportResourceV2.APPLICATION_REPORT_FROM_TEMPLATE_PATH)
            .parameter(app.getId(), BuildStageType.ID, template.getId())
            .post();

    assertResponseStatus(401, response);
  }

  @Test
  public void testPostCustomLicenseLegalApplicationReport_FromTemplate_withPerm() throws Exception {
    grantReadPermission(app.getId());
    grantPermission(app.getId(), Permission.LEGAL_REVIEWER);

    AttributionReportTemplate template =
        tempEntity.createNewAttributionReportTemplate("template name", "title");

    HttpRequest request = restRequest()
        .path(ApiLegalReportResourceV2.APPLICATION_REPORT_FROM_TEMPLATE_PATH)
        .parameter(app.getId(), BuildStageType.ID, template.getId());

    HttpResponse response = request.auth(authorized).post();
    assertResponseStatus(200, response);
  }
}
