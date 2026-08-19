/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.experimental.legal.ApiLicenseLegalHdsService;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalApplicationReportDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalComponentReportDTO;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.legal.AttributionReportTemplate;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.brain.variant.IqPostgresTest;
import com.sonatype.insight.brain.variant.IqTestContext;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import io.netty.channel.socket.ChannelOutputShutdownException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.api.v2.ApiLegalReportResourceV2.MAX_REQUEST_SIZE;
import static com.sonatype.insight.brain.api.v2.ApiLegalReportResourceV2.MAX_REQUEST_SIZE_MESSAGE;
import static com.sonatype.insight.brain.api.v2.ApiLegalReportResourceV2.REPORT_FORM_FOOTER;
import static com.sonatype.insight.brain.api.v2.ApiLegalReportResourceV2.REPORT_FORM_HEADER;
import static com.sonatype.insight.brain.api.v2.ApiLegalReportResourceV2.REPORT_FORM_TITLE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Package-scoped: touches {@link ApiLegalReportResourceV2}'s package-private {@code REPORT_FORM_TITLE}/
 * {@code REPORT_FORM_HEADER}/{@code REPORT_FORM_FOOTER} constants, and resolves report fixtures via
 * {@code getClass().getSimpleName()} — so the class keeps the original simple name and package (see
 * convert-resource-test-to-variant skill, Step 3).
 */
@IqPostgresTest
class ApiLegalReportResourceV2Test
{
  private static final String EMPTY_JSON_ARRAY = "[]";

  private IqTestContext ctx;

  private HttpRequest restRequest() {
    return ctx.restRequest().path(PublicApiPaths.LICENSE_LEGAL_RESOURCE_PATH_V2);
  }

  @Test
  void testGetDefaultLicenseLegalApplicationReport() throws Exception {
    List<Application> applications = prepareApplications(1);
    List<PolicyEvaluation> evaluations = prepareEvaluations(applications, BuildStageType.ID);
    mockReport(evaluations.get(0), getClass().getSimpleName());
    mockHdsResponse();

    HttpResponse response =
        restRequest().path(ApiLegalReportResourceV2.APPLICATION_PATH)
            .parameter(applications.get(0).getId())
            .get();

    ctx.assertResponseStatus(200, response);
    ApiLicenseLegalApplicationReportDTO apiLicenseLegalApplicationReportDTO =
        response.getBody(ApiLicenseLegalApplicationReportDTO.class);
    assertThat(apiLicenseLegalApplicationReportDTO).isNotNull();
    assertThat(apiLicenseLegalApplicationReportDTO.components).hasSize(14);
    assertThat(apiLicenseLegalApplicationReportDTO.licenseLegalMetadata).hasSize(8);
  }

  @Test
  void testGetDefaultLicenseLegalApplicationReportByStage() throws Exception {
    List<Application> applications = prepareApplications(1);
    List<PolicyEvaluation> buildStageEvaluations = prepareEvaluations(applications, BuildStageType.ID);
    List<PolicyEvaluation> releaseStageEvaluations = prepareEvaluations(applications, ReleaseStageType.ID);
    mockReport(buildStageEvaluations.get(0), getClass().getSimpleName());
    mockReport(releaseStageEvaluations.get(0), getClass().getSimpleName());
    mockHdsResponse();

    HttpResponse response =
        restRequest().path(ApiLegalReportResourceV2.APPLICATION_PATH_STAGE)
            .parameter(applications.get(0).getId(), BuildStageType.ID)
            .get();

    ctx.assertResponseStatus(200, response);
    ApiLicenseLegalApplicationReportDTO apiLicenseLegalApplicationReportDTO =
        response.getBody(ApiLicenseLegalApplicationReportDTO.class);
    assertThat(apiLicenseLegalApplicationReportDTO).isNotNull();
    assertThat(apiLicenseLegalApplicationReportDTO.components).hasSize(14);
    assertThat(apiLicenseLegalApplicationReportDTO.licenseLegalMetadata).hasSize(8);
  }

  @Test
  void testGetLicenseLegalApplicationReport_NoStage() throws Exception {
    List<Application> applications = prepareApplications(1);
    List<PolicyEvaluation> evaluations = prepareEvaluations(applications, BuildStageType.ID);
    mockReport(evaluations.get(0), getClass().getSimpleName());
    mockHdsResponse();

    HttpResponse response =
        restRequest().path(ApiLegalReportResourceV2.APPLICATION_PATH_STAGE)
            .parameter(applications.get(0).getId(), ReleaseStageType.ID)
            .get();

    ctx.assertResponseStatus(404, response);
  }

  @Test
  void testGetLicenseLegalApplicationReportWithStage_NotFound() throws Exception {
    String applicationPublicId = "doesNotExist";

    HttpResponse response =
        restRequest().path(ApiLegalReportResourceV2.APPLICATION_PATH_STAGE)
            .parameter(applicationPublicId, ReleaseStageType.ID)
            .get();

    ctx.assertResponseStatus(404, response);
    assertThat(response.getBodyText())
        .isEqualTo("Application with ID " + applicationPublicId + " does not exist.");
  }

  @Test
  void testGetLicenseLegalApplicationHTMLReport() throws Exception {
    List<Application> applications = prepareApplications(1);
    List<PolicyEvaluation> evaluations = prepareEvaluations(applications, BuildStageType.ID);
    mockReport(evaluations.get(0), getClass().getSimpleName());
    mockHdsResponse();

    HttpResponse response =
        restRequest().path(ApiLegalReportResourceV2.APPLICATION_REPORT_PATH)
            .parameter(applications.get(0).getId(), BuildStageType.ID)
            .get();

    ctx.assertResponseStatus(200, response);
    assertThat(response.getBodyText()).contains(applications.get(0).getPublicId());
  }

