/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.experimental.legal.ApiLicenseLegalHdsService;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.filter.AdvancedLegalPackDashboardFilter;
import com.sonatype.insight.brain.legal.LegalReportResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.security.InternalRealm;
import com.sonatype.insight.json.store.JsonUtils;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.model.filter.UserFilter.ACTIVE_FILTER_NAME;
import static com.sonatype.insight.brain.model.filter.UserFilterType.ADVANCED_LEGAL_PACK_DASHBOARD;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Named exactly as the original {@code LegalReportResourceTest} (rather than the default
 * {@code IqH2LegalReportResourceTest}) because {@link #createReport} resolves the mock report fixture
 * via {@code getClass().getSimpleName()}; the fixture on this module's classpath must live under
 * {@code /LegalReportResourceTest/report/}.
 */
@IqH2Test
class LegalReportResourceTest
{
  private static final String EMPTY_JSON_ARRAY = "[]";

  private IqTestContext ctx;

  private HttpRequest restRequest() {
    return ctx.restRequest().path(LegalReportResource.MULTI_APPLICATION_REPORT_FROM_FILTER);
  }

  private void createReport(PolicyEvaluation evaluation) throws IOException {
    ctx.createReportFile(evaluation.getOwnerId(), evaluation.getScanId(),
        "/" + getClass().getSimpleName() + "/report/");
  }

  @Test
  void testGetDefaultLicenseLegalApplicationReportFromActiveUserFilter() throws Exception {
    String filterName = "test filter";
    Application application = ctx.tempEntity().newApplicationWithParent();
    AdvancedLegalPackDashboardFilter advancedLegalPackDashboardFilter = new AdvancedLegalPackDashboardFilter();
    advancedLegalPackDashboardFilter.getApplicationFilters().add(application.getId());
    advancedLegalPackDashboardFilter.getStageTypeFilters().add(BuildStageType.ID);
    ctx.tempEntity()
        .newUserFilter(ctx.getUsername(), InternalRealm.ID, ACTIVE_FILTER_NAME,
            ADVANCED_LEGAL_PACK_DASHBOARD, JsonUtils.format(advancedLegalPackDashboardFilter), filterName);
    PolicyEvaluation policyEvaluationBuild =
        ctx.tempEntity().newPolicyEvaluation(application.getId(), BuildStageType.ID, TemporaryEntity.uuid());
    createReport(policyEvaluationBuild);

    PolicyEvaluation policyEvaluationRelease =
        ctx.tempEntity().newPolicyEvaluation(application.getId(), ReleaseStageType.ID, TemporaryEntity.uuid());

    createReport(policyEvaluationRelease);
    ctx.hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.METADATA_URL);
    ctx.hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.LEGAL_COMMENT_URL);
    ctx.hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.LEGAL_FILE_URL);
    ctx.hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.SOURCE_LINK_URL);

    HttpResponse response = restRequest()
        .path(LegalReportResource.REPORT)
        .part("title", "Default Report Title")
        .post();

    ctx.assertResponseStatus(200, response);
    assertThat(response.getBodyText()).contains("Default Report Title");
  }

  @Test
  void testGetLicenseLegalMultiApplicationReportFromActiveUserFilter_ReportNotFound() throws Exception {
    String filterName = "test filter";
    Application application = ctx.tempEntity().newApplicationWithParent();
    Application application2 = ctx.tempEntity().newApplicationWithParent();
    AdvancedLegalPackDashboardFilter advancedLegalPackDashboardFilter = new AdvancedLegalPackDashboardFilter();
    advancedLegalPackDashboardFilter.getApplicationFilters().add(application.getId());
    advancedLegalPackDashboardFilter.getApplicationFilters().add(application2.getId());
    advancedLegalPackDashboardFilter.getStageTypeFilters().add(BuildStageType.ID);
    ctx.tempEntity()
        .newUserFilter(ctx.getUsername(), InternalRealm.ID, ACTIVE_FILTER_NAME, ADVANCED_LEGAL_PACK_DASHBOARD,
            JsonUtils.format(advancedLegalPackDashboardFilter), filterName);
    ctx.tempEntity().newPolicyEvaluation(application.getId(), BuildStageType.ID, TemporaryEntity.uuid());
    ctx.hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.METADATA_URL);
    ctx.hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.LEGAL_COMMENT_URL);
    ctx.hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.LEGAL_FILE_URL);
    ctx.hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.SOURCE_LINK_URL);

    HttpResponse response = restRequest()
        .path(LegalReportResource.REPORT)
        .part("title", "Default Report Title")
        .post();

    ctx.assertResponseStatus(200, response);
    assertThat(response.getBodyText())
        .contains("<table id=\"table-of-contents\">")
        .doesNotContain("<div class=\"componentBox\">");
  }

  @Test
  void testGetLicenseLegalMultiApplicationReportFromActiveUserFilter_ReportNotAuthorized() throws Exception {
    ctx.hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.METADATA_URL);
    ctx.hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.LEGAL_COMMENT_URL);
    ctx.hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.LEGAL_FILE_URL);
    ctx.hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.SOURCE_LINK_URL);
    HttpResponse response = restRequest()
        .path(LegalReportResource.REPORT)
        .part("title", "title")
        .post();
    ctx.assertResponseStatus(403, response);
    assertThat(response.getBodyText()).isEqualTo("Not authorized to generate report for applications.");
  }

  @Test
  void testGetDefaultLicenseLegalApplicationReportFromActiveUserFilterNoFilterWithApps() throws Exception {
    Application application = ctx.tempEntity().newApplicationWithParent();

    PolicyEvaluation policyEvaluationBuild =
        ctx.tempEntity().newPolicyEvaluation(application.getId(), BuildStageType.ID, TemporaryEntity.uuid());
    createReport(policyEvaluationBuild);

    PolicyEvaluation policyEvaluationRelease =
        ctx.tempEntity().newPolicyEvaluation(application.getId(), ReleaseStageType.ID, TemporaryEntity.uuid());
    createReport(policyEvaluationRelease);

    ctx.hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.METADATA_URL);
    ctx.hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.LEGAL_COMMENT_URL);
    ctx.hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.LEGAL_FILE_URL);
    ctx.hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.SOURCE_LINK_URL);

    File noticeFile = createNoticeFile();

    HttpResponse response =
        restRequest().path(LegalReportResource.REPORT)
            .part("title", "Report title")
            .part("header", "Report header")
            .part("footer", "Report footer")
            .part("noticeFiles", noticeFile)
            .post();

    ctx.assertResponseStatus(200, response);
    String bodyText = response.getBodyText();

    assertThat(bodyText)
        .contains("notice file content")
        .contains("Report title")
        .contains("Report header")
        .contains("Report footer");
  }

  private File createNoticeFile() throws IOException {
    File file = File.createTempFile("notice", ".txt");
    file.deleteOnExit();
    FileUtils.writeStringToFile(file, "notice file content", StandardCharsets.UTF_8);
    return file;
  }
}
