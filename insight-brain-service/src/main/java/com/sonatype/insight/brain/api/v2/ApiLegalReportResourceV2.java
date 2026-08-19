/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.experimental.legal.AttributionReportService;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalApplicationReportDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalComponentReportDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.AttributionReportApplicationDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.AttributionReportTemplateDTO;
import com.sonatype.insight.brain.api.v2.service.legal.ApiLicenseLegalService;
import com.sonatype.insight.brain.api.v2.service.legal.report.ApplicationAttributionReportBuilder;
import com.sonatype.insight.brain.api.v2.service.legal.report.LegalCustomReportParameters;
import com.sonatype.insight.brain.api.v2.service.legal.report.LegalCustomReportParameters.Builder;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.io.FileUtils;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.message.internal.MediaTypes;
import org.glassfish.jersey.server.ContainerRequest;

@Named
@Timed
@Path(PublicApiPaths.LICENSE_LEGAL_RESOURCE_PATH_V2)
@Tag(name = "License Legal Metadata Report",
    description = "Use this REST API to retrieve license legal metadata in raw or HTML format.")
public class ApiLegalReportResourceV2
{
  public static final String APPLICATION_PATH = "application/{applicationId}";

  public static final String APPLICATION_PATH_STAGE = APPLICATION_PATH + "/stage/{stageId}";

  public static final String REPORT = "/report";

  public static final String FILTER = "/activeUserFilter";

  public static final String APPLICATION_REPORT_PATH = APPLICATION_PATH_STAGE + REPORT;

  public static final String MULTI_APPLICATION_REPORT_PATH = "multiApplication" + REPORT;

  public static final String MULTI_APPLICATION_REPORT_FROM_FILTER = "multiApplication" + FILTER + REPORT;

  public static final String MULTI_APPLICATION_REPORT_FROM_FILTER_TEMPLATE_PATH =
      MULTI_APPLICATION_REPORT_FROM_FILTER + "/templateId/{templateId}";

  public static final String CUSTOM_MULTI_APPLICATION_REPORT_PATH = "customMultiApplication" + REPORT;

  public static final String MULTI_APPLICATION_REPORT_PATH_FROM_TEMPLATE_PATH =
      MULTI_APPLICATION_REPORT_PATH + "/templateId/{templateId}";

  public static final String APPLICATION_REPORT_FROM_TEMPLATE_PATH =
      APPLICATION_REPORT_PATH + "/templateId/{templateId}";

  public static final String COMPONENT_PATH = "{ownerType: application|organization}/{ownerId}/component";

  public static final long MAX_REQUEST_SIZE = FileUtils.ONE_MB * 10; // 10MB

  public static final String MAX_REQUEST_SIZE_MESSAGE =
      "Request size must be smaller than " + (MAX_REQUEST_SIZE / FileUtils.ONE_MB) + " MB";

  static final String REPORT_FORM_TITLE = "title";

  static final String REPORT_FORM_HEADER = "header";

  static final String REPORT_FORM_FOOTER = "footer";

  static final String REPORT_FORM_TOC = "includeToc";

  static final String REPORT_FORM_STANDARD_LICENSE = "includeStandardLicenseTexts";

  static final String REPORT_FORM_SONATYPE_SPECIAL_LICENSES = "includeSonatypeSpecialLicenses";

  static final String REPORT_FORM_APPENDIX = "includeAppendix";

  static final String REPORT_FORM_NOTICE_FILES = "noticeFiles";

  static final String REPORT_FORM_INNER_SOURCE = "includeInnerSource";

  static final String FORM_DATA_APPLICATIONS = "applications";

  static final String FORM_DATA_STAGES = "stages";

  static final String DEFAULT_VALUE_FALSE = "false";

  private final ApiLicenseLegalService apiLicenseLegalServiceV2;

  private final AttributionReportService attributionReportService;

  private final ApplicationAttributionReportBuilder applicationAttributionReportBuilder;

  private final IdUtils idUtils;

  @Inject
  public ApiLegalReportResourceV2(
      final ApiLicenseLegalService apiLicenseLegalService,
      final AttributionReportService attributionReportService,
      final ApplicationAttributionReportBuilder applicationAttributionReportBuilder,
      final IdUtils idUtils)
  {
    this.apiLicenseLegalServiceV2 = apiLicenseLegalService;
    this.attributionReportService = attributionReportService;
    this.applicationAttributionReportBuilder = applicationAttributionReportBuilder;
    this.idUtils = idUtils;
  }

