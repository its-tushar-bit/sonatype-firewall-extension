/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.experimental.legal.AttributionReportService;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalApplicationReportDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalComponentReportDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.AttributionReportTemplateDTO;
import com.sonatype.insight.brain.api.v2.service.legal.ApiLicenseLegalService;
import com.sonatype.insight.brain.api.v2.service.legal.report.ApplicationAttributionReportBuilder;
import com.sonatype.insight.brain.api.v2.service.legal.report.LegalCustomReportParameters;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Throwables;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.message.internal.MediaTypes;
import org.glassfish.jersey.server.ContainerRequest;

@Named
@Timed
@Path(PublicApiPaths.LICENSE_LEGAL_RESOURCE_PATH_V2)
public class DefaultApiLegalReportResourceV2
    implements ApiLegalReportResourceV2
{
  public static final String APPLICATION_PATH = "application/{applicationId}";

  public static final String APPLICATION_PATH_STAGE = APPLICATION_PATH + "/stage/{stageId}";

  public static final String APPLICATION_REPORT_PATH = APPLICATION_PATH_STAGE + "/report";

  public static final String APPLICATION_REPORT_FROM_TEMPLATE_PATH =
      APPLICATION_REPORT_PATH + "/templateId/{templateId}";

  public static final String COMPONENT_PATH = "{ownerType: application|organization}/{ownerId}/component";

  static final String REPORT_FORM_TITLE = "title";

  static final String REPORT_FORM_HEADER = "header";

  static final String REPORT_FORM_FOOTER = "footer";

  static final String REPORT_FORM_TOC = "includeToc";

  static final String REPORT_FORM_STANDARD_LICENSE = "includeStandardLicenseTexts";

  static final String REPORT_FORM_APPENDIX = "includeAppendix";

  static final String REPORT_FORM_NOTICE_FILES = "noticeFiles";

  private final ApiLicenseLegalService apiLicenseLegalServiceV2;

  private final AttributionReportService attributionReportService;

  private final ApplicationAttributionReportBuilder applicationAttributionReportBuilder;

  @Context
  private HttpServletRequest httpRequest;

  @Inject
  public DefaultApiLegalReportResourceV2(
      final ApiLicenseLegalService apiLicenseLegalService,
      final AttributionReportService attributionReportService,
      final ApplicationAttributionReportBuilder applicationAttributionReportBuilder)
  {
    this.apiLicenseLegalServiceV2 = apiLicenseLegalService;
    this.attributionReportService = attributionReportService;
    this.applicationAttributionReportBuilder = applicationAttributionReportBuilder;
  }

  @Override
  @GET
  @Path(APPLICATION_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public ApiLicenseLegalApplicationReportDTO getLicenseLegalApplicationReport(
      @PathParam("applicationId") String applicationId)
  {
    return apiLicenseLegalServiceV2
        .getLicenseLegalApplicationReport(IdUtils.getOwnerNotNull(OwnerType.APPLICATION, applicationId));
  }

  @Override
  @GET
  @Path(APPLICATION_PATH_STAGE)
  @Produces(MediaType.APPLICATION_JSON)
  public ApiLicenseLegalApplicationReportDTO getLicenseLegalApplicationReport(
      @PathParam("applicationId") String applicationId, @PathParam("stageId") String stageId)
  {
    return apiLicenseLegalServiceV2
        .getLicenseLegalApplicationReport(IdUtils.getOwnerNotNull(OwnerType.APPLICATION, applicationId), stageId);
  }

  @Override
  @GET
  @Path(APPLICATION_REPORT_PATH)
  @Produces(MediaType.TEXT_HTML)
  public String getLicenseLegalApplicationHTMLReport(
      @PathParam("applicationId") String applicationId, @PathParam("stageId") String stageId)
  {
    final Owner app = IdUtils.getOwnerNotNull(OwnerType.APPLICATION, applicationId);
    final LegalCustomReportParameters reportParameters =
        LegalCustomReportParameters.builder().buildWithDefaults(app.getPublicId());

    return applicationAttributionReportBuilder
        .generateCustomLegalApplicationAttributionReport(
            app, stageId, reportParameters);
  }

  @Override
  @POST
  @Path(APPLICATION_REPORT_PATH)
  @Produces(MediaType.TEXT_HTML)
  public String getLicenseLegalCustomApplicationHTMLReport(
      @PathParam("applicationId") String applicationId,
      @PathParam("stageId") String stageId,
      FormDataMultiPart formData)
  {
    final LegalCustomReportParameters.Builder reportParametersBuilder = LegalCustomReportParameters.builder();
    try {
      reportParametersBuilder.withTitle(requireMultiPartValue(formData, REPORT_FORM_TITLE))
          .withHeader(getMultiPartValue(formData, REPORT_FORM_HEADER, ""))
          .withFooter(getMultiPartValue(formData, REPORT_FORM_FOOTER, ""))
          .withIncludeToc(Boolean.parseBoolean(getMultiPartValue(formData, REPORT_FORM_TOC, "true")))
          .withIncludeStandardLicenseTexts(
              Boolean.parseBoolean(getMultiPartValue(formData, REPORT_FORM_STANDARD_LICENSE, "true")))
          .withIncludeAppendix(Boolean.parseBoolean(getMultiPartValue(formData, REPORT_FORM_APPENDIX, "true")))
          .withNoticeFiles(getNoticeFilesFromFormData(formData))
          .build();
    }
    catch (final Exception ex) { // if we got exception at this point it's because of invalid request
      Throwables.throwIfInstanceOf(ex, BadRequestException.class);
      throw new BadRequestException(ex.getMessage());
    }

    return applicationAttributionReportBuilder
        .generateCustomLegalApplicationAttributionReport(
            IdUtils.getOwnerNotNull(OwnerType.APPLICATION, applicationId),
            stageId,
            reportParametersBuilder.build());
  }

  @Override
  @POST
  @Path(APPLICATION_REPORT_FROM_TEMPLATE_PATH)
  @Produces(MediaType.TEXT_HTML)
  public String getLicenseLegalCustomApplicationHTMLReport(
      @PathParam("applicationId") String applicationId,
      @PathParam("stageId") String stageId,
      @PathParam("templateId") String templateId,
      @Context ContainerRequest request)
  {

    AttributionReportTemplateDTO templateDTO = attributionReportService.getAttributionReportTemplateById(templateId)
        .orElseThrow(() -> new NotFoundException(String.format("No template with id %s found", templateId)));

    List<String> noticeFiles = new ArrayList<>();
    if (request != null && request.getLength() > 0) {
      final FormDataMultiPart multiPart = request.readEntity(FormDataMultiPart.class);
      try {
        noticeFiles = getNoticeFilesFromFormData(multiPart);
      }
      catch (final Exception ex) { // if we got exception at this point it's because of invalid request
        Throwables.throwIfInstanceOf(ex, BadRequestException.class);
        throw new BadRequestException(ex.getMessage());
      }
    }

    return applicationAttributionReportBuilder.generateCustomLegalApplicationAttributionReport(
        IdUtils.getOwnerNotNull(OwnerType.APPLICATION, applicationId),
        stageId,
        LegalCustomReportParameters.builder().fromAttributionReportTemplateDTO(templateDTO)
            .withNoticeFiles(noticeFiles)
            .build());
  }

  private String requireMultiPartValue(final FormDataMultiPart formData, final String paramName) {
    return Optional.ofNullable(formData.getField(paramName))
        .map(FormDataBodyPart::getValue)
        .orElseThrow(() -> new BadRequestException("Missing required parameter: " + paramName));
  }

  private String getMultiPartValue(
      final FormDataMultiPart formData,
      final String paramName,
      final String defaultValue)
  {
    return Optional.ofNullable(formData.getField(paramName))
        .map(FormDataBodyPart::getValue)
        .orElse(defaultValue);
  }

  private List<String> getNoticeFilesFromFormData(final FormDataMultiPart formData) {
    final List<FormDataBodyPart> parts = Optional.ofNullable(formData.getFields(REPORT_FORM_NOTICE_FILES))
        .orElse(Collections.emptyList());
    if (parts.isEmpty()) {
      return Collections.emptyList();
    }
    final List<String> invalidMime = new ArrayList<>();
    final List<String> files = parts.stream()
        .filter(part -> {
          if (MediaTypes.typeEqual(MediaType.TEXT_PLAIN_TYPE, part.getMediaType())) {
            return true;
          }
          invalidMime.add(part.getContentDisposition().getFileName());
          return false;
        })
        .map(FormDataBodyPart::getValue)
        .collect(Collectors.toList());
    if (!invalidMime.isEmpty()) {
      throw new BadRequestException("Following notice files must be plain text files: "
          + String.join(", ", invalidMime));
    }
    return files;
  }

  @Override
  @GET
  @Path(COMPONENT_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public ApiLicenseLegalComponentReportDTO getLicenseLegalComponentReport(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("ownerId") String ownerId,
      @QueryParam("componentIdentifier") ComponentIdentifier componentIdentifier,
      @QueryParam("packageUrl") String packageUrl,
      @QueryParam("hash") String hash,
      @QueryParam("identificationSource") String identificationSource,
      @QueryParam("scanId") String scanId) throws IOException
  {
    return apiLicenseLegalServiceV2.getLicenseLegalComponentReport(ownerType, ownerId, componentIdentifier, packageUrl,
        hash, httpRequest, identificationSource, scanId);
  }
}
