/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.landing.UserInterfaceLinksHelper;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.organization.ApplicationHelper;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.utils.HttpHeaderUtils;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.ConflictException;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spdx.jacksonstore.MultiFormatStore;
import org.spdx.jacksonstore.MultiFormatStore.Format;
import org.spdx.jacksonstore.MultiFormatStore.Verbose;
import org.spdx.library.InvalidSPDXAnalysisException;
import org.spdx.library.model.SpdxCreatorInformation;
import org.spdx.library.model.SpdxDocument;

@Named
@Singleton
public class ApiSpdxService
{
  private static final Logger log = LoggerFactory.getLogger(ApiSpdxService.class);

  static final Set<String> SPDX_FORMATS = ImmutableSet.of("json", "xml");

  static final Set<String> SPDX_VERSIONS = ImmutableSet.of("2.3");

  private final ApplicationHelper applicationHelper;

  private final BaseUrl baseUrl;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final VersionService versionService;

  @Inject
  public ApiSpdxService(
      ApplicationHelper applicationHelper,
      BaseUrl baseUrl,
      PolicyEvaluationDAO policyEvaluationDAO,
      VersionService versionService)
  {
    this.applicationHelper = applicationHelper;
    this.baseUrl = baseUrl;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.versionService = versionService;
  }

  @Authorize(permission = Permission.READ)
  public Response getByScanId(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) String applicationId,
      String scanId,
      String format,
      boolean generateCycloneDx,
      String spdxVersion)
  {
    if (!SystemConfigurationPropertyFeature.SPDX_EXPORT.isEnabled()) {
      throw new ConflictException("This API endpoint is currently disabled.");
    }

    Application application = applicationHelper.getApplicationByIdNotNull(applicationId);

    return getByScanId(application, scanId, validateFormat(format), generateCycloneDx,
        validateSpdxVersion(spdxVersion));
  }

  @Authorize(permission = Permission.READ)
  public Response getLatestForStage(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) String applicationId,
      String stageId,
      String format,
      boolean generateCycloneDx,
      String spdxVersion)
  {
    if (!SystemConfigurationPropertyFeature.SPDX_EXPORT.isEnabled()) {
      throw new ConflictException("This API endpoint is currently disabled.");
    }

    Application application = applicationHelper.getApplicationByIdNotNull(applicationId);
    PolicyEvaluation evaluation = policyEvaluationDAO.getLastByApplicationIdAndStageId(application.getId(), stageId);
    if (evaluation == null) {
      throw new NotFoundException("Unable to locate a policy evaluation for " + applicationId + " in stage " + stageId);
    }

    return getByScanId(application, evaluation.getScanId(), validateFormat(format), generateCycloneDx,
        validateSpdxVersion(spdxVersion));
  }

  private Response getByScanId(
      Application application,
      String scanId,
      String format,
      boolean generateCycloneDx,
      String spdxVersion)
  {
    AuditData.get().setScanId(scanId);

    try {
      String uri = getReportUrl(application.getPublicId(), scanId);

      final SpdxDocument document = createDocument(spdxVersion, uri);

      return generateResponse(document, application, format, generateCycloneDx);
    }
    catch (IOException | InvalidSPDXAnalysisException e) {
      throw new RuntimeException("An error occurred while generating the SPDX file", e);
    }
  }

  private String getReportUrl(String applicationPublicId, String scanId) {
    String iqBaseUrl = "";
    try {
      iqBaseUrl = baseUrl.get();
    }
    catch (IllegalStateException e) {
      log.warn("IQ Server base URL is not configured", e);
    }
    return iqBaseUrl + UserInterfaceLinksHelper.getReportUrl(applicationPublicId, scanId);
  }

  private SpdxDocument createDocument(String spdxVersion, String uri) throws InvalidSPDXAnalysisException {
    SpdxDocument spdxDocument = new SpdxDocument(uri);
    spdxDocument.setSpecVersion("SPDX-" + spdxVersion);

    SpdxCreatorInformation creatorInfo = new SpdxCreatorInformation();
    DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd")
        .appendLiteral('T')
        .appendPattern("HH:mm:ss")
        .appendLiteral('Z');
    String date = LocalDateTime.now(ZoneOffset.UTC).format(builder.toFormatter());

    creatorInfo.setCreated(date);
    creatorInfo.getCreators().add("Tool: Sonatype IQ Server - " + versionService.getFullVersion());
    spdxDocument.setCreationInfo(creatorInfo);

    return spdxDocument;
  }

  private Response generateResponse(
      final SpdxDocument document,
      final Application application,
      final String format,
      final boolean generateCycloneDx) throws IOException, InvalidSPDXAnalysisException
  {
    String content;
    MediaType type;
    if (generateCycloneDx) {
      type = MediaType.APPLICATION_OCTET_STREAM_TYPE;
    }
    else {
      type = "json".equals(format) ? MediaType.APPLICATION_JSON_TYPE : MediaType.APPLICATION_XML_TYPE;
    }

    Format spdxFormat = "json".equals(format) ? Format.JSON_PRETTY : Format.XML;
    MultiFormatStore multiFormatStore = new MultiFormatStore(document.getModelStore(), spdxFormat, Verbose.STANDARD);
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      multiFormatStore.serialize(document.getDocumentUri(), out);
      content = out.toString("UTF-8");
    }

    return Response.ok(content, type)
        .header(HttpHeaders.CONTENT_DISPOSITION,
            HttpHeaderUtils.buildContentDispositionHeaderValue(
                application.getPublicId() + ".spdx." + type.getSubtype()))
        .build();
  }

  private String validateFormat(String format) {
    if (SPDX_FORMATS.contains(format)) {
      return format;
    }
    throw new BadRequestException("Invalid format: " + format + ". Supported formats: " + SPDX_FORMATS);
  }

  private String validateSpdxVersion(String spdxVersion) {
    if (SPDX_VERSIONS.contains(spdxVersion)) {
      return spdxVersion;
    }
    throw new BadRequestException(
        "Invalid SPDX version: " + spdxVersion + ". Supported SPDX versions: " + SPDX_VERSIONS);
  }
}
