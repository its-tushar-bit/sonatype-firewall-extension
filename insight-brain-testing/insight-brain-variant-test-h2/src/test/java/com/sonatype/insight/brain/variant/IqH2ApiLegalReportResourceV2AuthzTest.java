/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.experimental.legal.ApiLicenseLegalHdsService;
import com.sonatype.insight.brain.api.v2.ApiLegalReportResourceV2;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.legal.AttributionReportTemplate;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@IqH2Test
class IqH2ApiLegalReportResourceV2AuthzTest
{
  private IqTestContext ctx;

  private Organization org;

  private Application app;

  private User unauthorized;

  private User authorized;

  @BeforeEach
  void setup() throws Exception {
    org = ctx.tempEntity().newOrganization();
    app = ctx.tempEntity().newApplication(org.getId());
    unauthorized = ctx.tempEntity().newUser();
    authorized = ctx.tempEntity().newUser();

    PolicyEvaluation policyEvaluation =
        ctx.tempEntity().newPolicyEvaluation(app.getId(), BuildStageType.ID, TemporaryEntity.uuid());
    ctx.createReportFile(policyEvaluation.getOwnerId(), policyEvaluation.getScanId(),
        "/ApiLegalReportResourceV2Test/report/");
    ctx.hdsRespondWith("[]").atUri(ApiLicenseLegalHdsService.METADATA_URL);
    ctx.hdsRespondWith("[]").atUri(ApiLicenseLegalHdsService.LEGAL_COMMENT_URL);
    ctx.hdsRespondWith("[]").atUri(ApiLicenseLegalHdsService.LEGAL_FILE_URL);
    ctx.hdsRespondWith("[]").atUri(ApiLicenseLegalHdsService.SOURCE_LINK_URL);
  }

  private void grantPermission(String contextId, Permission permission) {
    Role role = ctx.tempEntity().newRole(false /* global */, permission);
    ctx.tempEntity().newMembershipMapping(contextId, role.getId(), authorized.getUsername());
  }

  private void grantReadPermission(String contextId) {
    grantPermission(contextId, Permission.READ);
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().anon().path(PublicApiPaths.LICENSE_LEGAL_RESOURCE_PATH_V2);
  }

  @Test
  void testPostCustomLicenseLegalApplicationReport_FromTemplate_noPerm() throws Exception {
    AttributionReportTemplate template =
        ctx.tempEntity().createNewAttributionReportTemplate("template name", "title");

    HttpResponse response =
        restRequest().path(ApiLegalReportResourceV2.APPLICATION_REPORT_FROM_TEMPLATE_PATH)
            .parameter(app.getId(), BuildStageType.ID, template.getId())
            .post();

    ctx.assertResponseStatus(401, response);
  }

  @Test
  void testPostCustomLicenseLegalApplicationReport_FromTemplate_permOnApp() throws Exception {
    grantReadPermission(app.getId());

    AttributionReportTemplate template =
        ctx.tempEntity().createNewAttributionReportTemplate("template name", "title");

    HttpResponse response =
        restRequest().path(ApiLegalReportResourceV2.APPLICATION_REPORT_FROM_TEMPLATE_PATH)
            .parameter(app.getId(), BuildStageType.ID, template.getId())
            .post();

    ctx.assertResponseStatus(401, response);
  }

  @Test
  void testPostCustomLicenseLegalApplicationReport_FromTemplate_withPerm() throws Exception {
    grantReadPermission(app.getId());
    grantPermission(app.getId(), Permission.LEGAL_REVIEWER);

    AttributionReportTemplate template =
        ctx.tempEntity().createNewAttributionReportTemplate("template name", "title");

    HttpRequest request = restRequest()
        .path(ApiLegalReportResourceV2.APPLICATION_REPORT_FROM_TEMPLATE_PATH)
        .parameter(app.getId(), BuildStageType.ID, template.getId());

    HttpResponse response = request.auth(authorized).post();
    ctx.assertResponseStatus(200, response);
  }
}