  @Test
  void testPostLicenseLegalMultiApplicationHTMLReport() throws Exception {
    List<Application> applications = prepareApplications(2);
    List<PolicyEvaluation> evaluations = prepareEvaluations(applications, BuildStageType.ID);
    mockReport(evaluations.get(0), getClass().getSimpleName());
    mockReport(evaluations.get(1), getClass().getSimpleName());
    mockHdsResponse();

    HttpResponse response = restRequest().path(ApiLegalReportResourceV2.MULTI_APPLICATION_REPORT_PATH)
        .part("applications", applications.get(0).getPublicId() + "," + applications.get(1).getPublicId())
        .part("stages", BuildStageType.ID + "," + BuildStageType.ID)
        .post();

    ctx.assertResponseStatus(200, response);
    assertThat(response.getBodyText()).contains(applications.get(0).getPublicId());
    assertThat(response.getBodyText()).contains(applications.get(1).getPublicId());
  }

  @Test
  void testGetLicenseLegalMultiApplicationHTMLWithoutReport() throws Exception {
    List<Application> applications = prepareApplications(2);
    mockHdsResponse();

    HttpResponse response = restRequest().path(ApiLegalReportResourceV2.MULTI_APPLICATION_REPORT_PATH)
        .part("applications", applications.get(0).getPublicId() + "," + applications.get(1).getPublicId())
        .part("stages", BuildStageType.ID + "," + BuildStageType.ID)
        .post();

    ctx.assertResponseStatus(200, response);
    assertThat(response.getBodyText())
        .contains("<table id=\"table-of-contents\">")
        .doesNotContain("<div class=\"componentBox\">");
  }

  @Test
  void testPostLicenseLegalMultiApplicationHTMLReport_contentLengthTooLarge() throws Exception {
    List<Application> applications = prepareApplications(2);
    List<PolicyEvaluation> evaluations = prepareEvaluations(applications, BuildStageType.ID);
    mockReport(evaluations.get(0), getClass().getSimpleName());
    mockReport(evaluations.get(1), getClass().getSimpleName());
    mockHdsResponse();

    HttpResponse response =
        restRequest().path(ApiLegalReportResourceV2.MULTI_APPLICATION_REPORT_PATH)
            .header("Content-Length", String.valueOf(MAX_REQUEST_SIZE + 1))
            .part("applications", applications.get(0).getPublicId() + "," + applications.get(1).getPublicId())
            .part("stages", BuildStageType.ID + "," + BuildStageType.ID)
            .post();

    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo(MAX_REQUEST_SIZE_MESSAGE);
  }

  @Test
  void testPostLicenseLegalMultiApplicationHTMLReport_requestTooLarge() throws Exception {
    List<Application> applications = prepareApplications(2);
    List<PolicyEvaluation> evaluations = prepareEvaluations(applications, BuildStageType.ID);
    mockReport(evaluations.get(0), getClass().getSimpleName());
    mockReport(evaluations.get(1), getClass().getSimpleName());
    mockHdsResponse();

    HttpRequest request = restRequest().path(ApiLegalReportResourceV2.MULTI_APPLICATION_REPORT_PATH)
        .header("Content-Length", null)
        .part("applications", applications.get(0).getPublicId() + "," + applications.get(1).getPublicId())
        .part("stages", BuildStageType.ID + "," + BuildStageType.ID);
    addLargeRequestPart(request);

    HttpResponse response = tryRequestPost(request);
    if (response == null) {
      return;
    }

    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo(MAX_REQUEST_SIZE_MESSAGE);
  }

  @Test
  void testPostLicenseLegalMultiApplicationHTMLReport_noticeFileTooLarge() throws Exception {
    List<Application> applications = prepareApplications(2);
    List<PolicyEvaluation> evaluations = prepareEvaluations(applications, BuildStageType.ID);
    mockReport(evaluations.get(0), getClass().getSimpleName());
    mockReport(evaluations.get(1), getClass().getSimpleName());
    mockHdsResponse();
    File noticeFile = createLargeNoticeFile();

    HttpRequest request = restRequest().path(ApiLegalReportResourceV2.MULTI_APPLICATION_REPORT_PATH)
        .header("Content-Length", null)
        .part("applications", applications.get(0).getPublicId() + "," + applications.get(1).getPublicId())
        .part("stages", BuildStageType.ID + "," + BuildStageType.ID)
        .part("noticeFiles", noticeFile);

    HttpResponse response = tryRequestPost(request);
    if (response == null) {
      return;
    }

    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo(MAX_REQUEST_SIZE_MESSAGE);
  }

  @Test
  void testPostLicenseLegalCustomMultiApplicationHTMLReport() throws Exception {
    List<Application> applications = prepareApplications(2);
    List<PolicyEvaluation> evaluations = prepareEvaluations(applications, BuildStageType.ID);
    mockReport(evaluations.get(0), getClass().getSimpleName());
    mockReport(evaluations.get(1), getClass().getSimpleName());
    mockHdsResponse();
    File noticeFile = createNoticeFile();

    HttpResponse response = restRequest().path(ApiLegalReportResourceV2.CUSTOM_MULTI_APPLICATION_REPORT_PATH)
        .part("applications", applications.get(0).getPublicId() + "," + applications.get(1).getPublicId())
        .part("stages", BuildStageType.ID + "," + BuildStageType.ID)
        .part("title", "Report title")
        .part("header", "Report header")
        .part("footer", "Report footer")
        .part("noticeFiles", noticeFile)
        .post();

    ctx.assertResponseStatus(200, response);
    final String bodyText = response.getBodyText();

    assertThat(bodyText)
        .contains("notice file content")
        .contains("Report title")
        .contains("Report header")
        .contains("Report footer");
  }

