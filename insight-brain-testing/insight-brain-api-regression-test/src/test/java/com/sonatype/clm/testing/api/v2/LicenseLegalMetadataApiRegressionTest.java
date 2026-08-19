/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.api.v2;

import com.sonatype.clm.testing.api.AbstractIqApiTest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.ApiLegalAttributionReportTemplateResourceV2;
import com.sonatype.insight.brain.api.v2.dto.legal.AttributionReportTemplateDTO;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * API regression suite for {@code /api/v2/licenseLegalMetadata/...} — covers both
 * {@code ApiLegalReportResourceV2} (metadata retrieval) and
 * {@link ApiLegalAttributionReportTemplateResourceV2} (attribution report template CRUD).
 *
 * <p>
 * The class-level path constant is {@code LICENSE_LEGAL_RESOURCE_PATH_V2}
 * ({@code /api/v2/licenseLegalMetadata}). Sub-paths use singular {@code application} and
 * {@code stage} — an earlier draft of the plan showed plural forms which do not match the
 * actual resource annotations.
 */
public class LicenseLegalMetadataApiRegressionTest
    extends AbstractIqApiTest
{
  private static final String LEGAL_BASE = PublicApiPaths.LICENSE_LEGAL_RESOURCE_PATH_V2;

  private static final String TEMPLATE_PATH =
      LEGAL_BASE + "/" + ApiLegalAttributionReportTemplateResourceV2.REPORT_TEMPLATE_PATH;

  private static String applicationMetadataPath(final String applicationId) {
    return LEGAL_BASE + "/application/" + applicationId;
  }

  private static String applicationStageMetadataPath(final String applicationId, final String stageId) {
    return LEGAL_BASE + "/application/" + applicationId + "/stage/" + stageId;
  }

  private static String templatePath(final String templateId) {
    return TEMPLATE_PATH + templateId;
  }

  @Test
  public void testGetLicenseLegalApplication_unknownApp_returns404() throws Exception {
    HttpResponse response = apiGet(applicationMetadataPath(uniqueId("nonexistent-app")));
    assertResponseStatus(404, response);
  }

  @Test
  public void testGetLicenseLegalApplicationStage_unknownApp_returns404() throws Exception {
    HttpResponse response =
        apiGet(applicationStageMetadataPath(uniqueId("nonexistent-app"), BuildStageType.ID));
    assertResponseStatus(404, response);
  }

  @Test
  public void testGetLicenseLegalApplication_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(applicationMetadataPath(uniqueId("any-app")));
    assertResponseStatus(401, response);
  }

  /* ---- Attribution report template CRUD --------------------------------- */

  /**
   * The template is created through the HTTP API rather than via {@code tempEntity}, so
   * {@link com.sonatype.insight.brain.dataaccess.TemporaryEntity} does not register it for
   * teardown. Under {@code reuseForks=true} a leaked row would live for the rest of the fork
   * and pollute any test that later asserts an initial-state count. Self-clean via DELETE
   * after the shape assertions, mirroring {@link #testAttributionReportTemplate_createGetDelete_roundTrip}.
   */
  @Test
  public void testCreateAttributionReportTemplate_success() throws Exception {
    AttributionReportTemplateDTO template = new AttributionReportTemplateDTO();
    template.setTemplateName(uniqueName("Api Legal Tpl"));
    template.setDocumentTitle(uniqueName("Api Legal Title"));

    HttpResponse response = apiPostJson(TEMPLATE_PATH, template);
    assertResponseStatus(200, response);
    assertThatJson(response.getBodyText()).node("id").isString().isNotEmpty();
    assertThatJson(response.getBodyText()).node("templateName").isEqualTo(template.getTemplateName());
    assertThatJson(response.getBodyText()).node("documentTitle").isEqualTo(template.getDocumentTitle());

    String id = JsonPath.read(response.getBodyText(), "$.id");
    assertResponseStatus(204, apiDelete(templatePath(id)));
  }

  @Test
  public void testCreateAttributionReportTemplate_blankTitle_returns400() throws Exception {
    AttributionReportTemplateDTO template = new AttributionReportTemplateDTO();
    template.setTemplateName(uniqueName("Api Legal Tpl No Title"));
    template.setDocumentTitle("");

    HttpResponse response = apiPostJson(TEMPLATE_PATH, template);
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).containsIgnoringCase("template title");
  }

  @Test
  public void testCreateAttributionReportTemplate_blankName_returns400() throws Exception {
    AttributionReportTemplateDTO template = new AttributionReportTemplateDTO();
    template.setTemplateName("");
    template.setDocumentTitle(uniqueName("Api Legal Title Only"));

    HttpResponse response = apiPostJson(TEMPLATE_PATH, template);
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).containsIgnoringCase("template name");
  }

  @Test
  public void testGetAttributionReportTemplateById_notFound() throws Exception {
    HttpResponse response = apiGet(templatePath(uniqueId("nonexistent-template")));
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).containsIgnoringCase("no report with id");
  }

  @Test
  public void testGetAllAttributionReportTemplates_success() throws Exception {
    HttpResponse response = apiGet(TEMPLATE_PATH);
    assertResponseStatus(200, response);
    assertThatJson(response.getBodyText()).isArray();
  }

  @Test
  public void testDeleteAttributionReportTemplate_notFound() throws Exception {
    HttpResponse response = apiDelete(templatePath(uniqueId("nonexistent-template")));
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).containsIgnoringCase("does not exist");
  }

  @Test
  public void testAttributionReportTemplate_createGetDelete_roundTrip() throws Exception {
    AttributionReportTemplateDTO template = new AttributionReportTemplateDTO();
    template.setTemplateName(uniqueName("Api Legal Roundtrip Tpl"));
    template.setDocumentTitle(uniqueName("Api Legal Roundtrip Title"));

    HttpResponse createResponse = apiPostJson(TEMPLATE_PATH, template);
    assertResponseStatus(200, createResponse);
    // Extract via JsonPath (transitively available via json-unit-json-path) rather than regex —
    // the regex ".*\"id\":.*" is fragile if the server ever emits pretty-printed JSON, since
    // `.` doesn't match newlines by default.
    String id = JsonPath.read(createResponse.getBodyText(), "$.id");

    HttpResponse getResponse = apiGet(templatePath(id));
    assertResponseStatus(200, getResponse);
    assertThatJson(getResponse.getBodyText()).node("id").isEqualTo(id);

    HttpResponse deleteResponse = apiDelete(templatePath(id));
    assertResponseStatus(204, deleteResponse);

    HttpResponse getAgain = apiGet(templatePath(id));
    assertResponseStatus(404, getAgain);
  }

  /**
   * Auth guards for {@link ApiLegalAttributionReportTemplateResourceV2}. One test per verb the
   * resource exposes (POST, GET, DELETE) — a future per-method Shiro / {@code @PermitAll}
   * annotation on any single verb would silently bypass auth without a per-verb test catching
   * it.
   */
  @Test
  public void testCreateAttributionReportTemplate_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiPostJson(TEMPLATE_PATH, new AttributionReportTemplateDTO());
    assertResponseStatus(401, response);
  }

  @Test
  public void testGetAttributionReportTemplates_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(TEMPLATE_PATH);
    assertResponseStatus(401, response);
  }

  @Test
  public void testDeleteAttributionReportTemplate_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiDelete(templatePath(uniqueId("any-template")));
    assertResponseStatus(401, response);
  }
}
