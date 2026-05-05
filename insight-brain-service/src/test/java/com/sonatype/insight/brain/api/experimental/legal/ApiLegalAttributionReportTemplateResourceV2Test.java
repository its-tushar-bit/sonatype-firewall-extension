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
import com.sonatype.insight.brain.model.legal.AttributionReportTemplate;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.api.PublicApiPaths.LICENSE_LEGAL_RESOURCE_PATH_V2;
import static org.assertj.core.api.Assertions.assertThat;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class ApiLegalAttributionReportTemplateResourceV2Test
    extends AbstractResourceTest
{
  private static final String NONEXISTENT_ID = "nonexistent id";

  private static final String DUPLICATE_TEMPLATE_NAME = "duplicate title";

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(LICENSE_LEGAL_RESOURCE_PATH_V2);
  }

  @Before
  public void setup() throws Exception {
    setFeatures(LicensedFeature.ADVANCED_LEGAL_PACK);
  }

  @Test
  public void testGetAttributionReportTemplateById() throws Exception {
    AttributionReportTemplate report = tempEntity
        .createNewAttributionReportTemplate("template one", "title");
    tempEntity.createNewAttributionReportTemplate("template two", "second title");

    HttpResponse response = restRequest()
        .path(ApiLegalAttributionReportTemplateResourceV2.REPORT_TEMPLATE_PATH_ID)
        .parameter(report.getId())
        .auth()
        .get();

    assertResponseStatus(200, response);
    AttributionReportTemplateDTO result =
        response.getBody(AttributionReportTemplateDTO.class);
    assertThat(result).isNotNull();
    assertThat(result.getDocumentTitle()).isEqualTo(report.getDocumentTitle());
  }

  @Test
  public void testGetAllAttributionReportTemplates() throws Exception {
    AttributionReportTemplate report = tempEntity
        .createNewAttributionReportTemplate("template one", "title");
    AttributionReportTemplate report2 = tempEntity
        .createNewAttributionReportTemplate("template two", "title2");
    HttpResponse response =
        restRequest().path(ApiLegalAttributionReportTemplateResourceV2.REPORT_TEMPLATE_PATH).auth().get();
    assertResponseStatus(200, response);
    AttributionReportTemplateDTO[] result = response.getBody(AttributionReportTemplateDTO[].class);
    assertThat(result).hasSize(2);
    assertThat(result).extracting(AttributionReportTemplateDTO::getDocumentTitle)
        .containsExactlyInAnyOrder(report.getDocumentTitle(), report2.getDocumentTitle());
  }

  @Test
  public void testSaveAttributionReportTemplate() throws Exception {
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

    assertResponseStatus(200, savedResponse);
    assertThat(savedReportTemplateDTO.getDocumentTitle()).isEqualTo(reportTemplate.getDocumentTitle());

    reportTemplate.setDocumentTitle("updated title");
    reportTemplate.setId(savedReportTemplateDTO.getId());

    HttpResponse response = restRequest().path(ApiLegalAttributionReportTemplateResourceV2.REPORT_TEMPLATE_PATH)
        .body(AttributionReportTemplateDTO.fromReportTemplate(reportTemplate))
        .post();
    assertResponseStatus(200, response);

    response = restRequest()
        .path(ApiLegalAttributionReportTemplateResourceV2.REPORT_TEMPLATE_PATH_ID)
        .parameter(savedReportTemplateDTO.getId())
        .auth()
        .get();
    savedReportTemplateDTO = response.getBody(AttributionReportTemplateDTO.class);

    assertResponseStatus(200, response);
    assertThat(savedReportTemplateDTO.getDocumentTitle()).isEqualTo(reportTemplate.getDocumentTitle());
    assertThat(savedReportTemplateDTO.getHeader()).isEqualTo(reportTemplate.getDocumentHeader());
    assertThat(savedReportTemplateDTO.getFooter()).isEqualTo(reportTemplate.getDocumentFooter());
    assertThat(savedReportTemplateDTO.isIncludeTableOfContents()).isEqualTo(reportTemplate.isIncludeTableOfContents());
    assertThat(savedReportTemplateDTO.isIncludeAppendix()).isEqualTo(reportTemplate.isIncludeAppendix());
    assertThat(savedReportTemplateDTO.isIncludeStandardLicenseTexts())
        .isEqualTo(reportTemplate.isIncludeStandardLicenseTexts());
  }

  @Test
  public void testUpdateTemplateNameToExisting() throws Exception {
    tempEntity.createNewAttributionReportTemplate("template one", "title");
    AttributionReportTemplate persistedTemplate =
        tempEntity.createNewAttributionReportTemplate("template two", "title");

    persistedTemplate.setTemplateName("template one");

    HttpResponse response = restRequest().path(ApiLegalAttributionReportTemplateResourceV2.REPORT_TEMPLATE_PATH)
        .body(AttributionReportTemplateDTO.fromReportTemplate(persistedTemplate))
        .post();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Report template already exists with name %s", "template one");
  }

  @Test
  public void testSaveAttributionReportTemplateNullAndEmptyTitle() throws Exception {
    AttributionReportTemplate reportTemplate = new AttributionReportTemplate();
    HttpResponse savedResponse = restRequest()
        .path(ApiLegalAttributionReportTemplateResourceV2.REPORT_TEMPLATE_PATH)
        .body(AttributionReportTemplateDTO.fromReportTemplate(reportTemplate))
        .post();

    assertResponseStatus(400, savedResponse);
    assertThat(savedResponse.getBodyText()).isEqualTo("Report template title cannot be blank");

    reportTemplate.setDocumentTitle(" ");
    savedResponse = restRequest().path(ApiLegalAttributionReportTemplateResourceV2.REPORT_TEMPLATE_PATH)
        .body(AttributionReportTemplateDTO.fromReportTemplate(reportTemplate))
        .post();

    assertResponseStatus(400, savedResponse);
    assertThat(savedResponse.getBodyText()).isEqualTo("Report template title cannot be blank");

    reportTemplate.setDocumentTitle("title");
    savedResponse = restRequest().path(ApiLegalAttributionReportTemplateResourceV2.REPORT_TEMPLATE_PATH)
        .body(AttributionReportTemplateDTO.fromReportTemplate(reportTemplate))
        .post();

    assertResponseStatus(400, savedResponse);
    assertThat(savedResponse.getBodyText()).isEqualTo("Report template name cannot be blank");
  }

  @Test
  public void testSaveAttributionReportTemplateMissingTemplate() throws Exception {
    HttpResponse savedResponse = restRequest().path(
        ApiLegalAttributionReportTemplateResourceV2.REPORT_TEMPLATE_PATH).post();

    assertResponseStatus(400, savedResponse);
    assertThat(savedResponse.getBodyText()).isEqualTo("No report template provided");
  }

  @Test
  public void testGetAttributionReportTemplateDoesNotExist() throws Exception {
    HttpResponse response = restRequest()
        .path(ApiLegalAttributionReportTemplateResourceV2.REPORT_TEMPLATE_PATH_ID)
        .parameter(NONEXISTENT_ID)
        .get();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("No report with id %s", NONEXISTENT_ID);
  }

  @Test
  public void testSaveAttributionReportTemplateEmptyStringID() throws Exception {
    AttributionReportTemplate template = new AttributionReportTemplate();
    template.setId("");
    template.setDocumentTitle("");
    HttpResponse response = restRequest().path(ApiLegalAttributionReportTemplateResourceV2.REPORT_TEMPLATE_PATH)
        .body(AttributionReportTemplateDTO.fromReportTemplate(template))
        .post();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText())
        .isEqualTo("id cannot be an empty string. Leave id null and allow service to set one");
  }

  @Test
  public void testSaveAttributionReportTemplate_NullHeaderAndFooter() throws Exception {
    AttributionReportTemplate reportTemplate = new AttributionReportTemplate();
    reportTemplate.setTemplateName("template with null header and footer");
    reportTemplate.setDocumentTitle("title");
    HttpResponse savedResponse = restRequest()
        .path(ApiLegalAttributionReportTemplateResourceV2.REPORT_TEMPLATE_PATH)
        .body(AttributionReportTemplateDTO.fromReportTemplate(reportTemplate))
        .post();
    AttributionReportTemplateDTO savedReportTemplateDTO = savedResponse.getBody(AttributionReportTemplateDTO.class);

    assertResponseStatus(200, savedResponse);
    assertThat(savedReportTemplateDTO.getHeader()).isEmpty();
    assertThat(savedReportTemplateDTO.getFooter()).isEmpty();
  }

  @Test
  public void testDeleteAttributionReportTemplateById() throws Exception {
    AttributionReportTemplate tempReport =
        tempEntity.createNewAttributionReportTemplate("template to be deleted", "to be deleted");
    HttpResponse response = restRequest()
        .path(ApiLegalAttributionReportTemplateResourceV2.REPORT_TEMPLATE_PATH_ID)
        .parameter(tempReport.getId())
        .delete();
    assertResponseStatus(204, response);
    assertThat(response.getBodyText()).isEmpty();
    response = restRequest().path(ApiLegalAttributionReportTemplateResourceV2.REPORT_TEMPLATE_PATH).get();
    assertResponseStatus(200, response);
    assertThat(response.getBody(AttributionReportTemplateDTO[].class)).isEmpty();
  }

  @Test
  public void testDeleteAttributionReportTemplateDoesNotExist() throws Exception {
    HttpResponse response = restRequest()
        .path(ApiLegalAttributionReportTemplateResourceV2.REPORT_TEMPLATE_PATH_ID)
        .parameter(NONEXISTENT_ID)
        .delete();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Template with id %s does not exist", NONEXISTENT_ID);
  }

  @Test
  public void testSaveAttributionReportTemplateWithDuplicateTemplateName() throws Exception {
    tempEntity.createNewAttributionReportTemplate(DUPLICATE_TEMPLATE_NAME, "title");
    AttributionReportTemplateDTO toBeSaved = new AttributionReportTemplateDTO();
    toBeSaved.setTemplateName(DUPLICATE_TEMPLATE_NAME);
    toBeSaved.setDocumentTitle("other title");

    HttpResponse response = restRequest().path(ApiLegalAttributionReportTemplateResourceV2.REPORT_TEMPLATE_PATH)
        .body(toBeSaved)
        .post();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Report template already exists with name %s",
        DUPLICATE_TEMPLATE_NAME);
  }

  @Test
  public void testSaveAttributionReportTemplateWithHTMLCharacters() throws Exception {
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