  @Test
  void testPostLicenseLegalCustomMultiApplicationHTMLReport_contentLengthTooLarge() throws Exception {
    List<Application> applications = prepareApplications(2);
    List<PolicyEvaluation> evaluations = prepareEvaluations(applications, BuildStageType.ID);
    mockReport(evaluations.get(0), getClass().getSimpleName());
    mockReport(evaluations.get(1), getClass().getSimpleName());
    mockHdsResponse();

    HttpResponse response =
        restRequest().path(ApiLegalReportResourceV2.CUSTOM_MULTI_APPLICATION_REPORT_PATH)
            .header("Content-Length", String.valueOf(MAX_REQUEST_SIZE + 1))
            .part("applications", applications.get(0).getPublicId() + "," + applications.get(1).getPublicId())
            .post();

    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo(MAX_REQUEST_SIZE_MESSAGE);
  }

  @Test
  void testPostLicenseLegalCustomMultiApplicationHTMLReport_requestTooLarge() throws Exception {
    List<Application> applications = prepareApplications(2);
    List<PolicyEvaluation> evaluations = prepareEvaluations(applications, BuildStageType.ID);
    mockReport(evaluations.get(0), getClass().getSimpleName());
    mockReport(evaluations.get(1), getClass().getSimpleName());
    mockHdsResponse();

    HttpRequest request = restRequest().path(ApiLegalReportResourceV2.CUSTOM_MULTI_APPLICATION_REPORT_PATH)
        .header("Content-Length", null)
        .part("applications", applications.get(0).getPublicId() + "," + applications.get(1).getPublicId());
    addLargeRequestPart(request);

    HttpResponse response = tryRequestPost(request);
    if (response == null) {
      return;
    }

    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo(MAX_REQUEST_SIZE_MESSAGE);
  }

  @Test
  void testPostLicenseLegalCustomMultiApplicationHTMLReport_noticeFileTooLarge() throws Exception {
    List<Application> applications = prepareApplications(2);
    List<PolicyEvaluation> evaluations = prepareEvaluations(applications, BuildStageType.ID);
    mockReport(evaluations.get(0), getClass().getSimpleName());
    mockReport(evaluations.get(1), getClass().getSimpleName());
    mockHdsResponse();

    File noticeFile = createLargeNoticeFile();

    HttpRequest request =
        restRequest().path(ApiLegalReportResourceV2.CUSTOM_MULTI_APPLICATION_REPORT_PATH)
            .header("Content-Length", null)
            .part("applications", applications.get(0).getPublicId() + "," + applications.get(1).getPublicId())
            .part("noticeFiles", noticeFile);

    HttpResponse response = tryRequestPost(request);
    if (response == null) {
      return;
    }

    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo(MAX_REQUEST_SIZE_MESSAGE);
  }

  @Test
  void testPostLicenseLegalApplicationHTMLReport_withNoticeFile() throws Exception {
    List<Application> applications = prepareApplications(1);
    List<PolicyEvaluation> evaluations = prepareEvaluations(applications, BuildStageType.ID);
    mockReport(evaluations.get(0), getClass().getSimpleName());
    mockHdsResponse();
    File noticeFile = createNoticeFile();

    HttpResponse response =
        restRequest().path(ApiLegalReportResourceV2.APPLICATION_REPORT_PATH)
            .parameter(applications.get(0).getId(), BuildStageType.ID)
            .part("title", "Report title")
            .part("header", "Report header")
            .part("footer", "Report footer")
            .part("noticeFiles", noticeFile)
            .post();

    ctx.assertResponseStatus(200, response);
    final String bodyText = response.getBodyText();

    assertThat(bodyText)
        .contains("notice file content")
        .contains("Report title")
        .contains("Report header")
        .contains("Report footer");
  }

  @Test
  void testPostLicenseLegalApplicationHTMLReport_withNonTextNoticeFile() throws Exception {
    List<Application> applications = prepareApplications(1);
    List<PolicyEvaluation> evaluations = prepareEvaluations(applications, BuildStageType.ID);
    mockReport(evaluations.get(0), getClass().getSimpleName());
    mockHdsResponse();
    File noticeFile = createNoticeFile();

    HttpResponse response =
        restRequest().path(ApiLegalReportResourceV2.APPLICATION_REPORT_PATH)
            .parameter(applications.get(0).getId(), BuildStageType.ID)
            .part("title", "Report title")
            .part("header", "Report header")
            .part("footer", "Report footer")
            .part("noticeFiles", "notice.png", noticeFile, "image/png")
            .part("noticeFiles", "notice2.png", noticeFile, "image/png")
            .post();

    ctx.assertResponseStatus(400, response);
    final String bodyText = response.getBodyText();

    assertThat(bodyText).contains("Following notice files must be plain text files: notice.png, notice2.png");
  }

  @Test
  void testPostLicenseLegalApplicationHTMLReport_contentLengthTooLarge() throws Exception {
    List<Application> applications = prepareApplications(1);
    List<PolicyEvaluation> evaluations = prepareEvaluations(applications, BuildStageType.ID);
    mockReport(evaluations.get(0), getClass().getSimpleName());
    mockHdsResponse();

    HttpResponse response =
        restRequest().path(ApiLegalReportResourceV2.APPLICATION_REPORT_PATH)
            .header("Content-Length", String.valueOf(MAX_REQUEST_SIZE + 1))
            .parameter(applications.get(0).getId(), BuildStageType.ID)
            .part("title", "Report title")
            .post();

    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo(MAX_REQUEST_SIZE_MESSAGE);
  }

