/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.legal;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.sonatype.insight.brain.*;
import com.sonatype.insight.brain.api.experimental.legal.ApiLicenseLegalHdsService;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.filter.AdvancedLegalPackDashboardFilter;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.security.InternalRealm;
import com.sonatype.insight.brain.service.*;
import com.sonatype.insight.json.store.JsonUtils;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import static com.sonatype.insight.brain.model.filter.UserFilter.ACTIVE_FILTER_NAME;
import static com.sonatype.insight.brain.model.filter.UserFilterType.ADVANCED_LEGAL_PACK_DASHBOARD;
import static org.assertj.core.api.Assertions.assertThat;

public class LegalReportResourceTest
    extends AbstractResourceTest
{
  private static final String EMPTY_JSON_ARRAY = "[]";

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(LegalReportResource.MULTI_APPLICATION_REPORT_FROM_FILTER);
  }

  @Test
  public void testGetDefaultLicenseLegalApplicationReportFromActiveUserFilter() throws Exception {
    String filterName = "test filter";
    Application application = tempEntity.newApplicationWithParent();
    AdvancedLegalPackDashboardFilter advancedLegalPackDashboardFilter = new AdvancedLegalPackDashboardFilter();
    advancedLegalPackDashboardFilter.getApplicationFilters().add(application.getId());
    advancedLegalPackDashboardFilter.getStageTypeFilters().add(BuildStageType.ID);
    tempEntity.newUserFilter(getUsername(), InternalRealm.ID, ACTIVE_FILTER_NAME,
        ADVANCED_LEGAL_PACK_DASHBOARD, JsonUtils.format(advancedLegalPackDashboardFilter), filterName);
    PolicyEvaluation policyEvaluationBuild =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, TemporaryEntity.uuid());
    mockReport(policyEvaluationBuild, getClass().getSimpleName());

    PolicyEvaluation policyEvaluationRelease =
        tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID, TemporaryEntity.uuid());

    mockReport(policyEvaluationRelease, getClass().getSimpleName());
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.METADATA_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.LEGAL_COMMENT_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.LEGAL_FILE_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.SOURCE_LINK_URL);

    HttpResponse response = restRequest()
        .path(LegalReportResource.REPORT)
        .part("title", "Default Report Title")
        .post();

    assertResponseStatus(200, response);
    assertThat(response.getBodyText()).contains("Default Report Title");
  }

  @Test
  public void testGetLicenseLegalMultiApplicationReportFromActiveUserFilter_ReportNotFound() throws Exception {
    String filterName = "test filter";
    Application application = tempEntity.newApplicationWithParent();
    Application application2 = tempEntity.newApplicationWithParent();
    AdvancedLegalPackDashboardFilter advancedLegalPackDashboardFilter = new AdvancedLegalPackDashboardFilter();
    advancedLegalPackDashboardFilter.getApplicationFilters().add(application.getId());
    advancedLegalPackDashboardFilter.getApplicationFilters().add(application2.getId());
    advancedLegalPackDashboardFilter.getStageTypeFilters().add(BuildStageType.ID);
    tempEntity.newUserFilter(getUsername(), InternalRealm.ID, ACTIVE_FILTER_NAME, ADVANCED_LEGAL_PACK_DASHBOARD,
        JsonUtils.format(advancedLegalPackDashboardFilter), filterName);
    tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, TemporaryEntity.uuid());
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.METADATA_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.LEGAL_COMMENT_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.LEGAL_FILE_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.SOURCE_LINK_URL);

    HttpResponse response = restRequest()
        .path(LegalReportResource.REPORT)
        .part("title", "Default Report Title")
        .post();

    assertResponseStatus(200, response);
    assertThat(response.getBodyText())
        .contains("<table id=\"table-of-contents\">")
        .doesNotContain("<div class=\"componentBox\">");
  }

  @Test
  public void testGetLicenseLegalMultiApplicationReportFromActiveUserFilter_ReportNotAuthorized() throws Exception {
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.METADATA_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.LEGAL_COMMENT_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.LEGAL_FILE_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.SOURCE_LINK_URL);
    HttpResponse response = restRequest()
        .path(LegalReportResource.REPORT)
        .part("title", "title")
        .post();
    assertResponseStatus(403, response);
    assertThat(response.getBodyText()).isEqualTo("Not authorized to generate report for applications.");
  }

  @Test
  public void testGetDefaultLicenseLegalApplicationReportFromActiveUserFilterNoFilterWithApps() throws Exception {
    Application application = tempEntity.newApplicationWithParent();

    PolicyEvaluation policyEvaluationBuild =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, TemporaryEntity.uuid());
    mockReport(policyEvaluationBuild, getClass().getSimpleName());

    PolicyEvaluation policyEvaluationRelease =
        tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID, TemporaryEntity.uuid());
    mockReport(policyEvaluationRelease, getClass().getSimpleName());

    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.METADATA_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.LEGAL_COMMENT_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.LEGAL_FILE_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.SOURCE_LINK_URL);

    File noticeFile = createNoticeFile();

    HttpResponse response =
        restRequest().path(LegalReportResource.REPORT)
            .part("title", "Report title")
            .part("header", "Report header")
            .part("footer", "Report footer")
            .part("noticeFiles", noticeFile)
            .post();

    assertResponseStatus(200, response);
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