  @GET
  @Path(APPLICATION_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this REST API to retrieve the raw license legal data for components in " +
      "an application." +
      "\n" +
      "\n" +
      "Permissions required: Review Legal Obligations For Components Licenses",
      responses = {
        @ApiResponse(responseCode = "200",
            description = "The response contains a list of all components in the application and the corresponding " +
                "license legal metadata. For each component, the response includes component data and " +
                "license legal metadata." +
                "\n" +
                "\n" +
                "1. The component data includes:" +
                "<ul>" +
                "<li>`packageURL` is the package URL or pURL of the component.</li>" +
                "<li>`hash` is the truncated hash value and can be used in other REST API calls. It should " +
                "not be used as a checksum.</li>" +
                "<li>`componentIdentifier` includes the component format and its coordinates.</li>" +
                "<li>`displayName` is the display name of the component.</li>" +
                "<li>`licenseLegalData` contains the legal data.</li>" +
                "<li>`stageScans` is a list and each element contains `stageName` at which the " +
                "application was scanned, the `scanId` of the application scan, and the `scanDate`.</li>" +
                "</ul>" +
                "2. The `licenseLegalMetaData` is used as a dictionary for legal data and for each license " +
                "contains the license id(s), name, license text, obligations, license threat group, and whether " +
                "or not it is multi license.",
            useReturnTypeSchema = true)
      })
  public ApiLicenseLegalApplicationReportDTO getLicenseLegalApplicationReport(
      @Parameter(description = "Enter the application id or public id.",
          required = true) @PathParam("applicationId") String applicationId)
  {
    return apiLicenseLegalServiceV2
        .getLicenseLegalApplicationReport(idUtils.getOwnerNotNull(OwnerType.APPLICATION, applicationId));
  }