  @Test
  void testPostLicenseLegalApplicationHTMLReport_requestTooLarge() throws Exception {
    List<Application> applications = prepareApplications(1);
    List<PolicyEvaluation> evaluations = prepareEvaluations(applications, BuildStageType.ID);
    mockReport(evaluations.get(0), getClass().getSimpleName());
    mockHdsResponse();

    HttpRequest request = restRequest().path(ApiLegalReportResourceV2.APPLICATION_REPORT_PATH)
        .header("Content-Length", null)
        .parameter(applications.get(0).getId(), BuildStageType.ID);
    request.part("title", "Report title");
    addLargeRequestPart(request);

    HttpResponse response = tryRequestPost(request);
    if (response == null) {
      return;
    }

    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo(MAX_REQUEST_SIZE_MESSAGE);
  }

  @Test
  void testPostLicenseLegalApplicationHTMLReport_noticeFileTooLarge() throws Exception {
    List<Application> applications = prepareApplications(1);
    List<PolicyEvaluation> evaluations = prepareEvaluations(applications, BuildStageType.ID);
    mockReport(evaluations.get(0), getClass().getSimpleName());
    mockHdsResponse();
    File noticeFile = createLargeNoticeFile();

    HttpRequest request =
        restRequest().path(ApiLegalReportResourceV2.APPLICATION_REPORT_PATH)
            .header("Content-Length", null)
            .parameter(applications.get(0).getId(), BuildStageType.ID)
            .part("title", "Report title")
            .part("noticeFiles", noticeFile);

    HttpResponse response = tryRequestPost(request);
    if (response == null) {
      return;
    }

    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo(MAX_REQUEST_SIZE_MESSAGE);
  }

  @Test
  void testGetLicenseLegalApplicationHTMLReport_NoStage() throws Exception {
    List<Application> applications = prepareApplications(1);
    List<PolicyEvaluation> evaluations = prepareEvaluations(applications, BuildStageType.ID);
    mockReport(evaluations.get(0), getClass().getSimpleName());
    mockHdsResponse();

    HttpResponse response =
        restRequest().path(ApiLegalReportResourceV2.APPLICATION_REPORT_PATH)
            .parameter(applications.get(0).getId(), ReleaseStageType.ID)
            .get();

    ctx.assertResponseStatus(404, response);
  }

  @Test
  void testGetLicenseLegalApplicationReport_NotFound() throws Exception {
    String applicationPublicId = "doesNotExist";

    HttpResponse response =
        restRequest().path(ApiLegalReportResourceV2.APPLICATION_PATH)
            .parameter(applicationPublicId)
            .get();

    ctx.assertResponseStatus(404, response);
    assertThat(response.getBodyText())
        .isEqualTo("Application with ID " + applicationPublicId + " does not exist.");
  }

  @Test
  void testGetLicenseLegalComponentReport_ComponentIdentifier() throws Exception {
    Owner owner = ctx.tempEntity().newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    mockHdsResponse();

    HttpResponse response = restRequest().path(ApiLegalReportResourceV2.COMPONENT_PATH)
        .parameter(owner.getType().toString(), owner.getPublicId())
        .query("componentIdentifier", componentIdentifier)
        .get();

    ctx.assertResponseStatus(200, response);
    ApiLicenseLegalComponentReportDTO apiLicenseLegalComponentDTO =
        response.getBody(ApiLicenseLegalComponentReportDTO.class);
    assertThat(apiLicenseLegalComponentDTO).isNotNull();
    assertThat(apiLicenseLegalComponentDTO.component.componentIdentifier.toComponentIdentifier())
        .isEqualTo(componentIdentifier);
  }

  @Test
  void testGetLicenseLegalComponentReport_PackageUrl() throws Exception {
    Owner owner = ctx.tempEntity().newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    mockHdsResponse();

    HttpResponse response = restRequest().path(ApiLegalReportResourceV2.COMPONENT_PATH)
        .parameter(owner.getType().toString(), owner.getPublicId())
        .query("packageUrl", PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier).getPackageUrl())
        .get();

