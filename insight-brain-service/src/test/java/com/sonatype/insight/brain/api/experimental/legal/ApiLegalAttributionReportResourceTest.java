/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.legal;

import java.util.List;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;

import com.sonatype.insight.brain.api.v2.DefaultApiLegalAttributionReportResourceV2;
import com.sonatype.insight.brain.api.v2.dto.legal.AttributionReportTemplateDTO;

import com.sonatype.insight.brain.model.legal.AttributionReportTemplate;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.api.PublicApiPaths.LICENSE_LEGAL_RESOURCE_PATH_V2;
import static org.assertj.core.api.Assertions.assertThat;

public class ApiLegalAttributionReportResourceTest
    extends AbstractResourceTest
{
  private static final String NONEXISTENT_ID = "nonexistent id";

  private static final String DUPLICATE_TITLE = "duplicate title";

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
        .createNewAttributionReportTemplate("title");
    tempEntity.createNewAttributionReportTemplate("second title");

    HttpResponse response = restRequest()
        .path(DefaultApiLegalAttributionReportResourceV2.REPORT_TEMPLATE_PATH_ID)
        .parameter(report.getId())
        .auth().get();

    assertResponseStatus(200, response);
    AttributionReportTemplateDTO result =
        response.getBody(AttributionReportTemplateDTO.class);
    assertThat(result).isNotNull();
    assertThat(result.getDocumentTitle()).isEqualTo(report.getDocumentTitle());
  }

  @Test
  public void testGetAllAttributionReportTemplates() throws Exception {
    AttributionReportTemplate report = tempEntity
        .createNewAttributionReportTemplate("title");
    AttributionReportTemplate report2 = tempEntity
        .createNewAttributionReportTemplate("title2");
    HttpResponse response =
        restRequest().path(DefaultApiLegalAttributionReportResourceV2.REPORT_TEMPLATE_PATH).auth().get();
    assertResponseStatus(200, response);
    List<AttributionReportTemplateDTO> result =
        response.getBody(List.class);
    assertThat(result).hasSize(2);
    assertThat(result.toString()).contains(report.getDocumentTitle());
    assertThat(result.toString()).contains(report2.getDocumentTitle());
  }

  @Test
  public void testSaveAttributionReportTemplate() throws Exception {
    AttributionReportTemplate reportTemplate = new AttributionReportTemplate(
        "title",
        "header",
        "footer",
        false,
        false);
    HttpResponse savedResponse = restRequest().path(DefaultApiLegalAttributionReportResourceV2.REPORT_TEMPLATE_PATH)
        .body(AttributionReportTemplateDTO.fromReportTemplate(reportTemplate))
        .post();
    AttributionReportTemplateDTO savedReportTemplateDTO = savedResponse.getBody(AttributionReportTemplateDTO.class);

    assertResponseStatus(200, savedResponse);
    assertThat(savedReportTemplateDTO.getDocumentTitle()).isEqualTo(reportTemplate.getDocumentTitle());

    reportTemplate.setDocumentTitle("updated title");
    reportTemplate.setId(savedReportTemplateDTO.getId());

    restRequest().path(DefaultApiLegalAttributionReportResourceV2.REPORT_TEMPLATE_PATH)
        .body(AttributionReportTemplateDTO.fromReportTemplate(reportTemplate))
        .post();
    HttpResponse response = restRequest()
        .path(DefaultApiLegalAttributionReportResourceV2.REPORT_TEMPLATE_PATH_ID)
        .parameter(savedReportTemplateDTO.getId())
        .auth().get();
    savedReportTemplateDTO = response.getBody(AttributionReportTemplateDTO.class);

    assertResponseStatus(200, response);
    assertThat(savedReportTemplateDTO.getDocumentTitle()).isEqualTo(reportTemplate.getDocumentTitle());
  }

  @Test
  public void testSaveAttributionReportTemplateNullAndEmptyTitle() throws Exception {
    AttributionReportTemplate reportTemplate = new AttributionReportTemplate();
    HttpResponse savedResponse = restRequest().path(DefaultApiLegalAttributionReportResourceV2.REPORT_TEMPLATE_PATH)
        .body(AttributionReportTemplateDTO.fromReportTemplate(reportTemplate))
        .post();

    assertResponseStatus(400, savedResponse);
    assertThat(savedResponse.getBodyText()).isEqualTo("Report template title cannot be blank");

    reportTemplate.setDocumentTitle(" ");
    savedResponse = restRequest().path(DefaultApiLegalAttributionReportResourceV2.REPORT_TEMPLATE_PATH)
        .body(AttributionReportTemplateDTO.fromReportTemplate(reportTemplate))
        .post();

    assertResponseStatus(400, savedResponse);
    assertThat(savedResponse.getBodyText()).isEqualTo("Report template title cannot be blank");

  }

  @Test
  public void testSaveAttributionReportTemplateMissingTemplate() throws Exception {
    HttpResponse savedResponse = restRequest().path(
        DefaultApiLegalAttributionReportResourceV2.REPORT_TEMPLATE_PATH).post();

    assertResponseStatus(400, savedResponse);
    assertThat(savedResponse.getBodyText()).isEqualTo("No report template provided");
  }

  @Test
  public void testGetAttributionReportTemplateDoesNotExist() throws Exception {
    HttpResponse response = restRequest().path(DefaultApiLegalAttributionReportResourceV2.REPORT_TEMPLATE_PATH_ID)
        .parameter(NONEXISTENT_ID).get();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("No report with id %s", NONEXISTENT_ID);
  }

  @Test
  public void testSaveAttributionReportTemplateEmptyStringID() throws Exception {
    AttributionReportTemplate template = new AttributionReportTemplate();
    template.setId("");
    template.setDocumentTitle("");
    HttpResponse response = restRequest().path(DefaultApiLegalAttributionReportResourceV2.REPORT_TEMPLATE_PATH)
        .body(AttributionReportTemplateDTO.fromReportTemplate(template)).post();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText())
        .isEqualTo("id cannot be an empty string. Leave id null and allow service to set one");
  }

  @Test
  public void testDeleteAttributionReportTemplateById() throws Exception {
    AttributionReportTemplate tempReport = tempEntity.createNewAttributionReportTemplate("to be deleted");
    HttpResponse response = restRequest().path(DefaultApiLegalAttributionReportResourceV2.REPORT_TEMPLATE_PATH_ID)
        .parameter(tempReport.getId()).delete();
    assertResponseStatus(204, response);
    assertThat(response.getBodyText()).isEmpty();
    response = restRequest().path(DefaultApiLegalAttributionReportResourceV2.REPORT_TEMPLATE_PATH).get();
    assertResponseStatus(200, response);
    assertThat(response.getBody(List.class)).isEmpty();
  }

  @Test
  public void testDeleteAttributionReportTemplateDoesNotExist() throws Exception {
    HttpResponse response = restRequest().path(DefaultApiLegalAttributionReportResourceV2.REPORT_TEMPLATE_PATH_ID)
        .parameter(NONEXISTENT_ID).delete();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Template with id %s does not exist", NONEXISTENT_ID);
  }

  @Test
  public void testSaveAttributionReportTemplateWithDuplicateTitle() throws Exception {
    tempEntity.createNewAttributionReportTemplate(DUPLICATE_TITLE);
    AttributionReportTemplateDTO toBeSaved = new AttributionReportTemplateDTO();
    toBeSaved.setDocumentTitle(DUPLICATE_TITLE);

    HttpResponse response = restRequest().path(DefaultApiLegalAttributionReportResourceV2.REPORT_TEMPLATE_PATH)
        .body(toBeSaved).post();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Report template already exists with title %s", DUPLICATE_TITLE);
  }

  @Test
  public void testSaveAttributionReportTemplateWithHTMLCharacters() throws Exception {
    AttributionReportTemplateDTO reportTemplateDTO = new AttributionReportTemplateDTO(
        "<html>title</html>",
        "<html>header</html>",
        "<html>footer</html>",
        false, false
    );
    HttpResponse response = restRequest().path(DefaultApiLegalAttributionReportResourceV2.REPORT_TEMPLATE_PATH)
        .body(reportTemplateDTO).post();
    AttributionReportTemplateDTO savedDto = response.getBody(AttributionReportTemplateDTO.class);
    assertThat(savedDto.getDocumentTitle()).isEqualTo("&lt;html&gt;title&lt;/html&gt;");
    assertThat(savedDto.getHeader()).isEqualTo("&lt;html&gt;header&lt;/html&gt;");
    assertThat(savedDto.getFooter()).isEqualTo("&lt;html&gt;footer&lt;/html&gt;");
  }
}
