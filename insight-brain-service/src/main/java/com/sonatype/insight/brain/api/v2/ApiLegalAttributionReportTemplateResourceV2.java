/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.experimental.legal.AttributionReportService;
import com.sonatype.insight.brain.api.v2.dto.legal.AttributionReportTemplateDTO;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;

@Named
@Timed
@Path(PublicApiPaths.LICENSE_LEGAL_RESOURCE_PATH_V2)
@Tag(name = "License Legal Metadata Template",
    description = "Use this REST API to manage and customize templates for the license legal metadata generated in " +
        "HTML format.")
public class ApiLegalAttributionReportTemplateResourceV2
{
  public static final String REPORT_TEMPLATE_PATH = "report-template/";

  public static final String REPORT_TEMPLATE_PATH_ID = REPORT_TEMPLATE_PATH + "{id}";

  private final AttributionReportService attributionReportService;

  @Inject
  public ApiLegalAttributionReportTemplateResourceV2(
      AttributionReportService attributionReportService)
  {
    this.attributionReportService = attributionReportService;
  }

  @GET
  @Path(REPORT_TEMPLATE_PATH_ID)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to retrieve a template for license legal metadata." +
      "\n" +
      "\n" +
      "Permissions required: Review Legal Obligations For Components Licenses for the root organization.",
      responses = {
        @ApiResponse(responseCode = "200",
            description = "The response contains the stored template corresponding to the `id` provided:" +
                "<ul>" +
                "<li>`id` is the template id.</li>" +
                "<li>`templateName` indicates the name of the stored template.</li>" +
                "<li>`documentTitle` is the title that is displayed at the top of the report.</li>" +
                "<li>`header` is the text that will be displayed above the `documentTitle`.</li>" +
                "<li>`footer` is the text that will be displayed at the bottom of the report.<li>" +
                "<li>`includeTableOfContents` is `true` if a table of contents containing links to the " +
                "components and their licenses will be added to the report." +
                "<li>`includeAppendix` is `true` if standard license text will be grouped in the report " +
                "appendix.</li>" +
                "<li>`includeStandardLicenseTexts` is `true` if the standard license text will be displayed " +
                "for components with no license files.</li>" +
                "<li>`includeSonatypeSpecialLicenses` is `true` if Sonatype Special Licenses (e.g. " +
                "Generic-Copyleft-Clause, Generic-Liberal-Clause, See-License-Clause, Identity-Clause etc.) " +
                "will be included in the report.</li>" +
                "<li>`lastUpdatedAt` indicates the time the template was last updated.</li>" +
                "<li>`includeInnerSource` is `true` if InnerSource components will be included in the " +
                "report.</li>" +
                "</ul>",
            useReturnTypeSchema = true)
      })
  public AttributionReportTemplateDTO getAttributionReportTemplateById(
      @Parameter(description = "Enter the templateId for the report.",
          required = true) @PathParam("id") String reportId)
  {
    return attributionReportService.getAttributionReportTemplateById(reportId)
        .orElseThrow(
            () -> new NotFoundException("No report with id " + reportId));
  }

  @GET
  @Path(REPORT_TEMPLATE_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to retrieve templates for all reports." +
      "\n" +
      "\n" +
      "Permissions required: Review Legal Obligations For Components Licenses for the root organization",
      responses = {
        @ApiResponse(responseCode = "200",
            description = "The response contains the stored template details for all reports. " +
                "For each template:" +
                "<ul>" +
                "<li>`id` is the template id.</li>" +
                "<li>`templateName` indicates the name of the stored template.</li>" +
                "<li>`documentTitle` is the title that is displayed at the top of the report.</li>" +
                "<li>`header` is the text that will be displayed above the `documentTitle`.</li>" +
                "<li>`footer` is the text that will be displayed at the bottom of the report.<li>" +
                "<li>`includeTableOfContents` is `true` if a table of contents containing links to the components " +
                "and their licenses will be added to the report." +
                "<li>`includeAppendix` is `true` if standard license text will be grouped in the report " +
                "appendix.</li>" +
                "<li>`includeStandardLicenseTexts` is `true` if the standard license text will be displayed " +
                "for components with no license files.</li>" +
                "<li>`includeSonatypeSpecialLicenses` is `true` if Sonatype Special Licenses (e.g. " +
                "Generic-Copyleft-Clause, Generic-Liberal-Clause, See-License-Clause, Identity-Clause etc.) " +
                "will be included in the report.</li>" +
                "<li>`lastUpdatedAt` indicates the time the template was last updated.</li>" +
                "<li>`includeInnerSource` is `true` if InnerSource components will be included in the " +
                "report.</li>" +
                "</ul>",
            useReturnTypeSchema = true)
      })
  public List<AttributionReportTemplateDTO> getAllAttributionReportTemplates() {
    return attributionReportService.getAllAttributionReportTemplates();
  }

  @POST
  @Path(REPORT_TEMPLATE_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to create or update a template." +
      "\n" +
      "\n" +
      "Permissions required: Review Legal Obligations For Components Licenses for the root organization",
      responses = {
        @ApiResponse(responseCode = "200",
            description = "The response contains the details of the template created or updated.",
            useReturnTypeSchema = true)
      })
  public AttributionReportTemplateDTO saveAttributionReportTemplate(
      @RequestBody(description = "Specify the details for the template as:" +
          "<ul>" +
          "<li>`id` is the template id.</li>" +
          "<li>`templateName` indicates the name of the stored template.</li>" +
          "<li>`documentTitle` is the title that is displayed at the top of the report.</li>" +
          "<li>`header` is the text that will be displayed above the `documentTitle`.</li>" +
          "<li>`footer` is the text that will be displayed at the bottom of the report.<li>" +
          "<li>`includeTableOfContents` is `true` if a table of contents containing links to the components and " +
          "their licenses will be added to the report." +
          "<li>`includeAppendix` is `true` if standard license text will be grouped in the report appendix.</li>" +
          "<li>`includeStandardLicenseTexts` is `true` if the standard license text will be displayed for components " +
          "with no license files.</li>" +
          "<li>`includeSonatypeSpecialLicenses` is `true` if Sonatype Special Licenses (e.g. " +
          "Generic-Copyleft-Clause, Generic-Liberal-Clause, See-License-Clause, Identity-Clause etc.) will be " +
          "included in the report.</li>" +
          "<li>`includeInnerSource` is `true` if InnerSource components will be included in the " +
          "report.</li>" +
          "</ul>") AttributionReportTemplateDTO reportTemplateDTO)
  {
    if (reportTemplateDTO == null) {
      throw new BadRequestException("No report template provided");
    }
    if (reportTemplateDTO.getId() != null && StringUtils.isBlank(reportTemplateDTO.getId())) {
      throw new InvalidNameException("id cannot be an empty string. Leave id null and allow service to set one");
    }
    if (StringUtils.isBlank(reportTemplateDTO.getDocumentTitle())) {
      throw new InvalidNameException("Report template title cannot be blank");
    }

    if (StringUtils.isBlank(reportTemplateDTO.getTemplateName())) {
      throw new InvalidNameException("Report template name cannot be blank");
    }

    if (reportTemplateDTO.getHeader() == null) {
      reportTemplateDTO.setHeader("");
    }

    if (reportTemplateDTO.getFooter() == null) {
      reportTemplateDTO.setFooter("");
    }

    return attributionReportService.saveAttributionReportTemplate(reportTemplateDTO);
  }

  @DELETE
  @Path(REPORT_TEMPLATE_PATH_ID)
  @Operation(description = "Use this method to delete an existing template." +
      "\n" +
      "\n" +
      "Permissions required: Review Legal Obligations For Components Licenses for the root organization",
      responses = {
        @ApiResponse(responseCode = "204",
            description = "Template deleted successfully.")
      })
  public void deleteAttributionReportTemplate(
      @Parameter(description = "Enter the template id for the template to be deleted.") @PathParam("id") String id)
  {
    if (attributionReportService.getAttributionReportTemplateById_NoAuthz(id).isPresent()) {
      attributionReportService.deleteAttributionReportTemplateById(id);
    }
    else {
      throw new NotFoundException(String.format("Template with id %s does not exist", id));
    }
  }
}
