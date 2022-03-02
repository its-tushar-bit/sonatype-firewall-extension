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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.experimental.legal.ApiLicenseLegalHdsService;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalApplicationReportDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalComponentReportDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.legal.AttributionReportTemplate;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.thirdparty.ThirdPartyComponentDAO;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import static com.sonatype.insight.brain.api.v2.DefaultApiLegalReportResourceV2.REPORT_FORM_FOOTER;
import static com.sonatype.insight.brain.api.v2.DefaultApiLegalReportResourceV2.REPORT_FORM_HEADER;
import static com.sonatype.insight.brain.api.v2.DefaultApiLegalReportResourceV2.REPORT_FORM_TITLE;
import static org.assertj.core.api.Assertions.assertThat;

public class ApiLegalReportResourceV2Test
    extends AbstractResourceTest
{
  private static final String EMPTY_JSON_ARRAY = "[]";

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.LICENSE_LEGAL_RESOURCE_PATH_V2);
  }

  @Test
  public void testGetDefaultLicenseLegalApplicationReport() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, tempEntity.uuid());
    mockReport(policyEvaluation, getClass().getSimpleName());
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.METADATA_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.LEGAL_COMMENT_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.LEGAL_FILE_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.SOURCE_LINK_URL);

    HttpResponse response =
        restRequest().path(DefaultApiLegalReportResourceV2.APPLICATION_PATH)
            .parameter(application.getId())
            .get();

    assertResponseStatus(200, response);
    ApiLicenseLegalApplicationReportDTO
        apiLicenseLegalApplicationReportDTO = response.getBody(ApiLicenseLegalApplicationReportDTO.class);
    assertThat(apiLicenseLegalApplicationReportDTO).isNotNull();
    assertThat(apiLicenseLegalApplicationReportDTO.components).hasSize(14);
    assertThat(apiLicenseLegalApplicationReportDTO.licenseLegalMetadata).hasSize(8);
  }

  @Test
  public void testGetDefaultLicenseLegalApplicationReportByStage() throws Exception {
    Application application = tempEntity.newApplicationWithParent();

    PolicyEvaluation policyEvaluationBuild =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, tempEntity.uuid());
    mockReport(policyEvaluationBuild, getClass().getSimpleName());

    PolicyEvaluation policyEvaluationRelease =
        tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID, tempEntity.uuid());

    mockReport(policyEvaluationRelease, getClass().getSimpleName());
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.METADATA_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.LEGAL_COMMENT_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.LEGAL_FILE_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.SOURCE_LINK_URL);

    HttpResponse response =
        restRequest().path(DefaultApiLegalReportResourceV2.APPLICATION_PATH_STAGE)
            .parameter(application.getId(), BuildStageType.ID)
            .get();

    assertResponseStatus(200, response);
    ApiLicenseLegalApplicationReportDTO
        apiLicenseLegalApplicationReportDTO = response.getBody(ApiLicenseLegalApplicationReportDTO.class);
    assertThat(apiLicenseLegalApplicationReportDTO).isNotNull();
    assertThat(apiLicenseLegalApplicationReportDTO.components).hasSize(14);
    assertThat(apiLicenseLegalApplicationReportDTO.licenseLegalMetadata).hasSize(8);
  }

  @Test
  public void testGetLicenseLegalApplicationReport_NoStage() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, tempEntity.uuid());
    mockReport(policyEvaluation, getClass().getSimpleName());
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.METADATA_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.LEGAL_COMMENT_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.LEGAL_FILE_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.SOURCE_LINK_URL);

    HttpResponse response =
        restRequest().path(DefaultApiLegalReportResourceV2.APPLICATION_PATH_STAGE)
            .parameter(application.getId(), ReleaseStageType.ID)
            .get();

    assertResponseStatus(404, response);
  }

  @Test
  public void testGetLicenseLegalApplicationReportWithStage_NotFound() throws Exception {
    String applicationPublicId = "doesNotExist";

    HttpResponse response =
        restRequest().path(DefaultApiLegalReportResourceV2.APPLICATION_PATH_STAGE)
            .parameter(applicationPublicId, ReleaseStageType.ID)
            .get();

    assertResponseStatus(404, response);
    assertThat(response.getBodyText())
        .isEqualTo("Could not find an application with ID " + applicationPublicId + ".");
  }

  @Test
  public void testGetLicenseLegalApplicationHTMLReport() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, tempEntity.uuid());
    mockReport(policyEvaluation, getClass().getSimpleName());
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.METADATA_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.LEGAL_COMMENT_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.LEGAL_FILE_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.SOURCE_LINK_URL);

    HttpResponse response =
        restRequest().path(DefaultApiLegalReportResourceV2.APPLICATION_REPORT_PATH)
            .parameter(application.getId(), BuildStageType.ID)
            .get();

    assertResponseStatus(200, response);
    assertThat(response.getBodyText()).contains(application.getPublicId());
  }

  @Test
  public void testGetLicenseLegalMultiApplicationHTMLReport() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    Application application2 = tempEntity.newApplicationWithParent();
    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, tempEntity.uuid());
    PolicyEvaluation policyEvaluation2 =
        tempEntity.newPolicyEvaluation(application2.getId(), BuildStageType.ID, tempEntity.uuid());
    mockReport(policyEvaluation, getClass().getSimpleName());
    mockReport(policyEvaluation2, getClass().getSimpleName());
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.METADATA_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.LEGAL_COMMENT_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.LEGAL_FILE_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.SOURCE_LINK_URL);

    HttpResponse response = restRequest().path(DefaultApiLegalReportResourceV2.MULTI_APPLICATION_REPORT_PATH)
        .part("applications", application.getPublicId() + "," + application2.getPublicId())
        .part("stages", BuildStageType.ID + "," + BuildStageType.ID)
        .post();

    assertResponseStatus(200, response);
    assertThat(response.getBodyText()).contains(application.getPublicId());
    assertThat(response.getBodyText()).contains(application2.getPublicId());
  }
  
  @Test
  public void testGetLicenseLegalMultiApplicationHTMLWithoutReport() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    Application application2 = tempEntity.newApplicationWithParent();
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.METADATA_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.LEGAL_COMMENT_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.LEGAL_FILE_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.SOURCE_LINK_URL);

    HttpResponse response = restRequest().path(DefaultApiLegalReportResourceV2.MULTI_APPLICATION_REPORT_PATH)
        .part("applications", application.getPublicId() + "," + application2.getPublicId())
        .part("stages", BuildStageType.ID + "," + BuildStageType.ID).post();

    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).contains(application.getPublicId());
    assertThat(response.getBodyText()).contains(application2.getPublicId());
  }

  @Test
  public void testGetLicenseLegalCustomMultiApplicationHTMLReport() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    Application application2 = tempEntity.newApplicationWithParent();
    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, tempEntity.uuid());
    PolicyEvaluation policyEvaluation2 =
        tempEntity.newPolicyEvaluation(application2.getId(), BuildStageType.ID, tempEntity.uuid());
    mockReport(policyEvaluation, getClass().getSimpleName());
    mockReport(policyEvaluation2, getClass().getSimpleName());
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.METADATA_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.LEGAL_COMMENT_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.LEGAL_FILE_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.SOURCE_LINK_URL);
    
    File noticeFile = createNoticeFile();
    
    HttpResponse response = restRequest().path(DefaultApiLegalReportResourceV2.CUSTOM_MULTI_APPLICATION_REPORT_PATH)
        .part("applications", application.getPublicId() + "," + application2.getPublicId())
        .part("stages", BuildStageType.ID + "," + BuildStageType.ID)
        .part("title", "Report title")
        .part("header", "Report header")
        .part("footer", "Report footer")
        .part("noticeFiles", noticeFile)
        .post();

    assertResponseStatus(200, response);
    final String bodyText = response.getBodyText();

    assertThat(bodyText)
        .contains("notice file content")
        .contains("Report title")
        .contains("Report header")
        .contains("Report footer");
  }

  @Test
  public void testGetLicenseLegalApplicationHTMLReport_withNoticeFile() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, tempEntity.uuid());
    mockReport(policyEvaluation, getClass().getSimpleName());
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.METADATA_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.LEGAL_COMMENT_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.LEGAL_FILE_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.SOURCE_LINK_URL);

    File noticeFile = createNoticeFile();

    HttpResponse response =
        restRequest().path(DefaultApiLegalReportResourceV2.APPLICATION_REPORT_PATH)
            .parameter(application.getId(), BuildStageType.ID)
            .part("title", "Report title")
            .part("header", "Report header")
            .part("footer", "Report footer")
            .part("noticeFiles", noticeFile)
            .post();

    assertResponseStatus(200, response);
    final String bodyText = response.getBodyText();

    assertThat(bodyText)
        .contains("notice file content")
        .contains("Report title")
        .contains("Report header")
        .contains("Report footer");
  }

  @Test
  public void testGetLicenseLegalApplicationHTMLReport_withNonTextNoticeFile() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, tempEntity.uuid());
    mockReport(policyEvaluation, getClass().getSimpleName());
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.METADATA_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.LEGAL_COMMENT_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.LEGAL_FILE_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.SOURCE_LINK_URL);

    File noticeFile = createNoticeFile();

    HttpResponse response =
        restRequest().path(DefaultApiLegalReportResourceV2.APPLICATION_REPORT_PATH)
            .parameter(application.getId(), BuildStageType.ID)
            .part("title", "Report title")
            .part("header", "Report header")
            .part("footer", "Report footer")
            .part("noticeFiles", "notice.png", noticeFile, "image/png")
            .part("noticeFiles", "notice2.png", noticeFile, "image/png")
            .post();

    assertResponseStatus(400, response);
    final String bodyText = response.getBodyText();

    assertThat(bodyText).contains("Following notice files must be plain text files: notice.png, notice2.png");
  }

  @Test
  public void testGetLicenseLegalApplicationHTMLReport_NoStage() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, tempEntity.uuid());
    mockReport(policyEvaluation, getClass().getSimpleName());
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.METADATA_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.LEGAL_COMMENT_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.LEGAL_FILE_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.SOURCE_LINK_URL);

    HttpResponse response =
        restRequest().path(DefaultApiLegalReportResourceV2.APPLICATION_REPORT_PATH)
            .parameter(application.getId(), ReleaseStageType.ID)
            .get();

    assertResponseStatus(404, response);
  }

  @Test
  public void testGetLicenseLegalApplicationReport_NotFound() throws Exception {
    String applicationPublicId = "doesNotExist";

    HttpResponse response =
        restRequest().path(DefaultApiLegalReportResourceV2.APPLICATION_PATH)
            .parameter(applicationPublicId)
            .get();

    assertResponseStatus(404, response);
    assertThat(response.getBodyText())
        .isEqualTo("Could not find an application with ID " + applicationPublicId + ".");
  }

  @Test
  public void testGetLicenseLegalComponentReport_ComponentIdentifier() throws Exception {
    Owner owner = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.METADATA_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.LEGAL_COMMENT_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.LEGAL_FILE_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.SOURCE_LINK_URL);

    HttpResponse response = restRequest().path(DefaultApiLegalReportResourceV2.COMPONENT_PATH)
        .parameter(owner.getType().toString(), owner.getPublicId())
        .query("componentIdentifier", componentIdentifier)
        .get();

    assertResponseStatus(200, response);
    ApiLicenseLegalComponentReportDTO apiLicenseLegalComponentDTO =
        response.getBody(ApiLicenseLegalComponentReportDTO.class);
    assertThat(apiLicenseLegalComponentDTO).isNotNull();
    assertThat(apiLicenseLegalComponentDTO.component.componentIdentifier.toComponentIdentifier())
        .isEqualTo(componentIdentifier);
  }

  @Test
  public void testGetLicenseLegalComponentReport_PackageUrl() throws Exception {
    Owner owner = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.METADATA_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.LEGAL_COMMENT_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.LEGAL_FILE_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.SOURCE_LINK_URL);

    HttpResponse response = restRequest().path(DefaultApiLegalReportResourceV2.COMPONENT_PATH)
        .parameter(owner.getType().toString(), owner.getPublicId())
        .query("packageUrl", PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier).getPackageUrl())
        .get();

    assertResponseStatus(200, response);
    ApiLicenseLegalComponentReportDTO apiLicenseLegalComponentDTO =
        response.getBody(ApiLicenseLegalComponentReportDTO.class);
    assertThat(apiLicenseLegalComponentDTO).isNotNull();
    assertThat(apiLicenseLegalComponentDTO.component.componentIdentifier.toComponentIdentifier())
        .isEqualTo(componentIdentifier);
  }

  @Test
  public void testGetLicenseLegalComponentReport_Hash() throws Exception {
    Owner owner = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    String hash = "hash";
    tempEntity.newApplicationComponent(owner.getId(), BuildStageType.ID, hash, componentIdentifier);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.METADATA_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.LEGAL_COMMENT_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.LEGAL_FILE_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.SOURCE_LINK_URL);

    HttpResponse response = restRequest().path(DefaultApiLegalReportResourceV2.COMPONENT_PATH)
        .parameter(owner.getType().toString(), owner.getPublicId())
        .query("hash", hash)
        .get();

    assertResponseStatus(200, response);
    ApiLicenseLegalComponentReportDTO apiLicenseLegalComponentDTO =
        response.getBody(ApiLicenseLegalComponentReportDTO.class);
    assertThat(apiLicenseLegalComponentDTO).isNotNull();
    assertThat(apiLicenseLegalComponentDTO.component.componentIdentifier.toComponentIdentifier())
        .isEqualTo(componentIdentifier);
  }

  @Test
  public void testGetLicenseLegalComponentReport_ThirdParty() throws Exception {
    Owner owner = tempEntity.newApplicationWithParent();
    Map<String, String> coordinates = new HashMap<>();
    coordinates.put("name", "glibc");
    coordinates.put(ComponentIdentifier.VERSION, "2.24-11+deb9u3");
    ComponentIdentifier componentIdentifier = new ComponentIdentifier("debian-9", coordinates);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.METADATA_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.LEGAL_COMMENT_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.LEGAL_FILE_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.SOURCE_LINK_URL);
    String scanId = "scanId";
    mockThirdPartyReport(owner.getId(), scanId);

    HttpResponse response = restRequest().path(DefaultApiLegalReportResourceV2.COMPONENT_PATH)
        .parameter(owner.getType().toString(), owner.getPublicId())
        .query("componentIdentifier", componentIdentifier)
        .query("identificationSource", IdentificationSource.CLAIR.getId())
        .query("scanId", scanId)
        .get();

    assertResponseStatus(200, response);
    ApiLicenseLegalComponentReportDTO apiLicenseLegalComponentDTO =
        response.getBody(ApiLicenseLegalComponentReportDTO.class);
    assertThat(apiLicenseLegalComponentDTO).isNotNull();
    assertThat(apiLicenseLegalComponentDTO.component.componentIdentifier.toComponentIdentifier())
        .isEqualTo(componentIdentifier);
  }

  @Test
  public void testPostCustomLicenseLegalApplicationReport_NoTitle() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    HttpResponse response =
        restRequest().path(DefaultApiLegalReportResourceV2.APPLICATION_REPORT_PATH)
            .parameter(application.getId(), BuildStageType.ID)
            .part(REPORT_FORM_HEADER, "header")
            .part(REPORT_FORM_FOOTER, "footer")
            .post();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Missing required parameter: %s", REPORT_FORM_TITLE);
  }

  @Test
  public void testPostCustomLicenseLegalApplicationReport_FromTemplate() throws Exception {
    Application application = tempEntity.newApplicationWithParent();

    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, tempEntity.uuid());
    mockReport(policyEvaluation, getClass().getSimpleName());
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.METADATA_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.LEGAL_COMMENT_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.LEGAL_FILE_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.SOURCE_LINK_URL);

    AttributionReportTemplate template =
        tempEntity.createNewAttributionReportTemplate(
            "Template Name",
            "testPostCustomLicenseLegalApplicationReport_FromTemplateTITLE",
            "testPostCustomLicenseLegalApplicationReport_FromTemplateHEADER",
            "testPostCustomLicenseLegalApplicationReport_FromTemplateFOOTER",
            false, false, false);

    HttpResponse response =
        restRequest().path(DefaultApiLegalReportResourceV2.APPLICATION_REPORT_FROM_TEMPLATE_PATH)
            .parameter(application.getId(), BuildStageType.ID, template.getId())
            .post();

    assertResponseStatus(200, response);
    assertThat(response.getBodyText()).contains("testPostCustomLicenseLegalApplicationReport_FromTemplateTITLE");
    assertThat(response.getBodyText()).contains("testPostCustomLicenseLegalApplicationReport_FromTemplateHEADER");
    assertThat(response.getBodyText()).contains("testPostCustomLicenseLegalApplicationReport_FromTemplateFOOTER");
  }

  @Test
  public void testPostCustomLicenseLegalMultiApplicationReport_FromTemplate() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    Application application2 = tempEntity.newApplicationWithParent();

    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, tempEntity.uuid());
    PolicyEvaluation policyEvaluation2 =
        tempEntity.newPolicyEvaluation(application2.getId(), BuildStageType.ID, tempEntity.uuid());
    mockReport(policyEvaluation, getClass().getSimpleName());
    mockReport(policyEvaluation2, getClass().getSimpleName());
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.METADATA_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.LEGAL_COMMENT_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.LEGAL_FILE_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.SOURCE_LINK_URL);

    AttributionReportTemplate template = tempEntity.createNewAttributionReportTemplate("Template Name",
        "testPostCustomLicenseLegalApplicationReport_FromTemplateTITLE",
        "testPostCustomLicenseLegalApplicationReport_FromTemplateHEADER",
        "testPostCustomLicenseLegalApplicationReport_FromTemplateFOOTER", false, false, false);

    File file = createNoticeFile();

    HttpResponse response = restRequest()
        .path(DefaultApiLegalReportResourceV2.MULTI_APPLICATION_REPORT_PATH_FROM_TEMPLATE_PATH)
        .parameter(template.getId()).part("noticeFiles", file)
        .part("applications", application.getPublicId() + "," + application2.getPublicId())
        .part("stages", BuildStageType.ID + "," + BuildStageType.ID)
        .post();

    assertResponseStatus(200, response);
    assertThat(response.getBodyText()).contains("testPostCustomLicenseLegalApplicationReport_FromTemplateTITLE");
    assertThat(response.getBodyText()).contains("testPostCustomLicenseLegalApplicationReport_FromTemplateHEADER");
    assertThat(response.getBodyText()).contains("testPostCustomLicenseLegalApplicationReport_FromTemplateFOOTER");
  }

  @Test
  public void testPostCustomLicenseLegalApplicationReport_FromTemplate_withNoticeFiles() throws Exception {
    Application application = tempEntity.newApplicationWithParent();

    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, tempEntity.uuid());
    mockReport(policyEvaluation, getClass().getSimpleName());
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.METADATA_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.LEGAL_COMMENT_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.LEGAL_FILE_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.SOURCE_LINK_URL);

    AttributionReportTemplate template =
        tempEntity.createNewAttributionReportTemplate(
            "Template Name",
            "testPostCustomLicenseLegalApplicationReport_FromTemplateTITLE",
            "testPostCustomLicenseLegalApplicationReport_FromTemplateHEADER",
            "testPostCustomLicenseLegalApplicationReport_FromTemplateFOOTER",
            false, false, false);

    File file = createNoticeFile();

    HttpResponse response =
        restRequest().path(DefaultApiLegalReportResourceV2.APPLICATION_REPORT_FROM_TEMPLATE_PATH)
            .parameter(application.getId(), BuildStageType.ID, template.getId())
            .part("noticeFiles", file)
            .post();

    assertResponseStatus(200, response);
    assertThat(response.getBodyText()).contains("notice file content");
  }

  @Test
  public void testPostCustomLicenseLegalApplicationReport_FromTemplate_withNonTextNoticeFiles() throws Exception {
    Application application = tempEntity.newApplicationWithParent();

    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, tempEntity.uuid());
    mockReport(policyEvaluation, getClass().getSimpleName());
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.METADATA_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.LEGAL_COMMENT_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.LEGAL_FILE_URL);

    AttributionReportTemplate template =
        tempEntity.createNewAttributionReportTemplate(
            "Template Name",
            "testPostCustomLicenseLegalApplicationReport_FromTemplateTITLE",
            "testPostCustomLicenseLegalApplicationReport_FromTemplateHEADER",
            "testPostCustomLicenseLegalApplicationReport_FromTemplateFOOTER",
            false, false, false);

    File file = createNoticeFile();

    HttpResponse response =
        restRequest().path(DefaultApiLegalReportResourceV2.APPLICATION_REPORT_FROM_TEMPLATE_PATH)
            .parameter(application.getId(), BuildStageType.ID, template.getId())
            .part("noticeFiles", "notice.txt", file, "image/png")
            .post();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).contains("Following notice files must be plain text files: notice.txt");
  }

  @Test
  public void testPostCustomLicenseLegalApplicationReport_FromTemplate_NoSuchTemplate() throws Exception {
    Application application = tempEntity.newApplicationWithParent();

    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, tempEntity.uuid());
    mockReport(policyEvaluation, getClass().getSimpleName());
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.METADATA_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.LEGAL_COMMENT_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.LEGAL_FILE_URL);
    hdsRespondWith(EMPTY_JSON_ARRAY).atUri(ApiLicenseLegalHdsService.SOURCE_LINK_URL);

    HttpResponse response =
        restRequest().path(DefaultApiLegalReportResourceV2.APPLICATION_REPORT_FROM_TEMPLATE_PATH)
            .parameter(application.getId(), BuildStageType.ID, "INVALID")
            .post();

    assertResponseStatus(404, response);
  }

  private void mockThirdPartyReport(String applicationId, String scanId) {
    try {
      Path reportDir = getCLMServer().getInstance(InsightWork.class).getReportDir(applicationId, scanId).toPath();
      Files.createDirectories(reportDir);
      Files.write(reportDir.resolve("report.zip"), Collections.singletonList("report.zip"));
      File reportFile = reportDir.resolve("report.zip").toFile();
      String[] filenames = {
          ThirdPartyComponentDAO.THIRD_PARTY_BOM_JSON_FILENAME,
          ThirdPartyComponentDAO.THIRD_PARTY_LICENSE_JSON_FILENAME,
          ThirdPartyComponentDAO.THIRD_PARTY_SECURITY_JSON_FILENAME
      };
      for (String filename : filenames) {
        File file = Report.getCacheFile(reportFile, filename);
        FileUtils.copyURLToFile(
            Objects.requireNonNull(getClass().getResource("/" + getClass().getSimpleName() + "/report/" + filename)),
            file);
      }
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private File createNoticeFile() throws IOException {
    File file = File.createTempFile("notice", ".txt");
    file.deleteOnExit();
    FileUtils.writeStringToFile(file, "notice file content", StandardCharsets.UTF_8);
    return file;
  }
}