  @GET
  @Path(APPLICATION_PATH_STAGE)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to retrieve the raw license legal data for components in an application " +
      "based on the application scan at a specific stage." +
      "\n" +
      "\n" +
      "Permissions required: Review Legal Obligations For Components Licenses ",
      responses = {
        @ApiResponse(responseCode = "200",
            description = "The response contains a list of all components in the application. " +
                "For each component, the response includes component data and license legal metadata." +
                "\n" +
                "\n" +
                "1. The component data includes:" +
                "<ul>" +
                "<li>`packageURL` is the package URL or pURL of the component.</li>" +
                "<li>`hash` is the truncated hash value and can be used in other REST API calls. It should " +
                "not be used as a checksum.</li>" +
                "<li>`componentIdentifier` includes the component format and its coordinates.</li>" +
                "<li>`displayName` is the display name of the component.</li>" +
                "<li>`licenseLegalData` contains the legal data.</li>" +
                "<li>`stageScans` is a list and each element contains `stageName` at which the " +
                "application was scanned, the `scanId` of the application scan, and the `scanDate`.</li>" +
                "</ul>" +
                "2. The `licenseLegalMetaData` is used as a dictionary for legal data and for each license " +
                "contains the license id(s), name, license text, obligations, license threat group, and whether " +
                "or not it is multi license.",
            useReturnTypeSchema = true)
      })
  public ApiLicenseLegalApplicationReportDTO getLicenseLegalApplicationReport(
      @Parameter(description = "Enter the application id or public id.",
          required = true) @PathParam("applicationId") String applicationId,
      @Parameter(description = "Enter the stageId.", required = true) @PathParam("stageId") String stageId)
  {
    return apiLicenseLegalServiceV2.getLicenseLegalApplicationReport(
        idUtils.getOwnerNotNull(OwnerType.APPLICATION, applicationId), stageId, false,
        false);
  }

  @GET
  @Path(APPLICATION_REPORT_PATH)
  @Produces(MediaType.TEXT_HTML)
  @Operation(description = "Use this method to retrieve the license legal data for components in an application " +
      "at a specific stage, in HTML format." +
      "\n" +
      "\n" +
      "Permissions required: Review Legal Obligations For Components Licenses",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "The response contains license legal data in HTML format.",
            useReturnTypeSchema = true)
      })
  public String getLicenseLegalApplicationHTMLReport(
      @Parameter(description = "Enter the application id or public id.",
          required = true) @PathParam("applicationId") String applicationId,
      @Parameter(description = "Enter the stageId.") @PathParam("stageId") String stageId)
  {
    final Owner app = idUtils.getOwnerNotNull(OwnerType.APPLICATION, applicationId);
    final LegalCustomReportParameters reportParameters =
        LegalCustomReportParameters.builder().buildWithDefaults(app.getPublicId());

    return applicationAttributionReportBuilder
        .generateCustomLegalApplicationAttributionReport(app, stageId, reportParameters);
  }

  @POST
  @Path(APPLICATION_REPORT_PATH)
  @Produces(MediaType.TEXT_HTML)
  @Operation(description = "Use this method to retrieve and customize the license legal data for components " +
      "in an application at a specific stage, in HTML format." +
      "\n" +
      "\n" +
      "Permissions required: Review Legal Obligations For Components Licenses",
      responses = {
        @ApiResponse(responseCode = "200",
            description = "The response contains the customized license legal report in HTML format.",
            useReturnTypeSchema = true)
      })
  public String getLicenseLegalCustomApplicationHTMLReport(
      @Parameter(description = "Enter the application id or public id.",
          required = true) @PathParam("applicationId") String applicationId,
      @Parameter(description = "Enter the stageId.") @PathParam("stageId") String stageId,
      @Context ContainerRequest request) throws IOException
  {
    validateRequestSize(request);
    FormDataMultiPart formData = request.readEntity(FormDataMultiPart.class);

    final Builder reportParametersBuilder = LegalCustomReportParameters.builder();
    reportParametersBuilder.withTitle(requireMultiPartValue(formData, REPORT_FORM_TITLE))
        .withHeader(getMultiPartValue(formData, REPORT_FORM_HEADER, ""))
        .withFooter(getMultiPartValue(formData, REPORT_FORM_FOOTER, ""))
        .withIncludeToc(Boolean.parseBoolean(getMultiPartValue(formData, REPORT_FORM_TOC, "true")))
        .withIncludeStandardLicenseTexts(
            Boolean.parseBoolean(getMultiPartValue(formData, REPORT_FORM_STANDARD_LICENSE, "true")))
        .withIncludeAppendix(Boolean.parseBoolean(getMultiPartValue(formData, REPORT_FORM_APPENDIX, "true")))
        .withIncludeIncludeSonatypeSpecialLicenses(Boolean
            .parseBoolean(getMultiPartValue(formData, REPORT_FORM_SONATYPE_SPECIAL_LICENSES, DEFAULT_VALUE_FALSE)))
        .withNoticeFiles(getNoticeFilesFromFormData(formData))
        .withIncludeInnerSource(
            Boolean.parseBoolean(getMultiPartValue(formData, REPORT_FORM_INNER_SOURCE, DEFAULT_VALUE_FALSE)));

    return applicationAttributionReportBuilder
        .generateCustomLegalApplicationAttributionReport(
            idUtils.getOwnerNotNull(OwnerType.APPLICATION, applicationId),
            stageId,
            reportParametersBuilder.build());
  }

  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Path(CUSTOM_MULTI_APPLICATION_REPORT_PATH)
  @Produces(MediaType.TEXT_HTML)
  @Operation(description = "Use this method to generate license legal data in HTML format for all applications." +
      "\n" +
      "\n" +
      "Permissions required: Review Legal Obligations For Components Licenses",
      responses = {
        @ApiResponse(responseCode = "200",
            description = "The response contains license legal data in HTML format.",
            useReturnTypeSchema = true)
      })
  public String getLicenseLegalCustomMultiApplicationHTMLReport(
      @Context ContainerRequest request) throws IOException
  {
    validateRequestSize(request);
    FormDataMultiPart formData = request.readEntity(FormDataMultiPart.class);

    Set<AttributionReportApplicationDTO> applicationsAndStages;
    final Builder reportParametersBuilder = LegalCustomReportParameters.builder();
    reportParametersBuilder.withTitle(requireMultiPartValue(formData, REPORT_FORM_TITLE))
        .withHeader(getMultiPartValue(formData, REPORT_FORM_HEADER, ""))
        .withFooter(getMultiPartValue(formData, REPORT_FORM_FOOTER, ""))
        .withIncludeToc(Boolean.parseBoolean(getMultiPartValue(formData, REPORT_FORM_TOC, "true")))
        .withIncludeStandardLicenseTexts(
            Boolean.parseBoolean(getMultiPartValue(formData, REPORT_FORM_STANDARD_LICENSE, "true")))
        .withIncludeAppendix(Boolean.parseBoolean(getMultiPartValue(formData, REPORT_FORM_APPENDIX, "true")))
        .withNoticeFiles(getNoticeFilesFromFormData(formData))
        .withIncludeInnerSource(
            Boolean.parseBoolean(getMultiPartValue(formData, REPORT_FORM_INNER_SOURCE, DEFAULT_VALUE_FALSE)))
        .withIncludeIncludeSonatypeSpecialLicenses(Boolean
            .parseBoolean(getMultiPartValue(formData, REPORT_FORM_SONATYPE_SPECIAL_LICENSES, DEFAULT_VALUE_FALSE)));
    applicationsAndStages = getApplicationsAndStagesFromFormData(formData);
    return applicationAttributionReportBuilder
        .generateCustomLegalMultiApplicationAttributionReport(applicationsAndStages, reportParametersBuilder.build());
  }

  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Path(MULTI_APPLICATION_REPORT_FROM_FILTER_TEMPLATE_PATH)
  @Produces(MediaType.TEXT_HTML)
  @Operation(description = "Use this method to generate license legal data in HTML format for applications ," +
      "on which the logged in user has permissions." +
      "\n" +
      "\n" +
      "Permission required: Review Legal Obligations For Components Licenses",
      responses = {
        @ApiResponse(responseCode = "200",
            description = "The response contains license legal data in HTML format based on the specified template.",
            useReturnTypeSchema = true)
      })
  public String getLicenseLegalMultiApplicationReportFromActiveUserFilter(
      @Parameter(description = "Enter the templateId for the license legal data.",
          required = true) @PathParam("templateId") String templateId,
      @Context ContainerRequest request) throws IOException
  {
    validateRequestSize(request);

    AttributionReportTemplateDTO templateDTO =
        attributionReportService.getAttributionReportTemplateById_NoAuthz(templateId)
            .orElseThrow(() -> new NotFoundException(String.format("No template with id %s found", templateId)));
    List<String> noticeFiles = new ArrayList<>();
    if (request != null && request.getLength() > 0) {
      FormDataMultiPart multiPart = request.readEntity(FormDataMultiPart.class);
      noticeFiles = getNoticeFilesFromFormData(multiPart);
    }
    LegalCustomReportParameters reportParameters = LegalCustomReportParameters.builder()
        .fromAttributionReportTemplateDTO(templateDTO)
        .withNoticeFiles(noticeFiles)
        .build();
    return applicationAttributionReportBuilder
        .generateLegalMultiApplicationAttributionReportFromActiveUserFilter(reportParameters);
  }

  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Path(MULTI_APPLICATION_REPORT_PATH)
  @Produces(MediaType.TEXT_HTML)
  @Operation(description = "Use this method to generate license legal data in HTML format." +
      "\n" +
      "\n" +
      "Permissions required: Review Legal Obligations For Components Licenses",
      responses = {
        @ApiResponse(responseCode = "200",
            description = "License legal data in HTML format is generated.")
      })
  public String getLicenseLegalMultiApplicationHTMLReport(
      @Context ContainerRequest request) throws IOException
  {
    validateRequestSize(request);

    List<String> noticeFilesList = new ArrayList<>();
    Set<AttributionReportApplicationDTO> applicationsAndStagesSet = new HashSet<>();
    if (request != null && request.getLength() > 0) {
      final FormDataMultiPart multiPart = request.readEntity(FormDataMultiPart.class);
      noticeFilesList = getNoticeFilesFromFormData(multiPart);
      applicationsAndStagesSet = getApplicationsAndStagesFromFormData(multiPart);
    }
    LegalCustomReportParameters reportParameters =
        LegalCustomReportParameters.builder()
            .withNoticeFiles(noticeFilesList)
            .buildMultiApplicationWithDefaults(
                applicationsAndStagesSet.stream().map(n -> n.applicationPublicId).collect(Collectors.toSet()));
    return applicationAttributionReportBuilder
        .generateCustomLegalMultiApplicationAttributionReport(applicationsAndStagesSet, reportParameters);
  }

  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Path(MULTI_APPLICATION_REPORT_PATH_FROM_TEMPLATE_PATH)
  @Produces(MediaType.TEXT_HTML)
  @Operation(description = "Use this method to generate license legal data for all applications in HTML format " +
      "based on the given template." +
      "\n" +
      "\n" +
      "Permissions required: Review Legal Obligations For Components Licenses",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "The response contains license legal data in HTML format.")
      })
  public String getLicenseLegalCustomMultiApplicationHTMLReport(
      @Parameter(description = "Enter the `templateId` for the HTML report.",
          required = true) @PathParam("templateId") String templateId,
      @Context ContainerRequest request) throws IOException
  {
    validateRequestSize(request);

    AttributionReportTemplateDTO templateDTO =
        attributionReportService.getAttributionReportTemplateById_NoAuthz(templateId)
            .orElseThrow(() -> new NotFoundException(String.format("No template with id %s found", templateId)));
    List<String> noticeFiles = new ArrayList<>();
    Set<AttributionReportApplicationDTO> applicationsAndStages = new HashSet<>();
    if (request != null && request.getLength() > 0) {
      final FormDataMultiPart multiPart = request.readEntity(FormDataMultiPart.class);
      noticeFiles = getNoticeFilesFromFormData(multiPart);
      applicationsAndStages = getApplicationsAndStagesFromFormData(multiPart);
    }
    return applicationAttributionReportBuilder
        .generateCustomLegalMultiApplicationAttributionReport(applicationsAndStages, LegalCustomReportParameters
            .builder()
            .fromAttributionReportTemplateDTO(templateDTO)
            .withNoticeFiles(noticeFiles)
            .build());
  }

  @POST
  @Path(APPLICATION_REPORT_FROM_TEMPLATE_PATH)
  @Produces(MediaType.TEXT_HTML)
  @Operation(description = "Use this method to generate a license legal report in the specified HTML template " +
      "format." +
      "\n" +
      "\n" +
      "Permissions required: Review Legal Obligations For Components Licenses",
      responses = {
        @ApiResponse(responseCode = "200",
            description = "The response contains the license legal report for the specified application in" +
                " HTML format.")
      })
  public String getLicenseLegalCustomApplicationHTMLReport(
      @Parameter(description = "Enter the application id or public id.",
          required = true) @PathParam("applicationId") String applicationId,
      @Parameter(description = "Enter the stageId.", required = true) @PathParam("stageId") String stageId,
      @Parameter(description = "Enter the templateId for the HTML report format.",
          required = true) @PathParam("templateId") String templateId,
      @Context ContainerRequest request) throws IOException
  {
    validateRequestSize(request);

    AttributionReportTemplateDTO templateDTO =
        attributionReportService.getAttributionReportTemplateById_NoAuthz(templateId)
            .orElseThrow(() -> new NotFoundException(String.format("No template with id %s found", templateId)));

    List<String> noticeFiles = new ArrayList<>();
    if (request != null && request.getLength() > 0) {
      final FormDataMultiPart multiPart = request.readEntity(FormDataMultiPart.class);
      noticeFiles = getNoticeFilesFromFormData(multiPart);
    }

    return applicationAttributionReportBuilder.generateCustomLegalApplicationAttributionReport(
        idUtils.getOwnerNotNull(OwnerType.APPLICATION, applicationId),
        stageId,
        LegalCustomReportParameters.builder()
            .fromAttributionReportTemplateDTO(templateDTO)
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
        .toList();
    if (!invalidMime.isEmpty()) {
      throw new BadRequestException("Following notice files must be plain text files: "
          + String.join(", ", invalidMime));
    }
    return files;
  }

  private Set<AttributionReportApplicationDTO> getApplicationsAndStagesFromFormData(
      final FormDataMultiPart formData)
  {
    final List<FormDataBodyPart> applications =
        Optional.ofNullable(formData.getFields(FORM_DATA_APPLICATIONS)).orElse(Collections.emptyList());
    final List<FormDataBodyPart> stages =
        Optional.ofNullable(formData.getFields(FORM_DATA_STAGES)).orElse(Collections.emptyList());
    if (applications.isEmpty() || stages.isEmpty()) {
      return Collections.emptySet();
    }

    Set<String> applicationPublicIds = applications.stream()
        .map(FormDataBodyPart::getValue)
        .map(app -> app.split(","))
        .flatMap(Arrays::stream)
        .collect(Collectors.toSet());

    Set<String> stageIds = stages.stream()
        .map(FormDataBodyPart::getValue)
        .map(app -> app.split(","))
        .flatMap(Arrays::stream)
        .collect(Collectors.toSet());

    return resourceDTOFromSet(applicationPublicIds, stageIds);
  }

  private Set<AttributionReportApplicationDTO> resourceDTOFromSet(
      Set<String> applicationPublicIds,
      Set<String> stageIds)
  {
    Set<AttributionReportApplicationDTO> legalReportResourceApplicationDTO = new HashSet<>();
    applicationPublicIds.forEach(applicationPublicId -> stageIds.forEach(
        stageId -> legalReportResourceApplicationDTO
            .add(new AttributionReportApplicationDTO(null, applicationPublicId, stageId))));
    return legalReportResourceApplicationDTO;
  }

  @GET
  @Path(COMPONENT_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to retrieve the raw license legal data for a component by specifying the " +
      "component identifier or package URL or the component hash." +
      "\n" +
      "\n" +
      "Permissions required: Review Legal Obligations For Components Licenses",
      responses = {
        @ApiResponse(responseCode = "200",
            description = "The response contains the requested component data and the corresponding " +
                "license legal metadata." +
                "\n" +
                "\n" +
                "1. The component data includes:" +
                "<ul>" +
                "<li>`packageURL` is the package URL or pURL of the component.</li>" +
                "<li>`hash` is the truncated hash value and can be used in other REST API calls. It should " +
                "not be used as a checksum.</li>" +
                "<li>`componentIdentifier` includes the component format and its coordinates.</li>" +
                "<li>`displayName` is the display name of the component.</li>" +
                "<li>`licenseLegalData` contains the legal data.</li>" +
                "<li>`stageScans` is a list and each element contains `stageName` at which the " +
                "application was scanned, the `scanId` of the application scan, and the `scanDate`.</li>" +
                "</ul>" +
                "2. The `licenseLegalMetaData` is used as a dictionary for legal data and for each license " +
                "contains the license id(s), name, license text, obligations, license threat group, and whether " +
                "or not it is multi license.",
            useReturnTypeSchema = true)
      })
  public ApiLicenseLegalComponentReportDTO getLicenseLegalComponentReport(
      @Parameter(description = "Enter the ownerType", required = true) @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "Enter the ownerId corresponding to the ownerType.",
          required = true) @PathParam("ownerId") String ownerId,
      @Parameter(
          description = "Enter the componentIdentifier consisting of format and coordinates.") @QueryParam("componentIdentifier") ComponentIdentifier componentIdentifier,
      @Parameter(description = "Enter the package URL.") @QueryParam("packageUrl") String packageUrl,
      @Parameter(description = "Enter the component hash.") @QueryParam("hash") String hash,
      @Parameter(
          description = "Enter the identification source if a third party scan is used.") @QueryParam("identificationSource") String identificationSource,
      @Parameter(description = "Enter the scanId for the report where the component was identified (required if " +
          "identified by third party scan).") @QueryParam("scanId") String scanId) throws IOException
  {
    return apiLicenseLegalServiceV2.getLicenseLegalComponentReport(ownerType, ownerId, componentIdentifier, packageUrl,
        hash, identificationSource, scanId);
  }

  private void validateRequestSize(ContainerRequest request) throws IOException {
    // Fail fast evaluating the Content-Length header
    if (request.getLength() > MAX_REQUEST_SIZE) {
      throw new BadRequestException(MAX_REQUEST_SIZE_MESSAGE);
    }
    else if (request.getLength() != -1) { // -1 means the Content-Length header is not present in the request
      return;
    }

    // If the above validations succeed, validate the entity stream
    ByteArrayInputStream validatedRequest = validateRequestStreamSize(request.getEntityStream());
    request.setEntityStream(validatedRequest);
  }

  private ByteArrayInputStream validateRequestStreamSize(InputStream stream) throws IOException {
    if (stream == null) {
      throw new BadRequestException("Request stream is null");
    }

    int b;
    byte[] requestData = new byte[8192];
    byte[] requestByteArray;

    try (ByteArrayOutputStream requestOutputStream = new ByteArrayOutputStream();
        BufferedInputStream bufferedInputStream = new BufferedInputStream(stream))
    {
      while ((b = bufferedInputStream.readNBytes(requestData, 0, requestData.length)) != 0) {
        requestOutputStream.write(requestData, 0, b);
        if (requestOutputStream.size() > MAX_REQUEST_SIZE) {
          throw new BadRequestException(MAX_REQUEST_SIZE_MESSAGE);
        }
      }
      requestOutputStream.flush();
      requestByteArray = requestOutputStream.toByteArray();
    }

    // Returning a new stream, since we read and consume the original one in the validation process
    return new ByteArrayInputStream(requestByteArray);
  }
}