    ctx.assertResponseStatus(200, response);
    ApiLicenseLegalComponentReportDTO apiLicenseLegalComponentDTO =
        response.getBody(ApiLicenseLegalComponentReportDTO.class);
    assertThat(apiLicenseLegalComponentDTO).isNotNull();
    assertThat(apiLicenseLegalComponentDTO.component.componentIdentifier.toComponentIdentifier())
        .isEqualTo(componentIdentifier);
  }

  @Test
  void testGetLicenseLegalComponentReport_Hash() throws Exception {
    Owner owner = ctx.tempEntity().newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    String hash = "hash";
    ctx.tempEntity().newApplicationComponent(owner.getId(), BuildStageType.ID, hash, componentIdentifier);
    mockHdsResponse();

    HttpResponse response = restRequest().path(ApiLegalReportResourceV2.COMPONENT_PATH)
        .parameter(owner.getType().toString(), owner.getPublicId())
        .query("hash", hash)
        .get();

    ctx.assertResponseStatus(200, response);
    ApiLicenseLegalComponentReportDTO apiLicenseLegalComponentDTO =
        response.getBody(ApiLicenseLegalComponentReportDTO.class);
    assertThat(apiLicenseLegalComponentDTO).isNotNull();
    assertThat(apiLicenseLegalComponentDTO.component.componentIdentifier.toComponentIdentifier())
        .isEqualTo(componentIdentifier);
  }

  @Test
  void testGetLicenseLegalComponentReport_ThirdParty() throws Exception {
    Owner owner = ctx.tempEntity().newApplicationWithParent();
    Map<String, String> coordinates = new HashMap<>();
    coordinates.put("name", "glibc");
    coordinates.put(ComponentIdentifier.VERSION, "2.24-11+deb9u3");
    ComponentIdentifier componentIdentifier = new ComponentIdentifier("debian-9", coordinates);
    mockHdsResponse();
    String scanId = "scanId";
    mockThirdPartyReport(owner.getId(), scanId);

    HttpResponse response = restRequest().path(ApiLegalReportResourceV2.COMPONENT_PATH)
        .parameter(owner.getType().toString(), owner.getPublicId())
        .query("componentIdentifier", componentIdentifier)
        .query("identificationSource", IdentificationSource.CLAIR.getId())
        .query("scanId", scanId)
        .get();

    ctx.assertResponseStatus(200, response);
    ApiLicenseLegalComponentReportDTO apiLicenseLegalComponentDTO =
        response.getBody(ApiLicenseLegalComponentReportDTO.class);
    assertThat(apiLicenseLegalComponentDTO).isNotNull();
    assertThat(apiLicenseLegalComponentDTO.component.componentIdentifier.toComponentIdentifier())
        .isEqualTo(componentIdentifier);
  }

  @Test
  void testPostCustomLicenseLegalApplicationReport_NoTitle() throws Exception {
    List<Application> applications = prepareApplications(1);

    HttpResponse response =
        restRequest().path(ApiLegalReportResourceV2.APPLICATION_REPORT_PATH)
            .parameter(applications.get(0).getId(), BuildStageType.ID)
            .part(REPORT_FORM_HEADER, "header")
            .part(REPORT_FORM_FOOTER, "footer")
            .post();

    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Missing required parameter: %s", REPORT_FORM_TITLE);
  }

  @Test
  void testPostCustomLicenseLegalApplicationReport_FromTemplate() throws Exception {
    List<Application> applications = prepareApplications(1);
    List<PolicyEvaluation> evaluations = prepareEvaluations(applications, BuildStageType.ID);
    mockReport(evaluations.get(0), getClass().getSimpleName());
    mockHdsResponse();
    AttributionReportTemplate template = prepareAttributionReportTemplate();

    HttpResponse response =
        restRequest().path(ApiLegalReportResourceV2.APPLICATION_REPORT_FROM_TEMPLATE_PATH)
            .parameter(applications.get(0).getId(), BuildStageType.ID, template.getId())
            .post();

    ctx.assertResponseStatus(200, response);
    assertThat(response.getBodyText()).contains("testPostCustomLicenseLegalApplicationReport_FromTemplateTITLE");
    assertThat(response.getBodyText()).contains("testPostCustomLicenseLegalApplicationReport_FromTemplateHEADER");
    assertThat(response.getBodyText()).contains("testPostCustomLicenseLegalApplicationReport_FromTemplateFOOTER");
  }

  @Test
  void testPostCustomLicenseLegalApplicationReport_FromTemplate_withSonatypeSpecialLicenses() throws Exception {
    List<Application> applications = prepareApplications(1);
    List<PolicyEvaluation> evaluations = prepareEvaluations(applications, BuildStageType.ID);
    mockReport(evaluations.get(0), getClass().getSimpleName());
    mockHdsResponse();
    AttributionReportTemplate template = prepareAttributionReportTemplate();

    HttpResponse response =
        restRequest().path(ApiLegalReportResourceV2.APPLICATION_REPORT_FROM_TEMPLATE_PATH)
            .parameter(applications.get(0).getId(), BuildStageType.ID, template.getId())
            .post();

    ctx.assertResponseStatus(200, response);
    assertThat(response.getBodyText()).contains("testPostCustomLicenseLegalApplicationReport_FromTemplateTITLE");
    assertThat(response.getBodyText()).contains("testPostCustomLicenseLegalApplicationReport_FromTemplateHEADER");
    assertThat(response.getBodyText()).contains("testPostCustomLicenseLegalApplicationReport_FromTemplateFOOTER");
  }

  @Test
  void testPostCustomLicenseLegalMultiApplicationReport_FromTemplate() throws Exception {
    List<Application> applications = prepareApplications(2);
    List<PolicyEvaluation> evaluations = prepareEvaluations(applications, BuildStageType.ID);
    mockReport(evaluations.get(0), getClass().getSimpleName());
    mockReport(evaluations.get(1), getClass().getSimpleName());
    mockHdsResponse();
    AttributionReportTemplate template = prepareAttributionReportTemplate();
    File file = createNoticeFile();

    HttpResponse response = restRequest()
        .path(ApiLegalReportResourceV2.MULTI_APPLICATION_REPORT_PATH_FROM_TEMPLATE_PATH)
        .parameter(template.getId())
        .part("noticeFiles", file)
        .part("applications", applications.get(0).getPublicId() + "," + applications.get(1).getPublicId())
        .part("stages", BuildStageType.ID + "," + BuildStageType.ID)
        .post();

    ctx.assertResponseStatus(200, response);
    assertThat(response.getBodyText()).contains("testPostCustomLicenseLegalApplicationReport_FromTemplateTITLE");
    assertThat(response.getBodyText()).contains("testPostCustomLicenseLegalApplicationReport_FromTemplateHEADER");
    assertThat(response.getBodyText()).contains("testPostCustomLicenseLegalApplicationReport_FromTemplateFOOTER");
  }

  @Test
  void testPostCustomLicenseLegalMultiApplicationReport_FromTemplate_contentLengthTooLarge() throws Exception {
    List<Application> applications = prepareApplications(2);
    List<PolicyEvaluation> evaluations = prepareEvaluations(applications, BuildStageType.ID);
    mockReport(evaluations.get(0), getClass().getSimpleName());
    mockReport(evaluations.get(1), getClass().getSimpleName());
    mockHdsResponse();
    AttributionReportTemplate template = prepareAttributionReportTemplate();

    HttpResponse response = restRequest()
        .path(ApiLegalReportResourceV2.MULTI_APPLICATION_REPORT_PATH_FROM_TEMPLATE_PATH)
        .header("Content-Length", String.valueOf(MAX_REQUEST_SIZE + 1))
        .parameter(template.getId())
        .part("applications", applications.get(0).getPublicId() + "," + applications.get(1).getPublicId())
        .post();

    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo(MAX_REQUEST_SIZE_MESSAGE);
  }

  @Test
  void testPostCustomLicenseLegalMultiApplicationReport_FromTemplate_requestTooLarge() throws Exception {
    List<Application> applications = prepareApplications(2);
    List<PolicyEvaluation> evaluations = prepareEvaluations(applications, BuildStageType.ID);
    mockReport(evaluations.get(0), getClass().getSimpleName());
    mockReport(evaluations.get(1), getClass().getSimpleName());
    mockHdsResponse();
    AttributionReportTemplate template = prepareAttributionReportTemplate();

    HttpRequest request = restRequest().path(ApiLegalReportResourceV2.MULTI_APPLICATION_REPORT_PATH_FROM_TEMPLATE_PATH)
        .header("Content-Length", null)
        .parameter(template.getId())
        .part("applications", applications.get(0).getPublicId() + "," + applications.get(1).getPublicId());
    addLargeRequestPart(request);

    HttpResponse response = tryRequestPost(request);
    if (response == null) {
      return;
    }

    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo(MAX_REQUEST_SIZE_MESSAGE);
  }

  @Test
  void testPostCustomLicenseLegalMultiApplicationReport_FromTemplate_noticeFileTooLarge() throws Exception {
    List<Application> applications = prepareApplications(2);
    List<PolicyEvaluation> evaluations = prepareEvaluations(applications, BuildStageType.ID);
    mockReport(evaluations.get(0), getClass().getSimpleName());
    mockReport(evaluations.get(1), getClass().getSimpleName());
    mockHdsResponse();
    AttributionReportTemplate template = prepareAttributionReportTemplate();
    File noticeFile = createLargeNoticeFile();

    HttpRequest request = restRequest()
        .header("Content-Length", null)
        .path(ApiLegalReportResourceV2.MULTI_APPLICATION_REPORT_PATH_FROM_TEMPLATE_PATH)
        .parameter(template.getId())
        .part("applications", applications.get(0).getPublicId() + "," + applications.get(1).getPublicId())
        .part("noticeFiles", noticeFile);

    HttpResponse response = tryRequestPost(request);
    if (response == null) {
      return;
    }

    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo(MAX_REQUEST_SIZE_MESSAGE);
  }

  @Test
  void testPostLicenseLegalMultiApplicationReportFromActiveUserFilter_FromTemplate() throws Exception {
    List<Application> applications = prepareApplications(2);
    List<PolicyEvaluation> evaluations = prepareEvaluations(applications, BuildStageType.ID);
    mockReport(evaluations.get(0), getClass().getSimpleName());
    mockReport(evaluations.get(1), getClass().getSimpleName());
    mockHdsResponse();
    AttributionReportTemplate template = prepareAttributionReportTemplate();

    HttpResponse response = restRequest()
        .path(ApiLegalReportResourceV2.MULTI_APPLICATION_REPORT_FROM_FILTER_TEMPLATE_PATH)
        .parameter(template.getId())
        .post();

    ctx.assertResponseStatus(200, response);
    assertThat(response.getBodyText()).contains("testPostCustomLicenseLegalApplicationReport_FromTemplateTITLE");
    assertThat(response.getBodyText()).contains("testPostCustomLicenseLegalApplicationReport_FromTemplateHEADER");
    assertThat(response.getBodyText()).contains("testPostCustomLicenseLegalApplicationReport_FromTemplateFOOTER");
  }

  @Test
  void testPostLicenseLegalMultiApplicationReportFromActiveUserFilter_FromTemplate_contentLengthTooLarge() throws Exception {
    List<Application> applications = prepareApplications(2);
    List<PolicyEvaluation> evaluations = prepareEvaluations(applications, BuildStageType.ID);
    mockReport(evaluations.get(0), getClass().getSimpleName());
    mockReport(evaluations.get(1), getClass().getSimpleName());
    mockHdsResponse();
    AttributionReportTemplate template = prepareAttributionReportTemplate();

    HttpResponse response = restRequest()
        .path(ApiLegalReportResourceV2.MULTI_APPLICATION_REPORT_FROM_FILTER_TEMPLATE_PATH)
        .header("Content-Length", String.valueOf(MAX_REQUEST_SIZE + 1))
        .parameter(template.getId())
        .part("title", "Report title")
        .post();

    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo(MAX_REQUEST_SIZE_MESSAGE);
  }

  @Test
  void testPostLicenseLegalMultiApplicationReportFromActiveUserFilter_FromTemplate_requestTooLarge() throws Exception {
    List<Application> applications = prepareApplications(2);
    List<PolicyEvaluation> evaluations = prepareEvaluations(applications, BuildStageType.ID);
    mockReport(evaluations.get(0), getClass().getSimpleName());
    mockReport(evaluations.get(1), getClass().getSimpleName());
    mockHdsResponse();

    AttributionReportTemplate template = prepareAttributionReportTemplate();

    HttpRequest request = restRequest()
        .header("Content-Length", null)
        .path(ApiLegalReportResourceV2.MULTI_APPLICATION_REPORT_FROM_FILTER_TEMPLATE_PATH)
        .parameter(template.getId())
        .part("title", "Report title");
    addLargeRequestPart(request);

    HttpResponse response = tryRequestPost(request);
    if (response == null) {
      return;
    }

    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo(MAX_REQUEST_SIZE_MESSAGE);
  }

  @Test
  void testPostLicenseLegalMultiApplicationReportFromActiveUserFilter_FromTemplate_noticeFileTooLarge() throws Exception {
    List<Application> applications = prepareApplications(2);
    List<PolicyEvaluation> evaluations = prepareEvaluations(applications, BuildStageType.ID);
    mockReport(evaluations.get(0), getClass().getSimpleName());
    mockReport(evaluations.get(1), getClass().getSimpleName());
    mockHdsResponse();

    AttributionReportTemplate template = prepareAttributionReportTemplate();

    File noticeFile = createLargeNoticeFile();

    HttpRequest request = restRequest()
        .header("Content-Length", null)
        .path(ApiLegalReportResourceV2.MULTI_APPLICATION_REPORT_FROM_FILTER_TEMPLATE_PATH)
        .parameter(template.getId())
        .part("title", "Report title")
        .part("noticeFiles", noticeFile);

    HttpResponse response = tryRequestPost(request);
    if (response == null) {
      return;
    }

    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo(MAX_REQUEST_SIZE_MESSAGE);
  }

  @Test
  void testPostCustomLicenseLegalApplicationReport_FromTemplate_withNoticeFiles() throws Exception {
    List<Application> applications = prepareApplications(1);
    List<PolicyEvaluation> evaluations = prepareEvaluations(applications, BuildStageType.ID);
    mockReport(evaluations.get(0), getClass().getSimpleName());
    mockHdsResponse();
    AttributionReportTemplate template = prepareAttributionReportTemplate();
    File file = createNoticeFile();

    HttpResponse response =
        restRequest().path(ApiLegalReportResourceV2.APPLICATION_REPORT_FROM_TEMPLATE_PATH)
            .parameter(applications.get(0).getId(), BuildStageType.ID, template.getId())
            .part("noticeFiles", file)
            .post();

    ctx.assertResponseStatus(200, response);
    assertThat(response.getBodyText()).contains("notice file content");
  }

  @Test
  void testPostCustomLicenseLegalApplicationReport_FromTemplate_withNonTextNoticeFiles() throws Exception {
    List<Application> applications = prepareApplications(1);
    List<PolicyEvaluation> evaluations = prepareEvaluations(applications, BuildStageType.ID);
    mockReport(evaluations.get(0), getClass().getSimpleName());
    mockHdsResponse();
    AttributionReportTemplate template = prepareAttributionReportTemplate();
    File file = createNoticeFile();

    HttpResponse response =
        restRequest().path(ApiLegalReportResourceV2.APPLICATION_REPORT_FROM_TEMPLATE_PATH)
            .parameter(applications.get(0).getId(), BuildStageType.ID, template.getId())
            .part("noticeFiles", "notice.txt", file, "image/png")
            .post();

    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).contains("Following notice files must be plain text files: notice.txt");
  }

  @Test
  void testPostCustomLicenseLegalApplicationReport_FromTemplate_NoSuchTemplate() throws Exception {
    List<Application> applications = prepareApplications(1);
    List<PolicyEvaluation> evaluations = prepareEvaluations(applications, BuildStageType.ID);
    mockReport(evaluations.get(0), getClass().getSimpleName());
    mockHdsResponse();

    HttpResponse response =
        restRequest().path(ApiLegalReportResourceV2.APPLICATION_REPORT_FROM_TEMPLATE_PATH)
            .parameter(applications.get(0), BuildStageType.ID, "INVALID")
            .post();

    ctx.assertResponseStatus(404, response);
  }

  @Test
  void testPostCustomLicenseLegalApplicationReport_FromTemplate_contentLengthTooLarge() throws Exception {
    List<Application> applications = prepareApplications(1);
    List<PolicyEvaluation> evaluations = prepareEvaluations(applications, BuildStageType.ID);
    mockReport(evaluations.get(0), getClass().getSimpleName());
    mockHdsResponse();
    AttributionReportTemplate template = prepareAttributionReportTemplate();
    File file = createNoticeFile();

    HttpResponse response =
        restRequest().path(ApiLegalReportResourceV2.APPLICATION_REPORT_FROM_TEMPLATE_PATH)
            .header("Content-Length", String.valueOf(MAX_REQUEST_SIZE + 1))
            .parameter(applications.get(0).getId(), BuildStageType.ID, template.getId())
            .part("noticeFiles", "notice.txt", file)
            .post();

    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo(MAX_REQUEST_SIZE_MESSAGE);
  }

  @Test
  void testPostCustomLicenseLegalApplicationReport_FromTemplate_requestTooLarge() throws Exception {
    List<Application> applications = prepareApplications(1);
    List<PolicyEvaluation> evaluations = prepareEvaluations(applications, BuildStageType.ID);
    mockReport(evaluations.get(0), getClass().getSimpleName());
    mockHdsResponse();
    AttributionReportTemplate template = prepareAttributionReportTemplate();
    File file = createNoticeFile();

    HttpRequest request = restRequest().path(ApiLegalReportResourceV2.APPLICATION_REPORT_FROM_TEMPLATE_PATH)
        .header("Content-Length", null)
        .parameter(applications.get(0).getId(), BuildStageType.ID, template.getId())
        .part("noticeFiles", "notice.txt", file);
    addLargeRequestPart(request);

    HttpResponse response = tryRequestPost(request);
    if (response == null) {
      // ChannelOutputShutdownException can surface because the request size validation logic can throw a
      // bad request exception even when the client is not done writing a large request - test passes
      return;
    }

    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo(MAX_REQUEST_SIZE_MESSAGE);
  }

  @Test
  void testPostCustomLicenseLegalApplicationReport_FromTemplate_noticeFileTooLarge() throws Exception {
    List<Application> applications = prepareApplications(1);
    List<PolicyEvaluation> evaluations = prepareEvaluations(applications, BuildStageType.ID);
    mockReport(evaluations.get(0), getClass().getSimpleName());
    mockHdsResponse();
    AttributionReportTemplate template = prepareAttributionReportTemplate();
    File file = createLargeNoticeFile();

    HttpRequest request =
        restRequest().path(ApiLegalReportResourceV2.APPLICATION_REPORT_FROM_TEMPLATE_PATH)
            .header("Content-Length", null)
            .parameter(applications.get(0).getId(), BuildStageType.ID, template.getId())
            .part("noticeFiles", "notice.txt", file);

    HttpResponse response = tryRequestPost(request);
    if (response == null) {
      return;
    }

    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo(MAX_REQUEST_SIZE_MESSAGE);
  }

  private List<Application> prepareApplications(int numberOfApplications) {
    List<Application> applications = new ArrayList<>();
    for (int i = 0; i < numberOfApplications; i++) {
      Application application = ctx.tempEntity().newApplicationWithParent();
      applications.add(application);
    }
    return applications;
  }

  private List<PolicyEvaluation> prepareEvaluations(List<Application> applications, String forStage) {
    List<PolicyEvaluation> evaluations = new ArrayList<>();
    for (Application application : applications) {
      PolicyEvaluation policyEvaluation =
          ctx.tempEntity().newPolicyEvaluation(application.getId(), forStage, TemporaryEntity.uuid());
      evaluations.add(policyEvaluation);
    }
    return evaluations;
  }

  private AttributionReportTemplate prepareAttributionReportTemplate() {
    return ctx.tempEntity()
        .createNewAttributionReportTemplate("Template Name",
            "testPostCustomLicenseLegalApplicationReport_FromTemplateTITLE",
            "testPostCustomLicenseLegalApplicationReport_FromTemplateHEADER",
            "testPostCustomLicenseLegalApplicationReport_FromTemplateFOOTER", false,
            false, false, false, false);
  }

  private void mockThirdPartyReport(String applicationId, String scanId) {
    mockReport(applicationId, scanId, getClass().getSimpleName());
  }

  // Base-class helper not on IqTestContext: AbstractBaseIntegrationTest.mockReport(PolicyEvaluation, String)
  // resolves classpath fixtures directly into InsightWork's report dir (not the zipped-HDS-response flavor that
  // IqTestContext.mockReport(scanId, resourceName) provides), so it is inlined here.
  private void mockReport(PolicyEvaluation evaluation, String classSimpleName) {
    mockReport(evaluation.getOwnerId(), evaluation.getScanId(), classSimpleName);
  }

  private void mockReport(String applicationId, String scanId, String classSimpleName) {
    try {
      ReportHelper.saveMockReport(
          ctx.insightWork(), ctx.tempFolder(), "/" + classSimpleName + "/report/", applicationId, scanId);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void mockHdsResponse() {
    ctx.hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.METADATA_URL);
    ctx.hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.LEGAL_COMMENT_URL);
    ctx.hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.LEGAL_FILE_URL);
    ctx.hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.SOURCE_LINK_URL);
  }

  private File createNoticeFile() throws IOException {
    File file = ctx.tempFolder().newFile();
    FileUtils.writeStringToFile(file, "notice file content", StandardCharsets.UTF_8);
    return file;
  }

  private File createLargeNoticeFile() throws IOException {
    String largeContent = StringUtils.repeat('a', (int) MAX_REQUEST_SIZE + 1);
    File file = ctx.tempFolder().newFile();
    FileUtils.writeStringToFile(file, largeContent, StandardCharsets.UTF_8);
    return file;
  }

  // To emulate a large request based on the MAX_REQUEST_PART_SIZE, we add a high number of parts to the request
  private void addLargeRequestPart(HttpRequest request) {
    int partBytes = 102400; // 100 kb each part added
    for (int i = 0; i < (MAX_REQUEST_SIZE / partBytes) + 1; i++) {
      request.part(String.valueOf(i), StringUtils.repeat('a', partBytes));
    }
  }

  private HttpResponse tryRequestPost(HttpRequest request) throws Exception {
    HttpResponse response = null;
    try {
      response = request.post();
    }
    catch (ExecutionException e) {
      if (!(e.getCause() instanceof ChannelOutputShutdownException)) {
        throw e;
      }
      // ChannelOutputShutdownException can surface because the request size validation logic can throw a
      // bad request exception even when the client is not done writing a large request
      System.out.println(
          "ChannelOutputShutdownException can surface because the request size validation logic can throw a" +
              " bad request exception even when the client is not done writing a large request " + e.getMessage());
    }
    return response;
  }
}
