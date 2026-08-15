/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.v2.ApiLegalAttributionReportTemplateResourceV2;
import com.sonatype.insight.brain.api.v2.dto.legal.AttributionReportTemplateDTO;
import com.sonatype.insight.brain.model.legal.AttributionReportTemplate;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.api.PublicApiPaths.LICENSE_LEGAL_RESOURCE_PATH_V2;
import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2ApiLegalAttributionReportTemplateResourceV2Test
{
  private IqTestContext ctx;

  private static final String NONEXISTENT_ID = "nonexistent id";

  private static final String DUPLICATE_TEMPLATE_NAME = "duplicate title";

  private HttpRequest restRequest() {
    return ctx.restRequest().path(LICENSE_LEGAL_RESOURCE_PATH_V2);
  }

  @BeforeEach
  void setup() throws Exception {
    ctx.setFeatures(LicensedFeature.ADVANCED_LEGAL_PACK);
  }

  @Test
  void testGetAttributionReportTemplateById() throws Exception {
    AttributionReportTemplate report = ctx.tempEntity()
        .createNewAttributionReportTemplate("template one", "title");
    ctx.tempEntity().createNewAttributionReportTemplate("template two", "second title");

    HttpResponse response = restRequest()
        .path(ApiLegalAttributionReportTemplateResourceV2.REPORT_TEMPLATE_PATH_ID)
        .parameter(report.getId())
        .auth()
        .get();

    ctx.assertResponseStatus(200, response);
    AttributionReportTemplateDTO result =
        response.getBody(AttributionReportTemplateDTO.class);
    assertThat(result).isNotNull();
    assertThat(result.getDocumentTitle()).isEqualTo(report.getDocumentTitle());
  }

  @Test
  void testGetAllAttributionReportTemplates() throws Exception {
    AttributionReportTemplate report = ctx.tempEntity()
        .createNewAttributionReportTemplate("template one", "title");
    AttributionReportTemplate report2 = ctx.tempEntity()
        .createNewAttributionReportTemplate("template two", "title2");
    HttpResponse response =
        restRequest().path(ApiLegalAttributionReportTemplateResourceV2.REPORT_TEMPLATE_PATH).auth().get();
    ctx.assertResponseStatus(200, response);
    AttributionReportTemplateDTO[] result = response.getBody(AttributionReportTemplateDTO[].class);
    assertThat(result).hasSize(2);
    assertThat(result).extracting(AttributionReportTemplateDTO::getDocumentTitle)
        .containsExactlyInAnyOrder(report.getDocumentTitle(), report2.getDocumentTitle());
  }

  @Test
  void testSaveAttributionReportTemplate() throws Exception {
    AttributionReportTemplate reportTemplate = new AttributionReportTemplate(
        "template name",
        "title",
        "header",
        "footer",
        false,
        false,
        false,
        false,
        false);
    HttpResponse savedResponse = restRequest()
        .path(ApiLegalAttributionReportTemplateResourceV2.REPORT_TEMPLATE_PATH)
        .body(AttributionReportTemplateDTO.fromReportTemplate(reportTemplate))
        .post();
    AttributionReportTemplateDTO savedReportTemplateDTO = savedResponse.getBody(AttributionReportTemplateDTO.class);

    ctx.assertResponseStatus(200, savedResponse);
    assertThat(savedReportTemplateDTO.getDocumentTitle()).isEqualTo(reportTemplate.getDocumentTitle());

    reportTemplate.setDocumentTitle("updated title");
    reportTemplate.setId(savedReportTemplateDTO.getId());

    HttpResponse response = restRequest().path(ApiLegalAttributionReportTemplateResourceV2.REPORT_TEMPLATE_PATH)
        .body(AttributionReportTemplateDTO.fromReportTemplate(reportTemplate))
        .post();
    ctx.assertResponseStatus(200, response);

    response = restRequest()
        .path(ApiLegalAttributionReportTemplateResourceV2.REPORT_TEMPLATE_PATH_ID)
        .parameter(savedReportTemplateDTO.getId())
        .auth()
        .get();
    savedReportTemplateDTO = response.getBody(AttributionReportTemplateDTO.class);

    ctx.assertResponseStatus(200, response);
    assertThat(savedReportTemplateDTO.getDocumentTitle()).isEqualTo(reportTemplate.getDocumentTitle());
    assertThat(savedReportTemplateDTO.getHeader()).isEqualTo(reportTemplate.getDocumentHeader());
    assertThat(savedReportTemplateDTO.getFooter()).isEqualTo(reportTemplate.getDocumentFooter());
    assertThat(savedReportTemplateDTO.isIncludeTableOfContents()).isEqualTo(reportTemplate.isIncludeTableOfContents());
    assertThat(savedReportTemplateDTO.isIncludeAppendix()).isEqualTo(reportTemplate.isIncludeAppendix());
    assertThat(savedReportTemplateDTO.isIncludeStandardLicenseTexts())
        .isEqualTo(reportTemplate.isIncludeStandardLicenseTexts());
  }

  @Test
  void testUpdateTemplateNameToExisting() throws Exception {
    ctx.tempEntity().createNewAttributionReportTemplate("template one", "title");
    AttributionReportTemplate persistedTemplate =
        ctx.tempEntity().createNewAttributionReportTemplate("template two", "title");

    persistedTemplate.setTemplateName("template one");

    HttpResponse response = restRequest().path(ApiLegalAttributionReportTemplateResourceV2.REPORT_TEMPLATE_PATH)
        .body(AttributionReportTemplateDTO.fromReportTemplate(persistedTemplate))
        .post();

    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Report template already exists with name %s", "template one");
  }

  @Test
  void testSaveAttributionReportTemplateNullAndEmptyTitle() throws Exception {
    AttributionReportTemplate reportTemplate = new AttributionReportTemplate();
    HttpResponse savedResponse = restRequest()
        .path(ApiLegalAttributionReportTemplateResourceV2.REPORT_TEMPLATE_PATH)
        .body(AttributionReportTemplateDTO.fromReportTemplate(reportTemplate))
        .post();

    ctx.assertResponseStatus(400, savedResponse);
    assertThat(savedResponse.getBodyText()).isEqualTo("Report template title cannot be blank");

    reportTemplate.setDocumentTitle(" ");
    savedResponse = restRequest().path(ApiLegalAttributionReportTemplateResourceV2.REPORT_TEMPLATE_PATH)
        .body(AttributionReportTemplateDTO.fromReportTemplate(reportTemplate))
        .post();

    ctx.assertResponseStatus(400, savedResponse);
    assertThat(savedResponse.getBodyText()).isEqualTo("Report template title cannot be blank");

    reportTemplate.setDocumentTitle("title");
    savedResponse = restRequest().path(ApiLegalAttributionReportTemplateResourceV2.REPORT_TEMPLATE_PATH)
        .body(AttributionReportTemplateDTO.fromReportTemplate(reportTemplate))
        .post();

    ctx.assertResponseStatus(400, savedResponse);
    assertThat(savedResponse.getBodyText()).isEqualTo("Report template name cannot be blank");
  }

  @Test
  void testSaveAttributionReportTemplateMissingTemplate() throws Exception {
    HttpResponse savedResponse = restRequest().path(
        ApiLegalAttributionReportTemplateResourceV2.REPORT_TEMPLATE_PATH).post();

    ctx.assertResponseStatus(400, savedResponse);
    assertThat(savedResponse.getBodyText()).isEqualTo("No report template provided");
  }

  @Test
  void testGetAttributionReportTemplateDoesNotExist() throws Exception {
    HttpResponse response = restRequest()
        .path(ApiLegalAttributionReportTemplateResourceV2.REPORT_TEMPLATE_PATH_ID)
        .parameter(NONEXISTENT_ID)
        .get();
    ctx.assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("No report with id %s", NONEXISTENT_ID);
  }

  @Test
  void testSaveAttributionReportTemplateEmptyStringID() throws Exception {
    AttributionReportTemplate template = new AttributionReportTemplate();
    template.setId("");
    template.setDocumentTitle("");
    HttpResponse response = restRequest().path(ApiLegalAttributionReportTemplateResourceV2.REPORT_TEMPLATE_PATH)
        .body(AttributionReportTemplateDTO.fromReportTemplate(template))
        .post();
    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText())
        .isEqualTo("id cannot be an empty string. Leave id null and allow service to set one");
  }

  @Test
  void testSaveAttributionReportTemplate_NullHeaderAndFooter() throws Exception {
    AttributionReportTemplate reportTemplate = new AttributionReportTemplate();
    reportTemplate.setTemplateName("template with null header and footer");
    reportTemplate.setDocumentTitle("title");
    HttpResponse savedResponse = restRequest()
        .path(ApiLegalAttributionReportTemplateResourceV2.REPORT_TEMPLATE_PATH)
        .body(AttributionReportTemplateDTO.fromReportTemplate(reportTemplate))
        .post();
    AttributionReportTemplateDTO savedReportTemplateDTO = savedResponse.getBody(AttributionReportTemplateDTO.class);

    ctx.assertResponseStatus(200, savedResponse);
    assertThat(savedReportTemplateDTO.getHeader()).isEmpty();
    assertThat(savedReportTemplateDTO.getFooter()).isEmpty();
  }

  @Test
  void testDeleteAttributionReportTemplateById() throws Exception {
    AttributionReportTemplate tempReport =
        ctx.tempEntity().createNewAttributionReportTemplate("template to be deleted", "to be deleted");
    HttpResponse response = restRequest()
        .path(ApiLegalAttributionReportTemplateResourceV2.REPORT_TEMPLATE_PATH_ID)
        .parameter(tempReport.getId())
        .delete();
    ctx.assertResponseStatus(204, response);
    assertThat(response.getBodyText()).isEmpty();
    response = restRequest().path(ApiLegalAttributionReportTemplateResourceV2.REPORT_TEMPLATE_PATH).get();
    ctx.assertResponseStatus(200, response);
    assertThat(response.getBody(AttributionReportTemplateDTO[].class)).isEmpty();
  }

  @Test
  void testDeleteAttributionReportTemplateDoesNotExist() throws Exception {
    HttpResponse response = restRequest()
        .path(ApiLegalAttributionReportTemplateResourceV2.REPORT_TEMPLATE_PATH_ID)
        .parameter(NONEXISTENT_ID)
        .delete();
    ctx.assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Template with id %s does not exist", NONEXISTENT_ID);
  }

  @Test
  void testSaveAttributionReportTemplateWithDuplicateTemplateName() throws Exception {
    ctx.tempEntity().createNewAttributionReportTemplate(DUPLICATE_TEMPLATE_NAME, "title");
    AttributionReportTemplateDTO toBeSaved = new AttributionReportTemplateDTO();
    toBeSaved.setTemplateName(DUPLICATE_TEMPLATE_NAME);
    toBeSaved.setDocumentTitle("other title");

    HttpResponse response = restRequest().path(ApiLegalAttributionReportTemplateResourceV2.REPORT_TEMPLATE_PATH)
        .body(toBeSaved)
        .post();

    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Report template already exists with name %s",
        DUPLICATE_TEMPLATE_NAME);
  }

  @Test
  void testSaveAttributionReportTemplateWithHTMLCharacters() throws Exception {
    AttributionReportTemplateDTO reportTemplateDTO = new AttributionReportTemplateDTO(
        "<html>template name</html>",
        "<html>title</html>",
        "<html>header</html>",
        "<html>footer</html>",
        false, false, false, false,
        false);
    HttpResponse response = restRequest().path(ApiLegalAttributionReportTemplateResourceV2.REPORT_TEMPLATE_PATH)
        .body(reportTemplateDTO)
        .post();
    AttributionReportTemplateDTO savedDto = response.getBody(AttributionReportTemplateDTO.class);
    assertThat(savedDto.getTemplateName()).isEqualTo("<html>template name</html>");
    assertThat(savedDto.getDocumentTitle()).isEqualTo("<html>title</html>");
    assertThat(savedDto.getHeader()).isEqualTo("<html>header</html>");
    assertThat(savedDto.getFooter()).isEqualTo("<html>footer</html>");
  }
}
