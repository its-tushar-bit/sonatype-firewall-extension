/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.experimental.legal.AttributionReportService;
import com.sonatype.insight.brain.api.v2.dto.legal.AttributionReportTemplateDTO;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.unbescape.html.HtmlEscape;
import org.unbescape.html.HtmlEscapeLevel;
import org.unbescape.html.HtmlEscapeType;
import com.codahale.metrics.annotation.Timed;
import org.apache.commons.lang.StringUtils;

@Named
@Timed
@Path(PublicApiPaths.LICENSE_LEGAL_RESOURCE_PATH_V2)
public class DefaultApiLegalAttributionReportResourceV2
    implements ApiLicenseLegalReportResourceV2
{
  public static final String REPORT_TEMPLATE_PATH = "report-template/";

  public static final String REPORT_TEMPLATE_PATH_ID = REPORT_TEMPLATE_PATH + "{id}";

  private final AttributionReportService attributionReportService;

  @Inject
  public DefaultApiLegalAttributionReportResourceV2(
      AttributionReportService attributionReportService)
  {
    this.attributionReportService = attributionReportService;
  }

  @Override
  @GET
  @Path(REPORT_TEMPLATE_PATH_ID)
  @Produces(MediaType.APPLICATION_JSON)
  public AttributionReportTemplateDTO getAttributionReportTemplateById(@PathParam("id") String reportId) {
    return attributionReportService.getAttributionReportTemplateById(reportId).orElseThrow(
        () -> new NotFoundException("No report with id " + reportId)
    );
  }

  @Override
  @GET
  @Path(REPORT_TEMPLATE_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public List<AttributionReportTemplateDTO> getAllAttributionReportTemplates() {
    return attributionReportService.getAllAttributionReportTemplates();
  }

  @Override
  @POST
  @Path(REPORT_TEMPLATE_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public AttributionReportTemplateDTO saveAttributionReportTemplate(AttributionReportTemplateDTO reportTemplateDTO) {
    if (reportTemplateDTO == null) {
      throw new BadRequestException("No report template provided");
    }
    sanitizeAllTextFields(reportTemplateDTO);
    if (reportTemplateDTO.getId() != null && StringUtils.isBlank(reportTemplateDTO.getId())) {
      throw new InvalidNameException("id cannot be an empty string. Leave id null and allow service to set one");
    }
    else if (StringUtils.isBlank(reportTemplateDTO.getDocumentTitle())) {
      throw new InvalidNameException("Report template title cannot be blank");
    }
    else if (
        attributionReportService.getAttributionReportTemplateByTitle(reportTemplateDTO.getDocumentTitle()).isPresent())
    {
      throw new InvalidNameException(
          String.format("Report template already exists with title %s", reportTemplateDTO.getDocumentTitle()));
    }
    else {
      return attributionReportService.saveAttributionReportTemplate(reportTemplateDTO);
    }
  }

  @Override
  @DELETE
  @Path(REPORT_TEMPLATE_PATH_ID)
  public void deleteAttributionReportTemplate(@PathParam("id") String id) {
    if (attributionReportService.getAttributionReportTemplateById(id).isPresent()) {
      attributionReportService.deleteAttributionReportById(id);
    }
    else {
      throw new NotFoundException(String.format("Template with id %s does not exist", id));
    }
  }

  private AttributionReportTemplateDTO sanitizeAllTextFields(AttributionReportTemplateDTO reportTemplateDTO) {
    if (reportTemplateDTO.getId() != null) {
      reportTemplateDTO.setId(sanitizeString(reportTemplateDTO.getId()));
    }
    if (reportTemplateDTO.getDocumentTitle() != null) {
      reportTemplateDTO.setDocumentTitle(sanitizeString(reportTemplateDTO.getDocumentTitle()));
    }
    if (reportTemplateDTO.getHeader() != null) {
      reportTemplateDTO.setHeader(sanitizeString(reportTemplateDTO.getHeader()));
    }
    if (reportTemplateDTO.getFooter() != null) {
      reportTemplateDTO.setFooter(sanitizeString(reportTemplateDTO.getFooter()));
    }
    return reportTemplateDTO;
  }

  private String sanitizeString(final String text) {
    return HtmlEscape
      .escapeHtml(text,
          HtmlEscapeType.HTML5_NAMED_REFERENCES_DEFAULT_TO_HEXA,
          HtmlEscapeLevel.LEVEL_1_ONLY_MARKUP_SIGNIFICANT);
  }
}
